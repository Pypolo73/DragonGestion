package fr.dragon.admincore.inventory;

import java.util.UUID;

final class InventoryEditorSession {

    private final UUID viewerUuid;
    private final UUID targetUuid;
    private final String targetName;
    private final InventoryTarget targetType;
    private final InventoryDataHandle dataHandle;
    private boolean dirty;

    InventoryEditorSession(
        final UUID viewerUuid,
        final UUID targetUuid,
        final String targetName,
        final InventoryTarget targetType,
        final InventoryDataHandle dataHandle
    ) {
        this.viewerUuid = viewerUuid;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.targetType = targetType;
        this.dataHandle = dataHandle;
    }

    UUID viewerUuid() {
        return this.viewerUuid;
    }

    UUID targetUuid() {
        return this.targetUuid;
    }

    String targetName() {
        return this.targetName;
    }

    InventoryTarget targetType() {
        return this.targetType;
    }

    InventoryDataHandle dataHandle() {
        return this.dataHandle;
    }

    boolean dirty() {
        return this.dirty;
    }

    void markDirty() {
        this.dirty = true;
    }
}
