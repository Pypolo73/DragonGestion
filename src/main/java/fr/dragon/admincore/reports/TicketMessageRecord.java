package fr.dragon.admincore.reports;

import java.time.Instant;
import java.util.UUID;

public record TicketMessageRecord(
    long id,
    long ticketId,
    UUID authorUuid,
    String authorName,
    String content,
    Instant timestamp,
    boolean staffMessage
) {
}
