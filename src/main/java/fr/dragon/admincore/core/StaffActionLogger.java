package fr.dragon.admincore.core;

import fr.dragon.admincore.database.DatabaseManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class StaffActionLogger {

    public record StaffActionPage(List<StaffActionEntry> entries, int page, boolean hasNext, String staffFilter) {
    }

    private static volatile StaffActionLogger instance;

    private final DatabaseManager databaseManager;

    private StaffActionLogger(final DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public static StaffActionLogger create(final DatabaseManager databaseManager) {
        final StaffActionLogger logger = new StaffActionLogger(databaseManager);
        instance = logger;
        return logger;
    }

    public static StaffActionLogger get() {
        if (instance == null) {
            throw new IllegalStateException("StaffActionLogger n'est pas initialise");
        }
        return instance;
    }

    public static void clear() {
        instance = null;
    }

    public CompletableFuture<Void> log(
        final UUID staffUuid,
        final String staffName,
        final StaffActionType actionType,
        final UUID targetUuid,
        final String targetName,
        final String details
    ) {
        final String safeName = safeName(staffName);
        final String safeDetails = truncate(Objects.requireNonNullElse(details, ""), 255);
        return this.databaseManager.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO staff_actions (uuid_staff, name_staff, action_type, target_uuid, target_name, details, timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
                if (staffUuid == null) {
                    statement.setNull(1, Types.VARCHAR);
                } else {
                    statement.setString(1, staffUuid.toString());
                }
                statement.setString(2, safeName);
                statement.setString(3, actionType.name());
                if (targetUuid == null) {
                    statement.setNull(4, Types.VARCHAR);
                } else {
                    statement.setString(4, targetUuid.toString());
                }
                if (targetName == null || targetName.isBlank()) {
                    statement.setNull(5, Types.VARCHAR);
                } else {
                    statement.setString(5, truncate(targetName, 32));
                }
                statement.setString(6, safeDetails);
                statement.setLong(7, Instant.now().toEpochMilli());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> log(
        final CommandSender sender,
        final StaffActionType actionType,
        final UUID targetUuid,
        final String targetName,
        final String details
    ) {
        final UUID staffUuid = sender instanceof Player player ? player.getUniqueId() : null;
        return log(staffUuid, sender.getName(), actionType, targetUuid, targetName, details);
    }

    public CompletableFuture<StaffActionPage> page(final String staffFilter, final int page, final int pageSize) {
        final int safePage = Math.max(0, page);
        final int safePageSize = Math.max(1, pageSize);
        return this.databaseManager.query(connection -> {
            final boolean filtered = staffFilter != null && !staffFilter.isBlank();
            final String sql = filtered
                ? """
                    SELECT uuid_staff, name_staff, action_type, target_uuid, target_name, details, timestamp
                    FROM staff_actions
                    WHERE LOWER(name_staff) = LOWER(?)
                    ORDER BY timestamp DESC
                    LIMIT ? OFFSET ?
                    """
                : """
                    SELECT uuid_staff, name_staff, action_type, target_uuid, target_name, details, timestamp
                    FROM staff_actions
                    ORDER BY timestamp DESC
                    LIMIT ? OFFSET ?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = 1;
                if (filtered) {
                    statement.setString(index++, staffFilter);
                }
                statement.setInt(index++, safePageSize + 1);
                statement.setInt(index, safePage * safePageSize);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<StaffActionEntry> entries = new ArrayList<>();
                    while (resultSet.next()) {
                        entries.add(map(resultSet));
                    }
                    final boolean hasNext = entries.size() > safePageSize;
                    if (hasNext) {
                        entries.removeLast();
                    }
                    return new StaffActionPage(entries, safePage, hasNext, filtered ? staffFilter : null);
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Lecture des logs staff impossible", exception);
            }
        });
    }

    private StaffActionEntry map(final ResultSet resultSet) throws Exception {
        final String staffUuid = resultSet.getString("uuid_staff");
        final String targetUuid = resultSet.getString("target_uuid");
        final String rawType = resultSet.getString("action_type");
        final StaffActionType type;
        try {
            type = StaffActionType.valueOf(rawType.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return new StaffActionEntry(
                staffUuid == null ? null : UUID.fromString(staffUuid),
                resultSet.getString("name_staff"),
                StaffActionType.LUCKPERMS_EDIT,
                targetUuid == null ? null : UUID.fromString(targetUuid),
                resultSet.getString("target_name"),
                resultSet.getString("details"),
                Instant.ofEpochMilli(resultSet.getLong("timestamp"))
            );
        }
        return new StaffActionEntry(
            staffUuid == null ? null : UUID.fromString(staffUuid),
            resultSet.getString("name_staff"),
            type,
            targetUuid == null ? null : UUID.fromString(targetUuid),
            resultSet.getString("target_name"),
            resultSet.getString("details"),
            Instant.ofEpochMilli(resultSet.getLong("timestamp"))
        );
    }

    private String safeName(final String value) {
        if (value == null || value.isBlank()) {
            return "Console";
        }
        return truncate(value, 32);
    }

    private String truncate(final String value, final int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
