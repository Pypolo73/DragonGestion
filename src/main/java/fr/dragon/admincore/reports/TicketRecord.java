package fr.dragon.admincore.reports;

import java.time.Instant;
import java.util.UUID;

public record TicketRecord(
    long id,
    UUID reporterUuid,
    UUID targetUuid,
    String reporterName,
    String targetName,
    String reason,
    String reporterDiscord,
    String category,
    String description,
    TicketStatus status,
    UUID assignedStaffUuid,
    String assignedStaffName,
    String closureNote,
    Instant timestamp
) {

    public String effectiveDiscord() {
        return this.reporterDiscord == null || this.reporterDiscord.isBlank() ? "Non inscrit" : this.reporterDiscord;
    }

    public String effectiveCategory() {
        return this.category == null || this.category.isBlank() ? this.reason : this.category;
    }

    public String effectiveDescription() {
        return this.description == null ? "" : this.description;
    }
}
