package fr.dragon.admincore.gui;

import fr.dragon.admincore.database.PlayerProfile;
import fr.dragon.admincore.sanctions.SanctionRecord;
import fr.dragon.admincore.util.PlayerDisplayResolver;
import fr.dragon.admincore.util.TimeParser;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class AdminCoreMenus {

    private static final TextColor TITLE = TextColor.color(0xFFE45E);
    private static final TextColor LABEL = TextColor.color(0xD83BFF);
    private static final TextColor VALUE = TextColor.color(0xF5F0FF);
    private static final TextColor SUCCESS = TextColor.color(0x5DFF7D);
    private static final TextColor INFO = TextColor.color(0x67B9FF);
    private static final TextColor WARNING = TextColor.color(0xFFAF45);
    private static final TextColor DANGER = TextColor.color(0xFF6B6B);
    private static final TextColor MUTED = TextColor.color(0xB8A7D9);

    private AdminCoreMenus() {
    }

    public static void openMain(final Player player) {
        final Inventory inventory = Bukkit.createInventory(new AdminCoreMenuContext.MainHolder(), 27, title("AdminCore"));
        inventory.setItem(10, item(Material.REDSTONE_BLOCK, titleLine("Sanctions actives"), List.of(
            lore("Bannissements et mutes en cours.", VALUE),
            lore("Clique pour consulter la liste.", INFO)
        )));
        inventory.setItem(12, playerHead(null, titleLine("Recherche joueur"), List.of(
            lore("Par profil ou par pseudo.", VALUE),
            lore("Interface staff detaillee.", INFO)
        )));
        inventory.setItem(14, item(Material.CLOCK, titleLine("Sanctions recentes"), List.of(
            lore("Les dernieres sanctions appliquees.", VALUE),
            lore("Acces rapide a l'historique.", INFO)
        )));
        inventory.setItem(13, item(Material.WRITABLE_BOOK, titleLine("Tickets"), List.of(
            lore("Reports joueurs ouverts.", VALUE),
            lore("Assignation et cloture staff.", INFO)
        )));
        inventory.setItem(16, item(Material.IRON_SWORD, titleLine("Joueurs online"), List.of(
            lore("Teleportation staff en vanish.", VALUE),
            lore("Surveillance rapide des joueurs.", INFO)
        )));
        player.openInventory(inventory);
    }

    public static void openActive(final Player player, final List<SanctionRecord> sanctions) {
        final Inventory inventory = Bukkit.createInventory(new AdminCoreMenuContext.ActiveHolder(sanctions), 54, title("Sanctions actives"));
        int slot = 0;
        for (final SanctionRecord sanction : sanctions.stream().limit(54).toList()) {
            inventory.setItem(slot++, sanctionItem(sanction));
        }
        player.openInventory(inventory);
    }

    public static void openSearchMode(final Player player) {
        final Inventory inventory = Bukkit.createInventory(new AdminCoreMenuContext.SearchModeHolder(), 27, title("Recherche joueur"));
        inventory.setItem(11, item(Material.BOOK, titleLine("Recherche par profil"), List.of(
            lore("Defilement des profils staff.", VALUE),
            lore("Vue detaillee des sanctions.", INFO)
        )));
        inventory.setItem(15, item(Material.NAME_TAG, titleLine("Choisir par pseudo"), List.of(
            lore("Ouvre une saisie de pseudo.", VALUE),
            lore("Puis propose les joueurs trouves.", INFO)
        )));
        player.openInventory(inventory);
    }

    public static void openSearchResults(final Player player, final String query, final List<AdminCoreMenuContext.SearchResultEntry> names) {
        final Inventory inventory = Bukkit.createInventory(new AdminCoreMenuContext.SearchResultsHolder(query, names), 54, title("Resultats: " + query));
        int slot = 0;
        for (final AdminCoreMenuContext.SearchResultEntry name : names.stream().limit(45).toList()) {
            inventory.setItem(slot++, playerResultItem(name));
        }
        inventory.setItem(49, item(Material.ARROW, titleLine("Retour"), List.of(lore("Retour au menu recherche.", MUTED))));
        player.openInventory(inventory);
    }

    public static void openProfiles(final Player player, final int page, final List<PlayerProfile> profiles) {
        final List<String> names = profiles.stream().map(PlayerProfile::name).toList();
        final Inventory inventory = Bukkit.createInventory(new AdminCoreMenuContext.ProfileListHolder(page, names), 54, title("Profils joueurs"));
        int slot = 0;
        for (final PlayerProfile profile : profiles.stream().limit(45).toList()) {
            inventory.setItem(slot++, profileItem(profile));
        }
        inventory.setItem(45, item(Material.ARROW, titleLine("Page precedente"), List.of(lore("Page " + Math.max(1, page), MUTED))));
        inventory.setItem(49, item(Material.COMPASS, titleLine("Retour"), List.of(lore("Retour a la recherche.", MUTED))));
        inventory.setItem(53, item(Material.ARROW, titleLine("Page suivante"), List.of(lore("Page " + (page + 2), MUTED))));
        player.openInventory(inventory);
    }

    public static void openRecent(final Player player, final List<SanctionRecord> sanctions) {
        final Inventory inventory = Bukkit.createInventory(new AdminCoreMenuContext.RecentHolder(sanctions), 54, title("Sanctions recentes"));
        int slot = 0;
        for (final SanctionRecord sanction : sanctions.stream().limit(54).toList()) {
            inventory.setItem(slot++, sanctionItem(sanction));
        }
        player.openInventory(inventory);
    }

    public static void openOnlinePlayers(final Player player, final List<Player> players) {
        final Inventory inventory = Bukkit.createInventory(new AdminCoreMenuContext.OnlineHolder(players), 54, title("Joueurs online"));
        int slot = 0;
        for (final Player target : players.stream().limit(54).toList()) {
            inventory.setItem(slot++, onlinePlayerItem(target));
        }
        player.openInventory(inventory);
    }

    private static ItemStack sanctionItem(final SanctionRecord sanction) {
        final Material material = switch (sanction.type()) {
            case BAN -> Material.BARRIER;
            case MUTE -> Material.NOTE_BLOCK;
            case KICK -> Material.IRON_BOOTS;
            case WARN -> Material.PAPER;
        };
        return item(material, titleLine(sanction.type().name() + " • " + sanction.targetName()), List.of(
            statLine("Etat", colored(severityLabel(1), severityColor(1))),
            statLine("Raison", colored(sanction.reason(), VALUE)),
            statLine("Par", colored(sanction.actorName(), INFO)),
            statLine("Expiration", colored(sanction.expiresAt() == null ? "Permanent" : TimeParser.formatRemaining(sanction.expiresAt()), WARNING))
        ));
    }

    private static ItemStack playerResultItem(final AdminCoreMenuContext.SearchResultEntry entry) {
        return playerHead(entry.name(), colored(entry.name(), TITLE), List.of(
            statLine("Profil", colored(severityLabel(entry.sanctionCount()), severityColor(entry.sanctionCount()))),
            statLine("Sanctions", colored(Integer.toString(entry.sanctionCount()), countColor(entry.sanctionCount()))),
            lore("Clique pour ouvrir l'historique.", INFO)
        ));
    }

    private static ItemStack profileItem(final PlayerProfile profile) {
        final OfflinePlayer offlinePlayer = profile.uuid() == null ? Bukkit.getOfflinePlayer(profile.name()) : Bukkit.getOfflinePlayer(profile.uuid());
        return playerHead(
            profile.name(),
            colored(profile.name(), TITLE),
            List.of(
                statLine("Grade", PlayerDisplayResolver.resolveGradeComponent(offlinePlayer)),
                statLine("Niveau", PlayerDisplayResolver.levelComponent(profile.level())),
                statLine("Punitions", colored(Integer.toString(profile.sanctionCount()), countColor(profile.sanctionCount()))),
                statLine("Types", colored(PlayerDisplayResolver.punishmentTypes(profile.sanctionTypes()), typeColor(profile.sanctionCount()))),
                statLine("Client", colored(PlayerDisplayResolver.client(profile), INFO)),
                statLine("IP", colored(profile.ip() == null ? "Inconnue" : profile.ip(), VALUE))
            )
        );
    }

    private static ItemStack onlinePlayerItem(final Player target) {
        return playerHead(target.getName(), colored(target.getName(), TITLE), List.of(
            lore("Clique pour te teleporter en vanish.", VALUE),
            lore("Puis sanctionner rapidement ce joueur.", INFO)
        ));
    }

    private static String severityLabel(final int sanctions) {
        if (sanctions >= 8) {
            return "Rouge";
        }
        if (sanctions >= 4) {
            return "Orange";
        }
        if (sanctions >= 1) {
            return "Jaune";
        }
        return "Vert";
    }

    private static ItemStack item(final Material material, final Component name, final List<Component> loreLines) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(loreLines);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack playerHead(final String ownerName, final Component displayName, final List<Component> loreLines) {
        final ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (ownerName != null && ownerName.length() <= 16) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerName));
        }
        meta.displayName(displayName);
        meta.lore(loreLines);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static Component title(final String value) {
        return Component.text(value, LABEL).decoration(TextDecoration.ITALIC, false);
    }

    private static Component titleLine(final String value) {
        return Component.text(value, TITLE).decoration(TextDecoration.ITALIC, false);
    }

    private static Component lore(final String value, final TextColor color) {
        return Component.text("▸ ", LABEL).append(colored(value, color)).decoration(TextDecoration.ITALIC, false);
    }

    private static Component statLine(final String label, final Component value) {
        return Component.text("▸ ", LABEL)
            .append(Component.text(label + ": ", LABEL).decoration(TextDecoration.ITALIC, false))
            .append(value.decoration(TextDecoration.ITALIC, false))
            .decoration(TextDecoration.ITALIC, false);
    }

    private static Component colored(final String value, final TextColor color) {
        return Component.text(value, color).decoration(TextDecoration.ITALIC, false);
    }

    private static TextColor severityColor(final int sanctions) {
        if (sanctions >= 8) {
            return DANGER;
        }
        if (sanctions >= 4) {
            return WARNING;
        }
        if (sanctions >= 1) {
            return TITLE;
        }
        return SUCCESS;
    }

    private static TextColor countColor(final int sanctions) {
        return sanctions <= 0 ? SUCCESS : sanctions >= 6 ? DANGER : WARNING;
    }

    private static TextColor typeColor(final int sanctions) {
        return sanctions <= 0 ? MUTED : sanctions >= 6 ? DANGER : INFO;
    }
}
