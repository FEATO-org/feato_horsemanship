package jp.feato.horsemanship.listener;

import jp.feato.horsemanship.service.ExclusivePerkService;
import jp.feato.horsemanship.service.MountedEffectService;
import jp.feato.horsemanship.state.RiderState;
import jp.feato.horsemanship.state.RiderStateStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.Plugin;
import java.util.UUID;

public final class LifecycleListener implements Listener {
    private final RiderStateStore states;
    private final MountedEffectService effects;
    private final ExclusivePerkService perks;
    private final Plugin plugin;
    public LifecycleListener(Plugin plugin, RiderStateStore states, MountedEffectService effects, ExclusivePerkService perks) {
        this.plugin = plugin; this.states = states; this.effects = effects; this.perks = perks;
    }
    private void clear(Player player, boolean forget) {
        RiderState state = states.peek(player.getUniqueId());
        if (state != null) effects.clearMount(state);
        effects.clearRider(player);
        if (forget) { states.remove(player.getUniqueId()); perks.forget(player.getUniqueId()); }
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { clear(event.getPlayer(), true); }
    @EventHandler public void onDeath(PlayerDeathEvent event) { clear(event.getEntity(), true); }
    @EventHandler public void onWorld(PlayerChangedWorldEvent event) { clear(event.getPlayer(), false); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID oldMount = player.getVehicle() == null ? null : player.getVehicle().getUniqueId();
        Location from = event.getFrom().clone();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Location destination = event.getTo();
            boolean moved = destination != null && player.getWorld() == destination.getWorld() &&
                (from.getWorld() != destination.getWorld() || from.distanceSquared(destination) > 0.0001) &&
                player.getLocation().distanceSquared(destination) < 4.0;
            UUID currentMount = player.getVehicle() == null ? null : player.getVehicle().getUniqueId();
            if (shouldClearTeleport(event.isCancelled(), moved, oldMount, currentMount)) clear(player, false);
        });
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) return;
        UUID oldMount = event.getVehicle().getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> {
            UUID currentMount = player.getVehicle() == null ? null : player.getVehicle().getUniqueId();
            if (shouldClearExit(event.isCancelled(), oldMount, currentMount)) clear(player, false);
        });
    }
    public static boolean shouldClearExit(boolean cancelled, UUID oldMount, UUID currentMount) {
        return !cancelled && !oldMount.equals(currentMount);
    }
    public static boolean shouldClearTeleport(boolean cancelled, boolean moved, UUID oldMount, UUID currentMount) {
        return !cancelled && (moved || (oldMount != null && !oldMount.equals(currentMount)));
    }
}
