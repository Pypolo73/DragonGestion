package fr.dragon.admincore.database;

import fr.dragon.admincore.sanctions.CreateSanctionRequest;
import fr.dragon.admincore.sanctions.SanctionRecord;
import fr.dragon.admincore.sanctions.SanctionScope;
import fr.dragon.admincore.sanctions.SanctionType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SanctionRepository {

    private final DatabaseManager databaseManager;

    public SanctionRepository(final DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public java.util.concurrent.CompletableFuture<SanctionRecord> insert(final CreateSanctionRequest request) {
        return insert(request, null);
    }

    public java.util.concurrent.CompletableFuture<SanctionRecord> insert(final CreateSanctionRequest request, final UUID linkedTargetUuid) {
        return this.databaseManager.query(connection -> {
            final Instant now = Instant.now();
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO sanctions (
                    target_uuid, target_name, actor_uuid, actor_name, type, reason, created_at, expires_at, active, scope, scope_value, linked_target_uuid
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, request.targetUuid() == null ? null : request.targetUuid().toString());
                statement.setString(2, request.targetName());
                statement.setString(3, request.actorUuid() == null ? null : request.actorUuid().toString());
                statement.setString(4, request.actorName());
                statement.setString(5, request.type().name());
                statement.setString(6, request.reason());
                statement.setLong(7, now.toEpochMilli());
                if (request.expiresAt() != null) {
                    statement.setLong(8, request.expiresAt().toEpochMilli());
                } else {
                    statement.setNull(8, java.sql.Types.BIGINT);
                }
                statement.setInt(9, request.type() == SanctionType.KICK ? 0 : 1);
                statement.setString(10, request.scope().name());
                statement.setString(11, request.scopeValue());
                statement.setString(12, linkedTargetUuid == null ? null : linkedTargetUuid.toString());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    final long id = keys.next() ? keys.getLong(1) : -1L;
                    return new SanctionRecord(
                        id,
                        request.targetUuid(),
                        request.targetName(),
                        request.actorUuid(),
                        request.actorName(),
                        request.type(),
                        request.reason(),
                        now,
                        request.expiresAt(),
                        request.type() != SanctionType.KICK,
                        request.scope(),
                        request.scopeValue()
                    );
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Insertion de sanction impossible", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<Integer> deactivate(final SanctionType type, final UUID targetUuid, final String targetName) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE sanctions
                SET active = 0
                WHERE type = ? AND active = 1
                  AND ((target_uuid IS NOT NULL AND target_uuid = ?) OR LOWER(target_name) = LOWER(?))
                """)) {
                statement.setString(1, type.name());
                statement.setString(2, targetUuid == null ? null : targetUuid.toString());
                statement.setString(3, targetName);
                return statement.executeUpdate();
            } catch (final Exception exception) {
                throw new IllegalStateException("Desactivation de sanction impossible", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<Integer> deactivateLinkedIp(final SanctionType type, final UUID linkedTargetUuid) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE sanctions
                SET active = 0
                WHERE type = ? AND active = 1
                  AND scope = 'IP'
                  AND linked_target_uuid = ?
                """)) {
                statement.setString(1, type.name());
                statement.setString(2, linkedTargetUuid == null ? null : linkedTargetUuid.toString());
                return statement.executeUpdate();
            } catch (final Exception exception) {
                throw new IllegalStateException("Desactivation des sanctions IP liees impossible", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<Optional<SanctionRecord>> findActive(
        final UUID targetUuid,
        final String targetName,
        final String ip,
        final EnumSet<SanctionType> types
    ) {
        return this.databaseManager.query(connection -> {
            final String placeholders = String.join(",", types.stream().map(ignored -> "?").toList());
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT *
                FROM sanctions
                WHERE active = 1
                  AND type IN (%s)
                  AND (
                      (scope = 'PLAYER' AND ((target_uuid IS NOT NULL AND target_uuid = ?) OR LOWER(target_name) = LOWER(?)))
                      OR (scope = 'IP' AND scope_value = ?)
                  )
                ORDER BY CASE WHEN scope = 'PLAYER' THEN 0 ELSE 1 END ASC, created_at DESC
                LIMIT 1
                """.formatted(placeholders))) {
                int index = 1;
                for (final SanctionType type : types) {
                    statement.setString(index++, type.name());
                }
                statement.setString(index++, targetUuid == null ? null : targetUuid.toString());
                statement.setString(index++, targetName);
                statement.setString(index, ip);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Lecture de sanction active impossible", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<List<SanctionRecord>> history(final UUID targetUuid, final String targetName, final int limit) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT *
                FROM sanctions
                WHERE (target_uuid IS NOT NULL AND target_uuid = ?)
                   OR LOWER(target_name) = LOWER(?)
                ORDER BY created_at DESC
                LIMIT ?
                """)) {
                statement.setString(1, targetUuid == null ? null : targetUuid.toString());
                statement.setString(2, targetName);
                statement.setInt(3, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<SanctionRecord> entries = new ArrayList<>();
                    while (resultSet.next()) {
                        entries.add(map(resultSet));
                    }
                    return entries;
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Lecture d'historique impossible", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<List<SanctionRecord>> activeWarnings(final UUID targetUuid, final String targetName) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT *
                FROM sanctions
                WHERE type = 'WARN' AND active = 1
                  AND ((target_uuid IS NOT NULL AND target_uuid = ?) OR LOWER(target_name) = LOWER(?))
                ORDER BY created_at DESC
                """)) {
                statement.setString(1, targetUuid == null ? null : targetUuid.toString());
                statement.setString(2, targetName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<SanctionRecord> entries = new ArrayList<>();
                    while (resultSet.next()) {
                        entries.add(map(resultSet));
                    }
                    return entries;
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Lecture des warnings impossible", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<Integer> clearWarnings(final UUID targetUuid, final String targetName) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE sanctions
                SET active = 0
                WHERE type = 'WARN' AND active = 1
                  AND ((target_uuid IS NOT NULL AND target_uuid = ?) OR LOWER(target_name) = LOWER(?))
                """)) {
                statement.setString(1, targetUuid == null ? null : targetUuid.toString());
                statement.setString(2, targetName);
                return statement.executeUpdate();
            } catch (final Exception exception) {
                throw new IllegalStateException("Suppression des warnings impossible", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<List<SanctionRecord>> allSanctions() {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM sanctions ORDER BY created_at DESC");
                 ResultSet resultSet = statement.executeQuery()) {
                final List<SanctionRecord> entries = new ArrayList<>();
                while (resultSet.next()) {
                    entries.add(map(resultSet));
                }
                return entries;
            } catch (final Exception exception) {
                throw new IllegalStateException("Export des sanctions impossible", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<List<SanctionRecord>> recentActive(final int limit) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT *
                FROM sanctions
                WHERE active = 1
                ORDER BY created_at DESC
                LIMIT ?
                """)) {
                statement.setInt(1, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<SanctionRecord> entries = new ArrayList<>();
                    while (resultSet.next()) {
                        entries.add(map(resultSet));
                    }
                    return entries;
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Lecture des sanctions actives impossible", exception);
            }
        });
    }

    private SanctionRecord map(final ResultSet resultSet) throws Exception {
        final String targetUuid = resultSet.getString("target_uuid");
        final String actorUuid = resultSet.getString("actor_uuid");
        final Long expiresAt = (Long) resultSet.getObject("expires_at");
        return new SanctionRecord(
            resultSet.getLong("id"),
            targetUuid == null ? null : UUID.fromString(targetUuid),
            resultSet.getString("target_name"),
            actorUuid == null ? null : UUID.fromString(actorUuid),
            resultSet.getString("actor_name"),
            SanctionType.valueOf(resultSet.getString("type")),
            resultSet.getString("reason"),
            Instant.ofEpochMilli(resultSet.getLong("created_at")),
            expiresAt == null ? null : Instant.ofEpochMilli(expiresAt),
            resultSet.getInt("active") == 1,
            SanctionScope.valueOf(resultSet.getString("scope")),
            resultSet.getString("scope_value")
        );
    }
}
