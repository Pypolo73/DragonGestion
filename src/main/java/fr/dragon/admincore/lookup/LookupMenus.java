package fr.dragon.admincore.lookup;

import fr.dragon.admincore.database.PlayerProfile;
import fr.dragon.admincore.util.PlayerDisplayResolver;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class LookupMenus {

    private static final int[] SESSION_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault());

    private LookupMenus() {
    }

    public static void openOverview(final Player viewer, final UUID targetUuid, final String targetName, final PlayerProfile profile, final SessionSummary summary) {
        final Inventory inventory = Bukkit.createInventory(new OverviewHolder(targetUuid, targetName), 27, Component.text("Lookup: " + targetName, NamedTextColor.GOLD));
        inventory.setItem(11, summaryItem(targetUuid, targetName, profile, summary));
        inventory.setItem(13, button(Material.PAPER, "Historique sanctions", "Ouvrir les sanctions du joueur"));
        inventory.setItem(15, button(Material.CLOCK, "Sessions", "Ouvrir les sessions du joueur"));
        viewer.openInventory(inventory);
    }

    public static void openSessions(final Player viewer, final UUID targetUuid, final String targetName, final SessionRepository.SessionPage page) {
        final Inventory inventory = Bukkit.createInventory(new SessionsHolder(targetUuid, targetName, page.page(), page.hasNext()), 54, Component.text("Sessions: " + targetName, NamedTextColor.GOLD));
        for (int index = 0; index < Math.min(page.entries().size(), SESSION_SLOTS.length); index++) {
            inventory.setItem(SESSION_SLOTS[index], sessionItem(page.entries().get(index)));
        }
        if (page.page() > 0) {
            inventory.setItem(45, button(Material.ARROW, "Page precedente", "Voir les sessions precedentes"));
        }
        inventory.setItem(49, button(Material.COMPASS, "Retour", "Retour au lookup"));
        if (page.hasNext()) {
            inventory.setItem(53, button(Material.ARROW, "Page suivante", "Voir les sessions suivantes"));
        }
        viewer.openInventory(inventory);
    }

    private static ItemStack summaryItem(final UUID targetUuid, final String targetName, final PlayerProfile profile, final SessionSummary summary) {
        final OfflinePlayer offlinePlayer = targetUuid == null ? Bukkit.getOfflinePlayer(targetName) : Bukkit.getOfflinePlayer(targetUuid);
        final ItemStack item = new ItemStack(Material.PAPER);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(targetName, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            line("Grade", PlayerDisplayResolver.resolveGradeComponent(offlinePlayer)),
            line("Niveau", PlayerDisplayResolver.levelComponent(profile.level())),
            line("IP", Component.text(profile.ip() == null ? "Inconnue" : profile.ip(), NamedTextColor.WHITE)),
            line("Client", Component.text(PlayerDisplayResolver.client(profile), NamedTextColor.WHITE)),
            line("Sessions", Component.text(Long.toString(summary.totalSessions()), NamedTextColor.AQUA)),
            line("Temps cumule", Component.text(formatDuration(summary.totalPlaytimeMillis()), NamedTextColor.GREEN))
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack sessionItem(final SessionRecord record) {
        final ItemStack item = new ItemStack(Material.PAPER);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(FORMATTER.format(record.joinedAt()), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("Pseudo: " + record.name(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
            Component.text("IP: " + record.ip(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false),
            Component.text("Serveur: " + record.server(), NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false),
            Component.text("Duree: " + sessionDuration(record), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack button(final Material material, final String name, final String lore) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private static Component line(final String label, final Component value) {
        return Component.text(label + ": ", NamedTextColor.LIGHT_PURPLE)
            .append(value.decoration(TextDecoration.ITALIC, false))
            .decoration(TextDecoration.ITALIC, false);
    }

    private static String sessionDuration(final SessionRecord record) {
        final long millis = Duration.between(record.joinedAt(), record.quitAt() == null ? java.time.Instant.now() : record.quitAt()).toMillis();
        return formatDuration(millis);
    }

    private static String formatDuration(final long millis) {
        final Duration duration = Duration.ofMillis(Math.max(0L, millis));
        final long hours = duration.toHours();
        final long minutes = duration.toMinutesPart();
        final long seconds = duration.toSecondsPart();
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    public record OverviewHolder(UUID targetUuid, String targetName) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record SessionsHolder(UUID targetUuid, String targetName, int page, boolean hasNext) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
