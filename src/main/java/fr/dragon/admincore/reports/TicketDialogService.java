package fr.dragon.admincore.reports;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.dialog.ConfirmDialog;
import fr.dragon.admincore.dialog.PlayerTicketsDialog;
import fr.dragon.admincore.dialog.TicketCloseDialog;
import fr.dragon.admincore.dialog.TicketConversationDialog;
import fr.dragon.admincore.dialog.TicketReplyDialog;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class TicketDialogService {

    private static final int STAFF_MESSAGE_LIMIT = 8;
    private static final int PLAYER_MESSAGE_LIMIT = 6;
    private static final int PLAYER_PAGE_SIZE = 8;

    private final AdminCorePlugin plugin;

    public TicketDialogService(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void openStaffConversation(final Player player, final TicketRecord ticket) {
        this.plugin.getReportService().messages(ticket.id(), STAFF_MESSAGE_LIMIT).thenAccept(messages ->
            sync(() -> player.showDialog(TicketConversationDialog.create(
                ticket,
                messages,
                (response, audience) -> {
                    final String message = response.getText("message");
                    this.plugin.getReportService().sendMessage(player, ticket, message).thenRun(() ->
                        sync(() -> openStaffConversation(player, ticket))
                    ).exceptionally(throwable -> {
                        sync(() -> {
                            player.sendMessage(ticketMessageError(throwable));
                            openStaffConversation(player, ticket);
                        });
                        return null;
                    });
                },
                (response, audience) -> {
                }
            )))
        ).exceptionally(throwable -> {
            sync(() -> player.sendMessage(this.plugin.getMessageFormatter().message("errors.database")));
            return null;
        });
    }

    public void openCloseDialog(final Player player, final TicketRecord ticket, final Runnable onSuccess, final Runnable onCancel) {
        player.showDialog(TicketCloseDialog.create(ticket.targetName(), (response, audience) -> {
            final String note = response.getText("note");
            this.plugin.getReportService().close(player, ticket.id(), note).thenAccept(updated ->
                sync(() -> {
                    player.sendMessage(this.plugin.getMessageFormatter().message(
                        "reports.closed",
                        this.plugin.getMessageFormatter().text("id", Long.toString(updated.id()))
                    ));
                    onSuccess.run();
                })
            ).exceptionally(throwable -> {
                sync(() -> player.sendMessage(this.plugin.getMessageFormatter().message("errors.database")));
                return null;
            });
        }, (response, audience) -> sync(onCancel)));
    }

    public void openArchiveConfirm(final Player player, final TicketRecord ticket, final Runnable onSuccess, final Runnable onCancel) {
        player.showDialog(ConfirmDialog.create(
            "Archiver le ticket",
            java.util.List.of(Component.text("Archiver le ticket #" + ticket.id() + " ?")),
            "Archiver",
            "Annuler",
            (response, audience) -> this.plugin.getReportService().archive(player, ticket.id()).thenAccept(updated ->
                sync(() -> {
                    player.sendMessage(this.plugin.getMessageFormatter().message(
                        "reports.archived",
                        this.plugin.getMessageFormatter().text("id", Long.toString(updated.id()))
                    ));
                    onSuccess.run();
                })
            ).exceptionally(throwable -> {
                sync(() -> player.sendMessage(this.plugin.getMessageFormatter().message("errors.database")));
                return null;
            }),
            (response, audience) -> sync(onCancel)
        ));
    }

    public void openPlayerTickets(final Player player, final int page) {
        openPlayerTickets(player, page, () -> {
            final var reportCmd = new ReportCommand(this.plugin);
            reportCmd.onCommand(player, null, "report", new String[]{});
        });
    }

    public void openPlayerTickets(final Player player, final int page, final Runnable createCallback) {
        this.plugin.getReportService().reporterPage(player.getUniqueId(), Math.max(0, page), PLAYER_PAGE_SIZE).thenAccept(result ->
            sync(() -> player.showDialog(PlayerTicketsDialog.create(
                result.entries(),
                result.page(),
                result.hasNext(),
                ticket -> openPlayerReply(player, ticket),
                () -> openPlayerTickets(player, Math.max(0, result.page() - 1), createCallback),
                () -> openPlayerTickets(player, result.page() + 1, createCallback),
                () -> {
                },
                createCallback
            )))
        ).exceptionally(throwable -> {
            sync(() -> player.sendMessage(this.plugin.getMessageFormatter().message("errors.database")));
            return null;
        });
    }

    public void openPlayerReply(final Player player, final long ticketId) {
        this.plugin.getReportService().ticket(ticketId).thenAccept(optional ->
            sync(() -> {
                if (optional.isEmpty()) {
                    player.sendMessage(this.plugin.getMessageFormatter().message("reports.ticket-not-found"));
                    return;
                }
                openPlayerReply(player, optional.get());
            })
        ).exceptionally(throwable -> {
            sync(() -> player.sendMessage(this.plugin.getMessageFormatter().message("errors.database")));
            return null;
        });
    }

    public void openPlayerReply(final Player player, final TicketRecord ticket) {
        if (!isParticipant(player.getUniqueId(), ticket)) {
            player.sendMessage(this.plugin.getMessageFormatter().message("reports.ticket-not-owned"));
            return;
        }
        if (ticket.status() == TicketStatus.CLOSED || ticket.status() == TicketStatus.ARCHIVED) {
            player.sendMessage(this.plugin.getMessageFormatter().message("reports.ticket-not-active"));
            return;
        }
        this.plugin.getReportService().messages(ticket.id(), PLAYER_MESSAGE_LIMIT).thenAccept(messages ->
            sync(() -> player.showDialog(TicketReplyDialog.create(
                ticket,
                messages,
                (response, audience) -> {
                    final String message = response.getText("message");
                    this.plugin.getReportService().sendPlayerReply(player, ticket, message).thenRun(() ->
                        sync(() -> {
                            player.sendMessage(this.plugin.getMessageFormatter().message(
                                "reports.reply-sent",
                                this.plugin.getMessageFormatter().text("id", Long.toString(ticket.id()))
                            ));
                            openPlayerReply(player, ticket);
                        })
                    ).exceptionally(throwable -> {
                        sync(() -> player.sendMessage(ticketMessageError(throwable)));
                        return null;
                    });
                },
                (response, audience) -> {
                }
            )))
        ).exceptionally(throwable -> {
            sync(() -> player.sendMessage(this.plugin.getMessageFormatter().message("errors.database")));
            return null;
        });
    }

    private boolean isParticipant(final UUID playerUuid, final TicketRecord ticket) {
        return (ticket.reporterUuid() != null && ticket.reporterUuid().equals(playerUuid))
            || (ticket.targetUuid() != null && ticket.targetUuid().equals(playerUuid));
    }

    private Component ticketMessageError(final Throwable throwable) {
        if (throwable instanceof CompletionException completion && completion.getCause() != null) {
            return ticketMessageError(completion.getCause());
        }
        if (throwable instanceof IllegalArgumentException) {
            return this.plugin.getMessageFormatter().message("reports.message-empty");
        }
        if (throwable instanceof IllegalStateException illegalStateException) {
            final String message = illegalStateException.getMessage();
            if ("Ticket ferme".equalsIgnoreCase(message)) {
                return this.plugin.getMessageFormatter().message("reports.ticket-not-active");
            }
            if ("Ticket interdit".equalsIgnoreCase(message)) {
                return this.plugin.getMessageFormatter().message("reports.ticket-not-owned");
            }
            if ("Ticket introuvable".equalsIgnoreCase(message)) {
                return this.plugin.getMessageFormatter().message("reports.ticket-not-found");
            }
        }
        return this.plugin.getMessageFormatter().message("errors.database");
    }

    private void sync(final Runnable runnable) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, runnable);
    }
}
