package jp.feato.horsemanship.listener;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import jp.feato.horsemanship.service.ExclusivePerkService;
import jp.feato.horsemanship.service.Mounts;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public final class HorseRecoveryListener implements Listener {
    private final HorsemanshipConfig config;
    private final ExclusivePerkService perks;
    private final Mounts mounts;
    public HorseRecoveryListener(HorsemanshipConfig config, ExclusivePerkService perks, Mounts mounts) {
        this.config = config; this.perks = perks; this.mounts = mounts;
    }
    @EventHandler(ignoreCancelled = true)
    public void onRecover(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || !config.eligible(horse.getType())) return;
        Player rider = Mounts.driver(horse.getPassengers());
        if (rider != null && mounts.isDriver(rider) && perks.has(rider, "iron_journey"))
            event.setAmount(event.getAmount() * (1 + config.number("handling.iron-journey-recovery-bonus")));
    }
}
