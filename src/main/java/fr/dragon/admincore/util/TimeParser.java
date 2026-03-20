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

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\d+)(mo|d|h|m)", Pattern.CASE_INSENSITIVE);

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
            millis += switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
                case "m" -> amount * 60_000L;
                case "h" -> amount * 3_600_000L;
                case "d" -> amount * 86_400_000L;
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
        final long totalMinutes = Math.max(0L, (duration.toMillis() + 59_999L) / 60_000L);
        if (totalMinutes == 0L) {
            return "0m";
        }
        final List<String> parts = new ArrayList<>();
        long minutes = append(parts, totalMinutes, 43_200L, "mo");
        minutes = append(parts, minutes, 1_440L, "d");
        minutes = append(parts, minutes, 60L, "h");
        append(parts, minutes, 1L, "m");
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

    private static long append(final List<String> parts, final long sourceUnits, final long unitSize, final String suffix) {
        final long value = sourceUnits / unitSize;
        if (value > 0L) {
            parts.add(value + suffix);
        }
        return sourceUnits % unitSize;
    }
}
