package fr.dragon.admincore.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;

public final class StaffStatsDialog {

    private StaffStatsDialog() {
    }

    public static Dialog create(final Runnable backCallback) {
        final double tps = Bukkit.getTPS()[0];
        final long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024L * 1024L);
        final long maxMemory = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        final int loadedChunks = Bukkit.getWorlds().stream().mapToInt(world -> world.getLoadedChunks().length).sum();
        final int onlinePlayers = Bukkit.getOnlinePlayers().size();
        final long uptimeMillis = Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory();
        
        final TextColor tpsColor;
        if (tps > 18.0D) {
            tpsColor = TextColor.color(0x2ecc71);
        } else if (tps >= 15.0D) {
            tpsColor = TextColor.color(0xe67e22);
        } else {
            tpsColor = TextColor.color(0xe74c3c);
        }
        
        final List<DialogBody> body = List.of(
            DialogBody.plainMessage(
                Component.text("=== STATISTIQUES SERVEUR ===").color(TextColor.color(0xFFE45E)), 360),
            DialogBody.plainMessage(Component.text(" "), 360),
            DialogBody.plainMessage(
                Component.text("Joueurs en ligne: ")
                    .append(Component.text(Integer.toString(onlinePlayers)).color(TextColor.color(0x3498db))), 360),
            DialogBody.plainMessage(
                Component.text("TPS: ")
                    .append(Component.text(String.format(Locale.US, "%.2f", tps)).color(tpsColor)), 360),
            DialogBody.plainMessage(
                Component.text("RAM: ")
                    .append(Component.text(usedMemory + "/" + maxMemory + " MB").color(TextColor.color(0x9b59b6))), 360),
            DialogBody.plainMessage(
                Component.text("Chunks charges: ")
                    .append(Component.text(Integer.toString(loadedChunks)).color(TextColor.color(0xe67e22))), 360),
            DialogBody.plainMessage(Component.text(" "), 360),
            DialogBody.plainMessage(
                Component.text("Version: ")
                    .append(Component.text(Bukkit.getVersion()).color(NamedTextColor.GRAY)), 360),
            DialogBody.plainMessage(
                Component.text("Mondes: ")
                    .append(Component.text(Integer.toString(Bukkit.getWorlds().size())).color(NamedTextColor.GRAY)), 360)
        );
        
        final var buttons = List.of(
            DialogHelper.button(
                Component.text("Fermer")
                    .color(NamedTextColor.RED),
                150,
                io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response, audience) -> backCallback.run(), DialogHelper.singleUseOptions())
            )
        );
        
        return DialogHelper.create(
            Component.text("Statistiques serveur").color(TextColor.color(0xFFE45E)),
            body,
            List.of(),
            DialogType.multiAction(buttons, null, 1)
        );
    }
}
