package fr.dragon.admincore.sanctions;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.core.StaffRole;
import fr.dragon.admincore.util.TimeParser;
import java.time.Duration;
import java.time.Instant;
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

public final class SanctionCommand implements CommandExecutor, TabCompleter {

    private final AdminCorePlugin plugin;

    public SanctionCommand(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        final String name = command.getName().toLowerCase(Locale.ROOT);
        final SanctionType type = switch (name) {
            case "tempban", "ban" -> SanctionType.BAN;
            case "tempmute", "mute" -> SanctionType.MUTE;
            case "kick" -> SanctionType.KICK;
            case "warn" -> SanctionType.WARN;
            default -> null;
        };
        if (type == null || !this.plugin.getPermissionService().check(sender, permissionFor(name))) {
            return true;
        }

        final boolean timed = name.startsWith("temp");
        if (args.length == 0) {
            if (sender instanceof Player player) {
                this.plugin.getDialogSupportService().openPlayerPicker(player, "Selection du joueur", target -> startInteractive(player, target, type, timed, name));
            } else {
                sender.sendMessage("/" + name + " <joueur> " + (timed ? "<duree> [raison]" : "[raison]"));
            }
            return true;
        }

        final OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (args.length > 1) {
            final Duration duration = timed ? TimeParser.parse(args[1]).orElse(null) : null;
            if (timed && duration == null) {
                sender.sendMessage(this.plugin.getMessageFormatter().message("errors.invalid-duration"));
                return true;
            }
            final int reasonIndex = timed ? 2 : 1;
            final String reason = args.length > reasonIndex
                ? String.join(" ", Arrays.copyOfRange(args, reasonIndex, args.length))
                : this.plugin.getConfigLoader().config().getString("sanctions.default-reason", "Aucune raison fournie");
            execute(sender, target, type, reason, duration);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("/" + name + " <joueur> " + (timed ? "<duree> [raison]" : "[raison]"));
            return true;
        }
        startInteractive(player, target, type, timed, name);
        return true;
    }

    private void startInteractive(final Player player, final OfflinePlayer target, final SanctionType type, final boolean timed, final String label) {
        final String title = switch (type) {
            case BAN -> "Bannir " + target.getName();
            case MUTE -> "Mute " + target.getName();
            case KICK -> "Kick " + target.getName();
            case WARN -> "Warn " + target.getName();
        };
        if (timed) {
            this.plugin.getDialogSupportService().openTimedReasonFlow(player, title, result ->
                execute(player, target, type, result.reason(), result.duration(), result.actorLabel())
            );
        } else {
            this.plugin.getDialogSupportService().openReasonFlow(player, title, result ->
                execute(player, target, type, result.reason(), null, result.actorLabel())
            );
        }
    }

    private void execute(final CommandSender sender, final OfflinePlayer target, final SanctionType type, final String reason, final Duration duration) {
        execute(sender, target, type, reason, duration, sender.getName());
    }

