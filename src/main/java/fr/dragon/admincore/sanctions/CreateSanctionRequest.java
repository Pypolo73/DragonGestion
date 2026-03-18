package fr.dragon.admincore.sanctions;

import java.time.Instant;
import java.util.UUID;

public record CreateSanctionRequest(
    UUID targetUuid,
    String targetName,
    UUID actorUuid,
    String actorName,
    SanctionType type,
    String reason,
    Instant expiresAt,
    SanctionScope scope,
    String scopeValue
) {
}
