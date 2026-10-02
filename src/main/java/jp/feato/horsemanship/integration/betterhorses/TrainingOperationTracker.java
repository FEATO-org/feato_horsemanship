package jp.feato.horsemanship.integration.betterhorses;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps each interaction's pre-listener units separate, including same-tick interactions. */
public final class TrainingOperationTracker {
    private final Map<Object, Attempt> attempts = new IdentityHashMap<>();

    public void begin(Object event, UUID actor, BetterHorsesAdapter.Category category, double units) {
        attempts.put(event, new Attempt(actor, category, units));
    }

    public Gain finish(Object event, double units) {
        Attempt attempt = attempts.remove(event);
        if (attempt == null) return null;
        return new Gain(attempt.actor(), attempt.category(), Math.max(0, units - attempt.units()));
    }

    public BetterHorsesAdapter.Category category(Object event) {
        Attempt attempt = attempts.get(event);
        return attempt == null ? null : attempt.category();
    }

    public void clear() { attempts.clear(); }

    private record Attempt(UUID actor, BetterHorsesAdapter.Category category, double units) {}
    public record Gain(UUID actor, BetterHorsesAdapter.Category category, double units) {}
}
