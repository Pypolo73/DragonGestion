package fr.dragon.admincore.teleportation;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.dialog.TeleportDialog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TeleportCommands implements CommandExecutor, TabCompleter {
    private final AdminCorePlugin plugin;
    private final TeleportService service;
    private TeleportDialogs teleportDialogs;
    private TeleportDialog newTeleportDialog;

    public TeleportCommands(AdminCorePlugin plugin, TeleportService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void setTeleportDialogs(TeleportDialogs dialogs) {
        this.teleportDialogs = dialogs;
    }

    public void setNewTeleportDialog(TeleportDialog dialog) {
        this.newTeleportDialog = dialog;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Cette commande doit etre executee en jeu.", NamedTextColor.RED));
            return true;
        }
        String cmd = command.getName().toLowerCase();

        return switch (cmd) {
            case "sethome" -> handleSetHome(player, args);
            case "home" -> args.length == 0 ? handleHomeDialog(player) : handleHome(player, args);
            case "delhome" -> handleDelHome(player, args);
            case "listhomes", "homes" -> handleListHomes(player);
            case "setwarp" -> handleSetWarp(player, args);
            case "warp" -> args.length == 0 ? handleWarpDialog(player) : handleWarp(player, args);
            case "delwarp" -> handleDelWarp(player, args);
            case "listwarps", "warps" -> handleListWarps(player);
            case "spawn" -> handleSpawn(player);
            case "setspawn" -> handleSetSpawn(player);
            case "rtp" -> handleRtp(player);
            case "tpa" -> args.length == 0 ? handleTpaDialog(player) : handleTpa(player, args);
            case "tpahere" -> handleTpaHere(player, args);
            case "tpaccept" -> handleTpAccept(player, args);
            case "tpdeny" -> handleTpDeny(player, args);
            case "tpignore" -> handleTpIgnore(player);
            case "back" -> handleBack(player);
            case "phome", "publichome" -> handlePublicHome(player, args);
            case "phomelist", "publichomes" -> handlePublicHomesList(player);
            case "togglepublic" -> handleTogglePublic(player, args);
            case "teleportation", "teleport" -> handleTeleportationMenu(player);
            default -> false;
        };
    }

    private boolean handleSetHome(Player sender, String[] args) {
        String name = args.length > 0 ? args[0] : "home";
        if (!isValidName(name)) {
            send(sender, Component.text("Nom invalide. Utilisez uniquement lettres, chiffres, espaces et tirets.", NamedTextColor.RED));
            return true;
        }
        service.setHome(sender, name);
        return true;
    }

    private boolean handleHome(Player sender, String[] args) {
        if (args.length == 0) {
            List<TeleportData> homes = service.getHomes(sender);
            if (homes.isEmpty()) {
                send(sender, Component.text("Vous n'avez aucun home. Utilisez /sethome <nom>", NamedTextColor.YELLOW));
                return true;
            }
            send(sender, Component.text("Vos homes:", NamedTextColor.GREEN));
            for (TeleportData home : homes) {
                Component homeComp = Component.text("  - " + home.name(), NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("/home " + home.name()))
                    .hoverEvent(HoverEvent.showText(Component.text("Cliquez pour teleporter", NamedTextColor.GREEN)));
                send(sender, homeComp);
            }
            return true;
        }
        service.teleportToHome(sender, args[0]);
        return true;
    }

    private boolean handleDelHome(Player sender, String[] args) {
        if (args.length == 0) {
            send(sender, Component.text("Usage: /delhome <nom>", NamedTextColor.RED));
            return true;
        }
        service.deleteHome(sender, args[0]);
        return true;
    }

    private boolean handleListHomes(Player sender) {
        List<TeleportData> homes = service.getHomes(sender);
        if (homes.isEmpty()) {
            send(sender, Component.text("Vous n'avez aucun home.", NamedTextColor.YELLOW));
            return true;
        }
        
        send(sender, Component.text("=== Vos Homes ===", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));
        for (TeleportData home : homes) {
            String status = home.isPublic() ? " [Public]" : " [Prive]";
            Component homeComp = Component.text("  " + home.name() + status, home.isPublic() ? NamedTextColor.GOLD : NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/home " + home.name()))
                .hoverEvent(HoverEvent.showText(Component.text("Clic: Teleport | Description: " + home.description(), NamedTextColor.GRAY)));
            send(sender, homeComp);
        }
        send(sender, Component.text("Cliquez sur un home pour vous teleporter.", NamedTextColor.GRAY));
        return true;
    }

    private boolean handleSetWarp(Player sender, String[] args) {
        if (!sender.hasPermission("admincore.teleport.warp.create")) {
            send(sender, Component.text("Vous n'avez pas la permission de creer des warps.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            send(sender, Component.text("Usage: /setwarp <nom> [description]", NamedTextColor.RED));
            return true;
        }
        String name = args[0];
        String description = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "";
        if (!isValidName(name)) {
            send(sender, Component.text("Nom invalide. Utilisez uniquement lettres, chiffres, espaces et tirets.", NamedTextColor.RED));
            return true;
        }
        service.setWarp(sender, name, description);
        return true;
    }

    private boolean handleWarp(Player sender, String[] args) {
        if (args.length == 0) {
            List<TeleportData> warps = service.getWarps();
            if (warps.isEmpty()) {
                send(sender, Component.text("Aucun warp defini.", NamedTextColor.YELLOW));
                return true;
            }
            send(sender, Component.text("=== Warps Disponibles ===", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
            for (TeleportData warp : warps) {
                Component warpComp = Component.text("  - " + warp.name(), NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/warp " + warp.name()))
                    .hoverEvent(HoverEvent.showText(Component.text("Clic: Teleport | Lieu: " + warp.getLocationString(), NamedTextColor.GRAY)));
                send(sender, warpComp);
            }
            return true;
        }
        service.teleportToWarp(sender, args[0]);
        return true;
    }

    private boolean handleDelWarp(Player sender, String[] args) {
        if (!sender.hasPermission("admincore.teleport.warp.delete")) {
            send(sender, Component.text("Vous n'avez pas la permission de supprimer des warps.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            send(sender, Component.text("Usage: /delwarp <nom>", NamedTextColor.RED));
            return true;
        }
        service.deleteWarp(sender, args[0]);
        return true;
    }

    private boolean handleListWarps(Player sender) {
        List<TeleportData> warps = service.getWarps();
        if (warps.isEmpty()) {
            send(sender, Component.text("Aucun warp defini.", NamedTextColor.YELLOW));
            return true;
        }
        
        send(sender, Component.text("=== Warps ===", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        for (TeleportData warp : warps) {
            Component warpComp = Component.text("  " + warp.name(), NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand("/warp " + warp.name()))
                .hoverEvent(HoverEvent.showText(Component.text("Clic: Teleport | Lieu: " + warp.getLocationString(), NamedTextColor.GRAY)));
            send(sender, warpComp);
        }
        return true;
    }

    private boolean handleSpawn(Player sender) {
        service.teleportToSpawn(sender);
        return true;
    }

    private boolean handleSetSpawn(Player sender) {
        if (!sender.hasPermission("admincore.teleport.spawn.set")) {
            send(sender, Component.text("Vous n'avez pas la permission de definir le spawn.", NamedTextColor.RED));
            return true;
        }
        service.setSpawn(sender);
        return true;
    }

    private boolean handleRtp(Player sender) {
        service.startRtp(sender);
        return true;
    }

    private boolean handleTpa(Player sender, String[] args) {
        if (args.length == 0) {
            send(sender, Component.text("Usage: /tpa <joueur>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            send(sender, Component.text("Joueur introuvable.", NamedTextColor.RED));
            return true;
        }
        service.sendTpaRequest(sender, target, false);
        return true;
    }

    private boolean handleTpaHere(Player sender, String[] args) {
        if (args.length == 0) {
            send(sender, Component.text("Usage: /tpahere <joueur>", NamedTextColor.RED));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            send(sender, Component.text("Joueur introuvable.", NamedTextColor.RED));
            return true;
        }
        service.sendTpaRequest(sender, target, true);
        return true;
    }

    private boolean handleTpAccept(Player sender, String[] args) {
        service.acceptTpa(sender);
        return true;
    }

    private boolean handleTpDeny(Player sender, String[] args) {
        String targetName = args.length > 0 ? args[0] : null;
        if (targetName == null) {
            send(sender, Component.text("Usage: /tpdeny [joueur]", NamedTextColor.RED));
            return true;
        }
        service.declineTpa(sender, targetName);
        return true;
    }

    private boolean handleTpIgnore(Player sender) {
        service.toggleTpaIgnore(sender);
        return true;
    }

    private boolean handleBack(Player sender) {
        service.teleportBack(sender);
        return true;
    }

    private boolean handlePublicHome(Player sender, String[] args) {
        send(sender, Component.text("Fonctionnalite publique des homes en cours de developpement.", NamedTextColor.YELLOW));
        return true;
    }

    private boolean handlePublicHomesList(Player sender) {
        send(sender, Component.text("Fonctionnalite publique des homes en cours de developpement.", NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleTogglePublic(Player sender, String[] args) {
        if (args.length == 0) {
            send(sender, Component.text("Usage: /togglepublic <home>", NamedTextColor.RED));
            return true;
        }
        send(sender, Component.text("Cette fonctionnalite sera ajoutee prochainement.", NamedTextColor.YELLOW));
        return true;
    }

    private void send(Player sender, Component message) {
        sender.sendMessage(message);
    }

    private boolean isValidName(String name) {
        return name.matches("^[a-zA-Z0-9_\\s-]{1,32}$");
    }

    private boolean handleTeleportationMenu(Player player) {
        if (newTeleportDialog != null) {
            newTeleportDialog.openMainMenu(player);
        } else if (teleportDialogs != null) {
            teleportDialogs.openMainMenu(player);
        } else {
            player.sendMessage(Component.text("Les dialogs ne sont pas disponibles.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleHomeDialog(Player player) {
        if (teleportDialogs != null) {
            teleportDialogs.openHomeMenu(player);
        } else {
            handleHome(player, new String[]{});
        }
        return true;
    }

    private boolean handleWarpDialog(Player player) {
        if (teleportDialogs != null) {
            teleportDialogs.openWarpMenu(player);
        } else {
            handleWarp(player, new String[]{});
        }
        return true;
    }

    private boolean handleTpaDialog(Player player) {
        if (teleportDialogs != null) {
            teleportDialogs.openTPAMenu(player);
        } else {
            player.sendMessage(Component.text("Usage: /tpa <joueur>", NamedTextColor.YELLOW));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        String cmd = command.getName().toLowerCase();
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            switch (cmd) {
                case "home", "delhome" -> {
                    for (TeleportData home : service.getHomes(player)) {
                        completions.add(home.name());
                    }
                }
                case "warp", "delwarp" -> {
                    for (TeleportData warp : service.getWarps()) {
                        completions.add(warp.name());
                    }
                }
                case "tpa", "tpahere" -> {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.equals(sender)) {
                            completions.add(online.getName());
                        }
                    }
                }
                case "setwarp" -> completions.add("<nom>");
            }
        }

        String current = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(current))
            .sorted()
            .collect(Collectors.toList());
    }
}
