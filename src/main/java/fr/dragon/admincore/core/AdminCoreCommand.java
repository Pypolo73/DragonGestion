package fr.dragon.admincore.core;

import java.nio.file.Path;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

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
            sender.sendMessage("/admincore <reload|debug|db export>");
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            this.plugin.reloadPlugin();
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
        sender.sendMessage("/admincore <reload|debug|db export>");
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            return List.of("reload", "debug", "db");
        }
        if (args.length == 2 && "db".equalsIgnoreCase(args[0])) {
            return List.of("export");
        }
        return List.of();
    }
}
