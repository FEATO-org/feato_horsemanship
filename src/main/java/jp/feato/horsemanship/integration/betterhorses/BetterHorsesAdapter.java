package jp.feato.horsemanship.integration.betterhorses;

import jp.feato.horsemanship.logic.BalanceMath;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** BetterHorses 6.3 PDC and its internal TrainingManager are confined to this class. */
public final class BetterHorsesAdapter {
    public enum Category { BRUSHING, FEEDING }
    private static final Set<String> RESTRICTED_TRAITS = Set.of("hellmare", "fireheart", "featherhooves", "dashboost", "kickback", "ghosthorse", "heavenhooves", "undead", "frosthooves", "skyburst", "revenantcurse");
    private final Plugin plugin;
    private final Method addBrushing;
    private final Method addFeeding;
    public BetterHorsesAdapter(Plugin plugin) throws ReflectiveOperationException {
        this.plugin = plugin;
        Class<?> training = plugin.getClass().getClassLoader().loadClass("me.luisgamedev.betterhorses.training.TrainingManager");
        addBrushing = training.getMethod("addBrushingUnits", AbstractHorse.class, double.class);
        addFeeding = training.getMethod("addFeedingUnits", AbstractHorse.class, double.class);
    }
    public Material brushMaterial() {
        Material configured = Material.matchMaterial(plugin.getConfig().getString("training.categories.brushing.item", "BRUSH"));
        return configured == null ? Material.BRUSH : configured;
    }
    private NamespacedKey key(String name) { return new NamespacedKey(plugin, name); }
    public boolean isBetterHorse(AbstractHorse horse) {
        var data = horse.getPersistentDataContainer();
        return data.has(key("mount_type"), PersistentDataType.STRING) ||
            (data.has(key("health"), PersistentDataType.DOUBLE) && data.has(key("speed"), PersistentDataType.DOUBLE) && data.has(key("jump"), PersistentDataType.DOUBLE));
    }
    public double units(AbstractHorse horse, Category category) {
        String name = category == Category.BRUSHING ? "training_brushing_units" : "training_feeding_units";
        return horse.getPersistentDataContainer().getOrDefault(key(name), PersistentDataType.DOUBLE, 0.0);
    }
    public double progress(AbstractHorse horse, String category) {
        double units = category.equals("riding") ? ridingUnits(horse) : units(horse, category.equals("brushing") ? Category.BRUSHING : Category.FEEDING);
        double perPercent = Math.max(0.0001, plugin.getConfig().getDouble("training.categories." + category + ".units-per-percent", 10.0));
        return Math.max(0, Math.min(100, units / perPercent));
    }
    public boolean trainingEnabled(String category) {
        return trainingEnabled(plugin.getConfig(), category);
    }
    public static boolean trainingEnabled(FileConfiguration source, String category) {
        return source.getBoolean("training.enabled", false) &&
            source.getBoolean("training.categories." + category + ".enabled", true);
    }
    public String progressLabel(AbstractHorse horse, String category) {
        return progressLabel(trainingEnabled(category), progress(horse, category));
    }
    public static String progressLabel(boolean enabled, double progress) {
        return enabled ? String.format(Locale.ROOT, "%.1f%%", progress) : "無効";
    }
    public double ridingUnits(AbstractHorse horse) { return horse.getPersistentDataContainer().getOrDefault(key("training_riding_units"), PersistentDataType.DOUBLE, 0.0); }
    public void addTrainingBonus(AbstractHorse horse, Category category, double actualGain, double multiplier) {
        if (actualGain <= 0 || multiplier <= 0 || !isBetterHorse(horse)) return;
        double bonus = actualGain * multiplier;
        Method method = category == Category.BRUSHING ? addBrushing : addFeeding;
        try { method.invoke(null, horse, bonus); }
        catch (IllegalAccessException | InvocationTargetException exception) {
            plugin.getLogger().warning("Training assistance failed: " + exception.getMessage());
        }
    }
    public Snapshot snapshot(AbstractHorse horse) {
        return new Snapshot(horse.getUniqueId(), base(horse, Attribute.MAX_HEALTH), base(horse, Attribute.MOVEMENT_SPEED),
                base(horse, Attribute.JUMP_STRENGTH), string(horse, "gender").orElse("unknown"), string(horse, "trait").orElse("none"),
                ridingUnits(horse), units(horse, Category.BRUSHING), units(horse, Category.FEEDING));
    }
    private double base(AbstractHorse horse, Attribute attribute) {
        AttributeInstance instance = horse.getAttribute(attribute);
        return instance == null ? 0.0 : instance.getBaseValue();
    }
    private Optional<String> string(AbstractHorse horse, String name) {
        return Optional.ofNullable(horse.getPersistentDataContainer().get(key(name), PersistentDataType.STRING));
    }
    public long cooldownRemaining(AbstractHorse horse, long now) {
        long last = horse.getPersistentDataContainer().getOrDefault(key("cooldown"), PersistentDataType.LONG, 0L);
        long duration = Math.max(0, plugin.getConfig().getLong("settings.breeding-cooldown")) * 1000;
        return Math.max(0, last + duration - now);
    }
    public BalanceMath.Range range(AbstractHorse father, AbstractHorse mother, String stat) {
        FileConfiguration config = plugin.getConfig();
        String mountKey = mountKey(father.getType());
        double mutation = config.contains("mutation-factor." + mountKey + "." + stat) ?
                config.getDouble("mutation-factor." + mountKey + "." + stat) : config.getDouble("mutation-factor." + stat);
        double maximum = config.contains("max-stats." + mountKey + "." + stat) ?
                config.getDouble("max-stats." + mountKey + "." + stat) : config.getDouble("max-stats." + stat);
        Attribute attribute = switch (stat) {
            case "health" -> Attribute.MAX_HEALTH;
            case "speed" -> Attribute.MOVEMENT_SPEED;
            case "jump" -> Attribute.JUMP_STRENGTH;
            default -> throw new IllegalArgumentException("Unknown stat " + stat);
        };
        return BalanceMath.breedingRange(base(father, attribute), base(mother, attribute), mutation, maximum);
    }
    public Map<String, Double> traitProbabilities(Player breeder) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("traits.enabled")) return Map.of("none", 1.0);
        ConfigurationSection traits = config.getConfigurationSection("traits");
        if (traits == null) return Map.of("none", 1.0);
        Map<String, Double> chances = new LinkedHashMap<>();
        for (String name : traits.getKeys(false)) {
            if (name.equals("enabled")) continue;
            ConfigurationSection section = traits.getConfigurationSection(name);
            if (section != null && section.getBoolean("enabled")) chances.put(name.toLowerCase(), section.getDouble("chance"));
        }
        return BalanceMath.sequentialTraitProbabilities(chances, name ->
            breeder.hasPermission("betterhorses.trait.receive") &&
            (!RESTRICTED_TRAITS.contains(normalizeTrait(name)) || breeder.hasPermission("betterhorses.trait." + normalizeTrait(name))));
    }
    private static String normalizeTrait(String name) {
        return name.replace("_", "").replace("-", "").replace(" ", "");
    }
    private static String mountKey(EntityType type) {
        return switch (type) {
            case SKELETON_HORSE -> "skeleton-horses";
            case ZOMBIE_HORSE -> "zombie-horses";
            case DONKEY -> "donkeys";
            case MULE -> "mules";
            default -> "horse";
        };
    }
    public record Snapshot(UUID horse, double health, double speed, double jump, String gender, String trait,
                           double ridingUnits, double brushingUnits, double feedingUnits) {}
}
