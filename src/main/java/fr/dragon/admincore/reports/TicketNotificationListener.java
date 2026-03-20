package fr.dragon.admincore.reports;

import fr.dragon.admincore.core.AdminCorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class TicketNotificationListener implements Listener {

    private static final int NOTIFICATION_LIMIT = 5;

    private final AdminCorePlugin plugin;

    public TicketNotificationListener(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.plugin.getReportService().activeTicketsForPlayer(player.getUniqueId(), NOTIFICATION_LIMIT).thenAccept(tickets ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (tickets.isEmpty()) {
                    return;
                }
                if (tickets.size() == 1) {
                    final TicketRecord ticket = tickets.getFirst();
                    player.sendMessage(this.plugin.getMessageFormatter().deserialize(
                        "<prefix><gold>Tu as un ticket actif (#<id>) a consulter.</gold> <aqua><click:run_command:'/ticket reply <id>'>[Ouvrir]</click></aqua>",
                        this.plugin.getMessageFormatter().text("id", Long.toString(ticket.id()))
                    ));
                } else {
                    player.sendMessage(this.plugin.getMessageFormatter().deserialize(
                        "<prefix><gold>Tu as <count> tickets actifs a consulter.</gold> <aqua><click:run_command:'/ticket list'>[Voir la liste]</click></aqua>",
                        this.plugin.getMessageFormatter().text("count", Integer.toString(tickets.size()))
                    ));
                }
                player.sendMessage(this.plugin.getMessageFormatter().message("reports.discord-priority"));
            })
        ).exceptionally(throwable -> null);
    }
}
