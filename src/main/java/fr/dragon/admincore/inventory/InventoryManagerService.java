package fr.dragon.admincore.inventory;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class InventoryManagerService {

    private final AdminCorePlugin plugin;
    private final ConcurrentMap<UUID, InventoryEditorSession> sessions = new ConcurrentHashMap<>();
    private AxInventoryRestoreBridge backupBridge;

    public InventoryManagerService(final AdminCorePlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.backupBridge = new AxInventoryRestoreBridge(this.plugin);
    }

    public Optional<InventoryEditorSession> session(final UUID viewerUuid) {
        return Optional.ofNullable(this.sessions.get(viewerUuid));
    }

    public void openSelector(final Player viewer, final OfflinePlayer target) {
        InventoryMenus.openSelector(
            this.plugin,
            viewer,
            target.getUniqueId(),
            displayName(target),
            viewer.hasPermission(PermissionService.INVENTORY_EDIT)
        );
    }

    public void openReadOnly(final Player viewer, final OfflinePlayer target, final InventoryTarget targetType) {
        try {
            final InventoryDataHandle handle = new InventoryDataHandle(target);
            final ItemStack[] contents = handle.read(targetType);
            handle.close();
            InventoryMenus.openReadOnly(this.plugin, viewer, target.getUniqueId(), displayName(target), targetType, contents);
        } catch (final Exception exception) {
            this.plugin.getLogger().warning("Ouverture lecture seule impossible pour " + target.getUniqueId() + ": " + exception.getMessage());
            viewer.sendMessage(this.plugin.getMessageFormatter().message("inventory.open-error"));
        }
    }

    public void openEditor(final Player viewer, final OfflinePlayer target, final InventoryTarget targetType) {
        try {
            closeSession(viewer.getUniqueId());
            final InventoryDataHandle handle = new InventoryDataHandle(target);
            final InventoryEditorSession session = new InventoryEditorSession(
                viewer.getUniqueId(),
                target.getUniqueId(),
                displayName(target),
                targetType,
                handle
            );
            this.sessions.put(viewer.getUniqueId(), session);
            InventoryMenus.openEditor(this.plugin, viewer, session, handle.read(targetType));
        } catch (final Exception exception) {
            this.plugin.getLogger().warning("Ouverture edition impossible pour " + target.getUniqueId() + ": " + exception.getMessage());
            viewer.sendMessage(this.plugin.getMessageFormatter().message("inventory.open-error"));
        }
    }

    public void synchronize(final Player viewer, final Inventory inventory, final ItemStack cursor) {
        final InventoryEditorSession session = this.sessions.get(viewer.getUniqueId());
        if (session == null) {
            return;
        }
        final ItemStack[] contents = InventoryMenus.extractEditedContents(inventory, session.targetType());
        if (cursor != null && !cursor.getType().isAir()) {
            mergeCursor(contents, cursor);
            viewer.setItemOnCursor(null);
        }
        session.dataHandle().write(session.targetType(), contents);
    }

    public void shuffle(final Player viewer) {
        final InventoryEditorSession session = this.sessions.get(viewer.getUniqueId());
        if (session == null || session.targetType().isEnderChest()) {
            return;
        }
        final List<ItemStack> items = new ArrayList<>();
        for (final ItemStack item : session.dataHandle().read(session.targetType())) {
            if (item != null && !item.getType().isAir()) {
                items.add(item.clone());
            }
        }
        java.util.Collections.shuffle(items);
        final ItemStack[] shuffled = new ItemStack[session.targetType().contentSize()];
        for (int index = 0; index < Math.min(items.size(), shuffled.length); index++) {
            shuffled[index] = items.get(index);
        }
        session.dataHandle().write(session.targetType(), shuffled);
        reopenEditor(viewer, session.targetUuid(), session.targetName(), session.targetType());
    }

    public void openClearConfirm(final Player viewer, final UUID targetUuid, final String targetName, final InventoryTarget targetType) {
        InventoryMenus.openConfirm(
            this.plugin,
            viewer,
            targetUuid,
            targetName,
            targetType,
            InventoryConfirmAction.CLEAR,
            targetType.isEnderChest()
                ? "Vider l'enderchest de " + targetName + " ?"
                : "Vider l'inventaire de " + targetName + " ?",
            null,
            0
        );
    }

    public void clear(final Player viewer, final OfflinePlayer target, final InventoryTarget targetType) {
        try {
            final InventoryEditorSession session = this.sessions.get(viewer.getUniqueId());
            if (session != null) {
                session.dataHandle().write(targetType, new ItemStack[targetType.contentSize()]);
            } else {
                final InventoryDataHandle handle = new InventoryDataHandle(target);
                handle.write(targetType, new ItemStack[targetType.contentSize()]);
                handle.close();
            }
            viewer.sendMessage(this.plugin.getMessageFormatter().message(
                "inventory.cleared",
                this.plugin.getMessageFormatter().text("target", displayName(target))
            ));
            reopenEditor(viewer, target.getUniqueId(), displayName(target), targetType);
        } catch (final Exception exception) {
            this.plugin.getLogger().warning("Clear inventaire impossible pour " + target.getUniqueId() + ": " + exception.getMessage());
            viewer.sendMessage(this.plugin.getMessageFormatter().message("inventory.open-error"));
        }
    }

    public void openBackups(final Player viewer, final UUID targetUuid, final String targetName, final InventoryTarget targetType, final int page) {
        if (!viewer.hasPermission(PermissionService.INVENTORY_BACKUP)) {
            viewer.sendMessage(this.plugin.getMessageFormatter().message("errors.no-permission"));
            reopenEditor(viewer, targetUuid, targetName, targetType);
            return;
        }
        if (!backupsEnabled()) {
            viewer.sendMessage(this.plugin.getMessageFormatter().message("inventory.backup-disabled"));
            reopenEditor(viewer, targetUuid, targetName, targetType);
            return;
        }
        if (!this.backupBridge.isAvailable()) {
            viewer.sendMessage(this.plugin.getMessageFormatter().message("inventory.backup-unavailable"));
            reopenEditor(viewer, targetUuid, targetName, targetType);
            return;
        }
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        this.backupBridge.loadSnapshots(target, targetType).whenComplete((snapshots, throwable) ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (throwable != null) {
                    this.plugin.getLogger().warning("Lecture backups impossible pour " + targetUuid + ": " + throwable.getMessage());
                    viewer.sendMessage(this.plugin.getMessageFormatter().message("inventory.backup-unavailable"));
                    reopenEditor(viewer, targetUuid, targetName, targetType);
                    return;
                }
                InventoryMenus.openBackups(
                    this.plugin,
                    viewer,
                    targetUuid,
                    targetName,
                    targetType,
                    snapshots,
                    Math.max(0, page),
                    true,
                    retentionDays(),
                    maxSnapshotsPerPlayer(),
                    activeTriggers()
                );
            })
        );
    }

    public void openRestoreConfirm(
        final Player viewer,
        final UUID targetUuid,
        final String targetName,
        final InventoryTarget targetType,
        final InventoryBackupSnapshot snapshot,
        final int returnPage
    ) {
        final String formattedDate = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
            .format(snapshot.createdAt());
        InventoryMenus.openConfirm(
            this.plugin,
            viewer,
            targetUuid,
            targetName,
            targetType,
            InventoryConfirmAction.RESTORE,
            targetType.isEnderChest()
                ? "Restaurer l'enderchest du " + formattedDate + " ?"
                : "Restaurer l'inventaire du " + formattedDate + " ?",
            snapshot,
            returnPage
        );
    }

    public void restoreSnapshot(final Player viewer, final OfflinePlayer target, final InventoryTarget targetType, final InventoryBackupSnapshot snapshot) {
        this.backupBridge.loadItems(snapshot).whenComplete((items, throwable) ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (throwable != null) {
                    this.plugin.getLogger().warning("Restauration snapshot impossible pour " + target.getUniqueId() + ": " + throwable.getMessage());
                    viewer.sendMessage(this.plugin.getMessageFormatter().message("inventory.backup-restore-error"));
                    reopenEditor(viewer, target.getUniqueId(), displayName(target), targetType);
                    return;
                }
                try {
                    final InventoryDataHandle handle = new InventoryDataHandle(target);
                    handle.write(targetType, InventoryDataHandle.normalize(items, targetType.contentSize()));
                    handle.close();
                    viewer.sendMessage(this.plugin.getMessageFormatter().message(
                        "inventory.backup-restored",
                        this.plugin.getMessageFormatter().text("target", displayName(target)),
                        this.plugin.getMessageFormatter().text("type", snapshot.type())
                    ));
                    reopenEditor(viewer, target.getUniqueId(), displayName(target), targetType);
                } catch (final Exception exception) {
                    this.plugin.getLogger().warning("Application snapshot impossible pour " + target.getUniqueId() + ": " + exception.getMessage());
                    viewer.sendMessage(this.plugin.getMessageFormatter().message("inventory.backup-restore-error"));
                    reopenEditor(viewer, target.getUniqueId(), displayName(target), targetType);
                }
            })
        );
    }

    public void closeSession(final UUID viewerUuid) {
        final InventoryEditorSession removed = this.sessions.remove(viewerUuid);
        if (removed != null) {
            removed.dataHandle().close();
        }
    }

    public boolean backupsEnabled() {
        return this.plugin.getConfigLoader().inventoryBackup().getBoolean("enabled", true);
    }

    public void cancelConfirm(
        final Player viewer,
        final UUID targetUuid,
        final String targetName,
        final InventoryTarget targetType,
        final InventoryConfirmAction action,
        final int returnPage
    ) {
        if (action == InventoryConfirmAction.RESTORE) {
            openBackups(viewer, targetUuid, targetName, targetType, returnPage);
            return;
        }
        reopenEditor(viewer, targetUuid, targetName, targetType);
    }

    private void reopenEditor(final Player viewer, final UUID targetUuid, final String targetName, final InventoryTarget targetType) {
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () ->
            openEditor(viewer, Bukkit.getOfflinePlayer(targetUuid), targetType), 1L
        );
    }

    private int retentionDays() {
        return Math.max(0, this.plugin.getConfigLoader().inventoryBackup().getInt("retention-days", 30));
    }

    private int maxSnapshotsPerPlayer() {
        return Math.max(1, this.plugin.getConfigLoader().inventoryBackup().getInt("max-snapshots-per-player", 50));
    }

    private List<String> activeTriggers() {
        final ConfigurationSection section = this.plugin.getConfigLoader().inventoryBackup().getConfigurationSection("triggers");
        if (section == null) {
            return List.of();
        }
        return section.getKeys(false).stream()
            .filter(key -> section.getBoolean(key, false))
            .sorted()
            .toList();
    }

    private void mergeCursor(final ItemStack[] contents, final ItemStack cursor) {
        for (int index = 0; index < contents.length; index++) {
            if (contents[index] == null || contents[index].getType().isAir()) {
                contents[index] = cursor.clone();
                return;
            }
        }
    }

    private String displayName(final OfflinePlayer target) {
        return target.getName() == null ? target.getUniqueId().toString() : target.getName();
    }
}
