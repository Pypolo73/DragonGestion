package fr.dragon.admincore.gui;

import fr.dragon.admincore.chat.ChatHistoryEntry;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ChatHistoryMenu {

    private static final int PAGE_SIZE = 45;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private ChatHistoryMenu() {
    }

    public static void open(final Player player, final int page, final List<ChatHistoryEntry> entries) {
        open(player, page, null, entries);
    }

    public static void openForAuthor(final Player player, final String author, final int page, final List<ChatHistoryEntry> entries) {
        open(player, page, author, entries);
    }

    private static void open(final Player player, final int page, final String author, final List<ChatHistoryEntry> entries) {
        final Inventory inventory = Bukkit.createInventory(
            new Holder(page, entries, author),
            54,
            title(author == null ? "Messages recents" : "Messages: " + author)
        );
        final int start = Math.max(0, page) * PAGE_SIZE;
        final List<ChatHistoryEntry> pageEntries = entries.stream().skip(start).limit(PAGE_SIZE).toList();
        int slot = 0;
        for (final ChatHistoryEntry entry : pageEntries) {
            inventory.setItem(slot++, entryItem(entry, author != null));
        }
        inventory.setItem(45, navItem(Material.ARROW, "Page precedente"));
        inventory.setItem(49, navItem(Material.BOOK, "Fermer"));
        inventory.setItem(53, navItem(Material.ARROW, "Page suivante"));
        player.openInventory(inventory);
    }

    private static ItemStack entryItem(final ChatHistoryEntry entry, final boolean filtered) {
        final ItemStack item = new ItemStack(Material.PAPER);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(
                filtered ? "[" + TIME.format(entry.sentAt()) + "] " + entry.author() : "[" + TIME.format(entry.sentAt()) + "] " + entry.author(),
                NamedTextColor.GOLD
            )
            .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text(entry.message(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
            Component.text("Clic gauche: supprimer ce message.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
            Component.text("Clic droit: voir l'historique sanctions.", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack navItem(final Material material, final String label) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static Component title(final String value) {
        return Component.text(value, NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false);
    }

    public record Holder(int page, List<ChatHistoryEntry> entries, String authorFilter) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
