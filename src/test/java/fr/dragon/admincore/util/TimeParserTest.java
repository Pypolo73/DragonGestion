package fr.dragon.admincore.util;

import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimeParserTest {

    @Test
    void parsesCompositeDuration() {
        final Duration duration = TimeParser.parseOrThrow("1h30m");
        Assertions.assertEquals(Duration.ofMinutes(90), duration);
    }

    @Test
    void rejectsInvalidDuration() {
        Assertions.assertTrue(TimeParser.parse("abc").isEmpty());
    }

    @Test
    void formatsDuration() {
        Assertions.assertEquals("2d 3h 15m", TimeParser.format(Duration.ofDays(2).plusHours(3).plusMinutes(15)));
    }
}
