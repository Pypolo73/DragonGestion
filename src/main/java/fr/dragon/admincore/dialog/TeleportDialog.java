package fr.dragon.admincore.dialog;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.teleportation.TeleportService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeleportDialog {

    private final AdminCorePlugin plugin;
    private final TeleportService teleportService;

    public TeleportDialog(AdminCorePlugin plugin, TeleportService teleportService) {
        this.plugin = plugin;
        this.teleportService = teleportService;
    }

    public void openMainMenu(@NotNull Player player) {
        player.sendMessage(Component.text("═══════════════════════════════════════").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("        🎯 MENU DE TELEPORTATION        ").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("═══════════════════════════════════════").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("🏠 ").color(NamedTextColor.AQUA)
            .append(Component.text("Homes").color(NamedTextColor.WHITE))
            .append(Component.text(" - Vos points de spawn").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("   ├─ ").color(NamedTextColor.GREEN)
            .append(Component.text("/sethome [nom]").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/sethome "))
            .hoverEvent(HoverEvent.showText(Component.text("Creer un home")))));
        player.sendMessage(Component.text("   ├─ ").color(NamedTextColor.GREEN)
            .append(Component.text("/home").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/home "))
            .hoverEvent(HoverEvent.showText(Component.text("Voir vos homes")))));
        player.sendMessage(Component.text("   └─ ").color(NamedTextColor.RED)
            .append(Component.text("/delhome <nom>").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/delhome "))
            .hoverEvent(HoverEvent.showText(Component.text("Supprimer un home")))));

        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("🌍 ").color(NamedTextColor.GOLD)
            .append(Component.text("Warps").color(NamedTextColor.WHITE))
            .append(Component.text(" - Points de teleportations publics").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("   ├─ ").color(NamedTextColor.GREEN)
            .append(Component.text("/warp [nom]").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/warp "))
            .hoverEvent(HoverEvent.showText(Component.text("Se teleporter a un warp")))));
        player.sendMessage(Component.text("   ├─ ").color(NamedTextColor.YELLOW)
            .append(Component.text("/listwarps").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/listwarps"))
            .hoverEvent(Component.text("Liste des warps"))));
        if (player.hasPermission("admincore.teleportation.warp.create")) {
            player.sendMessage(Component.text("   ├─ ").color(NamedTextColor.GREEN)
                .append(Component.text("/setwarp <nom>").color(NamedTextColor.WHITE)
                .clickEvent(ClickEvent.runCommand("/setwarp "))
                .hoverEvent(HoverEvent.showText(Component.text("Creer un warp")))));
        }
        if (player.hasPermission("admincore.teleportation.warp.delete")) {
            player.sendMessage(Component.text("   └─ ").color(NamedTextColor.RED)
                .append(Component.text("/delwarp <nom>").color(NamedTextColor.WHITE)
                .clickEvent(ClickEvent.runCommand("/delwarp "))
                .hoverEvent(HoverEvent.showText(Component.text("Supprimer un warp")))));
        }

        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("⭐ ").color(NamedTextColor.LIGHT_PURPLE)
            .append(Component.text("Spawn").color(NamedTextColor.WHITE))
            .append(Component.text(" - Point de spawn du serveur").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("   ├─ ").color(NamedTextColor.GREEN)
            .append(Component.text("/spawn").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/spawn"))
            .hoverEvent(HoverEvent.showText(Component.text("Aller au spawn")))));
        if (player.hasPermission("admincore.teleportation.spawn.set")) {
            player.sendMessage(Component.text("   └─ ").color(NamedTextColor.GOLD)
                .append(Component.text("/setspawn").color(NamedTextColor.WHITE)
                .clickEvent(ClickEvent.runCommand("/setspawn"))
                .hoverEvent(HoverEvent.showText(Component.text("Definir le spawn")))));
        }

        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("🎲 ").color(NamedTextColor.GREEN)
            .append(Component.text("Teleportation Aleatoire (RTP)").color(NamedTextColor.WHITE))
            .append(Component.text(" - Position aleatoire").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("   └─ ").color(NamedTextColor.GREEN)
            .append(Component.text("/rtp").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/rtp"))
            .hoverEvent(HoverEvent.showText(Component.text("Teleportation aleatoire")))));

        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("📍 ").color(NamedTextColor.YELLOW)
            .append(Component.text("TPA").color(NamedTextColor.WHITE))
            .append(Component.text(" - Demander une teleportation").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("   ├─ ").color(NamedTextColor.AQUA)
            .append(Component.text("/tpa <joueur>").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/tpa "))
            .hoverEvent(HoverEvent.showText(Component.text("Demander a un joueur")))));
        player.sendMessage(Component.text("   ├─ ").color(NamedTextColor.AQUA)
            .append(Component.text("/tpahere <joueur>").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/tpahere "))
            .hoverEvent(HoverEvent.showText(Component.text("Demander a un joueur de venir")))));
        player.sendMessage(Component.text("   ├─ ").color(NamedTextColor.GREEN)
            .append(Component.text("/tpaccept").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/tpaccept"))
            .hoverEvent(HoverEvent.showText(Component.text("Accepter une demande")))));
        player.sendMessage(Component.text("   ├─ ").color(NamedTextColor.RED)
            .append(Component.text("/tpdeny").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/tpdeny"))
            .hoverEvent(HoverEvent.showText(Component.text("Refuser une demande")))));
        player.sendMessage(Component.text("   └─ ").color(NamedTextColor.GRAY)
            .append(Component.text("/tpignore").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/tpignore"))
            .hoverEvent(HoverEvent.showText(Component.text("Ignorer les demandes")))));

        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("↩️ ").color(NamedTextColor.RED)
            .append(Component.text("Back").color(NamedTextColor.WHITE))
            .append(Component.text(" - Retour a la position precedente").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("   └─ ").color(NamedTextColor.RED)
            .append(Component.text("/back").color(NamedTextColor.WHITE)
            .clickEvent(ClickEvent.runCommand("/back"))
            .hoverEvent(HoverEvent.showText(Component.text("Retourner a la position precedente")))));

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("═══════════════════════════════════════").color(NamedTextColor.GRAY));
    }
}