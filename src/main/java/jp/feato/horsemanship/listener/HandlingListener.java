package jp.feato.horsemanship.listener;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import jp.feato.horsemanship.service.ExclusivePerkService;
import jp.feato.horsemanship.service.MountedEffectService;
import jp.feato.horsemanship.service.Mounts;
import jp.feato.horsemanship.state.RiderState;
import jp.feato.horsemanship.state.RiderStateStore;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

public final class HandlingListener implements Listener {
    private final HorsemanshipConfig config;
    private final ExclusivePerkService perks;
    private final RiderStateStore states;
    private final Mounts mounts;
    private final MountedEffectService effects;
    public HandlingListener(HorsemanshipConfig config, ExclusivePerkService perks, RiderStateStore states, Mounts mounts, MountedEffectService effects) {
        this.config = config; this.perks = perks; this.states = states; this.mounts = mounts; this.effects = effects;
    }
    @EventHandler
    public void onMove(VehicleMoveEvent event) {
        if (!config.flag("handling.enabled") || !(event.getVehicle() instanceof AbstractHorse horse)) return;
        Player rider = Mounts.driver(horse.getPassengers());
        if (rider == null || !mounts.isDriver(rider)) return;
        RiderState state = states.peek(rider.getUniqueId());
        if (state == null || !horse.getUniqueId().equals(state.mount)) return;
        Vector velocity = horse.getVelocity();
        double horizontal = Math.hypot(velocity.getX(), velocity.getZ());
        if (!Double.isFinite(horizontal) || !Double.isFinite(velocity.getY())) return;
        boolean grounded = horse.isOnGround();
        boolean changed = false;
        if (grounded) state.jumpBoostApplied = false;
        if (!grounded && !state.jumpBoostApplied && velocity.getY() > 0.08) {
            double horizontalBonus = 0, verticalBonus = 0;
            if (perks.has(rider, "surefooted")) {
                horizontalBonus += config.number("handling.normal-jump-horizontal.surefooted");
                verticalBonus += config.number("handling.normal-jump-vertical.surefooted");
            }
            if (perks.has(rider, "master_of_reins")) {
                horizontalBonus += config.number("handling.normal-jump-horizontal.master-of-reins");
                verticalBonus += config.number("handling.normal-jump-vertical.master-of-reins");
            }
            if (effects.active(state, System.currentTimeMillis()) && perks.has(rider, "over_the_fence")) {
                horizontalBonus += config.number("technical.over-the-fence-horizontal");
                verticalBonus += config.number("technical.over-the-fence-vertical");
            }
            if (horizontalBonus > 0 || verticalBonus > 0) {
                velocity.setX(velocity.getX() * (1 + horizontalBonus));
                velocity.setZ(velocity.getZ() * (1 + horizontalBonus));
                velocity.setY(velocity.getY() * (1 + verticalBonus));
                changed = true;
            }
            state.jumpBoostApplied = true;
        }
        if (effects.active(state, System.currentTimeMillis()) && perks.has(rider, "lightning_start") &&
            System.currentTimeMillis() - state.urgeActive < 1000 && horizontal > 0.01) {
            var speedAttribute = horse.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speedAttribute != null && horizontal < speedAttribute.getValue()) {
                double increase = Math.min(config.number("handling.lightning-start-acceleration-per-move"), speedAttribute.getValue() - horizontal);
                double factor = (horizontal + increase) / horizontal;
                velocity.setX(velocity.getX() * factor);
                velocity.setZ(velocity.getZ() * factor);
                changed = true;
            }
        }
        double retention = 0;
        float yawDifference = Math.abs(event.getTo().getYaw() - event.getFrom().getYaw()) % 360;
        yawDifference = Math.min(yawDifference, 360 - yawDifference);
        if (yawDifference >= config.number("handling.turn-angle-degrees")) {
            String[] turn = {"ride_the_wind", "windborne", "seasoned_rider", "endless_road", "fine_control", "master_of_reins", "light_cavalry", "swift_rider"};
            for (String perk : turn) if (perks.has(rider, perk)) retention += config.number("handling.turn-retention." + perk.replace('_', '-'));
            if (System.currentTimeMillis() < state.oneAsOneEnd) retention += config.number("handling.turn-retention.one-as-one");
        }
        if (!state.wasGrounded && grounded) {
            String[] landing = {"over_the_fence", "surefooted", "sure_landing", "master_of_reins"};
            for (String perk : landing) if (perks.has(rider, perk)) retention += config.number("handling.landing-retention." + perk.replace('_', '-'));
            if (System.currentTimeMillis() < state.oneAsOneEnd) retention += config.number("handling.landing-retention.one-as-one");
        }
        if (retention > 0 && horizontal > 0.0001 && horizontal >= config.number("handling.minimum-horizontal-speed") && state.lastHorizontalSpeed > horizontal) {
            double restore = Math.min(Math.max(0, config.number("handling.max-restored-velocity-per-move")), (state.lastHorizontalSpeed - horizontal) * Math.min(1, retention));
            double factor = (horizontal + restore) / horizontal;
            velocity.setX(velocity.getX() * factor);
            velocity.setZ(velocity.getZ() * factor);
            changed = true;
        }
        state.lastHorizontalSpeed = horizontal;
        state.wasGrounded = grounded;
        if (changed && Double.isFinite(velocity.getX()) && Double.isFinite(velocity.getY()) && Double.isFinite(velocity.getZ()))
            horse.setVelocity(velocity);
    }
}
