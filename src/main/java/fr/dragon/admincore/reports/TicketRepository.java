package fr.dragon.admincore.reports;

import fr.dragon.admincore.database.DatabaseManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TicketRepository {

    public record TicketPage(List<TicketRecord> entries, int page, boolean hasNext) {
    }

    private final DatabaseManager databaseManager;

    public TicketRepository(final DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<TicketRecord> create(
        final UUID reporterUuid,
        final UUID targetUuid,
        final String reporterName,
        final String targetName,
        final String discordReporter,
        final String category,
        final String description
    ) {
        return this.databaseManager.query(connection -> {
            final Instant now = Instant.now();
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tickets (
                    uuid_reporter, uuid_cible, name_reporter, name_cible, raison,
                    discord_reporter, categorie, description,
                    statut, uuid_staff_assigned, name_staff_assigned, note_cloture, timestamp
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OPEN', NULL, NULL, NULL, ?)
                """, PreparedStatement.RETURN_GENERATED_KEYS)) {
                final String effectiveCategory = category == null || category.isBlank() ? "Autre" : category;
                final String effectiveDiscord = discordReporter == null || discordReporter.isBlank() ? "Non inscrit" : discordReporter;
                final String effectiveDescription = description == null ? "" : description;
                statement.setString(1, reporterUuid.toString());
                statement.setString(2, targetUuid.toString());
                statement.setString(3, reporterName);
                statement.setString(4, targetName);
                statement.setString(5, effectiveCategory);
                statement.setString(6, effectiveDiscord);
                statement.setString(7, effectiveCategory);
                if (effectiveDescription.isBlank()) {
                    statement.setNull(8, Types.VARCHAR);
                } else {
                    statement.setString(8, effectiveDescription);
                }
                statement.setLong(9, now.toEpochMilli());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return new TicketRecord(
                        keys.next() ? keys.getLong(1) : -1L,
                        reporterUuid,
                        targetUuid,
                        reporterName,
                        targetName,
                        effectiveCategory,
                        effectiveDiscord,
                        effectiveCategory,
                        effectiveDescription,
                        TicketStatus.OPEN,
                        null,
                        null,
                        null,
                        now
                    );
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Creation du ticket impossible", exception);
            }
        });
    }

    public CompletableFuture<TicketPage> openTickets(final int page, final int pageSize) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT *
                FROM tickets
                WHERE statut IN ('OPEN', 'IN_PROGRESS')
                ORDER BY timestamp DESC
                LIMIT ? OFFSET ?
                """)) {
                statement.setInt(1, pageSize + 1);
                statement.setInt(2, Math.max(0, page) * pageSize);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<TicketRecord> entries = new ArrayList<>();
                    while (resultSet.next()) {
                        entries.add(map(resultSet));
                    }
                    final boolean hasNext = entries.size() > pageSize;
                    if (hasNext) {
                        entries.removeLast();
                    }
                    return new TicketPage(entries, Math.max(0, page), hasNext);
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Lecture des tickets ouverts impossible", exception);
            }
        });
    }

    public CompletableFuture<TicketPage> closedTickets(final int page, final int pageSize) {
        return ticketsByStatus(page, pageSize, TicketStatus.CLOSED);
    }

    public CompletableFuture<TicketPage> archivedTickets(final int page, final int pageSize) {
        return ticketsByStatus(page, pageSize, TicketStatus.ARCHIVED);
    }

    public CompletableFuture<TicketPage> historyForTarget(final UUID targetUuid, final String targetName, final int page, final int pageSize) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT *
                FROM tickets
                WHERE statut IN ('CLOSED', 'ARCHIVED')
                  AND ((uuid_cible IS NOT NULL AND uuid_cible = ?) OR LOWER(name_cible) = LOWER(?))
                ORDER BY timestamp DESC
                LIMIT ? OFFSET ?
                """)) {
                statement.setString(1, targetUuid == null ? null : targetUuid.toString());
                statement.setString(2, targetName);
                statement.setInt(3, pageSize + 1);
                statement.setInt(4, Math.max(0, page) * pageSize);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<TicketRecord> entries = new ArrayList<>();
                    while (resultSet.next()) {
                        entries.add(map(resultSet));
                    }
                    final boolean hasNext = entries.size() > pageSize;
                    if (hasNext) {
                        entries.removeLast();
                    }
                    return new TicketPage(entries, Math.max(0, page), hasNext);
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Lecture de l'historique des tickets impossible", exception);
            }
        });
    }

    public CompletableFuture<Optional<TicketRecord>> assign(final long ticketId, final UUID staffUuid, final String staffName) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                UPDATE tickets
                SET statut = 'IN_PROGRESS', uuid_staff_assigned = ?, name_staff_assigned = ?
                WHERE id = ?
                  AND statut = 'OPEN'
                """)) {
                update.setString(1, staffUuid.toString());
                update.setString(2, staffName);
                update.setLong(3, ticketId);
                final int updated = update.executeUpdate();
                if (updated <= 0) {
                    return findById(connection, ticketId);
                }
                return findById(connection, ticketId);
            } catch (final Exception exception) {
                throw new IllegalStateException("Assignation du ticket impossible", exception);
            }
        });
    }

    public CompletableFuture<Optional<TicketRecord>> close(final long ticketId, final UUID staffUuid, final String staffName, final String note) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                UPDATE tickets
                SET statut = 'CLOSED', uuid_staff_assigned = ?, name_staff_assigned = ?, note_cloture = ?
                WHERE id = ?
                  AND statut IN ('OPEN', 'IN_PROGRESS')
                """)) {
                update.setString(1, staffUuid.toString());
                update.setString(2, staffName);
                if (note == null || note.isBlank()) {
                    update.setNull(3, Types.VARCHAR);
                } else {
                    update.setString(3, note);
                }
                update.setLong(4, ticketId);
                final int updated = update.executeUpdate();
                if (updated <= 0) {
                    return Optional.empty();
                }
                return findById(connection, ticketId);
            } catch (final Exception exception) {
                throw new IllegalStateException("Fermeture du ticket impossible", exception);
            }
        });
    }

    public CompletableFuture<Optional<TicketRecord>> archive(final long ticketId) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement update = connection.prepareStatement("""
                UPDATE tickets
                SET statut = 'ARCHIVED'
                WHERE id = ?
                  AND statut = 'CLOSED'
                """)) {
                update.setLong(1, ticketId);
                final int updated = update.executeUpdate();
                if (updated <= 0) {
                    return Optional.empty();
                }
                return findById(connection, ticketId);
            } catch (final Exception exception) {
                throw new IllegalStateException("Archivage du ticket impossible", exception);
            }
        });
    }

    public CompletableFuture<List<TicketMessageRecord>> messages(final long ticketId, final int limit) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, ticket_id, uuid_auteur, name_auteur, contenu, timestamp, is_staff
                FROM ticket_messages
                WHERE ticket_id = ?
                ORDER BY timestamp DESC
                LIMIT ?
                """)) {
                statement.setLong(1, ticketId);
                statement.setInt(2, Math.max(1, limit));
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<TicketMessageRecord> entries = new ArrayList<>();
                    while (resultSet.next()) {
                        entries.add(mapMessage(resultSet));
                    }
                    java.util.Collections.reverse(entries);
                    return entries;
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Lecture des messages ticket impossible", exception);
            }
        });
    }

    public CompletableFuture<TicketMessageRecord> addMessage(
        final long ticketId,
        final UUID authorUuid,
        final String authorName,
        final String content,
        final boolean isStaff
    ) {
        return this.databaseManager.query(connection -> {
            final Instant now = Instant.now();
            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO ticket_messages (ticket_id, uuid_auteur, name_auteur, contenu, timestamp, is_staff)
                VALUES (?, ?, ?, ?, ?, ?)
                """, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, ticketId);
                if (authorUuid == null) {
                    statement.setNull(2, Types.VARCHAR);
                } else {
                    statement.setString(2, authorUuid.toString());
                }
                statement.setString(3, authorName);
                statement.setString(4, content);
                statement.setLong(5, now.toEpochMilli());
                statement.setInt(6, isStaff ? 1 : 0);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return new TicketMessageRecord(
                        keys.next() ? keys.getLong(1) : -1L,
                        ticketId,
                        authorUuid,
                        authorName,
                        content,
                        now,
                        isStaff
                    );
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Ajout du message ticket impossible", exception);
            }
        });
    }

    private Optional<TicketRecord> findById(final java.sql.Connection connection, final long ticketId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM tickets WHERE id = ? LIMIT 1")) {
            statement.setLong(1, ticketId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    private CompletableFuture<TicketPage> ticketsByStatus(final int page, final int pageSize, final TicketStatus status) {
        return this.databaseManager.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                SELECT *
                FROM tickets
                WHERE statut = ?
                ORDER BY timestamp DESC
                LIMIT ? OFFSET ?
                """)) {
                statement.setString(1, status.name());
                statement.setInt(2, pageSize + 1);
                statement.setInt(3, Math.max(0, page) * pageSize);
                try (ResultSet resultSet = statement.executeQuery()) {
                    final List<TicketRecord> entries = new ArrayList<>();
                    while (resultSet.next()) {
                        entries.add(map(resultSet));
                    }
                    final boolean hasNext = entries.size() > pageSize;
                    if (hasNext) {
                        entries.removeLast();
                    }
                    return new TicketPage(entries, Math.max(0, page), hasNext);
                }
            } catch (final Exception exception) {
                throw new IllegalStateException("Lecture des tickets impossible pour le statut " + status, exception);
            }
        });
    }

    private TicketRecord map(final ResultSet resultSet) throws Exception {
        final String reporterUuid = resultSet.getString("uuid_reporter");
        final String targetUuid = resultSet.getString("uuid_cible");
        final String assignedUuid = resultSet.getString("uuid_staff_assigned");
        final String legacyReason = resultSet.getString("raison");
        final String category = resultSet.getString("categorie");
        final String description = resultSet.getString("description");
        return new TicketRecord(
            resultSet.getLong("id"),
            reporterUuid == null ? null : UUID.fromString(reporterUuid),
            targetUuid == null ? null : UUID.fromString(targetUuid),
            resultSet.getString("name_reporter"),
            resultSet.getString("name_cible"),
            category == null || category.isBlank() ? legacyReason : category,
            resultSet.getString("discord_reporter"),
            category == null || category.isBlank() ? legacyReason : category,
            description,
            TicketStatus.valueOf(resultSet.getString("statut")),
            assignedUuid == null ? null : UUID.fromString(assignedUuid),
            resultSet.getString("name_staff_assigned"),
            resultSet.getString("note_cloture"),
            Instant.ofEpochMilli(resultSet.getLong("timestamp"))
        );
    }

    private TicketMessageRecord mapMessage(final ResultSet resultSet) throws Exception {
        final String authorUuid = resultSet.getString("uuid_auteur");
        return new TicketMessageRecord(
            resultSet.getLong("id"),
            resultSet.getLong("ticket_id"),
            authorUuid == null ? null : UUID.fromString(authorUuid),
            resultSet.getString("name_auteur"),
            resultSet.getString("contenu"),
            Instant.ofEpochMilli(resultSet.getLong("timestamp")),
            resultSet.getInt("is_staff") == 1
        );
    }
}
