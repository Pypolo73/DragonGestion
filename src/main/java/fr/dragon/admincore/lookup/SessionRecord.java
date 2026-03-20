package fr.dragon.admincore.lookup;

import java.time.Instant;
import java.util.UUID;

public record SessionRecord(
    long id,
    UUID uuid,
    String ip,
    String name,
    String server,
    Instant joinedAt,
    Instant quitAt
) {
}
