package fr.dragon.admincore.dialog;

import fr.dragon.admincore.geo.GeoIPService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NetworkDialog {

    private final GeoIPService geoIPService;

    public NetworkDialog(GeoIPService geoIPService) {
        this.geoIPService = geoIPService;
    }

    public void openMainMenu(@NotNull Player player) {
        player.sendMessage(Component.text("═══════════════════════════════════════").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("        📡 CENTRE RESEAU        ").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("═══════════════════════════════════════").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("📡 ").color(NamedTextColor.AQUA)
            .append(Component.text("Info Joueur").color(NamedTextColor.WHITE))
            .clickEvent(ClickEvent.suggestCommand("/network info "))
            .hoverEvent(HoverEvent.showText(Component.text("Cliquez pour entrer un pseudo").color(NamedTextColor.GREEN))));
        player.sendMessage(Component.text("   └─ ").color(NamedTextColor.GREEN)
            .append(Component.text("/network info <joueur>").color(NamedTextColor.DARK_GRAY)));

        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("🌍 ").color(NamedTextColor.GREEN)
            .append(Component.text("Geolocalisation").color(NamedTextColor.WHITE))
            .clickEvent(ClickEvent.suggestCommand("/network geo "))
            .hoverEvent(HoverEvent.showText(Component.text("Cliquez pour entrer un pseudo").color(NamedTextColor.GREEN))));
        player.sendMessage(Component.text("   └─ ").color(NamedTextColor.GREEN)
            .append(Component.text("/network geo <joueur>").color(NamedTextColor.DARK_GRAY)));

        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("🔐 ").color(NamedTextColor.RED)
            .append(Component.text("Verification VPN").color(NamedTextColor.WHITE))
            .clickEvent(ClickEvent.suggestCommand("/checkvpn "))
            .hoverEvent(HoverEvent.showText(Component.text("Cliquez pour entrer un pseudo").color(NamedTextColor.GREEN))));
        player.sendMessage(Component.text("   └─ ").color(NamedTextColor.GREEN)
            .append(Component.text("/checkvpn <joueur>").color(NamedTextColor.DARK_GRAY)));

        player.sendMessage(Component.text(""));

        player.sendMessage(Component.text("🗑️ ").color(NamedTextColor.YELLOW)
            .append(Component.text("Vider le Cache").color(NamedTextColor.WHITE))
            .clickEvent(ClickEvent.runCommand("/network cache"))
            .hoverEvent(HoverEvent.showText(Component.text("Cliquez pour vider le cache").color(NamedTextColor.GREEN))));
        player.sendMessage(Component.text("   └─ ").color(NamedTextColor.GREEN)
            .append(Component.text("/network cache").color(NamedTextColor.DARK_GRAY)));

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("═══════════════════════════════════════").color(NamedTextColor.GRAY));
    }
}