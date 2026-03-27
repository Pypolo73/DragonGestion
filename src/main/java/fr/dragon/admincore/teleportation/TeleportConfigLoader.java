package fr.dragon.admincore.teleportation;

import fr.dragon.admincore.core.AdminCorePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class TeleportConfigLoader {
    private final AdminCorePlugin plugin;
    private File configFile;
    private FileConfiguration config;
    private TeleportConfig teleportConfig;

    public TeleportConfigLoader(AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File teleportationFolder = new File(plugin.getDataFolder(), "teleportation");
        if (!teleportationFolder.exists()) {
            teleportationFolder.mkdirs();
        }

        configFile = new File(teleportationFolder, "config.yml");
        if (!configFile.exists()) {
            try {
                plugin.saveResource("teleportation/config.yml", false);
                plugin.getLogger().info("Generated teleportation config.yml");
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save teleportation config: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        
        File messageFile = new File(teleportationFolder, "messages.yml");
        if (!messageFile.exists()) {
            try {
                plugin.saveResource("teleportation/messages.yml", false);
                plugin.getLogger().info("Generated teleportation messages.yml");
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save teleportation messages: " + e.getMessage());
            }
        }

        teleportConfig = loadTeleportConfig();
    }

    @SuppressWarnings("unchecked")
    private TeleportConfig loadTeleportConfig() {
        Map<String, Object> homeSection = config.getConfigurationSection("home") != null ? 
            config.getConfigurationSection("home").getValues(false) : Map.of();
        Map<String, Object> warpSection = config.getConfigurationSection("warp") != null ? 
            config.getConfigurationSection("warp").getValues(false) : Map.of();
        Map<String, Object> spawnSection = config.getConfigurationSection("spawn") != null ? 
            config.getConfigurationSection("spawn").getValues(false) : Map.of();
        Map<String, Object> rtpSection = config.getConfigurationSection("rtp") != null ? 
            config.getConfigurationSection("rtp").getValues(false) : Map.of();
        Map<String, Object> tpaSection = config.getConfigurationSection("tpa") != null ? 
            config.getConfigurationSection("tpa").getValues(false) : Map.of();
        Map<String, Object> backSection = config.getConfigurationSection("back") != null ? 
            config.getConfigurationSection("back").getValues(false) : Map.of();
        Map<String, Object> dbSection = config.getConfigurationSection("database") != null ? 
            config.getConfigurationSection("database").getValues(false) : Map.of();

        return new TeleportConfig(
            new TeleportConfig.HomeConfig(
                getInt(homeSection, "max-homes", 3),
                getInt(homeSection, "warmup", 3),
                getInt(homeSection, "cooldown", 0),
                getString(homeSection, "default-name", "home")
            ),
            new TeleportConfig.WarpConfig(
                getInt(warpSection, "max-warps", 50),
                getInt(warpSection, "warmup", 3),
                getInt(warpSection, "cooldown", 0)
            ),
            new TeleportConfig.SpawnConfig(
                getInt(spawnSection, "warmup", 3),
                getBool(spawnSection, "set-as-default-spawn", true),
                getBool(spawnSection, "teleport-new-players", true)
            ),
            new TeleportConfig.RtpConfig(
                getBool(rtpSection, "enabled", true),
                getInt(rtpSection, "max-attempts", 50),
                getInt(rtpSection, "min-distance", 100),
                getInt(rtpSection, "max-distance", 5000),
                getInt(rtpSection, "min-height", 60),
                getStringList(rtpSection, "allowed-worlds"),
                getStringList(rtpSection, "restricted-worlds"),
                getInt(rtpSection, "cooldown", 60),
                getInt(rtpSection, "warmup", 5)
            ),
            new TeleportConfig.TpaConfig(
                getInt(tpaSection, "request-timeout", 120),
                getInt(tpaSection, "warmup", 3),
                getBool(tpaSection, "allow-ignore", true)
            ),
            new TeleportConfig.BackConfig(
                getBool(backSection, "enabled", true),
                getBool(backSection, "save-on-death", true),
                getBool(backSection, "save-on-teleport", true),
                getStringList(backSection, "restricted-worlds")
            ),
            new TeleportConfig.DatabaseConfig(
                getString(dbSection, "type", "sqlite"),
                dbSection.containsKey("mysql") ? (Map<String, Object>) dbSection.get("mysql") : Map.of(),
                dbSection.containsKey("sqlite") ? (Map<String, Object>) dbSection.get("sqlite") : Map.of()
            )
        );
    }

    private int getInt(Map<String, Object> section, String key, int defaultValue) {
        Object value = section.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private boolean getBool(Map<String, Object> section, String key, boolean defaultValue) {
        Object value = section.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    private String getString(Map<String, Object> section, String key, String defaultValue) {
        Object value = section.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> section, String key) {
        Object value = section.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return List.of();
    }

    public TeleportConfig getConfig() {
        return teleportConfig;
    }

    public void reload() {
        load();
    }
}
