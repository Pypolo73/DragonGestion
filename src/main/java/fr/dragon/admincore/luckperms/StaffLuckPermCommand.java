package fr.dragon.admincore.luckperms;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StaffLuckPermCommand implements CommandExecutor {

    private final AdminCorePlugin plugin;

    public StaffLuckPermCommand(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!this.plugin.getPermissionService().check(sender, PermissionService.STAFFLUCKPERM)) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.player-only"));
            return true;
        }
        if (!this.plugin.getLuckPermsUiService().isAvailable()) {
            player.sendMessage(this.plugin.getMessageFormatter().message("luckperms.unavailable"));
            return true;
        }
        this.plugin.getLuckPermsUiService().openGroupSelection(player);
        return true;
    }
}
