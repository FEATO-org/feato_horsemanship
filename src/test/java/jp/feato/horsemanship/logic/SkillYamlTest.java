package jp.feato.horsemanship.logic;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class SkillYamlTest {
    @Test void pluginCommandAllowsSkillInspection() {
        var resource = getClass().getClassLoader().getResourceAsStream("plugin.yml");
        assertNotNull(resource);
        var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
        assertNotNull(yaml.getConfigurationSection("commands.horsemanship"));
        assertFalse(yaml.contains("commands.horsemanship.permission"));
    }
    @Test void balanceConfigIncludesCruiseThresholds() {
        var resource = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(resource);
        var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
        assertEquals(30.0, yaml.getDouble("speed.cruise-first-threshold-seconds"));
        assertEquals(60.0, yaml.getDouble("speed.cruise-second-threshold-seconds"));
        assertEquals(120.0, yaml.getDouble("speed.cruise-third-threshold-seconds"));
    }
    @Test void customSkillHasProgression() {
        var resource = getClass().getClassLoader().getResourceAsStream("horsemanship.yml");
        assertNotNull(resource);
        var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
        assertTrue(yaml.getBoolean("enabled"));
        assertEquals(100, yaml.getInt("experience.max_level"));
        assertNotNull(yaml.getConfigurationSection("perks"));
        assertTrue(yaml.getConfigurationSection("perks").getKeys(false).size() >= 40);
        assertEquals(50, yaml.getInt("perks.mounted_marksman.required_lv"));
        assertTrue(yaml.getStringList("perks.mounted_marksman.other_levels_required").contains("ARCHERY:50"));
        assertTrue(yaml.getStringList("perks.first_impact.requireperk_one").contains("light_cavalry"));
        assertEquals("relentless_pace", yaml.getString("perks.breakaway.horsemanship_exclusive"));
        assertEquals("breakaway", yaml.getString("perks.relentless_pace.horsemanship_exclusive"));
        assertEquals("light_cavalry", yaml.getString("perks.heavy_cavalry.horsemanship_exclusive"));
        assertEquals("heavy_cavalry", yaml.getString("perks.light_cavalry.horsemanship_exclusive"));
        assertEquals("swift_rider", yaml.getString("perks.iron_vanguard.horsemanship_exclusive"));
        assertEquals("iron_vanguard", yaml.getString("perks.swift_rider.horsemanship_exclusive"));
        assertTrue(yaml.getStringList("perks.iron_vanguard.requireperk_all").contains("heavy_cavalry"));
    }
}
