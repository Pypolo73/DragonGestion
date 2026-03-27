package fr.dragon.admincore.teleportation;

import fr.dragon.admincore.core.AdminCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TeleportDialogs {

    private final AdminCorePlugin plugin;
    private final TeleportService teleportService;

    public TeleportDialogs(@NotNull AdminCorePlugin plugin, @NotNull TeleportService teleportService) {
        this.plugin = plugin;
        this.teleportService = teleportService;
    }

    public void openMainMenu(@NotNull Player player) {
        player.sendMessage(Component.text("=== MENU DE TELEPORTATION ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("1. ").color(NamedTextColor.AQUA).append(Component.text("Gestion des Homes").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("2. ").color(NamedTextColor.GOLD).append(Component.text("Gestion des Warps").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("3. ").color(NamedTextColor.LIGHT_PURPLE).append(Component.text("Demande TPA").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("4. ").color(NamedTextColor.GREEN).append(Component.text("Teleportation Aleatoire (RTP)").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("5. ").color(NamedTextColor.RED).append(Component.text("Dernieres Morts").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("Utilise: /home, /warp, /tpa, /rtp").color(NamedTextColor.GRAY));
    }

    public void openHomeMenu(@NotNull Player player) {
        player.sendMessage(Component.text("=== GESTION DES HOMES ===").color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/sethome [nom] - Poser un home").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("/home [nom] - Se teleporter a un home").color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("/delhome <nom> - Supprimer un home").color(NamedTextColor.RED));
        player.sendMessage(Component.text("/listhomes - Liste de vos homes").color(NamedTextColor.YELLOW));
    }

    public void openWarpMenu(@NotNull Player player) {
        player.sendMessage(Component.text("=== GESTION DES WARPS ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("").color(NamedTextColor.WHITE));
        
        if (player.hasPermission("admincore.teleportation.warps.create")) {
            player.sendMessage(Component.text("/setwarp <nom> - Creer un warp").color(NamedTextColor.GREEN));
        }
        
        player.sendMessage(Component.text("/warp [nom] - Se teleporter a un warp").color(NamedTextColor.GOLD));
        
        if (player.hasPermission("admincore.teleportation.warps.delete")) {
            player.sendMessage(Component.text("/delwarp <nom> - Supprimer un warp").color(NamedTextColor.RED));
        }
        
        player.sendMessage(Component.text("/listwarps - Liste des warps").color(NamedTextColor.YELLOW));
    }

    public void openTPAMenu(@NotNull Player player) {
        player.sendMessage(Component.text("=== DEMANDE DE TELEPORTATION ===").color(NamedTextColor.LIGHT_PURPLE));
        player.sendMessage(Component.text("").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/tpa <joueur> - Demander a se teleporter vers un joueur").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("/tpahere <joueur> - Demander a un joueur de venir").color(NamedTextColor.BLUE));
        player.sendMessage(Component.text("/tpaccept - Accepter une requete").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/tpdeny - Refuser une requete").color(NamedTextColor.RED));
    }

    public void openDeathsMenu(@NotNull Player player) {
        List<TeleportLogger.DeathPosition> deaths = teleportService.getLogger().getDeathPositions(player);
        
        player.sendMessage(Component.text("=== DERNIERES MORTS ===").color(NamedTextColor.RED));
        player.sendMessage(Component.text("").color(NamedTextColor.WHITE));
        
        if (deaths.isEmpty()) {
            player.sendMessage(Component.text("Tu n'as aucune mort enregistree!").color(NamedTextColor.GRAY));
            return;
        }

        int index = 1;
        for (TeleportLogger.DeathPosition death : deaths) {
            player.sendMessage(Component.text(index + ". ").color(NamedTextColor.WHITE)
                .append(Component.text(death.world()).color(NamedTextColor.AQUA))
                .append(Component.text(" x:" + death.x()).color(NamedTextColor.GRAY))
                .append(Component.text(" y:" + death.y()).color(NamedTextColor.GRAY))
                .append(Component.text(" z:" + death.z()).color(NamedTextColor.GRAY))
                .append(Component.text(" (" + death.time() + ")").color(NamedTextColor.DARK_GRAY)));
            index++;
        }
        
        player.sendMessage(Component.text("").color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("Nombre max: " + teleportService.getLogger().getMaxDeathPositions()).color(NamedTextColor.GRAY));
    }
}
