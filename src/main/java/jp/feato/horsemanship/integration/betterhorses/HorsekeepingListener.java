package jp.feato.horsemanship.integration.betterhorses;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import jp.feato.horsemanship.integration.valhalla.ValhallaAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HorsekeepingListener implements Listener {
    private final Plugin plugin;
    private final BetterHorsesAdapter betterHorses;
    private final ValhallaAdapter valhalla;
    private final HorsemanshipConfig config;
    private final TrainingOperationTracker attempts = new TrainingOperationTracker();
    private final Map<UUID, FeedAttempt> feedAttempts = new HashMap<>();

    public HorsekeepingListener(Plugin plugin, BetterHorsesAdapter betterHorses, ValhallaAdapter valhalla, HorsemanshipConfig config) {
        this.plugin = plugin; this.betterHorses = betterHorses; this.valhalla = valhalla; this.config = config;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beforeInteraction(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof AbstractHorse horse) || !betterHorses.isBetterHorse(horse)) return;
        Player player = event.getPlayer();
        if (!valhalla.has(player, "horse_sense")) return;
        ItemStack item = player.getInventory().getItem(event.getHand());
        if (item == null) return;
        BetterHorsesAdapter.Category category;
        if (item.getType() == betterHorses.brushMaterial()) category = BetterHorsesAdapter.Category.BRUSHING;
        else if (horse instanceof Horse && isFeed(item.getType())) category = BetterHorsesAdapter.Category.FEEDING;
        else return;

        attempts.begin(event, player.getUniqueId(), category, betterHorses.units(horse, category));
        if (category == BetterHorsesAdapter.Category.FEEDING) {
            UUID horseId = horse.getUniqueId();
            feedAttempts.merge(horseId, new FeedAttempt(player.getUniqueId(), event),
                (first, next) -> new FeedAttempt(null, null));
            Bukkit.getScheduler().runTask(plugin, () -> feedAttempts.remove(horseId));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void afterInteraction(PlayerInteractEntityEvent event) {
        BetterHorsesAdapter.Category category = attempts.category(event);
        if (category == null || !(event.getRightClicked() instanceof AbstractHorse horse)) return;
        TrainingOperationTracker.Gain gain = attempts.finish(event, betterHorses.units(horse, category));
        if (gain == null || gain.units() <= 0 || !gain.actor().equals(event.getPlayer().getUniqueId())) return;
        Player actor = event.getPlayer();
        double multiplier = config.number("horsekeeping.training-assistance.horse-sense");
        if (valhalla.has(actor, "bloodline_study")) multiplier += config.number("horsekeeping.training-assistance.bloodline-study");
        if (valhalla.has(actor, "horse_whisperer")) multiplier += config.number("horsekeeping.training-assistance.horse-whisperer");
        if (valhalla.newGamePlus(actor) >= 1) multiplier += config.number("horsekeeping.training-assistance.ng-plus-master");
        betterHorses.addTrainingBonus(horse, gain.category(), gain.units(), multiplier);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFeedHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Horse horse) || !betterHorses.isBetterHorse(horse)) return;
        if (event.getRegainReason() != EntityRegainHealthEvent.RegainReason.EATING) return;
        FeedAttempt attempt = feedAttempts.remove(horse.getUniqueId());
        if (attempt == null || attempt.playerId() == null || attempt.interaction().isCancelled() || event.getAmount() <= 0) return;
        Player actor = Bukkit.getPlayer(attempt.playerId());
        if (actor == null || !valhalla.has(actor, "horse_sense")) return;
        double multiplier = config.number("horsekeeping.healing-bonus.horse-sense");
        if (valhalla.has(actor, "breeders_insight")) multiplier += config.number("horsekeeping.healing-bonus.breeders-insight");
        if (valhalla.has(actor, "horse_whisperer")) multiplier += config.number("horsekeeping.healing-bonus.horse-whisperer");
        // EATING carries the real heal amount; a Training max-HP recalculation does not.
        var maxHealth = horse.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;
        event.setAmount(cappedHealing(event.getAmount(), multiplier, horse.getHealth(), maxHealth.getValue()));
    }

    public static double cappedHealing(double eventAmount, double multiplier, double current, double maximum) {
        return Math.min(Math.max(0, maximum - current), eventAmount * (1 + Math.max(0, multiplier)));
    }

    private static boolean isFeed(Material type) {
        return type == Material.GOLDEN_APPLE || type == Material.ENCHANTED_GOLDEN_APPLE || type == Material.SUGAR ||
            type == Material.HAY_BLOCK || type == Material.WHEAT || type == Material.APPLE ||
            type == Material.GOLDEN_CARROT || type == Material.CARROT;
    }

    private record FeedAttempt(UUID playerId, PlayerInteractEntityEvent interaction) {}
    public void clear() { attempts.clear(); feedAttempts.clear(); }
}
