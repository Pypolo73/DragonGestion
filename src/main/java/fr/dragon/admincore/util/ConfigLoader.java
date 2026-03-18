package fr.dragon.admincore.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigLoader {

    private final JavaPlugin plugin;
    private FileConfiguration messages;

    public ConfigLoader(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.plugin.saveDefaultConfig();
        this.plugin.reloadConfig();
        this.messages = loadConfiguration("messages.yml");
    }

    public FileConfiguration config() {
        return this.plugin.getConfig();
    }

    public FileConfiguration messages() {
        return this.messages;
    }

    private FileConfiguration loadConfiguration(final String fileName) {
        final File file = new File(this.plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            this.plugin.saveResource(fileName, false);
        }
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        try {
            configuration.loadFromString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        } catch (final IOException exception) {
            this.plugin.getLogger().warning("Lecture impossible pour " + fileName + ": " + exception.getMessage());
        } catch (final Exception exception) {
            this.plugin.getLogger().warning("Chargement impossible pour " + fileName + ": " + exception.getMessage());
        }
        return configuration;
    }
}
