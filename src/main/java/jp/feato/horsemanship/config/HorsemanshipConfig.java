package jp.feato.horsemanship.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

public final class HorsemanshipConfig {
    private FileConfiguration source;
    private final Set<EntityType> mounts = EnumSet.noneOf(EntityType.class);
    private Material trigger;
    private final Logger logger;

    public HorsemanshipConfig(FileConfiguration source, Logger logger) {
        this.logger = logger;
        reload(source);
    }
    public void reload(FileConfiguration source) {
        this.source = source;
        mounts.clear();
        for (String name : source.getStringList("eligible-mounts")) {
            try { mounts.add(EntityType.valueOf(name)); }
            catch (IllegalArgumentException exception) { logger.warning("Unknown eligible mount: " + name); }
        }
        Material material = Material.matchMaterial(source.getString("urge.trigger-material", "LEAD"));
        trigger = material == null ? Material.LEAD : material;
    }
    public boolean eligible(EntityType type) { return mounts.contains(type); }
    public Material trigger() { return trigger; }
    public double number(String path) { return source.getDouble(path); }
    public boolean flag(String path) { return source.getBoolean(path); }
    public int integer(String path) { return source.getInt(path); }
    public boolean debug() { return source.getBoolean("debug"); }
    public Logger logger() { return logger; }
}
