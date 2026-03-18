package fr.dragon.admincore.sanctions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SanctionRecordTest {

    @Test
    void activeWhenNotExpired() {
        final Clock clock = Clock.fixed(Instant.parse("2026-03-18T10:00:00Z"), ZoneOffset.UTC);
        final SanctionRecord record = new SanctionRecord(
            1L,
            UUID.randomUUID(),
            "Notch",
            UUID.randomUUID(),
            "Admin",
            SanctionType.BAN,
            "Test",
            Instant.parse("2026-03-18T09:00:00Z"),
            Instant.parse("2026-03-18T12:00:00Z"),
            true,
            SanctionScope.PLAYER,
            "value"
        );
        Assertions.assertTrue(record.isActive(clock));
    }

    @Test
    void inactiveWhenExpired() {
        final Clock clock = Clock.fixed(Instant.parse("2026-03-18T13:00:00Z"), ZoneOffset.UTC);
        final SanctionRecord record = new SanctionRecord(
            1L,
            UUID.randomUUID(),
            "Notch",
            UUID.randomUUID(),
            "Admin",
            SanctionType.MUTE,
            "Test",
            Instant.parse("2026-03-18T09:00:00Z"),
            Instant.parse("2026-03-18T12:00:00Z"),
            true,
            SanctionScope.PLAYER,
            "value"
        );
        Assertions.assertFalse(record.isActive(clock));
    }
}
