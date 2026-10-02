package jp.feato.horsemanship.integration.betterhorses;

import jp.feato.horsemanship.integration.valhalla.ValhallaAdapter;
import jp.feato.horsemanship.logic.BalanceMath;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HorsekeepingCommand implements TabExecutor, Listener {
    private final BetterHorsesAdapter betterHorses;
    private final ValhallaAdapter valhalla;
    private final Map<UUID, UUID> firstParents = new HashMap<>();
    public HorsekeepingCommand(BetterHorsesAdapter betterHorses, ValhallaAdapter valhalla) {
        this.betterHorses = betterHorses; this.valhalla = valhalla;
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Player only."); return true; }
        if (args.length != 1) { player.sendMessage("Usage: /horsemanship <inspect|parent|predict>"); return true; }
        if (!valhalla.has(player, "horse_sense")) { player.sendMessage("馬を見る目が必要です。"); return true; }
        AbstractHorse target = target(player);
        if (target == null || !betterHorses.isBetterHorse(target)) { player.sendMessage("BetterHorsesの馬を近くで見てください。"); return true; }
        switch (args[0].toLowerCase()) {
            case "inspect" -> inspect(player, target);
            case "parent" -> {
                if (!valhalla.has(player, "breeders_insight")) { player.sendMessage("生産者の眼が必要です。"); return true; }
                firstParents.put(player.getUniqueId(), target.getUniqueId());
                player.sendMessage("最初の親を記録しました。次の親を見て /horsemanship predict を実行してください。");
            }
            case "predict" -> predict(player, target);
            default -> player.sendMessage("Usage: /horsemanship <inspect|parent|predict>");
        }
        return true;
    }
    private void inspect(Player player, AbstractHorse horse) {
        var info = betterHorses.snapshot(horse);
        player.sendMessage(String.format("馬: Health %.2f, Speed %.3f, Jump %.3f", info.health(), info.speed(), info.jump()));
        player.sendMessage("Gender " + info.gender() + ", Trait " + info.trait());
        player.sendMessage("Training: Riding " + betterHorses.progressLabel(horse, "riding") +
            ", Brushing " + betterHorses.progressLabel(horse, "brushing") +
            ", Feeding " + betterHorses.progressLabel(horse, "feeding"));
        if (valhalla.has(player, "bloodline_study"))
            player.sendMessage("繁殖CD: " + ((betterHorses.cooldownRemaining(horse, System.currentTimeMillis()) + 999) / 1000) + "秒");
    }
    private void predict(Player player, AbstractHorse second) {
        if (!valhalla.has(player, "breeders_insight")) { player.sendMessage("生産者の眼が必要です。"); return; }
        UUID firstId = firstParents.get(player.getUniqueId());
        Entity found = firstId == null ? null : Bukkit.getEntity(firstId);
        if (!(found instanceof AbstractHorse first) || !betterHorses.isBetterHorse(first)) { player.sendMessage("最初の親を選び直してください。"); return; }
        if (first.getUniqueId().equals(second.getUniqueId()) || first.getType() != second.getType()) { player.sendMessage("同種の別個体を選んでください。"); return; }
        for (String stat : List.of("health", "speed", "jump")) {
            BalanceMath.Range range = betterHorses.range(first, second, stat);
            player.sendMessage(String.format("%s 予測: %.3f ～ %.3f", stat, range.minimum(), range.maximum()));
        }
        if (valhalla.has(player, "bloodline_study")) {
            var probabilities = betterHorses.traitProbabilities(player);
            for (var entry : probabilities.entrySet())
                player.sendMessage(String.format("Trait %s: %.1f%%", entry.getKey(), entry.getValue() * 100));
            player.sendMessage("繁殖CD: " + ((Math.max(betterHorses.cooldownRemaining(first, System.currentTimeMillis()),
                betterHorses.cooldownRemaining(second, System.currentTimeMillis())) + 999) / 1000) + "秒");
        }
        if (valhalla.has(player, "horse_whisperer")) {
            var a = betterHorses.snapshot(first); var b = betterHorses.snapshot(second);
            player.sendMessage(String.format("親比較 H %.1f / %.1f, S %.3f / %.3f, J %.3f / %.3f",
                a.health(), b.health(), a.speed(), b.speed(), a.jump(), b.jump()));
        }
    }
    private AbstractHorse target(Player player) {
        Entity entity = player.getTargetEntity(6);
        if (!(entity instanceof AbstractHorse)) entity = player.getVehicle();
        return entity instanceof AbstractHorse horse ? horse : null;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player) || !valhalla.has(player, "horse_sense")) return List.of();
        List<String> values = valhalla.has(player, "breeders_insight") ? List.of("inspect", "parent", "predict") : List.of("inspect");
        return values.stream().filter(value -> value.startsWith(args[0].toLowerCase())).toList();
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { firstParents.remove(event.getPlayer().getUniqueId()); }
    public void clear() { firstParents.clear(); }
}
