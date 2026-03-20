package fr.dragon.admincore.core;

import java.time.Instant;
import java.util.UUID;

public record StaffActionEntry(
    UUID staffUuid,
    String staffName,
    StaffActionType actionType,
    UUID targetUuid,
    String targetName,
    String details,
    Instant timestamp
) {
}
