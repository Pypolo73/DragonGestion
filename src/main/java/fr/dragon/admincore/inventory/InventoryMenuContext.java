package fr.dragon.admincore.inventory;

import java.util.List;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class InventoryMenuContext {

    private InventoryMenuContext() {
    }

    public record ActionHolder(UUID targetUuid, String targetName, boolean canEdit, boolean canCreateBackup) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record SelectorHolder(UUID targetUuid, String targetName, InventorySelectorAction action) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record ReadOnlyHolder(UUID targetUuid, String targetName, InventoryTarget targetType) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record EditHolder(UUID viewerUuid, UUID targetUuid, String targetName, InventoryTarget targetType) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record BackupsHolder(
        UUID targetUuid,
        String targetName,
        InventoryTarget targetType,
        List<InventoryBackupSnapshot> snapshots,
        int page
    ) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record ConfirmHolder(
        UUID targetUuid,
        String targetName,
        InventoryTarget targetType,
        InventoryConfirmAction action,
        InventoryBackupSnapshot snapshot,
        int returnPage
    ) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
