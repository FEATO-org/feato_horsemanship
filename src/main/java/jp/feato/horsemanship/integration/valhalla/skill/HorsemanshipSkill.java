package jp.feato.horsemanship.integration.valhalla.skill;

import me.athlaeos.valhallammo.playerstats.profiles.Profile;
import me.athlaeos.valhallammo.skills.skills.Skill;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.InvalidConfigurationException;

public final class HorsemanshipSkill extends Skill {
    private final JavaPlugin plugin;
    private int order;
    public HorsemanshipSkill(JavaPlugin plugin) {
        super("HORSEMANSHIP");
        this.plugin = plugin;
    }
    @Override public void loadConfiguration() {
        File file = new File(plugin.getDataFolder(), "horsemanship.yml");
        if (!file.exists()) plugin.saveResource("horsemanship.yml", false);
        YamlConfiguration config = new YamlConfiguration();
        try { config.load(file); }
        catch (IOException | InvalidConfigurationException error) {
            throw new IllegalStateException("Cannot load " + file, error);
        }
        if (!config.getBoolean("enabled", true)) throw new IllegalStateException("Horsemanship skill configuration is disabled");
        if (config.getConfigurationSection("perks") == null || config.getString("experience.exp_level_curve") == null
                || config.getInt("experience.max_level") <= 0)
            throw new IllegalStateException("Horsemanship progression configuration is incomplete");
        // Valhalla reload may call this again. Common config appends rewards and perks.
        startingPerks.clear(); levelingPerks.clear(); perks.clear();
        specialLevelingPerks.clear(); specialLevelingMessages.clear();
        specialLevelingCommands.clear(); specialLevelingUndoCommands.clear();
        order = config.getInt("order", 15);
        loadCommonConfig(config, config);
    }
    @Override public boolean isLevelableSkill() { return true; }
    @Override public Class<? extends Profile> getProfileType() { return HorsemanshipProfile.class; }
    @Override public int getSkillTreeMenuOrderPriority() { return order; }
}
