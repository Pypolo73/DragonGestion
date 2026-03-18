package fr.dragon.admincore.sanctions;

import fr.dragon.admincore.core.AdminCorePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ConnectionSanctionListener implements Listener {

    private final AdminCorePlugin plugin;

    public ConnectionSanctionListener(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAsyncPreLogin(final AsyncPlayerPreLoginEvent event) {
        final String ip = event.getAddress().getHostAddress();
        final var activeBan = this.plugin.getSanctionService().findActiveBan(event.getUniqueId(), event.getName(), ip).join();
        activeBan.ifPresent(record -> event.disallow(
            AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
            SanctionVisuals.banScreen(
                this.plugin.getMessageFormatter(),
                record,
                this.plugin.getMessageFormatter().raw("discord.link", "discord.gg/example")
            )
        ));
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final String ip = event.getPlayer().getAddress() == null ? "" : event.getPlayer().getAddress().getAddress().getHostAddress();
        this.plugin.getPlayerSessionManager().updateName(event.getPlayer());
        this.plugin.getVanishService().refreshVisibility(event.getPlayer());
        this.plugin.getSanctionService().recordPlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName(), ip);
        this.plugin.getSanctionService().findActiveMute(event.getPlayer().getUniqueId(), event.getPlayer().getName(), ip).thenAccept(activeMute ->
            activeMute.ifPresent(record -> this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                event.getPlayer().sendActionBar(this.plugin.getMessageFormatter().message("sanctions.mute-actionbar"));
                event.getPlayer().sendMessage(SanctionVisuals.muteMessage(
                    this.plugin.getMessageFormatter(),
                    record,
                    this.plugin.getMessageFormatter().raw("discord.link", "discord.gg/example")
                ));
            }))
        );
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        if (this.plugin.getVanishService().isVanished(event.getPlayer().getUniqueId())) {
            this.plugin.getVanishService().setVanished(event.getPlayer(), false);
        }
        this.plugin.getPlayerSessionManager().remove(event.getPlayer().getUniqueId());
    }
}
