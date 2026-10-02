package jp.feato.horsemanship.listener;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import jp.feato.horsemanship.integration.valhalla.ValhallaAdapter;
import jp.feato.horsemanship.service.ExclusivePerkService;
import jp.feato.horsemanship.service.MountedEffectService;
import jp.feato.horsemanship.service.Mounts;
import jp.feato.horsemanship.state.RiderState;
import jp.feato.horsemanship.state.RiderStateStore;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class MountedCombatListener implements Listener {
    private final HorsemanshipConfig config;
    private final ValhallaAdapter valhalla;
    private final ExclusivePerkService perks;
    private final RiderStateStore states;
    private final Mounts mounts;
    private final MountedEffectService effects;
    public MountedCombatListener(HorsemanshipConfig config, ValhallaAdapter valhalla, ExclusivePerkService perks,
                                 RiderStateStore states, Mounts mounts, MountedEffectService effects) {
        this.config = config; this.valhalla = valhalla; this.perks = perks; this.states = states; this.mounts = mounts; this.effects = effects;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        Player attacker = attacker(event.getDamager());
        if (attacker == null || mounts.eligibleMount(attacker) == null || event.getFinalDamage() <= 0) return;
        boolean melee = event.getDamager() == attacker;
        if (melee) {
            double bonus = 0;
            if (perks.has(attacker, "cavalier")) bonus += config.number("combat.cavalier-melee-bonus");
            if (perks.has(attacker, "veteran_cavalry")) bonus += config.number("combat.veteran-cavalry-melee-bonus");
            RiderState state = states.peek(attacker.getUniqueId());
            long now = System.currentTimeMillis();
            if (state != null && !state.firstImpactUsed && perks.has(attacker, "first_impact") &&
                effects.active(state, now) && horizontalSpeed(mounts.eligibleMount(attacker)) >= config.number("combat.first-impact.minimum-speed")) {
                bonus += config.number("combat.first-impact.damage-bonus");
            }
            if (bonus > 0) event.setDamage(event.getDamage() * (1.0 + bonus));
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Entity target = event.getEntity();
        Player rider = null;
        boolean horse = false;
        if (target instanceof Player player && mounts.eligibleMount(player) != null) rider = player;
        else if (config.eligible(target.getType())) {
            rider = Mounts.driver(target.getPassengers());
            horse = true;
        }
        if (rider == null || !mounts.isDriver(rider)) return;
        RiderState state = states.peek(rider.getUniqueId());
        long now = System.currentTimeMillis();
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            double reduction = 0;
            if (perks.has(rider, "rein_sense")) reduction += config.number("technical.rein-sense-fall-reduction");
            if (perks.has(rider, "good_hands")) reduction += config.number("technical.good-hands-fall-reduction");
            if (perks.has(rider, "steady_hands")) reduction += config.number("technical.steady-hands-fall-reduction");
            if (perks.has(rider, "sure_landing")) reduction += config.number("technical.sure-landing-fall-reduction");
            event.setDamage(event.getDamage() * (1.0 - Math.min(0.9, reduction)));
        }
        if (state != null && effects.active(state, now) && perks.has(rider, "charge")) {
            double reduction = config.number(horse ? "combat.charge-horse-damage-reduction" : "combat.charge-rider-damage-reduction");
            if (perks.has(rider, "heavy_cavalry")) reduction += config.number("combat.heavy-charge-extra-reduction");
            if (perks.has(rider, "iron_vanguard")) reduction += config.number("combat.iron-vanguard-charge-extra-reduction");
            event.setDamage(event.getDamage() * (1.0 - Math.min(0.9, reduction)));
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFirstImpact(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || event.getFinalDamage() <= 0) return;
        RiderState state = states.peek(attacker.getUniqueId());
        long now = System.currentTimeMillis();
        Entity mount = mounts.eligibleMount(attacker);
        if (state == null || state.firstImpactUsed || mount == null || !effects.active(state, now) ||
            !perks.has(attacker, "first_impact") || horizontalSpeed(mount) < config.number("combat.first-impact.minimum-speed")) return;
        state.firstImpactUsed = true;
        double strength = config.number("combat.first-impact.knockback-bonus");
        if (perks.has(attacker, "iron_vanguard")) strength += config.number("combat.first-impact.iron-vanguard-extra-knockback");
        var direction = event.getEntity().getLocation().toVector().subtract(attacker.getLocation().toVector()).setY(0);
        if (direction.lengthSquared() > 0.0001 && strength > 0)
            event.getEntity().setVelocity(event.getEntity().getVelocity().add(direction.normalize().multiply(strength)));
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExperience(EntityDamageByEntityEvent event) {
        if (!config.flag("experience.mounted-combat.enabled") || event.getFinalDamage() <= 0) return;
        Player player = attacker(event.getDamager());
        if (player == null || mounts.eligibleMount(player) == null || player == event.getEntity()) return;
        RiderState state = states.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (now - state.lastCombatExperience < config.number("experience.mounted-combat.cooldown-millis")) return;
        state.lastCombatExperience = now;
        valhalla.addExperience(player, config.number("experience.mounted-combat.exp-per-hit"));
    }
    private static double horizontalSpeed(Entity entity) {
        var velocity = entity.getVelocity();
        return Math.hypot(velocity.getX(), velocity.getZ());
    }
    private Player attacker(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) return player;
        }
        return null;
    }
}
