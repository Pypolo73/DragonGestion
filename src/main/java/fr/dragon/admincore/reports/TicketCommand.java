package fr.dragon.admincore.reports;

import fr.dragon.admincore.core.AdminCorePlugin;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class TicketCommand implements CommandExecutor, TabCompleter {

    private final AdminCorePlugin plugin;

    public TicketCommand(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.player-only"));
            return true;
        }
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            this.plugin.getTicketDialogService().openPlayerTickets(player, 0);
            return true;
        }
        if ("reply".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                player.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>Usage: /ticket reply <id></red>"));
                return true;
            }
            final long ticketId = parseId(player, args[1]);
            if (ticketId < 0L) {
                return true;
            }
            this.plugin.getTicketDialogService().openPlayerReply(player, ticketId);
            return true;
        }
        player.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>Usage: /ticket <list|reply></red>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            return List.of("list", "reply").stream()
                .filter(entry -> entry.startsWith(args[0].toLowerCase(Locale.ROOT)))
                .toList();
        }
        return List.of();
    }

    private long parseId(final Player player, final String raw) {
        try {
            return Long.parseLong(raw);
        } catch (final NumberFormatException exception) {
            player.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>ID de ticket invalide.</red>"));
            return -1L;
        }
    }
}
