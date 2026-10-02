package jp.feato.horsemanship.integration;

import jp.feato.horsemanship.integration.betterhorses.BetterHorsesAdapter;
import jp.feato.horsemanship.integration.betterhorses.TrainingOperationTracker;
import jp.feato.horsemanship.integration.betterhorses.HorsekeepingListener;
import jp.feato.horsemanship.integration.valhalla.ExclusiveUnlockRegistration;
import jp.feato.horsemanship.listener.LifecycleListener;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationRegressionTest {
    @Test void oppositePerkBlocksAcquisitionAndRefundRestoresIt() {
        assertFalse(ExclusiveUnlockRegistration.canAcquire(List.of("breakaway"), List.of(), "breakaway"));
        assertFalse(ExclusiveUnlockRegistration.canAcquire(List.of(), List.of("breakaway"), "breakaway"));
        assertTrue(ExclusiveUnlockRegistration.canAcquire(List.of(), List.of(), "breakaway"));
    }

    @Test void cancelledLifecycleEventsKeepStateAndRealExitClearsIt() {
        UUID oldMount = UUID.randomUUID();
        UUID newMount = UUID.randomUUID();
        assertFalse(LifecycleListener.shouldClearExit(true, oldMount, null));
        assertFalse(LifecycleListener.shouldClearExit(false, oldMount, oldMount));
        assertTrue(LifecycleListener.shouldClearExit(false, oldMount, null));
        assertTrue(LifecycleListener.shouldClearExit(false, oldMount, newMount));
        assertFalse(LifecycleListener.shouldClearTeleport(true, true, oldMount, null));
        assertFalse(LifecycleListener.shouldClearTeleport(false, false, oldMount, oldMount));
        assertTrue(LifecycleListener.shouldClearTeleport(false, true, oldMount, oldMount));
    }

    @Test void sameTickTrainingOperationsKeepActorsAndExcludeOurBonus() {
        var tracker = new TrainingOperationTracker();
        Object firstEvent = new Object();
        Object secondEvent = new Object();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        tracker.begin(firstEvent, first, BetterHorsesAdapter.Category.BRUSHING, 10.0);
        var firstGain = tracker.finish(firstEvent, 12.0);
        assertNotNull(firstGain);
        assertEquals(first, firstGain.actor());
        assertEquals(2.0, firstGain.units());
        tracker.begin(secondEvent, second, BetterHorsesAdapter.Category.BRUSHING, 12.2);
        var secondGain = tracker.finish(secondEvent, 15.2);
        assertNotNull(secondGain);
        assertEquals(second, secondGain.actor());
        assertEquals(3.0, secondGain.units(), 0.00001);
        assertNull(tracker.finish(firstEvent, 15.2));
    }

    @Test void disabledTrainingIsLabeledWithoutDiscardingSavedProgress() {
        var config = new YamlConfiguration();
        config.set("training.enabled", false);
        config.set("training.categories.riding.enabled", true);
        assertFalse(BetterHorsesAdapter.trainingEnabled(config, "riding"));
        assertEquals("無効", BetterHorsesAdapter.progressLabel(false, 72.0));
        config.set("training.enabled", true);
        assertTrue(BetterHorsesAdapter.trainingEnabled(config, "riding"));
        assertEquals("72.0%", BetterHorsesAdapter.progressLabel(true, 72.0));
    }

    @Test void feedingHealingUsesEventAmountAndRespectsNewMaximum() {
        // A Training-driven HP increase is not an EntityRegainHealthEvent amount.
        assertEquals(2.2, HorsekeepingListener.cappedHealing(2.0, 0.1, 12.0, 20.0), 0.00001);
        assertEquals(1.0, HorsekeepingListener.cappedHealing(2.0, 0.1, 19.0, 20.0));
        assertEquals(0.0, HorsekeepingListener.cappedHealing(2.0, 0.1, 20.0, 20.0));
    }
}
