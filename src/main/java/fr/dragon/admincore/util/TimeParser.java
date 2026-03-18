package fr.dragon.admincore.util;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\d+)(mo|w|d|h|m|s)", Pattern.CASE_INSENSITIVE);

    private TimeParser() {
    }

    public static Optional<Duration> parse(final String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        final String compact = input.toLowerCase(Locale.ROOT).replace(" ", "");
        final Matcher matcher = TOKEN_PATTERN.matcher(compact);
        long millis = 0L;
        int matched = 0;
        while (matcher.find()) {
            matched += matcher.group(0).length();
            final long amount = Long.parseLong(matcher.group(1));
            millis += switch (matcher.group(2)) {
                case "s" -> amount * 1_000L;
                case "m" -> amount * 60_000L;
                case "h" -> amount * 3_600_000L;
                case "d" -> amount * 86_400_000L;
                case "w" -> amount * 604_800_000L;
                case "mo" -> amount * 2_592_000_000L;
                default -> 0L;
            };
        }
        if (millis <= 0L || matched != compact.length()) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofMillis(millis));
    }

    public static Duration parseOrThrow(final String input) {
        return parse(input).orElseThrow(() -> new IllegalArgumentException("Duree invalide: " + input));
    }

    public static String format(final Duration duration) {
        long seconds = Math.max(0L, duration.toSeconds());
        if (seconds == 0L) {
            return "0s";
        }
        final List<String> parts = new ArrayList<>();
        seconds = append(parts, seconds, 2_592_000L, "mo");
        seconds = append(parts, seconds, 604_800L, "w");
        seconds = append(parts, seconds, 86_400L, "d");
        seconds = append(parts, seconds, 3_600L, "h");
        seconds = append(parts, seconds, 60L, "m");
        append(parts, seconds, 1L, "s");
        return String.join(" ", parts);
    }

    public static String formatRemaining(final Instant expiry) {
        if (expiry == null) {
            return "Permanent";
        }
        final Duration remaining = Duration.between(Instant.now(), expiry);
        if (remaining.isNegative() || remaining.isZero()) {
            return "Expire";
        }
        return format(remaining);
    }

    private static long append(final List<String> parts, final long sourceSeconds, final long unitSeconds, final String suffix) {
        final long value = sourceSeconds / unitSeconds;
        if (value > 0L) {
            parts.add(value + suffix);
        }
        return sourceSeconds % unitSeconds;
    }
}
