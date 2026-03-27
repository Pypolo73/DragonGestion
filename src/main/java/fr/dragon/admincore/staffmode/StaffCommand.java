package fr.dragon.admincore.staffmode;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.core.StaffAssignmentResult;
import fr.dragon.admincore.core.StaffActionType;
import fr.dragon.admincore.core.StaffRole;
import fr.dragon.admincore.dialog.PlayerSearchDialog;
import fr.dragon.admincore.dialog.StaffAssignmentDialog;
import fr.dragon.admincore.dialog.StaffDashboardDialog;
import fr.dragon.admincore.dialog.StaffStatsDialog;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
        if ("staff".equals(name)) {
            return staffRoot(sender, args);
        }
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

    private boolean staffRoot(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.player-only"));
            return true;
        }
        if (args.length == 0) {
            if (this.plugin.getDialogSupportService().supportsDialogs(player)) {
                openStaffDashboard(player);
                return true;
            }
            sender.sendMessage("/staff <add|remove|list|pending|approve|deny>");
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> addStaff(sender, args);
            case "remove" -> removeStaff(sender, args);
            case "list" -> listStaff(sender);
            case "pending" -> listPending(sender);
            case "approve" -> approvePending(sender, args);
            case "deny" -> denyPending(sender, args);
            default -> {
                sender.sendMessage("/staff <add|remove|list|pending|approve|deny>");
                yield true;
            }
        };
    }

    private void openStaffDashboard(final Player player) {
        final StaffRole role = this.plugin.getStaffAccessService().roleOf(player);
        player.showDialog(StaffDashboardDialog.create(
            role,
            () -> openStats(player),
            () -> openInventorySearch(player),
            () -> openSanctions(player),
            () -> openPlayerLookup(player)
        ));
    }

    private void openStats(final Player player) {
        player.showDialog(StaffStatsDialog.create(() -> openStaffDashboard(player)));
    }

    private void openInventorySearch(final Player player) {
        this.plugin.getDialogSupportService().openPlayerPicker(player, "Voir inventaire", target -> {
            target.openInventory(target.getInventory());
        });
    }

    private void openSanctions(final Player player) {
        this.plugin.getDialogSupportService().openPlayerPicker(player, "Sanctionner", target -> {
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> player.chat("/sanction " + target.getName()), 1L);
        });
    }

    private void openPlayerLookup(final Player player) {
        player.showDialog(PlayerSearchDialog.create(
            (response, audience) -> {
                final String playerName = response.getText("player");
                if (playerName != null && !playerName.isBlank()) {
                    player.chat("/lookup " + playerName.trim());
                }
            },
            () -> openStaffDashboard(player)
        ));
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
        this.plugin.getStaffActionLogger().log(
            sender,
            StaffActionType.FREEZE_TOGGLE,
            target.getUniqueId(),
            target.getName(),
            frozen ? "Freeze active" : "Freeze retire"
        );
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
            .filter(player -> this.plugin.getStaffAccessService().isStaff(player) || this.plugin.getStaffModeService().isInStaffMode(player.getUniqueId()))
            .map(Player::getName)
            .reduce((left, right) -> left + ", " + right)
            .orElse("Aucun");
        sender.sendMessage(this.plugin.getMessageFormatter().message("staffmode.stafflist", this.plugin.getMessageFormatter().text("players", online)));
        return true;
    }

    private boolean addStaff(final CommandSender sender, final String[] args) {
        if (!this.plugin.getStaffAccessService().canManageAssignments(sender)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-permission"));
            return true;
        }
        if (args.length >= 3) {
            return assign(sender, args[1], StaffRole.fromInput(args[2]));
        }
        if (sender instanceof Player player && this.plugin.getDialogSupportService().supportsDialogs(player)) {
            openAssignmentDialog(player);
            return true;
        }
        sender.sendMessage("/staff add <joueur> <guide|modo|admin>");
        return true;
    }

    private boolean removeStaff(final CommandSender sender, final String[] args) {
        if (!this.plugin.getStaffAccessService().canManageAssignments(sender)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("/staff remove <joueur>");
            return true;
        }
        final OfflinePlayer target = resolveKnownPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-player-found"));
            return true;
        }
        sender.sendMessage(this.plugin.getMessageFormatter().message("staff.assign-pending-sync"));
        this.plugin.getStaffAccessService().removeStaffMember(target).whenComplete((result, throwable) ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (throwable != null) {
                    sender.sendMessage(this.plugin.getMessageFormatter().message("staff.assign-sync-error"));
                    return;
                }
                if (result == StaffAssignmentResult.NOT_FOUND) {
                    sender.sendMessage(this.plugin.getMessageFormatter().message("staff.assign-remove-missing"));
                    return;
                }
                if (result == StaffAssignmentResult.PROVIDER_UNAVAILABLE) {
                    sender.sendMessage(this.plugin.getMessageFormatter().message("staff.assign-provider-unavailable"));
                    return;
                }
                if (result == StaffAssignmentResult.ERROR || result == StaffAssignmentResult.CONFIGURATION_ERROR) {
                    sender.sendMessage(this.plugin.getMessageFormatter().message("staff.assign-sync-error"));
                    return;
                }
                sender.sendMessage(this.plugin.getMessageFormatter().message(
                    "staff.assign-removed",
                    this.plugin.getMessageFormatter().text("target", target.getName() == null ? args[1] : target.getName())
                ));
                this.plugin.getStaffActionLogger().log(
                    sender,
                    StaffActionType.LUCKPERMS_EDIT,
                    target.getUniqueId(),
                    target.getName() == null ? args[1] : target.getName(),
                    "Suppression du role staff via /staff remove"
                );
            })
        );
        return true;
    }

    private boolean listStaff(final CommandSender sender) {
        if (!this.plugin.getStaffAccessService().canManageAssignments(sender)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-permission"));
            return true;
        }
        final List<fr.dragon.admincore.core.StaffMemberRecord> members = this.plugin.getStaffAccessService().members();
        if (members.isEmpty()) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("staff.assign-list-empty"));
            return true;
        }
        sender.sendMessage(this.plugin.getMessageFormatter().message("staff.assign-list-header"));
        members.forEach(member -> sender.sendMessage(this.plugin.getMessageFormatter().message(
            "staff.assign-list-line",
            this.plugin.getMessageFormatter().text("target", member.lastKnownName()),
            this.plugin.getMessageFormatter().text("role", member.role().displayName())
        )));
        return true;
    }

    private boolean listPending(final CommandSender sender) {
        final StaffRole approverRole = this.plugin.getStaffAccessService().approverRoleOf(sender);
        if (!approverRole.isStaff() && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-permission"));
            return true;
        }
        final List<fr.dragon.admincore.sanctions.PendingSanctionRequest> pending = this.plugin.getSanctionApprovalService().pending(approverRole);
        if (pending.isEmpty()) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("admin.approval-none-pending"));
            return true;
        }
        pending.forEach(request -> sender.sendMessage(this.plugin.getMessageFormatter().message(
            "admin.approval-pending-line",
            this.plugin.getMessageFormatter().text("id", Integer.toString(request.id())),
            this.plugin.getMessageFormatter().text("requester", request.requesterName()),
            this.plugin.getMessageFormatter().text("type", request.request().type().name()),
            this.plugin.getMessageFormatter().text("target", request.request().targetName()),
            this.plugin.getMessageFormatter().text("duration", request.duration() == null ? "Permanent" : fr.dragon.admincore.util.TimeParser.format(request.duration())),
            this.plugin.getMessageFormatter().text("approverRole", request.requiredApproverRole().displayName())
        )));
        return true;
    }

    private boolean approvePending(final CommandSender sender, final String[] args) {
        final StaffRole approverRole = this.plugin.getStaffAccessService().approverRoleOf(sender);
        if (!approverRole.isStaff() && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("/staff approve <id>");
            return true;
        }
        final Integer id = parseId(sender, args[1]);
        if (id == null) {
            return true;
        }
        if (this.plugin.getSanctionApprovalService().approve(id, approverRole).isEmpty()) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("admin.approval-invalid-id"));
            return true;
        }
        sender.sendMessage(this.plugin.getMessageFormatter().message(
            "admin.approval-processed",
            this.plugin.getMessageFormatter().text("id", Integer.toString(id)),
            this.plugin.getMessageFormatter().text("state", "approuvee")
        ));
        return true;
    }

    private boolean denyPending(final CommandSender sender, final String[] args) {
        final StaffRole approverRole = this.plugin.getStaffAccessService().approverRoleOf(sender);
        if (!approverRole.isStaff() && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("/staff deny <id>");
            return true;
        }
        final Integer id = parseId(sender, args[1]);
        if (id == null) {
            return true;
        }
        if (this.plugin.getSanctionApprovalService().deny(id, approverRole).isEmpty()) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("admin.approval-invalid-id"));
            return true;
        }
        sender.sendMessage(this.plugin.getMessageFormatter().message(
            "admin.approval-processed",
            this.plugin.getMessageFormatter().text("id", Integer.toString(id)),
            this.plugin.getMessageFormatter().text("state", "refusee")
        ));
        return true;
    }

    private void openAssignmentDialog(final Player player) {
        if (!this.plugin.getDialogSupportService().supportsDialogs(player)) {
            player.sendMessage("/staff add <joueur> <guide|modo|admin>");
            return;
        }
        player.showDialog(StaffAssignmentDialog.create(
            (response, audience) -> assign(player, response.getText("player"), StaffRole.GUIDE),
            (response, audience) -> assign(player, response.getText("player"), StaffRole.MODERATOR),
            (response, audience) -> assign(player, response.getText("player"), StaffRole.ADMIN),
            (response, audience) -> player.sendMessage(this.plugin.getMessageFormatter().message("dialogs.action-cancelled"))
        ));
    }

    private boolean assign(final CommandSender sender, final String rawPlayerName, final StaffRole role) {
        if (role == StaffRole.NONE || rawPlayerName == null || rawPlayerName.isBlank()) {
            sender.sendMessage("/staff add <joueur> <guide|modo|admin>");
            return true;
        }
        final OfflinePlayer target = resolveKnownPlayer(rawPlayerName.trim());
        if (target == null) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-player-found"));
            return true;
        }
        sender.sendMessage(this.plugin.getMessageFormatter().message("staff.assign-pending-sync"));
        this.plugin.getStaffAccessService().assignStaffMember(target, role).whenComplete((result, throwable) ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (throwable != null) {
                    sender.sendMessage(this.plugin.getMessageFormatter().message("staff.assign-sync-error"));
                    return;
                }
                if (result == StaffAssignmentResult.PROVIDER_UNAVAILABLE) {
                    sender.sendMessage(this.plugin.getMessageFormatter().message("staff.assign-provider-unavailable"));
                    return;
                }
                if (result == StaffAssignmentResult.CONFIGURATION_ERROR) {
                    sender.sendMessage(this.plugin.getMessageFormatter().message("staff.assign-config-error"));
                    return;
                }
                if (result != StaffAssignmentResult.SUCCESS) {
                    sender.sendMessage(this.plugin.getMessageFormatter().message("staff.assign-sync-error"));
                    return;
                }
                sender.sendMessage(this.plugin.getMessageFormatter().message(
                    "staff.assign-added",
                    this.plugin.getMessageFormatter().text("target", target.getName() == null ? rawPlayerName : target.getName()),
                    this.plugin.getMessageFormatter().text("role", role.displayName())
                ));
                this.plugin.getStaffActionLogger().log(
                    sender,
                    StaffActionType.LUCKPERMS_EDIT,
                    target.getUniqueId(),
                    target.getName() == null ? rawPlayerName : target.getName(),
                    "Attribution du role " + role.displayName() + " via /staff add"
                );
            })
        );
        return true;
    }

    private OfflinePlayer resolveKnownPlayer(final String name) {
        final Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return java.util.Arrays.stream(Bukkit.getOfflinePlayers())
            .filter(player -> player.getName() != null && player.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    private Integer parseId(final CommandSender sender, final String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (final NumberFormatException exception) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("admin.approval-invalid-id"));
            return null;
        }
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
        if ("staff".equalsIgnoreCase(command.getName())) {
            if (args.length == 1) {
                return List.of("add", "remove", "list", "pending", "approve", "deny");
            }
            if (args.length == 2 && List.of("add", "remove").contains(args[0].toLowerCase(Locale.ROOT))) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
            }
            if (args.length == 3 && "add".equalsIgnoreCase(args[0])) {
                return List.of("guide", "modo", "admin");
            }
            return List.of();
        }
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
