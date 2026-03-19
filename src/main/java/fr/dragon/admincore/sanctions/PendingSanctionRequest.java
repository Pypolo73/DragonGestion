package fr.dragon.admincore.sanctions;

import fr.dragon.admincore.core.StaffRole;
import java.time.Duration;
import java.util.UUID;

public record PendingSanctionRequest(
    int id,
    UUID requesterUuid,
    String requesterName,
    CreateSanctionRequest request,
    Duration duration,
    StaffRole requiredApproverRole
) {
}
