package jp.feato.horsemanship.service;

import jp.feato.horsemanship.integration.valhalla.ValhallaAdapter;
import jp.feato.horsemanship.logic.ExclusivePerks;
import org.bukkit.entity.Player;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public final class ExclusivePerkService {
    private static final Set<String> MOBILITY_BRANCH = Set.of("full_gallop", "quick_response", "lightning_start", "ride_the_wind", "windborne");
    private static final Set<String> ENDURANCE_BRANCH = Set.of("trailwise", "steady_pace", "second_wind", "iron_journey", "endless_road");
    private final ValhallaAdapter valhalla;
    private final Logger logger;
    private final Set<UUID> warned = new HashSet<>();
    public ExclusivePerkService(ValhallaAdapter valhalla, Logger logger) { this.valhalla = valhalla; this.logger = logger; }
    public ExclusivePerks.Route route(Player player, String first, String second) {
        Set<String> active = new HashSet<>();
        if (valhalla.has(player, first)) active.add(first);
        if (valhalla.has(player, second)) active.add(second);
        var route = ExclusivePerks.resolve(active, first, second);
        if (route == ExclusivePerks.Route.CONFLICT && warned.add(player.getUniqueId()))
            logger.warning("Conflicting Horsemanship perks for " + player.getUniqueId() + ": " + first + ", " + second + "; both effects suppressed");
        if (route != ExclusivePerks.Route.CONFLICT) warned.remove(player.getUniqueId());
        return route;
    }
    public boolean has(Player player, String perk) {
        if (MOBILITY_BRANCH.contains(perk))
            return route(player, "breakaway", "relentless_pace") == ExclusivePerks.Route.FIRST && valhalla.has(player, perk);
        if (ENDURANCE_BRANCH.contains(perk))
            return route(player, "breakaway", "relentless_pace") == ExclusivePerks.Route.SECOND && valhalla.has(player, perk);
        if (perk.equals("first_impact"))
            return route(player, "heavy_cavalry", "light_cavalry") != ExclusivePerks.Route.CONFLICT &&
                route(player, "heavy_cavalry", "light_cavalry") != ExclusivePerks.Route.NONE && valhalla.has(player, perk);
        if (perk.equals("breakaway") || perk.equals("relentless_pace"))
            return allowed(player, perk, "breakaway", "relentless_pace");
        if (perk.equals("heavy_cavalry") || perk.equals("light_cavalry"))
            return allowed(player, perk, "heavy_cavalry", "light_cavalry");
        if (perk.equals("iron_vanguard") || perk.equals("swift_rider")) {
            if (!allowed(player, perk, "iron_vanguard", "swift_rider")) return false;
            return has(player, perk.equals("iron_vanguard") ? "heavy_cavalry" : "light_cavalry");
        }
        return valhalla.has(player, perk);
    }
    private boolean allowed(Player player, String perk, String first, String second) {
        var route = route(player, first, second);
        return (route == ExclusivePerks.Route.FIRST && perk.equals(first))
            || (route == ExclusivePerks.Route.SECOND && perk.equals(second));
    }
    public void forget(UUID player) { warned.remove(player); }
}
