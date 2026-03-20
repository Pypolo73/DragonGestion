package fr.dragon.admincore.gui;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.StaffActionEntry;
import fr.dragon.admincore.core.StaffActionLogger;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class StaffLogsMenu {

    public static final int PAGE_SIZE = 28;
    private static final int[] CONTENT_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault());

    private StaffLogsMenu() {
    }

    public static void open(final Player player, final StaffActionLogger.StaffActionPage page) {
        final String title = page.staffFilter() == null || page.staffFilter().isBlank()
            ? "Logs staff"
            : "Logs: " + page.staffFilter();
        final Inventory inventory = Bukkit.createInventory(new Holder(page), 54, Component.text(title, NamedTextColor.GOLD));
        for (int index = 0; index < Math.min(page.entries().size(), CONTENT_SLOTS.length); index++) {
            inventory.setItem(CONTENT_SLOTS[index], entryItem(page.entries().get(index)));
        }
        if (page.page() > 0) {
            inventory.setItem(45, button(Material.ARROW, "Page precedente", "Revenir a la page " + page.page()));
        }
        inventory.setItem(49, button(Material.BARRIER, "Fermer", "Fermer les logs"));
        if (page.hasNext()) {
            inventory.setItem(53, button(Material.ARROW, "Page suivante", "Aller a la page " + (page.page() + 2)));
        }
        player.openInventory(inventory);
    }

    public static void changePage(final AdminCorePlugin plugin, final Player player, final Holder holder, final int delta) {
        final int targetPage = Math.max(0, holder.page().page() + delta);
        plugin.getStaffActionLogger().page(holder.page().staffFilter(), targetPage, PAGE_SIZE).thenAccept(page ->
            plugin.getServer().getScheduler().runTask(plugin, () -> open(player, page))
        ).exceptionally(throwable -> {
            plugin.getServer().getScheduler().runTask(plugin, () ->
                player.sendMessage(plugin.getMessageFormatter().message("errors.database"))
            );
            return null;
        });
    }

    private static ItemStack entryItem(final StaffActionEntry entry) {
        final ItemStack item = new ItemStack(Material.PAPER);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(entry.actionType().displayName(), entry.actionType().color())
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            line("Staff", entry.staffName()),
            line("Cible", entry.targetName() == null || entry.targetName().isBlank() ? "Aucune" : entry.targetName()),
            line("Details", entry.details()),
            line("Date", FORMATTER.format(entry.timestamp()))
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack button(final Material material, final String name, final String description) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static Component line(final String label, final String value) {
        return Component.text(label + ": ", NamedTextColor.LIGHT_PURPLE)
            .append(Component.text(value, NamedTextColor.WHITE))
            .decoration(TextDecoration.ITALIC, false);
    }

    public record Holder(StaffActionLogger.StaffActionPage page) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
