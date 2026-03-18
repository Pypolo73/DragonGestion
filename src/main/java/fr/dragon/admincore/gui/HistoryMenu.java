package fr.dragon.admincore.gui;

import fr.dragon.admincore.sanctions.SanctionRecord;
import fr.dragon.admincore.util.TimeParser;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class HistoryMenu {

    private HistoryMenu() {
    }

    public static void open(final Player player, final String targetName, final List<SanctionRecord> sanctions) {
        final Inventory inventory = Bukkit.createInventory(new Holder(targetName), 54, Component.text("Historique: " + targetName));
        int slot = 0;
        for (final SanctionRecord sanction : sanctions.stream().limit(54).toList()) {
            inventory.setItem(slot++, itemFrom(sanction));
        }
        player.openInventory(inventory);
    }

    private static ItemStack itemFrom(final SanctionRecord sanction) {
        final Material material = switch (sanction.type()) {
            case BAN -> Material.BARRIER;
            case MUTE -> Material.NOTE_BLOCK;
            case KICK -> Material.IRON_BOOTS;
            case WARN -> Material.PAPER;
        };
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(sanction.type().name() + " - " + sanction.targetName()));
        meta.lore(List.of(
            Component.text("Raison: " + sanction.reason()),
            Component.text("Par: " + sanction.actorName()),
            Component.text("Creation: " + sanction.createdAt()),
            Component.text("Expiration: " + (sanction.expiresAt() == null ? "Permanent" : TimeParser.formatRemaining(sanction.expiresAt())))
        ));
        item.setItemMeta(meta);
        return item;
    }

    public record Holder(String targetName) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
