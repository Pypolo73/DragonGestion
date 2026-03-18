package fr.dragon.admincore.chat;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class ChatCommand implements CommandExecutor, TabCompleter {

    private final AdminCorePlugin plugin;

    public ChatCommand(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        final String name = command.getName().toLowerCase(Locale.ROOT);
        if (!this.plugin.getPermissionService().check(sender, permission(name))) {
            return true;
        }
        return switch (name) {
            case "clearchat" -> clearChat(sender, args);
            case "slowmode" -> slowMode(sender, args);
            case "chatlock" -> chatLock(sender);
            case "muteall" -> muteAll(sender);
            case "broadcast" -> broadcast(sender, args);
            default -> true;
        };
    }

    private boolean clearChat(final CommandSender sender, final String[] args) {
        if (args.length == 0 && sender instanceof Player player) {
            this.plugin.getDialogSupportService().openClearChatFlow(player, this.plugin.getChatService()::clearChat);
            return true;
        }
        final String type = args.length == 0 ? "all" : args[0].toLowerCase(Locale.ROOT);
        final ClearChatSelection selection = new ClearChatSelection(
            "all".equals(type) || "public".equals(type),
            "all".equals(type) || "players".equals(type),
            "logs".equals(type),
            "all".equals(type) || "system".equals(type),
            args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : ""
        );
        this.plugin.getChatService().clearChat(selection);
        sender.sendMessage(this.plugin.getMessageFormatter().message("chat.clear-done"));
        return true;
    }

    private boolean slowMode(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage(this.plugin.getMessageFormatter().message(
                "chat.slowmode-status",
                this.plugin.getMessageFormatter().text("seconds", Integer.toString(this.plugin.getChatService().getSlowModeSeconds()))
            ));
            return true;
        }
        if ("off".equalsIgnoreCase(args[0])) {
            this.plugin.getChatService().setSlowMode(0);
        } else {
            this.plugin.getChatService().setSlowMode(Integer.parseInt(args[0]));
        }
        sender.sendMessage(this.plugin.getMessageFormatter().message(
            "chat.slowmode-updated",
            this.plugin.getMessageFormatter().text("seconds", Integer.toString(this.plugin.getChatService().getSlowModeSeconds()))
        ));
        return true;
    }

    private boolean chatLock(final CommandSender sender) {
        final boolean enabled = this.plugin.getChatService().toggleChatLock();
        sender.sendMessage(this.plugin.getMessageFormatter().message(enabled ? "chat.lock-enabled" : "chat.lock-disabled"));
        return true;
    }

    private boolean muteAll(final CommandSender sender) {
        final boolean enabled = this.plugin.getChatService().toggleMuteAll();
        sender.sendMessage(this.plugin.getMessageFormatter().message(enabled ? "chat.muteall-enabled" : "chat.muteall-disabled"));
        return true;
    }

    private boolean broadcast(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/broadcast <message>");
            return true;
        }
        this.plugin.getChatService().broadcast(String.join(" ", args));
        return true;
    }

    private String permission(final String name) {
        return switch (name) {
            case "clearchat" -> PermissionService.CLEARCHAT;
            case "slowmode" -> PermissionService.SLOWMODE;
            case "chatlock" -> PermissionService.CHATLOCK;
            case "muteall" -> PermissionService.MUTEALL;
            case "broadcast" -> PermissionService.BROADCAST;
            default -> PermissionService.ADMIN;
        };
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if ("clearchat".equalsIgnoreCase(command.getName()) && args.length == 1) {
            return List.of("all", "public", "players", "logs", "system");
        }
        if ("slowmode".equalsIgnoreCase(command.getName()) && args.length == 1) {
            return List.of("off", "5", "10", "30", "60");
        }
        return List.of();
    }
}
