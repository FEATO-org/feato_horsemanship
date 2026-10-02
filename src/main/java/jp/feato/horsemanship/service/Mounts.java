package jp.feato.horsemanship.service;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.util.List;

public final class Mounts {
    private final HorsemanshipConfig config;
    public Mounts(HorsemanshipConfig config) { this.config = config; }
    public Entity eligibleMount(Player rider) {
        Entity vehicle = rider.getVehicle();
        return vehicle != null && config.eligible(vehicle.getType()) ? vehicle : null;
    }
    public boolean isDriver(Player rider) {
        Entity mount = eligibleMount(rider);
        return mount != null && driver(mount.getPassengers()) == rider;
    }
    public static Player driver(List<Entity> passengers) {
        for (Entity passenger : passengers) if (passenger instanceof Player player) return player;
        return null;
    }
}
