package jp.feato.horsemanship.listener;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import jp.feato.horsemanship.integration.valhalla.ValhallaAdapter;
import jp.feato.horsemanship.logic.BalanceMath;
import jp.feato.horsemanship.service.ExclusivePerkService;
import jp.feato.horsemanship.service.Mounts;
import jp.feato.horsemanship.state.RiderState;
import jp.feato.horsemanship.state.RiderStateStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;

public final class RidingMovementListener implements Listener {
    private final HorsemanshipConfig config;
    private final ValhallaAdapter valhalla;
    private final ExclusivePerkService perks;
    private final RiderStateStore states;
    private final Mounts mounts;
    public RidingMovementListener(HorsemanshipConfig config, ValhallaAdapter valhalla, ExclusivePerkService perks, RiderStateStore states, Mounts mounts) {
        this.config = config; this.valhalla = valhalla; this.perks = perks; this.states = states; this.mounts = mounts;
    }
    @EventHandler
    public void onMove(VehicleMoveEvent event) {
        if (!config.eligible(event.getVehicle().getType())) return;
        Player driver = Mounts.driver(event.getVehicle().getPassengers());
        if (driver == null || !mounts.isDriver(driver)) return;
        if (event.getFrom().getWorld() != event.getTo().getWorld()) return;
        double distance = event.getFrom().distance(event.getTo());
        if (!BalanceMath.validMovement(distance, config.number("experience.movement.max-distance-per-move"))) return;
        RiderState state = states.get(driver.getUniqueId());
        if (!event.getVehicle().getUniqueId().equals(state.movementMount)) {
            state.movementDistance = 0;
            state.distanceOnMount = 0;
            state.movementMount = event.getVehicle().getUniqueId();
        }
        state.movementDistance += distance;
        state.distanceOnMount += distance;
        double batch = config.number("experience.movement.batch-distance");
        if (batch <= 0) return;
        int count = (int) (state.movementDistance / batch);
        if (count <= 0) return;
        state.movementDistance -= count * batch;
        double bonus = 1;
        if (perks.has(driver, "first_saddle")) bonus += config.number("experience.first-saddle-bonus");
        if (perks.has(driver, "rein_sense")) bonus += config.number("experience.rein-sense-bonus");
        if (perks.has(driver, "good_hands")) bonus += config.number("experience.good-hands-bonus");
        if (perks.has(driver, "long_haul")) bonus += config.number("experience.long-haul-bonus");
        if (perks.has(driver, "iron_journey")) bonus += config.number("experience.iron-journey-bonus");
        if (BalanceMath.trailwiseActive(state.distanceOnMount, config.number("experience.trailwise.minimum-distance")) && perks.has(driver, "trailwise"))
            bonus += config.number("experience.trailwise.bonus");
        if (valhalla.newGamePlus(driver) >= 1) bonus += config.number("experience.ng-plus-master-bonus");
        bonus = BalanceMath.capExperienceMultiplier(bonus, config.number("experience.movement.max-multiplier"));
        valhalla.addExperience(driver, count * config.number("experience.movement.exp-per-batch") * bonus);
    }
}
