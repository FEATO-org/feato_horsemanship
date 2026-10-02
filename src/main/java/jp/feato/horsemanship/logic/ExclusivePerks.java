package jp.feato.horsemanship.logic;

import java.util.Set;

public final class ExclusivePerks {
    private ExclusivePerks() {}
    public enum Route { NONE, FIRST, SECOND, CONFLICT }
    public static Route resolve(Set<String> active, String first, String second) {
        boolean a = active.contains(first);
        boolean b = active.contains(second);
        if (a && b) return Route.CONFLICT;
        if (a) return Route.FIRST;
        if (b) return Route.SECOND;
        return Route.NONE;
    }
}
