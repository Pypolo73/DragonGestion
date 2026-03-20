package fr.dragon.admincore.inventory;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class InventoryCommand implements CommandExecutor, TabCompleter {

    private final AdminCorePlugin plugin;

    public InventoryCommand(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.player-only"));
            return true;
        }
        final boolean canView = player.hasPermission(PermissionService.INVENTORY_VIEW) || player.hasPermission(PermissionService.INVENTORY_EDIT);
        if (!canView) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-permission"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("/inventory <joueur>");
            return true;
        }
        final OfflinePlayer target = resolveTarget(args[0]);
        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-player-found"));
            return true;
        }
        this.plugin.getInventoryManagerService().openActionSelection(player, target);
        return true;
    }

    private OfflinePlayer resolveTarget(final String raw) {
        final Player online = Bukkit.getPlayerExact(raw);
        if (online != null) {
            return online;
        }
        final OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(raw);
        if (cached != null) {
            return cached;
        }
        return Arrays.stream(Bukkit.getOfflinePlayers())
            .filter(player -> player.getName() != null && player.getName().equalsIgnoreCase(raw))
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            final String query = args[0].toLowerCase(Locale.ROOT);
            return Arrays.stream(Bukkit.getOfflinePlayers())
                .map(OfflinePlayer::getName)
                .filter(name -> name != null && name.toLowerCase(Locale.ROOT).startsWith(query))
                .limit(50)
                .toList();
        }
        return List.of();
    }
}
