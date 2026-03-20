package fr.dragon.admincore.vanish;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.core.StaffActionType;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class VanishCommand implements CommandExecutor, TabCompleter {

    private final AdminCorePlugin plugin;

    public VanishCommand(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!this.plugin.getPermissionService().check(sender, PermissionService.VANISH)) {
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(this.plugin.getMessageFormatter().message("errors.player-only"));
                return true;
            }
            final boolean vanished = this.plugin.getVanishService().toggle(player);
            this.plugin.getStaffActionLogger().log(
                player,
                StaffActionType.VANISH_TOGGLE,
                player.getUniqueId(),
                player.getName(),
                vanished ? "Vanish active via /vanish" : "Vanish desactive via /vanish"
            );
            sender.sendMessage(this.plugin.getMessageFormatter().message(vanished ? "vanish.enabled" : "vanish.disabled"));
            return true;
        }
        final Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-player-found"));
            return true;
        }
        final boolean vanished = this.plugin.getVanishService().toggle(target);
        this.plugin.getStaffActionLogger().log(
            sender,
            StaffActionType.VANISH_TOGGLE,
            target.getUniqueId(),
            target.getName(),
            vanished ? "Vanish active sur cible via /vanish" : "Vanish desactive sur cible via /vanish"
        );
        sender.sendMessage(this.plugin.getMessageFormatter().message(
            vanished ? "vanish.enabled-target" : "vanish.disabled-target",
            this.plugin.getMessageFormatter().text("target", target.getName())
        ));
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
