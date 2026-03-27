package fr.dragon.admincore.network;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.geo.GeoIPService;
import fr.dragon.admincore.dialog.NetworkDialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class NetworkCommand implements CommandExecutor, TabCompleter {

    private final AdminCorePlugin plugin;
    private final GeoIPService geoIPService;
    private NetworkDialog networkDialog;

    public NetworkCommand(AdminCorePlugin plugin, GeoIPService geoIPService) {
        this.plugin = plugin;
        this.geoIPService = geoIPService;
    }

    public void setNetworkDialog(NetworkDialog dialog) {
        this.networkDialog = dialog;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PermissionService.CHECKVPN)) {
            sender.sendMessage(Component.text("Vous n'avez pas la permission.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player && networkDialog != null) {
                networkDialog.openMainMenu(player);
            } else {
                showHelp(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "info" -> handleNetworkInfo(sender, args);
            case "geo", "geolocation" -> handleGeoInfo(sender, args);
            case "reload" -> handleReload(sender);
            case "cache" -> handleCache(sender);
            default -> {
                showHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleNetworkInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Utilisation: /network info <joueur>").color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Joueur non trouve.").color(NamedTextColor.RED));
            return true;
        }

        showPlayerNetworkInfo(sender, target);
        
        geoIPService.getPlayerGeoInfo(target, sender);
        return true;
    }

    private void showPlayerNetworkInfo(CommandSender sender, Player target) {
        InetAddress address = target.getAddress() != null ? target.getAddress().getAddress() : null;
        
        sender.sendMessage(Component.text("═══════════════════════════════════").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("        📡 Infos Reseau: " + target.getName()).color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("═══════════════════════════════════").color(NamedTextColor.GRAY));
        
        if (address != null) {
            String ip = address.getHostAddress();
            sender.sendMessage(Component.text("IP: ").color(NamedTextColor.AQUA).append(Component.text(ip).color(NamedTextColor.WHITE)));
            
            try {
                String hostname = address.getHostName();
                if (!hostname.equals(ip)) {
                    sender.sendMessage(Component.text("Hostname: ").color(NamedTextColor.AQUA).append(Component.text(hostname).color(NamedTextColor.WHITE)));
                }
            } catch (Exception ignored) {}
            
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                String subnet = parts[0] + "." + parts[1] + "." + parts[2] + ".0/24";
                sender.sendMessage(Component.text("Subnet: ").color(NamedTextColor.AQUA).append(Component.text(subnet).color(NamedTextColor.WHITE)));
            }
        } else {
            sender.sendMessage(Component.text("IP: ").color(NamedTextColor.AQUA).append(Component.text("Non disponible").color(NamedTextColor.RED)));
        }
        
        long ping = getPing(target);
        NamedTextColor pingColor = ping < 50 ? NamedTextColor.GREEN : ping < 100 ? NamedTextColor.YELLOW : NamedTextColor.RED;
        sender.sendMessage(Component.text("Ping: ").color(NamedTextColor.AQUA).append(Component.text(ping + "ms").color(pingColor)));
        
        sender.sendMessage(Component.text("═══════════════════════════════════").color(NamedTextColor.GRAY));
    }

    private long getPing(Player player) {
        try {
            return player.getPing();
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean handleGeoInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Utilisation: /network geo <joueur>").color(NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Joueur non trouve.").color(NamedTextColor.RED));
            return true;
        }

        geoIPService.getPlayerGeoInfo(target, sender);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        sender.sendMessage(Component.text("Configuration reseau rechargee.").color(NamedTextColor.GREEN));
        return true;
    }
    
    private boolean handleCache(CommandSender sender) {
        geoIPService.clearCache();
        sender.sendMessage(Component.text("Cache GeoIP efface.").color(NamedTextColor.GREEN));
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("═══════════════════════════════════").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("        📡 Commandes Reseau        ").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("═══════════════════════════════════").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/network info <joueur>").color(NamedTextColor.AQUA).append(Component.text(" - Infos reseau + geo").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/network geo <joueur>").color(NamedTextColor.AQUA).append(Component.text(" - Info geographique").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/network cache").color(NamedTextColor.AQUA).append(Component.text(" - Vider le cache").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/network reload").color(NamedTextColor.AQUA).append(Component.text(" - Recharger config").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("═══════════════════════════════════").color(NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("info", "geo", "cache", "reload");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("geo"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return new ArrayList<>();
    }
}