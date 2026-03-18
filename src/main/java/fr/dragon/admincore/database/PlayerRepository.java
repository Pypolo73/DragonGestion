package fr.dragon.admincore.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PlayerRepository {

    private final DatabaseManager databaseManager;

    public PlayerRepository(final DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public java.util.concurrent.CompletableFuture<Void> upsert(final UUID uuid, final String name, final String ip) {
        return this.databaseManager.execute(connection -> {
            final long now = Instant.now().toEpochMilli();
            try (PreparedStatement playerStatement = connection.prepareStatement("""
                INSERT INTO players (uuid, last_name, last_ip, first_seen, last_seen)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    last_name = excluded.last_name,
                    last_ip = excluded.last_ip,
                    last_seen = excluded.last_seen
                """);
                 PreparedStatement ipStatement = connection.prepareStatement("""
                     INSERT INTO player_ips (player_uuid, player_name, ip, seen_at)
                     VALUES (?, ?, ?, ?)
                     """)) {
                playerStatement.setString(1, uuid.toString());
                playerStatement.setString(2, name);
                playerStatement.setString(3, ip);
                playerStatement.setLong(4, now);
                playerStatement.setLong(5, now);
                playerStatement.executeUpdate();

                ipStatement.setString(1, uuid.toString());
                ipStatement.setString(2, name);
                ipStatement.setString(3, ip);
                ipStatement.setLong(4, now);
                ipStatement.executeUpdate();
            }
        });
    }

    public java.util.concurrent.CompletableFuture<Optional<String>> findLatestIp(final String targetName) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT last_ip FROM players WHERE LOWER(last_name) = LOWER(?) LIMIT 1
                """)) {
                statement.setString(1, targetName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.ofNullable(resultSet.getString("last_ip")) : Optional.empty();
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Impossible de lire la derniere IP", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<List<String>> findAccountsByIp(final String ip) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT DISTINCT player_name
                FROM player_ips
                WHERE ip = ?
                ORDER BY player_name ASC
                """)) {
                statement.setString(1, ip);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<String> accounts = new ArrayList<>();
                    while (resultSet.next()) {
                        accounts.add(resultSet.getString("player_name"));
                    }
                    return accounts;
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Impossible de lire les comptes lies a l'IP", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<List<String>> findAlts(final UUID targetUuid, final String targetName) {
        return this.databaseManager.query(connection -> {
            try {
                final Set<String> ips = new HashSet<>();
                try (PreparedStatement ipStatement = connection.prepareStatement("""
                    SELECT DISTINCT ip
                    FROM player_ips
                    WHERE player_uuid = ? OR LOWER(player_name) = LOWER(?)
                    """)) {
                    ipStatement.setString(1, targetUuid == null ? null : targetUuid.toString());
                    ipStatement.setString(2, targetName);
                    try (ResultSet resultSet = ipStatement.executeQuery()) {
                        while (resultSet.next()) {
                            ips.add(resultSet.getString("ip"));
                        }
                    }
                }

                if (ips.isEmpty()) {
                    return List.of();
                }

                final String placeholders = String.join(",", ips.stream().map(ignored -> "?").toList());
                try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT DISTINCT player_name
                    FROM player_ips
                    WHERE ip IN (%s)
                      AND LOWER(player_name) <> LOWER(?)
                    ORDER BY player_name ASC
                    """.formatted(placeholders))) {
                    int index = 1;
                    for (final String ip : ips) {
                        statement.setString(index++, ip);
                    }
                    statement.setString(index, targetName);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        final List<String> accounts = new ArrayList<>();
                        while (resultSet.next()) {
                            accounts.add(resultSet.getString("player_name"));
                        }
                        return accounts;
                    }
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Impossible de lire les alts", exception);
            }
        });
    }
}
