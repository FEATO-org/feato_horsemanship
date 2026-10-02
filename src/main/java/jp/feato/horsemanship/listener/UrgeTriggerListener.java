package jp.feato.horsemanship.listener;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import jp.feato.horsemanship.service.UrgeService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class UrgeTriggerListener implements Listener {
    private final HorsemanshipConfig config;
    private final UrgeService urge;
    public UrgeTriggerListener(HorsemanshipConfig config, UrgeService urge) { this.config = config; this.urge = urge; }
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != config.trigger()) return;
        urge.trigger(event.getPlayer(), System.currentTimeMillis());
    }
}
