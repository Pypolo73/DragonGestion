package fr.dragon.admincore.inventory;

import fr.dragon.admincore.core.AdminCorePlugin;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public final class InventoryListener implements Listener {

    private final AdminCorePlugin plugin;

    public InventoryListener(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        final Object holder = event.getInventory().getHolder();
        if (holder instanceof InventoryMenuContext.SelectorHolder selectorHolder) {
            handleSelectorClick(player, selectorHolder, event);
            return;
        }
        if (holder instanceof InventoryMenuContext.ReadOnlyHolder) {
            event.setCancelled(true);
            return;
        }
        if (holder instanceof InventoryMenuContext.EditHolder editHolder) {
            handleEditClick(player, editHolder, event);
            return;
        }
        if (holder instanceof InventoryMenuContext.BackupsHolder backupsHolder) {
            handleBackupClick(player, backupsHolder, event);
            return;
        }
        if (holder instanceof InventoryMenuContext.ConfirmHolder confirmHolder) {
            handleConfirmClick(player, confirmHolder, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        final Object holder = event.getInventory().getHolder();
        if (holder instanceof InventoryMenuContext.SelectorHolder
            || holder instanceof InventoryMenuContext.ReadOnlyHolder
            || holder instanceof InventoryMenuContext.BackupsHolder
            || holder instanceof InventoryMenuContext.ConfirmHolder) {
            event.setCancelled(true);
            return;
        }
        if (!(holder instanceof InventoryMenuContext.EditHolder editHolder)) {
            return;
        }
        for (final int rawSlot : event.getRawSlots()) {
            if (!InventoryMenus.isEditableContentSlot(editHolder.targetType(), rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
        this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
            this.plugin.getInventoryManagerService().synchronize(player, event.getInventory(), player.getItemOnCursor())
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        final Object holder = event.getInventory().getHolder();
        if (!(holder instanceof InventoryMenuContext.EditHolder)) {
            return;
        }
        this.plugin.getInventoryManagerService().synchronize(player, event.getInventory(), player.getItemOnCursor());
        this.plugin.getInventoryManagerService().closeSession(player.getUniqueId());
    }

    private void handleSelectorClick(final Player player, final InventoryMenuContext.SelectorHolder holder, final InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        final org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(holder.targetUuid());
        if (event.getRawSlot() == 10) {
            this.plugin.getInventoryManagerService().openReadOnly(player, target, InventoryTarget.INVENTORY);
            return;
        }
        if (event.getRawSlot() == 12) {
            this.plugin.getInventoryManagerService().openReadOnly(player, target, InventoryTarget.ENDER_CHEST);
            return;
        }
        if (holder.canEdit() && event.getRawSlot() == 14) {
            this.plugin.getInventoryManagerService().openEditor(player, target, InventoryTarget.INVENTORY);
            return;
        }
        if (holder.canEdit() && event.getRawSlot() == 16) {
            this.plugin.getInventoryManagerService().openEditor(player, target, InventoryTarget.ENDER_CHEST);
            return;
        }
        if (event.getRawSlot() == 22) {
            player.closeInventory();
        }
    }

    private void handleEditClick(final Player player, final InventoryMenuContext.EditHolder holder, final InventoryClickEvent event) {
        final int rawSlot = event.getRawSlot();
        final InventoryView view = event.getView();
        if (rawSlot >= view.getTopInventory().getSize() || event.getClickedInventory() == null) {
            event.setCancelled(true);
            return;
        }
        if (rawSlot == 45 && !holder.targetType().isEnderChest()) {
            event.setCancelled(true);
            this.plugin.getInventoryManagerService().shuffle(player);
            return;
        }
        if (rawSlot == 49) {
            event.setCancelled(true);
            this.plugin.getInventoryManagerService().openClearConfirm(player, holder.targetUuid(), holder.targetName(), holder.targetType());
            return;
        }
        if (rawSlot == 53) {
            event.setCancelled(true);
            this.plugin.getInventoryManagerService().openBackups(player, holder.targetUuid(), holder.targetName(), holder.targetType(), 0);
            return;
        }
        if (!InventoryMenus.isEditableContentSlot(holder.targetType(), rawSlot)) {
            event.setCancelled(true);
            return;
        }
        if (event.getClick() == ClickType.DOUBLE_CLICK
            || event.getClick() == ClickType.NUMBER_KEY
            || event.getClick() == ClickType.SWAP_OFFHAND
            || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
            || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
            this.plugin.getInventoryManagerService().synchronize(player, view.getTopInventory(), player.getItemOnCursor())
        );
    }

    private void handleBackupClick(final Player player, final InventoryMenuContext.BackupsHolder holder, final InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == 45) {
            if (holder.page() <= 0) {
                return;
            }
            this.plugin.getInventoryManagerService().openBackups(player, holder.targetUuid(), holder.targetName(), holder.targetType(), holder.page() - 1);
            return;
        }
        if (event.getRawSlot() == 53) {
            final int nextPage = holder.page() + 1;
            if (nextPage * InventoryMenus.backupPageSize() >= holder.snapshots().size()) {
                return;
            }
            this.plugin.getInventoryManagerService().openBackups(player, holder.targetUuid(), holder.targetName(), holder.targetType(), nextPage);
            return;
        }
        if (event.getRawSlot() == 49) {
            return;
        }
        final int index = backupIndex(event.getRawSlot(), holder.page());
        if (index < 0 || index >= holder.snapshots().size()) {
            return;
        }
        this.plugin.getInventoryManagerService().openRestoreConfirm(
            player,
            holder.targetUuid(),
            holder.targetName(),
            holder.targetType(),
            holder.snapshots().get(index),
            holder.page()
        );
    }

    private void handleConfirmClick(final Player player, final InventoryMenuContext.ConfirmHolder holder, final InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        final org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(holder.targetUuid());
        if (event.getRawSlot() == 11) {
            if (holder.action() == InventoryConfirmAction.RESTORE && holder.snapshot() != null) {
                this.plugin.getInventoryManagerService().restoreSnapshot(player, target, holder.targetType(), holder.snapshot());
                return;
            }
            if (holder.action() == InventoryConfirmAction.CLEAR) {
                this.plugin.getInventoryManagerService().clear(player, target, holder.targetType());
            }
            return;
        }
        if (event.getRawSlot() == 15) {
            this.plugin.getInventoryManagerService().cancelConfirm(
                player,
                holder.targetUuid(),
                holder.targetName(),
                holder.targetType(),
                holder.action(),
                holder.returnPage()
            );
        }
    }

    private int backupIndex(final int rawSlot, final int page) {
        final List<Integer> slots = java.util.Arrays.stream(new int[] {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        }).boxed().toList();
        final int position = slots.indexOf(rawSlot);
        return position < 0 ? -1 : page * slots.size() + position;
    }
}
