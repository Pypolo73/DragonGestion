package fr.dragon.admincore.chat;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.gui.ChatHistoryMenu;
import java.util.Arrays;
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
        if ("chat".equals(name)) {
            return rootChat(sender, args);
        }
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

    private boolean rootChat(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/chat <clear|slowmode|lock|muteall|broadcast>");
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "clear" -> {
                if (!this.plugin.getPermissionService().check(sender, PermissionService.CLEARCHAT)) {
                    yield true;
                }
                yield clearChat(sender, Arrays.copyOfRange(args, 1, args.length));
            }
            case "slowmode" -> {
                if (!this.plugin.getPermissionService().check(sender, PermissionService.SLOWMODE)) {
                    yield true;
                }
                yield slowMode(sender, Arrays.copyOfRange(args, 1, args.length));
            }
            case "lock", "chatlock" -> {
                if (!this.plugin.getPermissionService().check(sender, PermissionService.CHATLOCK)) {
                    yield true;
                }
                yield chatLock(sender);
            }
            case "muteall" -> {
                if (!this.plugin.getPermissionService().check(sender, PermissionService.MUTEALL)) {
                    yield true;
                }
                yield muteAll(sender);
            }
            case "broadcast", "bc" -> {
                if (!this.plugin.getPermissionService().check(sender, PermissionService.BROADCAST)) {
                    yield true;
                }
                yield broadcast(sender, Arrays.copyOfRange(args, 1, args.length));
            }
            default -> {
                sender.sendMessage("/chat <clear|slowmode|lock|muteall|broadcast>");
                yield true;
            }
        };
    }

    private boolean clearChat(final CommandSender sender, final String[] args) {
        if (args.length == 0 && sender instanceof Player player) {
            this.plugin.getDialogSupportService().openClearChatFlow(player, this.plugin.getChatService()::clearChat);
            return true;
        }
        if (args.length >= 1 && ("message".equalsIgnoreCase(args[0]) || "messages".equalsIgnoreCase(args[0]) || "history".equalsIgnoreCase(args[0]))) {
            return openHistory(sender);
        }
        if (args.length >= 1 && "player".equalsIgnoreCase(args[0])) {
            return clearPlayer(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        final String type = args.length == 0 ? "all" : args[0].toLowerCase(Locale.ROOT);
        final ClearChatSelection selection = new ClearChatSelection(
            "all".equals(type) || "public".equals(type) || "chatall".equals(type),
            "all".equals(type) || "players".equals(type),
            "logs".equals(type),
            "all".equals(type) || "system".equals(type),
            args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : ""
        );
        this.plugin.getChatService().clearChat(selection);
        sender.sendMessage(this.plugin.getMessageFormatter().message("chat.clear-done"));
        return true;
    }

    private boolean clearPlayer(final CommandSender sender, final String[] args) {
        if (args.length == 0 && sender instanceof Player player) {
            this.plugin.getDialogSupportService().openPlayerPicker(player, "Choisir le joueur a nettoyer", target -> {
                this.plugin.getChatService().clearChatFor(target, "Chat nettoye par le staff");
                player.sendMessage(this.plugin.getMessageFormatter().message(
                    "chat.clear-target-done",
                    this.plugin.getMessageFormatter().text("target", target.getName())
                ));
            });
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/chat clear player <joueur>");
            return true;
        }
        final Player target = this.plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-player-found"));
            return true;
        }
        this.plugin.getChatService().clearChatFor(target, "Chat nettoye par le staff");
        sender.sendMessage(this.plugin.getMessageFormatter().message(
            "chat.clear-target-done",
            this.plugin.getMessageFormatter().text("target", target.getName())
        ));
        return true;
    }

    private boolean openHistory(final CommandSender sender) {
        final int limit = Math.max(1, this.plugin.getConfigLoader().config().getInt(
            "chat.clear-message-history-limit",
            this.plugin.getConfigLoader().config().getInt("chat.history-limit", 100)
        ));
        final List<ChatHistoryEntry> entries = this.plugin.getChatService().recentMessages(limit);
        if (entries.isEmpty()) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("chat.history-empty"));
            return true;
        }
        if (sender instanceof Player player) {
            ChatHistoryMenu.open(player, 0, entries);
            return true;
        }
        entries.stream().limit(20).forEach(entry -> sender.sendMessage("[" + entry.sentAt() + "] " + entry.author() + ": " + entry.message()));
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
        if ("chat".equalsIgnoreCase(command.getName())) {
            if (args.length == 1) {
                return List.of("clear", "slowmode", "lock", "muteall", "broadcast");
            }
            if (args.length == 2 && "clear".equalsIgnoreCase(args[0])) {
                return List.of("all", "chatall", "player", "messages");
            }
            if (args.length == 3 && "clear".equalsIgnoreCase(args[0]) && "player".equalsIgnoreCase(args[1])) {
                return this.plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
            }
            if (args.length == 2 && "slowmode".equalsIgnoreCase(args[0])) {
                return List.of("off", "5", "10", "30", "60");
            }
            return List.of();
        }
        if ("clearchat".equalsIgnoreCase(command.getName()) && args.length == 1) {
            return List.of("all", "chatall", "public", "players", "player", "messages", "logs", "system");
        }
        if ("slowmode".equalsIgnoreCase(command.getName()) && args.length == 1) {
            return List.of("off", "5", "10", "30", "60");
        }
        return List.of();
    }
}
