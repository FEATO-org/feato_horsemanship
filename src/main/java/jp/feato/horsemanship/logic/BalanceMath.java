package jp.feato.horsemanship.logic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class BalanceMath {
    private BalanceMath() {}

    public static double fatigueChance(double base, double reduction, double minimum) {
        return Math.max(minimum, Math.min(1.0, base - reduction));
    }

    public static double oneAsOneChance(double fatigueChance, double conditionalChance) {
        return (1.0 - fatigueChance) * conditionalChance;
    }

    public static Range breedingRange(double father, double mother, double mutation, double maximum) {
        double base = (father + mother) / 2.0;
        return new Range(base - mutation, Math.min(base + mutation, maximum));
    }

    public static Map<String, Double> sequentialTraitProbabilities(Map<String, Double> orderedChances) {
        return sequentialTraitProbabilities(orderedChances, ignored -> true);
    }

    public static Map<String, Double> sequentialTraitProbabilities(Map<String, Double> orderedChances, Predicate<String> permitted) {
        Map<String, Double> result = new LinkedHashMap<>();
        double remaining = 1.0;
        double denied = 0.0;
        for (var entry : orderedChances.entrySet()) {
            double chance = Math.max(0.0, Math.min(1.0, entry.getValue()));
            if (permitted.test(entry.getKey())) result.put(entry.getKey(), remaining * chance);
            else denied += remaining * chance;
            remaining *= 1.0 - chance;
        }
        result.put("none", remaining + denied);
        return result;
    }

    public static boolean validMovement(double distance, double maximum) {
        return Double.isFinite(distance) && distance > 0 && distance <= maximum;
    }

    public static double capExperienceMultiplier(double multiplier, double maximum) {
        return Math.min(Math.max(1.0, maximum), Math.max(0.0, multiplier));
    }

    public static boolean trailwiseActive(double distanceOnMount, double minimumDistance) {
        return distanceOnMount >= Math.max(0, minimumDistance);
    }

    public record Range(double minimum, double maximum) {}
}
