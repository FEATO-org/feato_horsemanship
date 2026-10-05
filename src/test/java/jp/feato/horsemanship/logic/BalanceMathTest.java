package jp.feato.horsemanship.logic;

import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class BalanceMathTest {
    @Test void movementExperienceDoesNotRequireFirstSaddle() {
        var resource = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(resource);
        var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
            new java.io.InputStreamReader(resource, java.nio.charset.StandardCharsets.UTF_8));
        double amount = config.getDouble("experience.movement.exp-per-batch");
        double cap = config.getDouble("experience.movement.max-multiplier");
        assertEquals(100.0, config.getDouble("experience.movement.batch-distance"));
        assertEquals(10.0, amount * BalanceMath.capExperienceMultiplier(1.0, cap), 0.000001);
        assertEquals(10.5, amount * BalanceMath.capExperienceMultiplier(1.0 + config.getDouble("experience.first-saddle-bonus"), cap), 0.000001);
    }
    @Test void trailwiseBonusRespectsTotalExperienceCap() {
        assertFalse(BalanceMath.trailwiseActive(999.9, 1000.0));
        assertTrue(BalanceMath.trailwiseActive(1000.0, 1000.0));
        assertEquals(1.25, BalanceMath.capExperienceMultiplier(1.25, 1.5));
        assertEquals(1.5, BalanceMath.capExperienceMultiplier(1.62, 1.5));
    }
    @Test void fatigueAndCapstone() {
        assertEquals(0.05, BalanceMath.fatigueChance(0.15, 0.30, 0.05));
        assertEquals(0.425, BalanceMath.oneAsOneChance(0.15, 0.50), 0.000001);
    }
    @Test void breedingAndOrderedTraits() {
        var range = BalanceMath.breedingRange(10, 20, 2, 16);
        assertEquals(13, range.minimum());
        assertEquals(16, range.maximum());
        var chances = new LinkedHashMap<String, Double>();
        chances.put("A", 0.1); chances.put("B", 0.1);
        var result = BalanceMath.sequentialTraitProbabilities(chances);
        assertEquals(0.1, result.get("A"), 0.000001);
        assertEquals(0.09, result.get("B"), 0.000001);
        assertEquals(0.81, result.get("none"), 0.000001);
    }
    @Test void deniedTraitConsumesItsSequentialChance() {
        var chances = new LinkedHashMap<String, Double>();
        chances.put("A", 0.1); chances.put("B", 0.1);
        var result = BalanceMath.sequentialTraitProbabilities(chances, name -> !name.equals("A"));
        assertFalse(result.containsKey("A"));
        assertEquals(0.09, result.get("B"), 0.000001);
        assertEquals(0.91, result.get("none"), 0.000001);
    }
    @Test void movementAndExclusion() {
        assertFalse(BalanceMath.validMovement(13, 12));
        assertFalse(BalanceMath.validMovement(Double.NaN, 12));
        assertTrue(BalanceMath.validMovement(2, 12));
        assertEquals(ExclusivePerks.Route.CONFLICT, ExclusivePerks.resolve(Set.of("a", "b"), "a", "b"));
        assertEquals(ExclusivePerks.Route.FIRST, ExclusivePerks.resolve(Set.of("a"), "a", "b"));
    }
}
