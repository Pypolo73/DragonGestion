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

    public java.util.concurrent.CompletableFuture<Void> upsert(final UUID uuid, final String name, final String ip, final String clientBrand, final int level) {
        return this.databaseManager.execute(connection -> {
            final long now = Instant.now().toEpochMilli();
            try (PreparedStatement playerStatement = connection.prepareStatement("""
                INSERT INTO players (uuid, last_name, last_ip, last_client_brand, last_level, first_seen, last_seen)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET
                    last_name = excluded.last_name,
                    last_ip = excluded.last_ip,
                    last_client_brand = excluded.last_client_brand,
                    last_level = excluded.last_level,
                    last_seen = excluded.last_seen
                """);
                 PreparedStatement ipStatement = connection.prepareStatement("""
                     INSERT INTO player_ips (player_uuid, player_name, ip, seen_at)
                     VALUES (?, ?, ?, ?)
                     """)) {
                playerStatement.setString(1, uuid.toString());
                playerStatement.setString(2, name);
                playerStatement.setString(3, ip);
                playerStatement.setString(4, clientBrand);
                playerStatement.setInt(5, level);
                playerStatement.setLong(6, now);
                playerStatement.setLong(7, now);
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

    public java.util.concurrent.CompletableFuture<List<String>> searchNames(final String query, final int limit) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT DISTINCT last_name
                FROM players
                WHERE LOWER(last_name) LIKE LOWER(?)
                ORDER BY last_seen DESC
                LIMIT ?
                """)) {
                statement.setString(1, query + "%");
                statement.setInt(2, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<String> matches = new ArrayList<>();
                    while (resultSet.next()) {
                        matches.add(resultSet.getString("last_name"));
                    }
                    return matches;
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Impossible de rechercher les pseudos", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<List<PlayerProfile>> listProfiles(final int offset, final int limit) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT p.uuid, p.last_name, p.last_ip, p.last_client_brand, p.last_level,
                       COUNT(s.id) AS sanction_count
                FROM players p
                LEFT JOIN sanctions s
                    ON ((s.target_uuid IS NOT NULL AND s.target_uuid = p.uuid) OR LOWER(s.target_name) = LOWER(p.last_name))
                GROUP BY p.uuid, p.last_name, p.last_ip, p.last_client_brand, p.last_level, p.last_seen
                ORDER BY p.last_seen DESC
                LIMIT ? OFFSET ?
                """)) {
                statement.setInt(1, limit);
                statement.setInt(2, offset);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<PlayerProfile> profiles = new ArrayList<>();
                    while (resultSet.next()) {
                        final String uuid = resultSet.getString("uuid");
                        profiles.add(new PlayerProfile(
                            uuid == null ? null : UUID.fromString(uuid),
                            resultSet.getString("last_name"),
                            resultSet.getString("last_ip"),
                            resultSet.getString("last_client_brand"),
                            resultSet.getInt("last_level"),
                            resultSet.getInt("sanction_count"),
                            List.of()
                        ));
                    }
                    return profiles;
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Impossible de lister les profils", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<PlayerProfile> profile(final UUID targetUuid, final String targetName) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT p.uuid, p.last_name, p.last_ip, p.last_client_brand, p.last_level,
                       COUNT(s.id) AS sanction_count
                FROM players p
                LEFT JOIN sanctions s
                    ON ((s.target_uuid IS NOT NULL AND s.target_uuid = p.uuid) OR LOWER(s.target_name) = LOWER(p.last_name))
                WHERE (p.uuid = ?) OR LOWER(p.last_name) = LOWER(?)
                GROUP BY p.uuid, p.last_name, p.last_ip, p.last_client_brand, p.last_level
                LIMIT 1
                """)) {
                statement.setString(1, targetUuid == null ? null : targetUuid.toString());
                statement.setString(2, targetName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return new PlayerProfile(targetUuid, targetName, null, null, 0, 0, List.of());
                    }
                    final String uuid = resultSet.getString("uuid");
                    final List<String> types = sanctionTypes(connection, uuid, resultSet.getString("last_name"));
                    return new PlayerProfile(
                        uuid == null ? null : UUID.fromString(uuid),
                        resultSet.getString("last_name"),
                        resultSet.getString("last_ip"),
                        resultSet.getString("last_client_brand"),
                        resultSet.getInt("last_level"),
                        resultSet.getInt("sanction_count"),
                        types
                    );
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Impossible de lire le profil joueur", exception);
            }
        });
    }

    private List<String> sanctionTypes(final java.sql.Connection connection, final String uuid, final String name) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT DISTINCT type
            FROM sanctions
            WHERE ((target_uuid IS NOT NULL AND target_uuid = ?) OR LOWER(target_name) = LOWER(?))
            ORDER BY created_at DESC
            LIMIT 5
            """)) {
            statement.setString(1, uuid);
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                final List<String> types = new ArrayList<>();
                while (resultSet.next()) {
                    types.add(resultSet.getString("type"));
                }
                return types;
            }
        }
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

    public java.util.concurrent.CompletableFuture<Integer> countDistinctAccountsOnIp(final String ip) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(DISTINCT player_uuid)
                FROM player_ips
                WHERE ip = ?
                """)) {
                statement.setString(1, ip);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getInt(1) : 0;
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Comptage des comptes sur IP impossible", exception);
            }
        });
    }

    public java.util.concurrent.CompletableFuture<Integer> countRecentConnections(final UUID uuid, final Instant since) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM player_ips
                WHERE player_uuid = ?
                  AND seen_at >= ?
                """)) {
                statement.setString(1, uuid.toString());
                statement.setLong(2, since.toEpochMilli());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getInt(1) : 0;
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Comptage des connexions recentes impossible", exception);
            }
        });
    }
}
