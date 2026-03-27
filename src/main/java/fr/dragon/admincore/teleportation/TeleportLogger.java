package fr.dragon.admincore.teleportation;

import fr.dragon.admincore.core.AdminCorePlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportLogger {

    private final AdminCorePlugin plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private final Map<UUID, List<DeathPosition>> deathPositions = new ConcurrentHashMap<>();
    private int maxDeathPositions = 10;

    public TeleportLogger(@NotNull AdminCorePlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadDeathPositions();
    }

    private void loadConfig() {
        var config = plugin.getConfigLoader().config();
        if (config != null) {
            maxDeathPositions = config.getInt("teleportation.death-positions.max-saved", 10);
        }
    }

    private void loadDeathPositions() {
        File logsDir = getLogsDirectory();
        if (!logsDir.exists()) {
            return;
        }

        File[] files = logsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                org.bukkit.configuration.file.YamlConfiguration yaml = 
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
                
                if (yaml.contains("deaths")) {
                    for (String key : yaml.getConfigurationSection("deaths").getKeys(false)) {
                        String playerName = yaml.getString("deaths." + key + ".player", "");
                        int x = yaml.getInt("deaths." + key + ".x", 0);
                        int y = yaml.getInt("deaths." + key + ".y", 0);
                        int z = yaml.getInt("deaths." + key + ".z", 0);
                        String world = yaml.getString("deaths." + key + ".world", "world");
                        String time = yaml.getString("deaths." + key + ".time", "");
                        
                        DeathPosition pos = new DeathPosition(playerName, world, x, y, z, time);
                        
                        for (Player online : plugin.getServer().getOnlinePlayers()) {
                            if (online.getName().equalsIgnoreCase(playerName)) {
                                deathPositions.computeIfAbsent(online.getUniqueId(), k -> new ArrayList<>()).add(pos);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Erreur lors du chargement des logs: " + file.getName());
            }
        }
    }

    private File getLogsDirectory() {
        File dir = new File(plugin.getDataFolder(), "admincore/teleportation/logs");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public void logDeath(Player player) {
        Location loc = player.getLocation();
        String date = dateFormat.format(new Date());
        String time = timeFormat.format(new Date());

        DeathPosition deathPos = new DeathPosition(
            player.getName(),
            loc.getWorld().getName(),
            loc.getBlockX(),
            loc.getBlockY(),
            loc.getBlockZ(),
            time
        );

        deathPositions.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
            .add(0, deathPos);

        while (deathPositions.get(player.getUniqueId()).size() > maxDeathPositions) {
            deathPositions.get(player.getUniqueId()).remove(deathPositions.get(player.getUniqueId()).size() - 1);
        }

        saveDeathToFile(player.getName(), deathPos, date);
    }

    private void saveDeathToFile(String playerName, DeathPosition death, String date) {
        File logsDir = getLogsDirectory();
        File logFile = new File(logsDir, date + ".yml");

        org.bukkit.configuration.file.YamlConfiguration yaml;
        if (logFile.exists()) {
            yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(logFile);
        } else {
            yaml = new org.bukkit.configuration.file.YamlConfiguration();
        }

        String path = "deaths." + System.currentTimeMillis();
        yaml.set(path + ".player", playerName);
        yaml.set(path + ".type", "death");
        yaml.set(path + ".world", death.world());
        yaml.set(path + ".x", death.x());
        yaml.set(path + ".y", death.y());
        yaml.set(path + ".z", death.z());
        yaml.set(path + ".time", death.time());

        try {
            yaml.save(logFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Erreur lors de la sauvegarde du log: " + e.getMessage());
        }
    }

    public void logTeleport(Player player, String type) {
        Location loc = player.getLocation();
        String date = dateFormat.format(new Date());
        String time = timeFormat.format(new Date());

        saveTeleportToFile(player.getName(), type, loc, date, time);
    }

    private void saveTeleportToFile(String playerName, String type, Location loc, String date, String time) {
        File logsDir = getLogsDirectory();
        File logFile = new File(logsDir, date + ".yml");

        org.bukkit.configuration.file.YamlConfiguration yaml;
        if (logFile.exists()) {
            yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(logFile);
        } else {
            yaml = new org.bukkit.configuration.file.YamlConfiguration();
        }

        String path = "teleportations." + System.currentTimeMillis();
        yaml.set(path + ".player", playerName);
        yaml.set(path + ".type", type);
        yaml.set(path + ".world", loc.getWorld().getName());
        yaml.set(path + ".x", loc.getBlockX());
        yaml.set(path + ".y", loc.getBlockY());
        yaml.set(path + ".z", loc.getBlockZ());
        yaml.set(path + ".time", time);

        try {
            yaml.save(logFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Erreur lors de la sauvegarde du log: " + e.getMessage());
        }
    }

    public List<DeathPosition> getDeathPositions(Player player) {
        return deathPositions.getOrDefault(player.getUniqueId(), new ArrayList<>());
    }

    public int getMaxDeathPositions() {
        return maxDeathPositions;
    }

    public void setMaxDeathPositions(int max) {
        this.maxDeathPositions = max;
    }

    public record DeathPosition(
        String player,
        String world,
        int x,
        int y,
        int z,
        String time
    ) {
        public String getLocationString() {
            return world + " x:" + x + " y:" + y + " z:" + z;
        }
    }
}
