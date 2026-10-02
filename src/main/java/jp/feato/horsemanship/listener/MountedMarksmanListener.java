package jp.feato.horsemanship.listener;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import jp.feato.horsemanship.service.ExclusivePerkService;
import jp.feato.horsemanship.service.Mounts;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

public final class MountedMarksmanListener implements Listener {
    private final HorsemanshipConfig config;
    private final ExclusivePerkService perks;
    private final Mounts mounts;
    public MountedMarksmanListener(HorsemanshipConfig config, ExclusivePerkService perks, Mounts mounts) {
        this.config = config; this.perks = perks; this.mounts = mounts;
    }
    @EventHandler(ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.getShooter() instanceof Player shooter)) return;
        if (mounts.eligibleMount(shooter) == null || !perks.has(shooter, "mounted_marksman")) return;
        Vector velocity = arrow.getVelocity();
        double speed = velocity.length();
        if (speed <= 0) return;
        double correction = Math.max(0, Math.min(1, config.number("handling.mounted-marksman-aim-correction")));
        Vector direction = velocity.normalize().multiply(1 - correction).add(shooter.getEyeLocation().getDirection().multiply(correction)).normalize();
        arrow.setVelocity(direction.multiply(speed));
    }
}
