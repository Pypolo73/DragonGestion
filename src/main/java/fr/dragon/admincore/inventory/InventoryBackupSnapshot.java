package fr.dragon.admincore.inventory;

import java.time.Instant;

public record InventoryBackupSnapshot(
    int id,
    Instant createdAt,
    String type,
    Object handle
) {
}
