package fr.dragon.admincore.movement;

import fr.dragon.admincore.core.PermissionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class FlyCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Cette commande doit etre executee en jeu.").color(NamedTextColor.RED));
            return true;
        }

        String cmd = command.getName().toLowerCase();

        if (cmd.equals("flyspeed")) {
            return handleFlySpeed(player, args);
        } else {
            return handleFly(player, args);
        }
    }

    private boolean handleFly(Player player, String[] args) {
        if (!player.hasPermission(PermissionService.ADMIN + ".fly")) {
            player.sendMessage(Component.text("Vous n'avez pas la permission.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            boolean currentFly = player.getAllowFlight();
            player.setAllowFlight(!currentFly);
            player.sendMessage(Component.text("Mode flight: ")
                .color(NamedTextColor.AQUA)
                .append(Component.text(!currentFly ? "ACTIVE" : "DESACTIVE").color(!currentFly ? NamedTextColor.GREEN : NamedTextColor.RED)));
            return true;
        }

        Player target = player.getServer().getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Joueur non trouve.").color(NamedTextColor.RED));
            return true;
        }

        boolean currentFly = target.getAllowFlight();
        target.setAllowFlight(!currentFly);
        target.sendMessage(Component.text("Mode flight: ")
            .color(NamedTextColor.AQUA)
            .append(Component.text(!currentFly ? "ACTIVE" : "DESACTIVE").color(!currentFly ? NamedTextColor.GREEN : NamedTextColor.RED)));
        
        player.sendMessage(Component.text("Mode flight de ").color(NamedTextColor.GREEN)
            .append(Component.text(target.getName()).color(NamedTextColor.WHITE))
            .append(Component.text(" est maintenant: ").color(NamedTextColor.GREEN))
            .append(Component.text(!currentFly ? "ACTIVE" : "DESACTIVE").color(!currentFly ? NamedTextColor.GREEN : NamedTextColor.RED)));

        return true;
    }

    private boolean handleFlySpeed(Player player, String[] args) {
        if (!player.hasPermission(PermissionService.ADMIN + ".flyspeed")) {
            player.sendMessage(Component.text("Vous n'avez pas la permission.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            float currentSpeed = player.getFlySpeed();
            player.sendMessage(Component.text("Vitesse de vol actuelle: ").color(NamedTextColor.AQUA)
                .append(Component.text(String.format("%.1f", currentSpeed * 10)).color(NamedTextColor.WHITE))
                .append(Component.text("/10").color(NamedTextColor.GRAY)));
            return true;
        }

        Player target = player.getServer().getPlayerExact(args[0]);
        float speed;
        
        if (target != null && args.length > 1) {
            try {
                speed = Float.parseFloat(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Vitesse invalide. Utilisez 1-10.").color(NamedTextColor.RED));
                return true;
            }
            
            if (speed < 1 || speed > 10) {
                player.sendMessage(Component.text("Vitesse doit etre entre 1 et 10.").color(NamedTextColor.RED));
                return true;
            }
            
            target.setFlySpeed(speed / 10f);
            target.sendMessage(Component.text("Vitesse de vol reglee a: ").color(NamedTextColor.AQUA)
                .append(Component.text(speed + "/10").color(NamedTextColor.WHITE)));
            
            player.sendMessage(Component.text("Vitesse de vol de ").color(NamedTextColor.GREEN)
                .append(Component.text(target.getName()).color(NamedTextColor.WHITE))
                .append(Component.text(" reglee a ").color(NamedTextColor.GREEN))
                .append(Component.text(speed + "/10").color(NamedTextColor.WHITE)));
        } else {
            try {
                speed = Float.parseFloat(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Vitesse invalide. Utilisez 1-10.").color(NamedTextColor.RED));
                return true;
            }
            
            if (speed < 1 || speed > 10) {
                player.sendMessage(Component.text("Vitesse doit etre entre 1 et 10.").color(NamedTextColor.RED));
                return true;
            }
            
            player.setFlySpeed(speed / 10f);
            player.sendMessage(Component.text("Vitesse de vol reglee a: ").color(NamedTextColor.AQUA)
                .append(Component.text(speed + "/10").color(NamedTextColor.WHITE)));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return null;
        }
        return List.of();
    }
}