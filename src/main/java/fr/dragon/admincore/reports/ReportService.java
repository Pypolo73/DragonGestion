package fr.dragon.admincore.reports;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.core.StaffActionType;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class ReportService {

    public static final int PAGE_SIZE = 28;

    private final AdminCorePlugin plugin;
    private final TicketRepository repository;

    public ReportService(final AdminCorePlugin plugin, final TicketRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public CompletableFuture<TicketRecord> create(
        final Player reporter,
        final Player target,
        final String discordReporter,
        final String category,
        final String description
    ) {
        final String sanitizedDiscord = sanitize(discordReporter, 64, "Non inscrit");
        final String sanitizedCategory = sanitize(category, 64, "Autre");
        final String sanitizedDescription = sanitize(description, 200, "");
        return this.repository.create(
                reporter.getUniqueId(),
                target.getUniqueId(),
                reporter.getName(),
                target.getName(),
                sanitizedDiscord,
                sanitizedCategory,
                sanitizedDescription
            )
            .thenApply(ticket -> {
                this.plugin.getStaffActionLogger().log(
                    reporter,
                    StaffActionType.TICKET_CREATE,
                    target.getUniqueId(),
                    target.getName(),
                    "Ticket #" + ticket.id() + " | " + sanitizedCategory
                        + (sanitizedDescription.isBlank() ? "" : " | " + sanitizedDescription)
                );
                notifyStaff(ticket);
                return ticket;
            });
    }

    public CompletableFuture<TicketRepository.TicketPage> openPage(final int page) {
        return this.repository.openTickets(page, PAGE_SIZE);
    }

    public CompletableFuture<TicketRepository.TicketPage> history(final UUID targetUuid, final String targetName, final int page) {
        return this.repository.historyForTarget(targetUuid, targetName, page, PAGE_SIZE);
    }

    public CompletableFuture<TicketRepository.TicketPage> closedPage(final int page) {
        return this.repository.closedTickets(page, PAGE_SIZE);
    }

    public CompletableFuture<TicketRepository.TicketPage> archivedPage(final int page) {
        return this.repository.archivedTickets(page, PAGE_SIZE);
    }

    public CompletableFuture<TicketRepository.TicketPage> reporterPage(final UUID reporterUuid, final int page, final int pageSize) {
        return this.repository.reporterTickets(reporterUuid, page, pageSize);
    }

    public CompletableFuture<java.util.Optional<TicketRecord>> ticket(final long ticketId) {
        return this.repository.findById(ticketId);
    }

    public CompletableFuture<TicketRecord> assign(final Player staff, final long ticketId) {
        return this.repository.assign(ticketId, staff.getUniqueId(), staff.getName()).thenCompose(optional -> {
            if (optional.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Ticket introuvable"));
            }
            final TicketRecord ticket = optional.get();
            this.plugin.getStaffActionLogger().log(
                staff,
                StaffActionType.TICKET_ASSIGN,
                ticket.targetUuid(),
                ticket.targetName(),
                "Ticket #" + ticket.id()
            );
            return CompletableFuture.completedFuture(ticket);
        });
    }

    public CompletableFuture<TicketRecord> close(final Player staff, final long ticketId, final String note) {
        final String sanitizedNote = sanitize(note, 200, "");
        return this.repository.close(ticketId, staff.getUniqueId(), staff.getName(), sanitizedNote).thenCompose(optional -> {
            if (optional.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Ticket introuvable"));
            }
            final TicketRecord ticket = optional.get();
            this.plugin.getStaffActionLogger().log(
                staff,
                StaffActionType.TICKET_CLOSE,
                ticket.targetUuid(),
                ticket.targetName(),
                "Ticket #" + ticket.id() + " | " + sanitizedNote
            );
            return CompletableFuture.completedFuture(ticket);
        });
    }

    public CompletableFuture<TicketRecord> archive(final Player staff, final long ticketId) {
        return this.repository.archive(ticketId).thenCompose(optional -> {
            if (optional.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Ticket introuvable"));
            }
            final TicketRecord ticket = optional.get();
            this.plugin.getStaffActionLogger().log(
                staff,
                StaffActionType.TICKET_ARCHIVE,
                ticket.targetUuid(),
                ticket.targetName(),
                "Ticket #" + ticket.id()
            );
            return CompletableFuture.completedFuture(ticket);
        });
    }

    public CompletableFuture<java.util.List<TicketMessageRecord>> messages(final long ticketId, final int limit) {
        return this.repository.messages(ticketId, limit);
    }

    public CompletableFuture<Void> sendMessage(final Player staff, final TicketRecord ticket, final String rawMessage) {
        final String message = sanitize(rawMessage, 200, "");
        if (message.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Message vide"));
        }
        return this.repository.addMessage(ticket.id(), staff.getUniqueId(), staff.getName(), message, true)
            .thenAccept(record -> {
                this.plugin.getStaffActionLogger().log(
                    staff,
                    StaffActionType.TICKET_MESSAGE,
                    ticket.targetUuid(),
                    ticket.targetName(),
                    "Ticket #" + ticket.id() + " | " + message
                );
                final Player target = ticket.targetUuid() == null ? Bukkit.getPlayerExact(ticket.targetName()) : Bukkit.getPlayer(ticket.targetUuid());
                if (target == null) {
                    return;
                }
                final Component replyButton = Component.text("[Repondre]", NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("/ticket reply " + ticket.id()));
                final Component component = Component.text()
                    .append(Component.text("[Ticket] ", NamedTextColor.GOLD))
                    .append(Component.text(staff.getName() + " : ", NamedTextColor.GRAY))
                    .append(Component.text(message, NamedTextColor.WHITE))
                    .append(Component.text(" ", NamedTextColor.WHITE))
                    .append(replyButton)
                    .build();
                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> target.sendMessage(component));
            });
    }

    public CompletableFuture<Void> sendPlayerReply(final Player reporter, final TicketRecord ticket, final String rawMessage) {
        final String message = sanitize(rawMessage, 200, "");
        if (message.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Message vide"));
        }
        return this.repository.addMessage(ticket.id(), reporter.getUniqueId(), reporter.getName(), message, false)
            .thenRun(() -> {
                final Player assigned = ticket.assignedStaffUuid() == null ? null : Bukkit.getPlayer(ticket.assignedStaffUuid());
                if (assigned != null && assigned.isOnline()) {
                    this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                        assigned.sendActionBar(this.plugin.getMessageFormatter().deserialize(
                            "<gold>Nouvelle reponse de <white><player></white> sur le ticket <white>#<id></white></gold>",
                            this.plugin.getMessageFormatter().text("player", reporter.getName()),
                            this.plugin.getMessageFormatter().text("id", Long.toString(ticket.id()))
                        ))
                    );
                    return;
                }
                notifyStaffReply(ticket, reporter.getName());
            });
    }

    private void notifyStaff(final TicketRecord ticket) {
        final TagResolver id = this.plugin.getMessageFormatter().text("id", Long.toString(ticket.id()));
        final TagResolver reporter = this.plugin.getMessageFormatter().text("reporter", ticket.reporterName());
        final TagResolver target = this.plugin.getMessageFormatter().text("target", ticket.targetName());
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(PermissionService.REPORTS)) {
                continue;
            }
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                online.sendActionBar(this.plugin.getMessageFormatter().message("reports.notify-actionbar", id, reporter, target));
                online.playSound(online.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
            });
        }
    }

    private void notifyStaffReply(final TicketRecord ticket, final String reporterName) {
        final TagResolver id = this.plugin.getMessageFormatter().text("id", Long.toString(ticket.id()));
        final TagResolver reporter = this.plugin.getMessageFormatter().text("reporter", reporterName);
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(PermissionService.REPORTS)) {
                continue;
            }
            this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                online.sendActionBar(this.plugin.getMessageFormatter().deserialize(
                    "<gold>Nouvelle reponse de <white><reporter></white> sur le ticket <white>#<id></white></gold>",
                    reporter,
                    id
                ))
            );
        }
    }

    private String sanitize(final String value, final int max, final String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        final String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
