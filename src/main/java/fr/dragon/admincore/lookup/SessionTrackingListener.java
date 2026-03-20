package fr.dragon.admincore.lookup;

import fr.dragon.admincore.core.AdminCorePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class SessionTrackingListener implements Listener {

    private final AdminCorePlugin plugin;

    public SessionTrackingListener(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogin(final PlayerLoginEvent event) {
        final String ip = event.getAddress() == null ? "" : event.getAddress().getHostAddress();
        this.plugin.getLookupService().recordJoin(event.getPlayer().getUniqueId(), ip, event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        this.plugin.getLookupService().recordQuit(event.getPlayer().getUniqueId());
    }
}
