package fr.dragon.admincore.gui;

import fr.dragon.admincore.reports.TicketRecord;
import fr.dragon.admincore.reports.TicketRepository;
import fr.dragon.admincore.reports.TicketStatus;
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

public final class TicketMenu {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault());

    private TicketMenu() {
    }

    public static void openOpen(final Player player, final TicketRepository.TicketPage page) {
        final Inventory inventory = Bukkit.createInventory(new OpenHolder(page), 54, Component.text("Tickets actifs", NamedTextColor.GOLD));
        fillEntries(inventory, page.entries(), TicketView.ACTIVE);
        navigation(inventory, page.page(), page.hasNext());
        filters(inventory, TicketView.ACTIVE);
        player.openInventory(inventory);
    }

    public static void openClosed(final Player player, final TicketRepository.TicketPage page) {
        final Inventory inventory = Bukkit.createInventory(new StatusHolder(TicketView.CLOSED, page), 54, Component.text("Tickets fermes", NamedTextColor.GOLD));
        fillEntries(inventory, page.entries(), TicketView.CLOSED);
        navigation(inventory, page.page(), page.hasNext());
        filters(inventory, TicketView.CLOSED);
        player.openInventory(inventory);
    }

    public static void openArchives(final Player player, final TicketRepository.TicketPage page) {
        final Inventory inventory = Bukkit.createInventory(new StatusHolder(TicketView.ARCHIVED, page), 54, Component.text("Archives tickets", NamedTextColor.GOLD));
        fillEntries(inventory, page.entries(), TicketView.ARCHIVED);
        navigation(inventory, page.page(), page.hasNext());
        filters(inventory, TicketView.ARCHIVED);
        player.openInventory(inventory);
    }

    public static void openHistory(final Player player, final String targetName, final TicketRepository.TicketPage page) {
        final Inventory inventory = Bukkit.createInventory(new HistoryHolder(targetName, page), 54, Component.text("Historique tickets: " + targetName, NamedTextColor.GOLD));
        fillEntries(inventory, page.entries(), TicketView.HISTORY);
        navigation(inventory, page.page(), page.hasNext());
        inventory.setItem(49, button(Material.BARRIER, "Fermer", "Fermer le menu"));
        player.openInventory(inventory);
    }

    private static void fillEntries(final Inventory inventory, final List<TicketRecord> entries, final TicketView view) {
        for (int slot = 0; slot < Math.min(28, entries.size()); slot++) {
            inventory.setItem(slot, item(entries.get(slot), view));
        }
    }

    private static void navigation(final Inventory inventory, final int page, final boolean hasNext) {
        if (page > 0) {
            inventory.setItem(45, button(Material.ARROW, "Page precedente", "Revenir en arriere"));
        }
        inventory.setItem(49, button(Material.BARRIER, "Fermer", "Fermer le menu"));
        if (hasNext) {
            inventory.setItem(53, button(Material.ARROW, "Page suivante", "Continuer"));
        }
    }

    private static void filters(final Inventory inventory, final TicketView view) {
        inventory.setItem(36, filterButton(Material.WRITABLE_BOOK, "Tickets actifs", view == TicketView.ACTIVE));
        inventory.setItem(40, filterButton(Material.BOOK, "Tickets fermes", view == TicketView.CLOSED));
        inventory.setItem(44, filterButton(Material.ENDER_CHEST, "Archives", view == TicketView.ARCHIVED));
    }

    private static ItemStack filterButton(final Material material, final String name, final boolean selected) {
        return button(material, name, selected ? "Vue actuelle" : "Ouvrir cette vue");
    }

    private static ItemStack item(final TicketRecord ticket, final TicketView view) {
        final ItemStack item = new ItemStack(ticket.status() == TicketStatus.ARCHIVED ? Material.ENDER_PEARL : Material.BOOK);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("#" + ticket.id() + " " + ticket.targetName(), color(ticket.status()))
            .decoration(TextDecoration.ITALIC, false));
        final java.util.ArrayList<Component> lore = new java.util.ArrayList<>();
        lore.add(line("Reporter", ticket.reporterName()));
        lore.add(line("Cible", ticket.targetName()));
        lore.add(line("Discord", ticket.effectiveDiscord()));
        lore.add(line("Categorie", ticket.effectiveCategory()));
        if (!ticket.effectiveDescription().isBlank()) {
            lore.add(line("Details", ticket.effectiveDescription()));
        }
        lore.add(line("Statut", ticket.status().name()));
        lore.add(line("Assigne", ticket.assignedStaffName() == null ? "Aucun" : ticket.assignedStaffName()));
        lore.add(line("Date", DATE_FORMAT.format(ticket.timestamp())));
        if (ticket.closureNote() != null && !ticket.closureNote().isBlank()) {
            lore.add(line("Cloture", ticket.closureNote()));
        }
        if (view == TicketView.ACTIVE) {
            lore.add(Component.empty());
            lore.add(Component.text("Clic gauche: discussion / prise en charge", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Clic droit: fermer", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        } else if (view == TicketView.CLOSED) {
            lore.add(Component.empty());
            lore.add(Component.text("Shift + clic droit: archiver", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static Component line(final String label, final String value) {
        return Component.text(label + ": ", NamedTextColor.LIGHT_PURPLE)
            .append(Component.text(value, NamedTextColor.WHITE))
            .decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack button(final Material material, final String name, final String lore) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static NamedTextColor color(final TicketStatus status) {
        return switch (status) {
            case OPEN -> NamedTextColor.RED;
            case IN_PROGRESS -> NamedTextColor.YELLOW;
            case CLOSED -> NamedTextColor.GREEN;
            case ARCHIVED -> NamedTextColor.GRAY;
        };
    }

    public enum TicketView {
        ACTIVE,
        CLOSED,
        ARCHIVED,
        HISTORY
    }

    public record OpenHolder(TicketRepository.TicketPage page) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record StatusHolder(TicketView view, TicketRepository.TicketPage page) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record HistoryHolder(String targetName, TicketRepository.TicketPage page) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
