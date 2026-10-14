package jp.feato.horsemanship.listener;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import jp.feato.horsemanship.integration.valhalla.ValhallaAdapter;
import jp.feato.horsemanship.service.ExclusivePerkService;
import jp.feato.horsemanship.service.Mounts;
import jp.feato.horsemanship.state.RiderStateStore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RidingMovementTest {
    @Test void batchesKeepRemainderWithoutAnyPerk() {
        var f = new Fixture();
        f.travel(49);
        verifyNoInteractions(f.valhalla);
        assertEquals(49, f.remainder(), 0.000001);
        f.move(3);
        verify(f.valhalla).addExperience(f.player, 5);
        assertEquals(2, f.remainder(), 0.000001);
        for (int distance : new int[]{50, 100, 125}) {
            var batch = new Fixture();
            batch.travel(distance);
            verify(batch.valhalla, times(distance / 50)).addExperience(batch.player, 5);
            assertEquals(distance % 50, batch.remainder(), 0.000001);
        }
    }
    @Test void multipleBatchesInOneValidMoveKeepRemainder() {
        var f = new Fixture();
        f.yaml.set("experience.movement.max-distance-per-move", 200);
        f.move(125);
        verify(f.valhalla).addExperience(f.player, 10);
        assertEquals(25, f.remainder(), 0.000001);
    }
    @Test void jumpsWorldChangesAndNonDriversDoNotAccumulate() {
        var f = new Fixture();
        f.move(13);
        f.listener.onMove(new VehicleMoveEvent(f.vehicle, new Location(f.world, 0, 0, 0),
            new Location(mock(World.class), 10, 0, 0)));
        when(f.mounts.isDriver(f.player)).thenReturn(false);
        f.move(10);
        when(f.mounts.isDriver(f.player)).thenReturn(true);
        when(f.vehicle.getPassengers()).thenReturn(List.of());
        f.move(10);
        verifyNoInteractions(f.valhalla);
        assertNull(f.states.peek(f.player.getUniqueId()));
    }
    @Test void mountChangeDiscardsPreviousRemainder() {
        var f = new Fixture();
        f.travel(49);
        when(f.vehicle.getUniqueId()).thenReturn(UUID.randomUUID());
        f.move(3);
        assertEquals(3, f.remainder(), 0.000001);
        assertEquals(3, f.states.get(f.player.getUniqueId()).distanceOnMount, 0.000001);
        verifyNoInteractions(f.valhalla);
    }
    private static class Fixture {
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
            getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8));
        final Player player = mock(Player.class);
        final Vehicle vehicle = mock(Vehicle.class);
        final World world = mock(World.class);
        final Mounts mounts = mock(Mounts.class);
        final ValhallaAdapter valhalla = mock(ValhallaAdapter.class);
        final RiderStateStore states = new RiderStateStore();
        final RidingMovementListener listener;
        Fixture() {
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(vehicle.getUniqueId()).thenReturn(UUID.randomUUID());
            when(vehicle.getType()).thenReturn(EntityType.HORSE);
            when(vehicle.getPassengers()).thenReturn(List.of(player));
            when(mounts.isDriver(player)).thenReturn(true);
            listener = new RidingMovementListener(new HorsemanshipConfig(yaml, Logger.getAnonymousLogger()),
                valhalla, mock(ExclusivePerkService.class), states, mounts);
        }
        void move(double distance) {
            listener.onMove(new VehicleMoveEvent(vehicle, new Location(world, 0, 0, 0),
                new Location(world, distance, 0, 0)));
        }
        void travel(double distance) {
            while (distance > 0) {
                double step = Math.min(10, distance);
                move(step);
                distance -= step;
            }
        }
        double remainder() { return states.get(player.getUniqueId()).movementDistance; }
    }
}
