package jp.feato.horsemanship.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RiderStateStore {
    private final Map<UUID, RiderState> states = new HashMap<>();
    public RiderState get(UUID rider) { return states.computeIfAbsent(rider, ignored -> new RiderState()); }
    public RiderState peek(UUID rider) { return states.get(rider); }
    public void remove(UUID rider) { states.remove(rider); }
    public void clear() { states.clear(); }
}