    private void execute(
        final CommandSender sender,
        final OfflinePlayer target,
        final SanctionType type,
        final String reason,
        final Duration duration,
        final String actorLabel
    ) {
        final CreateSanctionRequest request = new CreateSanctionRequest(
            target.getUniqueId(),
            target.getName() == null ? "unknown" : target.getName(),
            sender instanceof Player player ? player.getUniqueId() : null,
            actorLabel,
            type,
            reason,
            duration == null ? null : Instant.now().plus(duration),
            SanctionScope.PLAYER,
            target.getUniqueId().toString()
        );
        final StaffRole requiredApproverRole = sender instanceof Player player
            ? this.plugin.getStaffAccessService().requiredApproverRole(player, type, duration)
            : StaffRole.NONE;
        if (sender instanceof Player player && requiredApproverRole.isStaff()) {
            final PendingSanctionRequest pending = this.plugin.getSanctionApprovalService().submit(
                player.getUniqueId(),
                player.getName(),
                request,
                duration,
                requiredApproverRole
            );
            sender.sendMessage(this.plugin.getMessageFormatter().message(
                "admin.approval-submitted",
                this.plugin.getMessageFormatter().text("id", Integer.toString(pending.id())),
                this.plugin.getMessageFormatter().text("type", type.name()),
                this.plugin.getMessageFormatter().text("target", request.targetName()),
                this.plugin.getMessageFormatter().text("approverRole", requiredApproverRole.displayName())
            ));
            return;
        }
        this.plugin.getSanctionService().create(request).whenComplete((record, throwable) -> this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            if (throwable != null) {
                this.plugin.getLogger().severe("Sanction impossible: " + throwable.getMessage());
                sender.sendMessage(this.plugin.getMessageFormatter().message("errors.database"));
                return;
            }
            sender.sendMessage(this.plugin.getMessageFormatter().message(
                "sanctions.applied",
                this.plugin.getMessageFormatter().text("type", type.name()),
                this.plugin.getMessageFormatter().text("target", record.targetName()),
                this.plugin.getMessageFormatter().text("reason", record.reason()),
                this.plugin.getMessageFormatter().text("duration", duration == null ? "Permanent" : TimeParser.format(duration))
            ));
            final Player onlineTarget = Bukkit.getPlayer(record.targetUuid());
            if (onlineTarget == null) {
                return;
            }
            this.plugin.getStaffAccessService().refresh(onlineTarget);
            if (type == SanctionType.BAN) {
                onlineTarget.kick(SanctionVisuals.banScreen(
                    this.plugin.getConfigLoader(),
                    this.plugin.getMessageFormatter(),
                    record,
                    this.plugin.getMessageFormatter().raw("discord.link", "discord.gg/example")
                ));
            } else if (type == SanctionType.KICK) {
                onlineTarget.kick(this.plugin.getMessageFormatter().message(
                    "sanctions.kick-screen",
                    this.plugin.getMessageFormatter().text("reason", record.reason()),
                    this.plugin.getMessageFormatter().text("actor", record.actorName())
                ));
            } else if (type == SanctionType.MUTE) {
                onlineTarget.sendActionBar(this.plugin.getMessageFormatter().message("sanctions.mute-actionbar"));
                onlineTarget.sendMessage(SanctionVisuals.muteMessage(
                    this.plugin.getMessageFormatter(),
                    record,
                    this.plugin.getMessageFormatter().raw("discord.link", "discord.gg/example")
                ));
            } else if (type == SanctionType.WARN) {
                onlineTarget.sendMessage(this.plugin.getMessageFormatter().message(
                    "sanctions.warn-notify",
                    this.plugin.getMessageFormatter().text("reason", record.reason()),
                    this.plugin.getMessageFormatter().text("actor", record.actorName())
                ));
            }
        }));
    }

    private String permissionFor(final String name) {
        return switch (name) {
            case "tempban" -> PermissionService.TEMPBAN;
            case "tempmute" -> PermissionService.TEMPMUTE;
            case "ban" -> PermissionService.BAN;
            case "mute" -> PermissionService.MUTE;
            case "kick" -> PermissionService.KICK;
            case "warn" -> PermissionService.WARN;
            default -> PermissionService.ADMIN;
        };
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && command.getName().toLowerCase(Locale.ROOT).startsWith("temp")) {
            return this.plugin.getConfigLoader().config().getStringList("sanctions.presets.durations").stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                .toList();
        }
        final int reasonIndex = command.getName().toLowerCase(Locale.ROOT).startsWith("temp") ? 3 : 2;
        if (args.length == reasonIndex) {
            return this.plugin.getConfigLoader().config().getStringList("sanctions.presets.reasons").stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[reasonIndex - 1].toLowerCase(Locale.ROOT)))
                .toList();
        }
        return List.of();
    }
}
