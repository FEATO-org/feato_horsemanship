package jp.feato.horsemanship.service;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import jp.feato.horsemanship.integration.valhalla.ValhallaAdapter;
import jp.feato.horsemanship.state.RiderState;
import jp.feato.horsemanship.state.RiderStateStore;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.UUID;

public final class MountedEffectService {
    private final NamespacedKey speedKey;
    private final NamespacedKey resistanceKey;
    private final NamespacedKey riderResistanceKey;
    private final HorsemanshipConfig config;
    private final ValhallaAdapter valhalla;
    private final ExclusivePerkService perks;
    private final RiderStateStore states;
    private final Mounts mounts;

    public MountedEffectService(Plugin plugin, HorsemanshipConfig config, ValhallaAdapter valhalla,
                                ExclusivePerkService perks, RiderStateStore states, Mounts mounts) {
        this.speedKey = new NamespacedKey(plugin, "mounted_speed");
        this.resistanceKey = new NamespacedKey(plugin, "mounted_knockback_resistance");
        this.riderResistanceKey = new NamespacedKey(plugin, "rider_knockback_resistance");
        this.config = config; this.valhalla = valhalla; this.perks = perks; this.states = states; this.mounts = mounts;
    }
    public void tick(long now) {
        for (Player rider : Bukkit.getOnlinePlayers()) {
            RiderState state = states.get(rider.getUniqueId());
            Entity mount = mounts.eligibleMount(rider);
            if (mount == null || !mounts.isDriver(rider)) {
                if (state.mount != null) clearMount(state);
                remove(rider, Attribute.KNOCKBACK_RESISTANCE, riderResistanceKey);
                continue;
            }
            if (!mount.getUniqueId().equals(state.mount)) {
                clearMount(state);
                state.mount = mount.getUniqueId();
                state.mountedSince = now;
            }
            double bonus = speedBonus(rider, state, now);
            setModifier(mount, Attribute.MOVEMENT_SPEED, speedKey, bonus);
            double resistance = perks.has(rider, "cavalier") ? config.number("combat.cavalier-knockback-resistance") : 0;
            if (active(state, now) && perks.has(rider, "charge")) resistance += config.number("combat.charge-knockback-resistance");
            setModifier(mount, Attribute.KNOCKBACK_RESISTANCE, resistanceKey, resistance);
            setModifier(rider, Attribute.KNOCKBACK_RESISTANCE, riderResistanceKey, resistance);
        }
    }
    private double speedBonus(Player rider, RiderState state, long now) {
        double speed = config.number("speed.initial-penalty");
        if (perks.has(rider, "rein_sense")) speed = 0;
        else if (perks.has(rider, "first_saddle")) speed = config.number("speed.first-saddle-penalty");
        if (perks.has(rider, "fleetfoot")) speed += config.number("speed.fleetfoot");
        if (perks.has(rider, "full_gallop")) speed += config.number("speed.full-gallop");
        long riding = now - state.mountedSince;
        double cruiseFactor = perks.has(rider, "endless_road") ? config.number("handling.endless-road-cruise-time-factor") : 1.0;
        if (perks.has(rider, "long_haul")) {
            if (riding >= config.number("speed.cruise-first-threshold-seconds") * 1000 * cruiseFactor) speed += config.number("speed.cruise-30-seconds");
            if (riding >= config.number("speed.cruise-second-threshold-seconds") * 1000 * cruiseFactor) speed += config.number("speed.cruise-60-seconds");
        }
        if (perks.has(rider, "steady_pace") && riding >= config.number("speed.cruise-third-threshold-seconds") * 1000 * cruiseFactor) speed += config.number("speed.cruise-120-seconds");
        if (active(state, now)) {
            double urgeBonus = state.lastUrgeBonus;
            if (perks.has(rider, "mutual_trust") || perks.has(rider, "full_gallop") || perks.has(rider, "windborne")) {
                double fadeMillis = Math.max(1, config.number("handling.urge-end-fade-seconds") * 1000);
                urgeBonus *= Math.min(1, Math.max(0, (state.urgeEnd - now) / fadeMillis));
            }
            speed += urgeBonus;
        }
        else if (now < state.oneAsOneEnd) speed += state.lastUrgeBonus * config.number("one-as-one.retained-urge-bonus-fraction");
        if (now < state.fatigueEnd) {
            double penalty = config.number("fatigue.speed-penalty");
            if (perks.has(rider, "long_haul")) penalty += config.number("fatigue.penalty-reduction.long-haul");
            if (perks.has(rider, "steady_pace")) penalty += config.number("fatigue.penalty-reduction.steady-pace");
            speed += Math.min(0, penalty);
        }
        return speed;
    }
    public boolean active(RiderState state, long now) { return state != null && state.urgeActive > 0 && now >= state.urgeActive && now < state.urgeEnd; }
    private static void setModifier(Entity entity, Attribute attribute, NamespacedKey key, double amount) {
        if (!(entity instanceof LivingEntity living)) return;
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(key);
        if (existing != null && Math.abs(existing.getAmount() - amount) < 0.000001) return;
        if (existing != null) instance.removeModifier(key);
        if (Math.abs(amount) > 0.000001)
            instance.addTransientModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }
    public void clearMount(RiderState state) {
        UUID id = state.mount;
        if (id != null) {
            Entity entity = Bukkit.getEntity(id);
            if (entity instanceof LivingEntity living) {
                remove(living, Attribute.MOVEMENT_SPEED, speedKey);
                remove(living, Attribute.KNOCKBACK_RESISTANCE, resistanceKey);
            }
        }
        state.clearMount();
    }
    private static void remove(LivingEntity entity, Attribute attribute, NamespacedKey key) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.getModifier(key) != null) instance.removeModifier(key);
    }
    public void clearRider(Player rider) { remove(rider, Attribute.KNOCKBACK_RESISTANCE, riderResistanceKey); }
    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            RiderState state = states.peek(player.getUniqueId());
            if (state != null) clearMount(state);
            clearRider(player);
        }
    }
}
