package fr.dragon.admincore.staffmode;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class StaffCommand implements CommandExecutor, TabCompleter {

    private final AdminCorePlugin plugin;

    public StaffCommand(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        final String name = command.getName().toLowerCase(Locale.ROOT);
        if (!this.plugin.getPermissionService().check(sender, permission(name))) {
            return true;
        }
        return switch (name) {
            case "staffmode" -> toggleStaffMode(sender);
            case "freeze" -> freeze(sender, args);
            case "spy" -> spy(sender);
            case "invsee" -> invsee(sender, args);
            case "stafflist" -> staffList(sender);
            default -> true;
        };
    }

    private boolean toggleStaffMode(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.player-only"));
            return true;
        }
        final boolean enabled = this.plugin.getStaffModeService().toggleStaffMode(player);
        sender.sendMessage(this.plugin.getMessageFormatter().message(enabled ? "staffmode.enabled" : "staffmode.disabled"));
        return true;
    }

    private boolean freeze(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/freeze <joueur>");
            return true;
        }
        final Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-player-found"));
            return true;
        }
        final boolean frozen = this.plugin.getStaffModeService().toggleFreeze(target);
        sender.sendMessage(this.plugin.getMessageFormatter().message(
            frozen ? "staffmode.freeze-enabled" : "staffmode.freeze-disabled",
            this.plugin.getMessageFormatter().text("target", target.getName())
        ));
        return true;
    }

    private boolean spy(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.player-only"));
            return true;
        }
        final boolean enabled = this.plugin.getStaffModeService().toggleSpy(player);
        this.plugin.getChatService().getSpyEnabled().add(player.getUniqueId());
        if (!enabled) {
            this.plugin.getChatService().getSpyEnabled().remove(player.getUniqueId());
        }
        sender.sendMessage(this.plugin.getMessageFormatter().message(enabled ? "staffmode.spy-enabled" : "staffmode.spy-disabled"));
        return true;
    }

    private boolean invsee(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.player-only"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/invsee <joueur>");
            return true;
        }
        final Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-player-found"));
            return true;
        }
        player.openInventory(target.getInventory());
        return true;
    }

    private boolean staffList(final CommandSender sender) {
        final String online = Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.hasPermission(PermissionService.STAFF) || this.plugin.getStaffModeService().isInStaffMode(player.getUniqueId()))
            .map(Player::getName)
            .reduce((left, right) -> left + ", " + right)
            .orElse("Aucun");
        sender.sendMessage(this.plugin.getMessageFormatter().message("staffmode.stafflist", this.plugin.getMessageFormatter().text("players", online)));
        return true;
    }

    private String permission(final String name) {
        return switch (name) {
            case "staffmode" -> PermissionService.STAFFMODE;
            case "freeze" -> PermissionService.FREEZE;
            case "spy" -> PermissionService.SPY;
            case "invsee" -> PermissionService.INVSEE;
            case "stafflist" -> PermissionService.STAFFLIST;
            default -> PermissionService.ADMIN;
        };
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
