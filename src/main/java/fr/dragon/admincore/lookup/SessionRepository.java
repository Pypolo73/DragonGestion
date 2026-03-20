package fr.dragon.admincore.lookup;

import fr.dragon.admincore.database.DatabaseManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SessionRepository {

    public record SessionPage(List<SessionRecord> entries, int page, boolean hasNext) {
    }

    private final DatabaseManager databaseManager;

    public SessionRepository(final DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Void> recordJoin(final UUID uuid, final String ip, final String name, final String server, final Instant joinedAt) {
        return this.databaseManager.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO sessions (uuid, ip, name, server, timestamp_join, timestamp_quit)
                VALUES (?, ?, ?, ?, ?, NULL)
                """)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, ip);
                statement.setString(3, name);
                statement.setString(4, server);
                statement.setLong(5, joinedAt.toEpochMilli());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> recordQuit(final UUID uuid, final Instant quitAt) {
        return this.databaseManager.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE sessions
                SET timestamp_quit = ?
                WHERE id = (
                    SELECT id
                    FROM (
                        SELECT id
                        FROM sessions
                        WHERE uuid = ? AND timestamp_quit IS NULL
                        ORDER BY timestamp_join DESC
                        LIMIT 1
                    ) latest
                )
                """)) {
                statement.setLong(1, quitAt.toEpochMilli());
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            }
        });
    }

    public CompletableFuture<SessionSummary> summary(final UUID uuid, final String name) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*) AS total_sessions,
                       COALESCE(SUM(COALESCE(timestamp_quit, ?) - timestamp_join), 0) AS total_playtime
                FROM sessions
                WHERE uuid = ? OR LOWER(name) = LOWER(?)
                """)) {
                statement.setLong(1, Instant.now().toEpochMilli());
                statement.setString(2, uuid == null ? null : uuid.toString());
                statement.setString(3, name);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return new SessionSummary(0, 0);
                    }
                    return new SessionSummary(resultSet.getLong("total_sessions"), resultSet.getLong("total_playtime"));
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Lecture du resume de sessions impossible", exception);
            }
        });
    }

    public CompletableFuture<SessionPage> page(final UUID uuid, final String name, final int page, final int pageSize) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT *
                FROM sessions
                WHERE uuid = ? OR LOWER(name) = LOWER(?)
                ORDER BY timestamp_join DESC
                LIMIT ? OFFSET ?
                """)) {
                statement.setString(1, uuid == null ? null : uuid.toString());
                statement.setString(2, name);
                statement.setInt(3, pageSize + 1);
                statement.setInt(4, Math.max(0, page) * pageSize);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<SessionRecord> entries = new ArrayList<>();
                    while (resultSet.next()) {
                        entries.add(map(resultSet));
                    }
                    final boolean hasNext = entries.size() > pageSize;
                    if (hasNext) {
                        entries.removeLast();
                    }
                    return new SessionPage(entries, Math.max(0, page), hasNext);
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Lecture des sessions impossible", exception);
            }
        });
    }

    private SessionRecord map(final ResultSet resultSet) throws Exception {
        final Long quitAt = (Long) resultSet.getObject("timestamp_quit");
        return new SessionRecord(
            resultSet.getLong("id"),
            UUID.fromString(resultSet.getString("uuid")),
            resultSet.getString("ip"),
            resultSet.getString("name"),
            resultSet.getString("server"),
            Instant.ofEpochMilli(resultSet.getLong("timestamp_join")),
            quitAt == null ? null : Instant.ofEpochMilli(quitAt)
        );
    }
}
