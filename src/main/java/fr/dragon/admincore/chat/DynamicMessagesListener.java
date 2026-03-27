package fr.dragon.admincore.chat;

import fr.dragon.admincore.core.AdminCorePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DynamicMessagesListener implements Listener {

    private final AdminCorePlugin plugin;
    private final DynamicMessagesService service;

    public DynamicMessagesListener(final AdminCorePlugin plugin, final DynamicMessagesService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        if (!plugin.getConfigLoader().config().getBoolean("chat.enabled", true)) {
            return;
        }
        event.setJoinMessage(null);
        service.onPlayerJoin(event);
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        if (!plugin.getConfigLoader().config().getBoolean("chat.enabled", true)) {
            return;
        }
        event.setQuitMessage(null);
        service.onPlayerQuit(event);
    }

    @EventHandler
    public void onDeath(final PlayerDeathEvent event) {
        if (!plugin.getConfigLoader().config().getBoolean("chat.enabled", true)) {
            return;
        }
        event.setDeathMessage(null);
        service.onPlayerDeath(event);
    }
}