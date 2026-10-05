package jp.feato.horsemanship;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import jp.feato.horsemanship.integration.valhalla.skill.HorsemanshipProfile;
import jp.feato.horsemanship.integration.valhalla.skill.HorsemanshipSkill;
import me.athlaeos.valhallammo.playerstats.profiles.ProfileRegistry;
import me.athlaeos.valhallammo.skills.skills.SkillRegistry;
import jp.feato.horsemanship.integration.valhalla.ValhallaAdapter;
import jp.feato.horsemanship.integration.valhalla.ExclusiveUnlockRegistration;
import jp.feato.horsemanship.integration.betterhorses.BetterHorsesAdapter;
import jp.feato.horsemanship.integration.betterhorses.HorsekeepingCommand;
import jp.feato.horsemanship.integration.betterhorses.HorsekeepingListener;
import jp.feato.horsemanship.listener.LifecycleListener;
import jp.feato.horsemanship.listener.HandlingListener;
import jp.feato.horsemanship.listener.HorseRecoveryListener;
import jp.feato.horsemanship.listener.MountedCombatListener;
import jp.feato.horsemanship.listener.MountedMarksmanListener;
import jp.feato.horsemanship.listener.RidingMovementListener;
import jp.feato.horsemanship.listener.UrgeTriggerListener;
import jp.feato.horsemanship.service.ExclusivePerkService;
import jp.feato.horsemanship.service.MountedEffectService;
import jp.feato.horsemanship.service.Mounts;
import jp.feato.horsemanship.service.UrgeService;
import jp.feato.horsemanship.state.RiderStateStore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

public final class HorsemanshipPlugin extends JavaPlugin implements TabExecutor {
    private final RiderStateStore states = new RiderStateStore();
    private MountedEffectService effects;
    private HorsemanshipConfig config;
    private HorsekeepingCommand horsekeepingCommand;
    private HorsekeepingListener horsekeepingListener;
    @Override public void onEnable() {
        saveDefaultConfig();
        reloadSettings();
        Plugin dependency = Bukkit.getPluginManager().getPlugin("ValhallaMMO");
        if (dependency == null || !dependency.isEnabled()) {
            getLogger().severe("ValhallaMMO is required");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        ValhallaAdapter valhalla;
        try {
            if (SkillRegistry.getSkill("HORSEMANSHIP") != null)
                throw new IllegalStateException("HORSEMANSHIP is already registered by another skill. Remove the legacy ValhallaMMO skills/custom horsemanship.yml (including HORSEMANSHIP.yml).");
            if (ProfileRegistry.getPersistence() == null)
                throw new IllegalStateException("ValhallaMMO profile database is not initialized");
            ExclusiveUnlockRegistration.register(dependency);
            ProfileRegistry.registerProfileType(new HorsemanshipProfile(null));
            HorsemanshipSkill skill = new HorsemanshipSkill(this);
            SkillRegistry.registerSkill(skill);
            if (SkillRegistry.getSkill("HORSEMANSHIP") != skill
                    || skill.getProfileType() != HorsemanshipProfile.class)
                throw new IllegalStateException("HORSEMANSHIP dedicated Skill/Profile registration failed");
            valhalla = new ValhallaAdapter(dependency);
            getLogger().info("HORSEMANSHIP registered with dedicated HorsemanshipSkill/HorsemanshipProfile");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            getLogger().log(java.util.logging.Level.SEVERE, "Cannot initialize Valhalla integration", exception);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Plugin betterHorsesPlugin = Bukkit.getPluginManager().getPlugin("BetterHorses");
        if (betterHorsesPlugin == null || !betterHorsesPlugin.isEnabled()) {
            getLogger().warning("BetterHorses is absent; Horsekeeping integration is unavailable");
        } else {
            try {
                BetterHorsesAdapter betterHorses = new BetterHorsesAdapter(betterHorsesPlugin);
                horsekeepingCommand = new HorsekeepingCommand(betterHorses, valhalla);
                horsekeepingListener = new HorsekeepingListener(this, betterHorses, valhalla, config);
                Bukkit.getPluginManager().registerEvents(horsekeepingCommand, this);
                Bukkit.getPluginManager().registerEvents(horsekeepingListener, this);
            } catch (ReflectiveOperationException exception) {
                getLogger().warning("Horsekeeping integration unavailable: " + exception.getMessage());
            }
        }
        ExclusivePerkService perks = new ExclusivePerkService(valhalla, getLogger());
        Mounts mounts = new Mounts(config);
        effects = new MountedEffectService(this, config, valhalla, perks, states, mounts);
        UrgeService urge = new UrgeService(config, perks, valhalla, states, mounts, effects);
        var manager = Bukkit.getPluginManager();
        manager.registerEvents(new RidingMovementListener(config, valhalla, perks, states, mounts), this);
        manager.registerEvents(new HandlingListener(config, perks, states, mounts, effects), this);
        manager.registerEvents(new HorseRecoveryListener(config, perks, mounts), this);
        manager.registerEvents(new MountedCombatListener(config, valhalla, perks, states, mounts, effects), this);
        manager.registerEvents(new MountedMarksmanListener(config, perks, mounts), this);
        manager.registerEvents(new UrgeTriggerListener(config, urge), this);
        manager.registerEvents(new LifecycleListener(this, states, effects, perks), this);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            urge.tick(now);
            effects.tick(now);
        }, 1L, 5L);
        if (getCommand("horsemanship") != null) getCommand("horsemanship").setExecutor(this);
        getLogger().info("FEATO Horsemanship enabled");
    }
    @Override public void onDisable() {
        if (effects != null) effects.clearAll();
        if (horsekeepingCommand != null) horsekeepingCommand.clear();
        if (horsekeepingListener != null) horsekeepingListener.clear();
        states.clear();
    }
    private void reloadSettings() {
        reloadConfig();
        config = new HorsemanshipConfig(getConfig(), getLogger());
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("feato.horsemanship.admin")) { sender.sendMessage("No permission."); return true; }
            reloadConfig();
            config.reload(getConfig());
            sender.sendMessage("Horsemanship configuration reloaded.");
            return true;
        }
        if (horsekeepingCommand != null) return horsekeepingCommand.onCommand(sender, command, label, args);
        sender.sendMessage("Usage: /horsemanship reload");
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        List<String> options = new java.util.ArrayList<>();
        if (sender.hasPermission("feato.horsemanship.admin") && "reload".startsWith(args[0].toLowerCase())) options.add("reload");
        if (horsekeepingCommand != null) options.addAll(horsekeepingCommand.onTabComplete(sender, command, alias, args));
        return options;
    }
}
