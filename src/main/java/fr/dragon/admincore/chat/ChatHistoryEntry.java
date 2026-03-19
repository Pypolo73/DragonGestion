package fr.dragon.admincore.chat;

import java.time.Instant;

public record ChatHistoryEntry(
    Instant sentAt,
    String author,
    String message
) {
}
