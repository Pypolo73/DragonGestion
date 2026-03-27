package fr.dragon.admincore.teleportation;

import fr.dragon.admincore.core.AdminCorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportListener implements Listener {
    private final AdminCorePlugin plugin;
    private final TeleportService service;
    private final TeleportConfig config;

    public TeleportListener(AdminCorePlugin plugin, TeleportService service, TeleportConfig config) {
        this.plugin = plugin;
        this.service = service;
        this.config = config;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        service.handlePlayerDeath(player);
        service.getLogger().logDeath(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        service.cancelTeleport(player);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            return;
        }
        
        if (service.isTeleporting(player)) {
            return;
        }
        
        if (event.getFrom().getWorld() != event.getTo().getWorld()) {
            return;
        }
        
        double distance = event.getFrom().distance(event.getTo());
        if (distance > 50) {
            service.saveBackPosition(player, event.getFrom(), "teleport");
            service.getLogger().logTeleport(player, "teleport");
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        if (config.spawn().teleportNewPlayers()) {
            if (!plugin.getServer().getOnlinePlayers().contains(player)) {
                return;
            }
            
            if (service.getSpawn() != null) {
                org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            service.teleportToSpawn(player);
                        }
                    }
                };
                runnable.runTaskLater(plugin, 5L);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
    }
}
