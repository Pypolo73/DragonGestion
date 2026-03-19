package fr.dragon.admincore.core;

import fr.dragon.admincore.gui.AdminCoreMenus;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class AdminCoreCommand implements CommandExecutor, TabCompleter {

    private final AdminCorePlugin plugin;

    public AdminCoreCommand(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!this.plugin.getPermissionService().check(sender, PermissionService.ADMIN)) {
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player player) {
                AdminCoreMenus.openMain(player);
            } else {
                sender.sendMessage("/admincore <reload|debug|db export>");
            }
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            this.plugin.reloadPlugin();
            for (final var online : this.plugin.getServer().getOnlinePlayers()) {
                this.plugin.getStaffAccessService().refresh(online);
            }
            sender.sendMessage(this.plugin.getMessageFormatter().message("admin.reload"));
            return true;
        }
        if ("debug".equalsIgnoreCase(args[0])) {
            sender.sendMessage(this.plugin.getMessageFormatter().message(
                "admin.debug",
                this.plugin.getMessageFormatter().text("staffmode", Integer.toString(this.plugin.getStaffModeService().getFrozenPlayers().size())),
                this.plugin.getMessageFormatter().text("vanished", Integer.toString(this.plugin.getVanishService().getVanishedPlayers().size())),
                this.plugin.getMessageFormatter().text("slowmode", Integer.toString(this.plugin.getChatService().getSlowModeSeconds()))
            ));
            return true;
        }
        if (args.length >= 2 && "db".equalsIgnoreCase(args[0]) && "export".equalsIgnoreCase(args[1])) {
            final Path exportDir = this.plugin.getDataFolder().toPath().resolve("exports");
            this.plugin.getSanctionService().exportCsv(exportDir).thenAccept(file ->
                this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                    sender.sendMessage(this.plugin.getMessageFormatter().message("admin.db-export", this.plugin.getMessageFormatter().text("file", file.toString())))
                )
            );
            return true;
        }
        if ("pending".equalsIgnoreCase(args[0])) {
            final List<fr.dragon.admincore.sanctions.PendingSanctionRequest> pending = this.plugin.getSanctionApprovalService().pending(StaffRole.ADMIN);
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
        if (args.length >= 2 && "approve".equalsIgnoreCase(args[0])) {
            final Integer id = parseId(sender, args[1]);
            if (id == null) {
                return true;
            }
            if (this.plugin.getSanctionApprovalService().approve(id, StaffRole.ADMIN).isEmpty()) {
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
        if (args.length >= 2 && "deny".equalsIgnoreCase(args[0])) {
            final Integer id = parseId(sender, args[1]);
            if (id == null) {
                return true;
            }
            if (this.plugin.getSanctionApprovalService().deny(id, StaffRole.ADMIN).isEmpty()) {
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
        sender.sendMessage("/admincore <reload|debug|db export|pending|approve|deny>");
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            return List.of("reload", "debug", "db", "pending", "approve", "deny");
        }
        if (args.length == 2 && "db".equalsIgnoreCase(args[0])) {
            return List.of("export");
        }
        return List.of();
    }

    private Integer parseId(final CommandSender sender, final String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (final NumberFormatException exception) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("admin.approval-invalid-id"));
            return null;
        }
    }
}
