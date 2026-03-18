package fr.dragon.admincore.sanctions;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public record SanctionRecord(
    long id,
    UUID targetUuid,
    String targetName,
    UUID actorUuid,
    String actorName,
    SanctionType type,
    String reason,
    Instant createdAt,
    Instant expiresAt,
    boolean active,
    SanctionScope scope,
    String scopeValue
) {

    public boolean isPermanent() {
        return this.expiresAt == null;
    }

    public boolean isExpired(final Clock clock) {
        return this.expiresAt != null && this.expiresAt.isBefore(clock.instant());
    }

    public boolean isActive(final Clock clock) {
        return this.active && !isExpired(clock);
    }
}
