package fr.dragon.admincore.gui;

import fr.dragon.admincore.sanctions.SanctionRecord;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ActiveSanctionsMenu {

    private ActiveSanctionsMenu() {
    }

    public static void openWarnings(final Player player, final String targetName, final List<SanctionRecord> warnings) {
        final Inventory inventory = Bukkit.createInventory(new Holder(targetName), 54, Component.text("Warnings: " + targetName));
        int slot = 0;
        for (final SanctionRecord warning : warnings.stream().limit(54).toList()) {
            inventory.setItem(slot++, warningItem(warning));
        }
        player.openInventory(inventory);
    }

    private static ItemStack warningItem(final SanctionRecord warning) {
        final ItemStack item = new ItemStack(Material.YELLOW_TERRACOTTA);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Warning - " + warning.targetName()));
        meta.lore(List.of(
            Component.text("Raison: " + warning.reason()),
            Component.text("Par: " + warning.actorName()),
            Component.text("Date: " + warning.createdAt()),
            Component.text("Active: " + warning.active())
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
