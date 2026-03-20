package fr.dragon.admincore.reports;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.gui.TicketMenu;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class StaffTicketCommand implements CommandExecutor, TabCompleter {

    private final AdminCorePlugin plugin;

    public StaffTicketCommand(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (!this.plugin.getPermissionService().check(sender, PermissionService.STAFFTICKET)) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>Cette commande doit etre lancee en jeu.</red>"));
            return true;
        }
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            this.plugin.getReportService().openPage(0).thenAccept(page ->
                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> TicketMenu.openOpen(player, page))
            ).exceptionally(throwable -> {
                this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                    player.sendMessage(this.plugin.getMessageFormatter().message("errors.database"))
                );
                return null;
            });
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "view" -> view(player, args);
            case "assign" -> assign(player, args);
            case "close" -> close(player, args);
            case "archive" -> archive(player, args);
            default -> {
                player.sendMessage(this.plugin.getMessageFormatter().deserialize(
                    "<prefix><red>Usage: /staffticket <list|view|assign|close|archive></red>"
                ));
                yield true;
            }
        };
    }

    private boolean view(final Player player, final String[] args) {
        if (args.length < 2) {
            player.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>Usage: /staffticket view <id></red>"));
            return true;
        }
        final long ticketId = parseId(player, args[1]);
        if (ticketId < 0L) {
            return true;
        }
        this.plugin.getReportService().ticket(ticketId).thenAccept(optional ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (optional.isEmpty()) {
                    player.sendMessage(this.plugin.getMessageFormatter().message("reports.ticket-not-found"));
                    return;
                }
                this.plugin.getTicketDialogService().openStaffConversation(player, optional.get());
            })
        ).exceptionally(throwable -> {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                player.sendMessage(this.plugin.getMessageFormatter().message("errors.database"))
            );
            return null;
        });
        return true;
    }

    private boolean assign(final Player player, final String[] args) {
        if (args.length < 2) {
            player.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>Usage: /staffticket assign <id></red>"));
            return true;
        }
        final long ticketId = parseId(player, args[1]);
        if (ticketId < 0L) {
            return true;
        }
        this.plugin.getReportService().ticket(ticketId).thenCompose(optional -> {
            if (optional.isEmpty()) {
                return java.util.concurrent.CompletableFuture.completedFuture(Optional.<TicketRecord>empty());
            }
            final TicketRecord ticket = optional.get();
            if (ticket.status() == TicketStatus.OPEN) {
                return this.plugin.getReportService().assign(player, ticketId).thenApply(Optional::of);
            }
            if (ticket.status() == TicketStatus.IN_PROGRESS
                && ticket.assignedStaffUuid() != null
                && ticket.assignedStaffUuid().equals(player.getUniqueId())) {
                return java.util.concurrent.CompletableFuture.completedFuture(Optional.of(ticket));
            }
            return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("busy"));
        }).thenAccept(optional ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (optional.isEmpty()) {
                    player.sendMessage(this.plugin.getMessageFormatter().message("reports.ticket-not-found"));
                    return;
                }
                this.plugin.getTicketDialogService().openStaffConversation(player, optional.get());
            })
        ).exceptionally(throwable -> {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (rootCause(throwable) instanceof IllegalStateException) {
                    player.sendMessage(this.plugin.getMessageFormatter().message("reports.ticket-busy"));
                    return;
                }
                player.sendMessage(this.plugin.getMessageFormatter().message("errors.database"));
            });
            return null;
        });
        return true;
    }

    private boolean close(final Player player, final String[] args) {
        if (args.length < 2) {
            player.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>Usage: /staffticket close <id></red>"));
            return true;
        }
        final long ticketId = parseId(player, args[1]);
        if (ticketId < 0L) {
            return true;
        }
        this.plugin.getReportService().ticket(ticketId).thenAccept(optional ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (optional.isEmpty()) {
                    player.sendMessage(this.plugin.getMessageFormatter().message("reports.ticket-not-found"));
                    return;
                }
                this.plugin.getTicketDialogService().openCloseDialog(player, optional.get(), () -> {
                }, () -> {
                });
            })
        ).exceptionally(throwable -> {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                player.sendMessage(this.plugin.getMessageFormatter().message("errors.database"))
            );
            return null;
        });
        return true;
    }

    private boolean archive(final Player player, final String[] args) {
        if (args.length < 2) {
            player.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>Usage: /staffticket archive <id></red>"));
            return true;
        }
        final long ticketId = parseId(player, args[1]);
        if (ticketId < 0L) {
            return true;
        }
        this.plugin.getReportService().ticket(ticketId).thenAccept(optional ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (optional.isEmpty()) {
                    player.sendMessage(this.plugin.getMessageFormatter().message("reports.ticket-not-found"));
                    return;
                }
                final TicketRecord ticket = optional.get();
                if (ticket.status() != TicketStatus.CLOSED) {
                    player.sendMessage(this.plugin.getMessageFormatter().message("reports.ticket-archive-invalid"));
                    return;
                }
                this.plugin.getTicketDialogService().openArchiveConfirm(player, ticket, () -> {
                }, () -> {
                });
            })
        ).exceptionally(throwable -> {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                player.sendMessage(this.plugin.getMessageFormatter().message("errors.database"))
            );
            return null;
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            return List.of("list", "view", "assign", "close", "archive").stream()
                .filter(entry -> entry.startsWith(args[0].toLowerCase(Locale.ROOT)))
                .toList();
        }
        return List.of();
    }

    private long parseId(final Player player, final String raw) {
        try {
            return Long.parseLong(raw);
        } catch (final NumberFormatException exception) {
            player.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>ID de ticket invalide.</red>"));
            return -1L;
        }
    }

    private Throwable rootCause(final Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
