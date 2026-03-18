package fr.dragon.admincore.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NoteRepository {

    public record NoteEntry(long id, UUID targetUuid, String targetName, UUID actorUuid, String actorName, String note, Instant createdAt) {
    }

    private final DatabaseManager databaseManager;

    public NoteRepository(final DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public java.util.concurrent.CompletableFuture<Void> insert(
        final UUID targetUuid,
        final String targetName,
        final UUID actorUuid,
        final String actorName,
        final String note
    ) {
        return this.databaseManager.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO player_notes (target_uuid, target_name, actor_uuid, actor_name, note, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
                statement.setString(1, targetUuid == null ? null : targetUuid.toString());
                statement.setString(2, targetName);
                statement.setString(3, actorUuid == null ? null : actorUuid.toString());
                statement.setString(4, actorName);
                statement.setString(5, note);
                statement.setLong(6, Instant.now().toEpochMilli());
                statement.executeUpdate();
            }
        });
    }

    public java.util.concurrent.CompletableFuture<List<NoteEntry>> findAll(final UUID targetUuid, final String targetName) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT *
                FROM player_notes
                WHERE (target_uuid IS NOT NULL AND target_uuid = ?)
                   OR LOWER(target_name) = LOWER(?)
                ORDER BY created_at DESC
                """)) {
                statement.setString(1, targetUuid == null ? null : targetUuid.toString());
                statement.setString(2, targetName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<NoteEntry> entries = new ArrayList<>();
                    while (resultSet.next()) {
                        entries.add(new NoteEntry(
                            resultSet.getLong("id"),
                            resultSet.getString("target_uuid") == null ? null : UUID.fromString(resultSet.getString("target_uuid")),
                            resultSet.getString("target_name"),
                            resultSet.getString("actor_uuid") == null ? null : UUID.fromString(resultSet.getString("actor_uuid")),
                            resultSet.getString("actor_name"),
                            resultSet.getString("note"),
                            Instant.ofEpochMilli(resultSet.getLong("created_at"))
                        ));
                    }
                    return entries;
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Impossible de lire les notes", exception);
            }
        });
    }
}
