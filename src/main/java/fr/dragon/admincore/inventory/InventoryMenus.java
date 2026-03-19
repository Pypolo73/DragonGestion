package fr.dragon.admincore.inventory;

import fr.dragon.admincore.core.AdminCorePlugin;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class InventoryMenus {

    private static final int[] BACKUP_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private static final DateTimeFormatter SNAPSHOT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private InventoryMenus() {
    }

    static void openSelector(
        final AdminCorePlugin plugin,
        final Player viewer,
        final java.util.UUID targetUuid,
        final String targetName,
        final boolean canEdit
    ) {
        final Inventory inventory = Bukkit.createInventory(
            new InventoryMenuContext.SelectorHolder(targetUuid, targetName, canEdit),
            27,
            plugin.getMessageFormatter().deserialize(
                "<gray>Gestion de </gray><gold><player></gold>",
                plugin.getMessageFormatter().text("player", targetName)
            )
        );
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler());
        }
        inventory.setItem(10, action(Material.CHEST, "<gold>Voir l'inventaire</gold>", List.of("<gray>Ouverture en lecture seule.</gray>")));
        inventory.setItem(12, action(Material.ENDER_CHEST, "<light_purple>Voir l'enderchest</light_purple>", List.of("<gray>Ouverture en lecture seule.</gray>")));
        if (canEdit) {
            inventory.setItem(14, action(Material.HOPPER, "<green>Modifier l'inventaire</green>", List.of("<gray>Edition directe des items.</gray>")));
            inventory.setItem(16, action(Material.SHULKER_BOX, "<aqua>Modifier l'enderchest</aqua>", List.of("<gray>Edition directe des items.</gray>")));
        }
        inventory.setItem(22, action(Material.BARRIER, "<red>Fermer</red>", List.of()));
        viewer.openInventory(inventory);
    }

    static void openReadOnly(
        final AdminCorePlugin plugin,
        final Player viewer,
        final java.util.UUID targetUuid,
        final String targetName,
        final InventoryTarget targetType,
        final ItemStack[] contents
    ) {
        final Inventory inventory = Bukkit.createInventory(
            new InventoryMenuContext.ReadOnlyHolder(targetUuid, targetName, targetType),
            54,
            plugin.getMessageFormatter().deserialize(
                targetType.isEnderChest()
                    ? "<gray>Enderchest de </gray><gold><player></gold> <dark_gray>[lecture seule]</dark_gray>"
                    : "<gray>Inventaire de </gray><gold><player></gold> <dark_gray>[lecture seule]</dark_gray>",
                plugin.getMessageFormatter().text("player", targetName)
            )
        );
        fillContent(inventory, targetType, contents);
        viewer.openInventory(inventory);
    }

    static void openEditor(
        final AdminCorePlugin plugin,
        final Player viewer,
        final InventoryEditorSession session,
        final ItemStack[] contents
    ) {
        final Inventory inventory = Bukkit.createInventory(
            new InventoryMenuContext.EditHolder(session.viewerUuid(), session.targetUuid(), session.targetName(), session.targetType()),
            54,
            plugin.getMessageFormatter().deserialize(
                session.targetType().isEnderChest()
                    ? "<gray>Enderchest de </gray><gold><player></gold> <dark_gray>[edition]</dark_gray>"
                    : "<gray>Inventaire de </gray><gold><player></gold> <dark_gray>[edition]</dark_gray>",
                plugin.getMessageFormatter().text("player", session.targetName())
            )
        );
        fillContent(inventory, session.targetType(), contents);
        fillActionBar(plugin, inventory, session.targetType());
        viewer.openInventory(inventory);
    }

    static void openBackups(
        final AdminCorePlugin plugin,
        final Player viewer,
        final java.util.UUID targetUuid,
        final String targetName,
        final InventoryTarget targetType,
        final List<InventoryBackupSnapshot> snapshots,
        final int page,
        final boolean backupsEnabled,
        final int retentionDays,
        final int maxSnapshots,
        final List<String> activeTriggers
    ) {
        final Inventory inventory = Bukkit.createInventory(
            new InventoryMenuContext.BackupsHolder(targetUuid, targetName, targetType, snapshots, page),
            54,
            plugin.getMessageFormatter().deserialize(
                "<gray>Backups de </gray><gold><player></gold> <dark_gray>[<context>]</dark_gray>",
                plugin.getMessageFormatter().text("player", targetName),
                plugin.getMessageFormatter().text("context", targetType.label())
            )
        );
        final int start = page * BACKUP_SLOTS.length;
        for (int index = 0; index < BACKUP_SLOTS.length; index++) {
            final int snapshotIndex = start + index;
            if (snapshotIndex >= snapshots.size()) {
                break;
            }
            inventory.setItem(BACKUP_SLOTS[index], snapshotItem(snapshots.get(snapshotIndex)));
        }
        inventory.setItem(45, navItem(Material.ARROW, "<yellow>Page precedente</yellow>"));
        inventory.setItem(49, infoItem(backupsEnabled, retentionDays, maxSnapshots, activeTriggers));
        inventory.setItem(53, navItem(Material.ARROW, "<yellow>Page suivante</yellow>"));
        viewer.openInventory(inventory);
    }

    static void openConfirm(
        final AdminCorePlugin plugin,
        final Player viewer,
        final java.util.UUID targetUuid,
        final String targetName,
        final InventoryTarget targetType,
        final InventoryConfirmAction action,
        final String description,
        final InventoryBackupSnapshot snapshot,
        final int returnPage
    ) {
        final Inventory inventory = Bukkit.createInventory(
            new InventoryMenuContext.ConfirmHolder(targetUuid, targetName, targetType, action, snapshot, returnPage),
            27,
            plugin.getMessageFormatter().deserialize(
                "<gray>Confirmation </gray><gold><player></gold>",
                plugin.getMessageFormatter().text("player", targetName)
            )
        );
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler());
        }
        inventory.setItem(11, action(Material.LIME_WOOL, "<green>Confirmer</green>", List.of("<gray>Appliquer l'action.</gray>")));
        inventory.setItem(
            13,
            action(
                action == InventoryConfirmAction.CLEAR ? Material.BARREL : Material.CLOCK,
                action == InventoryConfirmAction.CLEAR ? "<red>Vider</red>" : "<gold>Restaurer</gold>",
                List.of("<white>" + description + "</white>")
            )
        );
        inventory.setItem(15, action(Material.RED_WOOL, "<red>Annuler</red>", List.of("<gray>Retour.</gray>")));
        viewer.openInventory(inventory);
    }

    static int backupPageSize() {
        return BACKUP_SLOTS.length;
    }

    static boolean isEditableContentSlot(final InventoryTarget targetType, final int rawSlot) {
        return rawSlot >= 0 && rawSlot < targetType.contentSize();
    }

    static ItemStack[] extractEditedContents(final Inventory inventory, final InventoryTarget targetType) {
        final ItemStack[] contents = new ItemStack[targetType.contentSize()];
        for (int slot = 0; slot < targetType.contentSize(); slot++) {
            contents[slot] = InventoryDataHandle.clone(inventory.getItem(slot));
        }
        return contents;
    }

    private static void fillContent(final Inventory inventory, final InventoryTarget targetType, final ItemStack[] contents) {
        for (int slot = 0; slot < targetType.contentSize(); slot++) {
            inventory.setItem(slot, slot < contents.length ? InventoryDataHandle.clone(contents[slot]) : null);
        }
    }

    private static void fillActionBar(final AdminCorePlugin plugin, final Inventory inventory, final InventoryTarget targetType) {
        for (int slot = 45; slot <= 53; slot++) {
            inventory.setItem(slot, filler());
        }
        if (!targetType.isEnderChest()) {
            inventory.setItem(45, action(Material.SLIME_BALL, "<green>Shuffle</green>", List.of("<gray>Melange aleatoirement les items.</gray>")));
        }
        inventory.setItem(
            49,
            action(Material.BARREL, "<red>Clear</red>", List.of(
                targetType.isEnderChest()
                    ? "<gray>Vide completement l'enderchest.</gray>"
                    : "<gray>Vide completement l'inventaire.</gray>"
            ))
        );
        inventory.setItem(53, action(Material.BOOK, "<gold>Backups</gold>", List.of("<gray>Voir et restaurer un snapshot.</gray>")));
        for (int slot = targetType.contentSize(); slot < 45; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler());
            }
        }
    }

    private static ItemStack snapshotItem(final InventoryBackupSnapshot snapshot) {
        return action(Material.CLOCK, "<gold>" + SNAPSHOT_DATE.format(snapshot.createdAt()) + "</gold>", List.of(
            "<gray>Type: <white>" + snapshot.type() + "</white></gray>",
            "<yellow>Clique pour restaurer.</yellow>"
        ));
    }

    private static ItemStack infoItem(
        final boolean backupsEnabled,
        final int retentionDays,
        final int maxSnapshots,
        final List<String> activeTriggers
    ) {
        final List<String> lore = new ArrayList<>();
        lore.add(backupsEnabled ? "<green>Backups actives</green>" : "<red>Backups desactivees</red>");
        lore.add("<gray>Retention: <white>" + retentionDays + " jours</white></gray>");
        lore.add("<gray>Max / joueur: <white>" + maxSnapshots + "</white></gray>");
        lore.add("<gray>Triggers:</gray>");
        if (activeTriggers.isEmpty()) {
            lore.add("<dark_gray>- aucun</dark_gray>");
        } else {
            for (final String trigger : activeTriggers) {
                lore.add("<gray>- <white>" + trigger + "</white></gray>");
            }
        }
        return action(Material.COMPARATOR, "<aqua>Etat des backups</aqua>", lore);
    }

    private static ItemStack navItem(final Material material, final String title) {
        return action(material, title, List.of());
    }

    private static ItemStack filler() {
        final ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.space());
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack action(final Material material, final String title, final List<String> loreLines) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(title));
        if (!loreLines.isEmpty()) {
            meta.lore(loreLines.stream().map(line ->
                net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(line)
            ).toList());
        }
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }
}
