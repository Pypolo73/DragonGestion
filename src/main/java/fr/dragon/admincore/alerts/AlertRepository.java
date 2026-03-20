package fr.dragon.admincore.alerts;

import fr.dragon.admincore.database.DatabaseManager;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AlertRepository {

    private final DatabaseManager databaseManager;

    public AlertRepository(final DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Boolean> existsRecent(final AlertType type, final UUID targetUuid, final Instant since) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM alerts
                WHERE type = ?
                  AND uuid_cible = ?
                  AND timestamp >= ?
                LIMIT 1
                """)) {
                statement.setString(1, type.name());
                statement.setString(2, targetUuid.toString());
                statement.setLong(3, since.toEpochMilli());
                try (var resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Verification d'alerte recente impossible", exception);
            }
        });
    }

    public CompletableFuture<Void> insert(final AlertType type, final UUID targetUuid, final String details) {
        return this.databaseManager.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO alerts (type, uuid_cible, timestamp, details)
                VALUES (?, ?, ?, ?)
                """)) {
                statement.setString(1, type.name());
                statement.setString(2, targetUuid.toString());
                statement.setLong(3, Instant.now().toEpochMilli());
                statement.setString(4, details);
                statement.executeUpdate();
            }
        });
    }
}
