package fr.dragon.admincore.core;

import java.util.UUID;

public record StaffMemberRecord(
    UUID uuid,
    String lastKnownName,
    StaffRole role
) {
}
