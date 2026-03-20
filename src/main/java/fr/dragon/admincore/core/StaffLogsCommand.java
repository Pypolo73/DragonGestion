package fr.dragon.admincore.core;

import fr.dragon.admincore.gui.StaffLogsMenu;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class StaffLogsCommand implements CommandExecutor, TabCompleter {

    private final AdminCorePlugin plugin;

    public StaffLogsCommand(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!this.plugin.getPermissionService().check(sender, PermissionService.STAFFLOGS)) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>Cette commande ouvre un GUI et doit etre lancee en jeu.</red>"));
            return true;
        }
        final String filter = args.length == 0 ? null : args[0].trim();
        this.plugin.getStaffActionLogger().page(filter, 0, StaffLogsMenu.PAGE_SIZE).thenAccept(page ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> StaffLogsMenu.open(player, page))
        ).exceptionally(throwable -> {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                player.sendMessage(this.plugin.getMessageFormatter().message("errors.database"))
            );
            return null;
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        final String prefix = args[0].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }
}
