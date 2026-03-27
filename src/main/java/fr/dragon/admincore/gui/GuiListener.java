package fr.dragon.admincore.gui;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.database.PlayerProfile;
import fr.dragon.admincore.chat.ChatHistoryEntry;
import fr.dragon.admincore.dialog.PlayerSearchDialog;
import fr.dragon.admincore.lookup.LookupMenus;
import fr.dragon.admincore.lookup.SessionSummary;
import fr.dragon.admincore.reports.TicketRecord;
import fr.dragon.admincore.sanctions.SanctionRecord;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;

public final class GuiListener implements Listener {

    private final AdminCorePlugin plugin;

    public GuiListener(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        final Object holder = event.getInventory().getHolder();
        if (holder == null) {
            return;
        }
        if (holder instanceof HistoryMenu.Holder || holder instanceof ActiveSanctionsMenu.Holder) {
            event.setCancelled(true);
            return;
        }
        if (holder instanceof ChatHistoryMenu.Holder historyHolder) {
            event.setCancelled(true);
            handleChatHistoryClick(player, historyHolder, event.getSlot(), event.getClick());
            return;
        }
        if (holder instanceof AdminCoreMenuContext.MainHolder) {
            event.setCancelled(true);
            handleMainClick(player, event.getSlot());
            return;
        }
        if (holder instanceof AdminCoreMenuContext.ActiveHolder activeHolder) {
            event.setCancelled(true);
            handleSanctionClick(player, activeHolder.sanctions(), event.getSlot());
            return;
        }
        if (holder instanceof AdminCoreMenuContext.SearchModeHolder) {
            event.setCancelled(true);
            handleSearchModeClick(player, event.getSlot());
            return;
        }
        if (holder instanceof AdminCoreMenuContext.SearchResultsHolder resultsHolder) {
            event.setCancelled(true);
            handleSearchResultClick(player, resultsHolder, event.getSlot());
            return;
        }
        if (holder instanceof AdminCoreMenuContext.ProfileListHolder profileListHolder) {
            event.setCancelled(true);
            handleProfileListClick(player, profileListHolder, event.getSlot());
            return;
        }
        if (holder instanceof AdminCoreMenuContext.RecentHolder recentHolder) {
            event.setCancelled(true);
            handleSanctionClick(player, recentHolder.sanctions(), event.getSlot());
            return;
        }
        if (holder instanceof TicketMenu.OpenHolder openHolder) {
            event.setCancelled(true);
            handleOpenTicketsClick(player, openHolder, event.getSlot(), event.getClick());
            return;
        }
        if (holder instanceof TicketMenu.StatusHolder statusHolder) {
            event.setCancelled(true);
            handleTicketStatusClick(player, statusHolder, event.getSlot(), event.getClick());
            return;
        }
        if (holder instanceof TicketMenu.HistoryHolder historyHolder) {
            event.setCancelled(true);
            handleTicketHistoryClick(player, historyHolder, event.getSlot());
            return;
        }
        if (holder instanceof LookupMenus.OverviewHolder overviewHolder) {
            event.setCancelled(true);
            handleLookupOverviewClick(player, overviewHolder, event.getSlot());
            return;
        }
        if (holder instanceof LookupMenus.SessionsHolder sessionsHolder) {
            event.setCancelled(true);
            handleLookupSessionsClick(player, sessionsHolder, event.getSlot());
            return;
        }
        if (holder instanceof AdminCoreMenuContext.OnlineHolder onlineHolder) {
            event.setCancelled(true);
            handleOnlineClick(player, onlineHolder.players(), event.getSlot());
            return;
        }
        if (holder instanceof StaffLogsMenu.Holder staffLogsHolder) {
            event.setCancelled(true);
            handleStaffLogsClick(player, staffLogsHolder, event.getSlot());
        }
    }

    private void handleMainClick(final Player player, final int slot) {
        if (slot == 10) {
            this.plugin.getSanctionService().recentActiveSanctions(54).thenAccept(records ->
                nextTick(() -> AdminCoreMenus.openActive(player, records))
            );
            return;
        }
        if (slot == 12) {
            nextTick(() -> AdminCoreMenus.openSearchMode(player));
            return;
        }
        if (slot == 14) {
            this.plugin.getSanctionService().recentActiveSanctions(54).thenAccept(records ->
                nextTick(() -> AdminCoreMenus.openRecent(player, records))
            );
            return;
        }
        if (slot == 13) {
            this.plugin.getReportService().openPage(0).thenAccept(page ->
                nextTick(() -> TicketMenu.openOpen(player, page))
            );
            return;
        }
        if (slot == 16) {
            final List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers().stream().filter(target -> !target.equals(player)).toList());
            nextTick(() -> AdminCoreMenus.openOnlinePlayers(player, players));
        }
    }

    private void handleSearchModeClick(final Player player, final int slot) {
        if (slot == 11) {
            this.plugin.getSanctionService().listProfiles(0, 45).thenAccept(profiles ->
                nextTick(() -> AdminCoreMenus.openProfiles(player, 0, profiles))
            );
            return;
        }
        if (slot != 15) {
            return;
        }
        player.closeInventory();
        player.showDialog(PlayerSearchDialog.create("Recherche pseudo", "", (response, audience) -> {
            final String input = response.getText("player") == null ? "" : response.getText("player").trim();
            if (input.isEmpty()) {
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> AdminCoreMenus.openSearchMode(player), 1L);
                return;
            }
            this.plugin.getSanctionService().searchPlayerNames(input, 45).thenAccept(results ->
                CompletableFuture.allOf(results.stream().map(name ->
                    this.plugin.getSanctionService()
                        .history(Bukkit.getOfflinePlayer(name).getUniqueId(), name, 54)
                        .thenApply(history -> new AdminCoreMenuContext.SearchResultEntry(name, history.size()))
                ).toArray(CompletableFuture[]::new)).thenRun(() -> sync(() -> {
                    if (results.isEmpty()) {
                        player.sendMessage(plugin.getMessageFormatter().message("errors.no-player-found"));
                        nextTick(() -> AdminCoreMenus.openSearchMode(player));
                        return;
                    }
                    final List<AdminCoreMenuContext.SearchResultEntry> entries = results.stream()
                        .map(name -> new AdminCoreMenuContext.SearchResultEntry(
                            name,
                            plugin.getSanctionService().history(Bukkit.getOfflinePlayer(name).getUniqueId(), name, 54).join().size()
                        ))
                        .sorted(Comparator.comparingInt(AdminCoreMenuContext.SearchResultEntry::sanctionCount).reversed())
                        .toList();
                    nextTick(() -> AdminCoreMenus.openSearchResults(player, input, entries));
                }))
            );
        }, () -> AdminCoreMenus.openSearchMode(player)));
    }

    private void handleSearchResultClick(final Player player, final AdminCoreMenuContext.SearchResultsHolder holder, final int slot) {
        if (slot == 49) {
            nextTick(() -> AdminCoreMenus.openSearchMode(player));
            return;
        }
        if (slot < 0 || slot >= holder.results().size()) {
            return;
        }
        final String targetName = holder.results().get(slot).name();
        openLookupOverview(player, Bukkit.getOfflinePlayer(targetName).getUniqueId(), targetName);
    }

    private void handleProfileListClick(final Player player, final AdminCoreMenuContext.ProfileListHolder holder, final int slot) {
        if (slot == 49) {
            nextTick(() -> AdminCoreMenus.openSearchMode(player));
            return;
        }
        if (slot == 45) {
            final int page = Math.max(0, holder.page() - 1);
            this.plugin.getSanctionService().listProfiles(page * 45, 45).thenAccept(profiles ->
                nextTick(() -> AdminCoreMenus.openProfiles(player, page, profiles))
            );
            return;
        }
        if (slot == 53) {
            final int page = holder.page() + 1;
            this.plugin.getSanctionService().listProfiles(page * 45, 45).thenAccept(profiles ->
                nextTick(() -> AdminCoreMenus.openProfiles(player, page, profiles))
            );
            return;
        }
        if (slot < 0 || slot >= holder.names().size()) {
            return;
        }
        final String targetName = holder.names().get(slot);
        openLookupOverview(player, Bukkit.getOfflinePlayer(targetName).getUniqueId(), targetName);
    }

    private void handleSanctionClick(final Player player, final List<SanctionRecord> sanctions, final int slot) {
        if (slot < 0 || slot >= sanctions.size()) {
            return;
        }
        final SanctionRecord sanction = sanctions.get(slot);
        this.plugin.getSanctionService().history(sanction.targetUuid(), sanction.targetName(), 54).thenAccept(history ->
            nextTick(() -> HistoryMenu.open(player, sanction.targetName(), history))
        );
    }

    private void handleOnlineClick(final Player player, final List<Player> players, final int slot) {
        if (slot < 0 || slot >= players.size()) {
            return;
        }
        final Player target = players.get(slot);
        this.plugin.getVanishService().setVanished(player, true);
        if (this.plugin.getStaffModeService().isInStaffMode(player.getUniqueId())
            && !this.plugin.getStaffModeService().isObservationMode(player.getUniqueId())) {
            this.plugin.getStaffModeService().toggleObservationMode(player);
        } else if (!this.plugin.getStaffModeService().isInStaffMode(player.getUniqueId())) {
            player.setGameMode(GameMode.CREATIVE);
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        player.teleport(target.getLocation());
        player.sendMessage(this.plugin.getMessageFormatter().message(
            "admin.online-target",
            this.plugin.getMessageFormatter().text("target", target.getName())
        ));
    }

    private void handleChatHistoryClick(final Player player, final ChatHistoryMenu.Holder holder, final int slot, final ClickType click) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 45) {
            if (holder.page() <= 0) {
                return;
            }
            reopenChatHistory(player, holder.authorFilter(), holder.page() - 1);
            return;
        }
        if (slot == 53) {
            final int nextPage = holder.page() + 1;
            if (nextPage * 45 >= holder.entries().size()) {
                return;
            }
            reopenChatHistory(player, holder.authorFilter(), nextPage);
            return;
        }
        final int index = holder.page() * 45 + slot;
        if (slot < 0 || slot >= 45 || index >= holder.entries().size()) {
            return;
        }
        final ChatHistoryEntry entry = holder.entries().get(index);
        if (click.isRightClick()) {
            this.plugin.getSanctionService().history(Bukkit.getOfflinePlayer(entry.author()).getUniqueId(), entry.author(), 54).thenAccept(history ->
                nextTick(() -> HistoryMenu.open(player, entry.author(), history))
            );
            return;
        }
        if (this.plugin.getChatService().removeRecentMessage(entry)) {
            player.sendMessage(this.plugin.getMessageFormatter().message(
                "chat.message-removed",
                this.plugin.getMessageFormatter().text("author", entry.author())
            ));
        }
        reopenChatHistory(player, holder.authorFilter(), holder.page());
    }

    private void reopenChatHistory(final Player player, final String authorFilter, final int page) {
        final int limit = Math.max(1, this.plugin.getConfigLoader().config().getInt(
            "chat.clear-message-history-limit",
            this.plugin.getConfigLoader().config().getInt("chat.history-limit", 100)
        ));
        nextTick(() -> {
            if (authorFilter == null || authorFilter.isBlank()) {
                ChatHistoryMenu.open(player, Math.max(0, page), this.plugin.getChatService().recentMessages(limit));
                return;
            }
            ChatHistoryMenu.openForAuthor(
                player,
                authorFilter,
                Math.max(0, page),
                this.plugin.getChatService().recentMessagesByAuthor(authorFilter, limit)
            );
        });
    }

    private void handleStaffLogsClick(final Player player, final StaffLogsMenu.Holder holder, final int slot) {
        if (slot == 45 && holder.page().page() > 0) {
            StaffLogsMenu.changePage(this.plugin, player, holder, -1);
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 53 && holder.page().hasNext()) {
            StaffLogsMenu.changePage(this.plugin, player, holder, 1);
        }
    }

    private void handleOpenTicketsClick(final Player player, final TicketMenu.OpenHolder holder, final int slot, final ClickType click) {
        if (slot == 36) {
            return;
        }
        if (slot == 40) {
            this.plugin.getReportService().closedPage(0).thenAccept(page ->
                nextTick(() -> TicketMenu.openClosed(player, page))
            );
            return;
        }
        if (slot == 44) {
            this.plugin.getReportService().archivedPage(0).thenAccept(page ->
                nextTick(() -> TicketMenu.openArchives(player, page))
            );
            return;
        }
        if (slot == 45 && holder.page().page() > 0) {
            this.plugin.getReportService().openPage(holder.page().page() - 1).thenAccept(page ->
                nextTick(() -> TicketMenu.openOpen(player, page))
            );
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 53 && holder.page().hasNext()) {
            this.plugin.getReportService().openPage(holder.page().page() + 1).thenAccept(page ->
                nextTick(() -> TicketMenu.openOpen(player, page))
            );
            return;
        }
        if (slot < 0 || slot >= 28 || slot >= holder.page().entries().size()) {
            return;
        }
        final TicketRecord ticket = holder.page().entries().get(slot);
        if (click.isLeftClick()) {
            if (ticket.status() == fr.dragon.admincore.reports.TicketStatus.OPEN) {
                this.plugin.getReportService().assign(player, ticket.id()).thenAccept(updated ->
                    sync(() -> {
                        player.sendMessage(this.plugin.getMessageFormatter().message(
                            "reports.assigned",
                            this.plugin.getMessageFormatter().text("id", Long.toString(updated.id()))
                        ));
                        this.plugin.getTicketDialogService().openStaffConversation(player, updated);
                    })
                ).exceptionally(throwable -> {
                    sync(() -> player.sendMessage(this.plugin.getMessageFormatter().message("errors.database")));
                    return null;
                });
                return;
            }
            this.plugin.getTicketDialogService().openStaffConversation(player, ticket);
            return;
        }
        if (click.isRightClick()) {
            openTicketCloseFlow(player, ticket, holder.page().page());
        }
    }

    private void handleTicketStatusClick(final Player player, final TicketMenu.StatusHolder holder, final int slot, final ClickType click) {
        if (slot == 36) {
            this.plugin.getReportService().openPage(0).thenAccept(page ->
                nextTick(() -> TicketMenu.openOpen(player, page))
            );
            return;
        }
        if (slot == 40) {
            if (holder.view() != TicketMenu.TicketView.CLOSED) {
                this.plugin.getReportService().closedPage(0).thenAccept(page ->
                    nextTick(() -> TicketMenu.openClosed(player, page))
                );
            }
            return;
        }
        if (slot == 44) {
            if (holder.view() != TicketMenu.TicketView.ARCHIVED) {
                this.plugin.getReportService().archivedPage(0).thenAccept(page ->
                    nextTick(() -> TicketMenu.openArchives(player, page))
                );
            }
            return;
        }
        if (slot == 45 && holder.page().page() > 0) {
            openTicketStatusPage(player, holder.view(), holder.page().page() - 1);
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 53 && holder.page().hasNext()) {
            openTicketStatusPage(player, holder.view(), holder.page().page() + 1);
            return;
        }
        if (slot < 0 || slot >= 28 || slot >= holder.page().entries().size()) {
            return;
        }
        if (holder.view() != TicketMenu.TicketView.CLOSED || !click.isShiftClick() || !click.isRightClick()) {
            return;
        }
        final TicketRecord ticket = holder.page().entries().get(slot);
        this.plugin.getReportService().archive(player, ticket.id()).thenAccept(updated ->
            sync(() -> {
                player.sendMessage(this.plugin.getMessageFormatter().message(
                    "reports.archived",
                    this.plugin.getMessageFormatter().text("id", Long.toString(updated.id()))
                ));
                openTicketStatusPage(player, TicketMenu.TicketView.CLOSED, holder.page().page());
            })
        ).exceptionally(throwable -> {
            sync(() -> player.sendMessage(this.plugin.getMessageFormatter().message("errors.database")));
            return null;
        });
    }

    private void handleTicketHistoryClick(final Player player, final TicketMenu.HistoryHolder holder, final int slot) {
        if (slot == 45 && holder.page().page() > 0) {
            final String targetName = holder.targetName();
            this.plugin.getReportService().history(Bukkit.getOfflinePlayer(targetName).getUniqueId(), targetName, holder.page().page() - 1).thenAccept(page ->
                nextTick(() -> TicketMenu.openHistory(player, targetName, page))
            );
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 53 && holder.page().hasNext()) {
            final String targetName = holder.targetName();
            this.plugin.getReportService().history(Bukkit.getOfflinePlayer(targetName).getUniqueId(), targetName, holder.page().page() + 1).thenAccept(page ->
                nextTick(() -> TicketMenu.openHistory(player, targetName, page))
            );
        }
    }

    private void openTicketStatusPage(final Player player, final TicketMenu.TicketView view, final int page) {
        if (view == TicketMenu.TicketView.CLOSED) {
            this.plugin.getReportService().closedPage(page).thenAccept(result ->
                nextTick(() -> TicketMenu.openClosed(player, result))
            );
            return;
        }
        this.plugin.getReportService().archivedPage(page).thenAccept(result ->
            nextTick(() -> TicketMenu.openArchives(player, result))
        );
    }

    private void openTicketCloseFlow(final Player player, final TicketRecord ticket, final int returnPage) {
        this.plugin.getTicketDialogService().openCloseDialog(
            player,
            ticket,
            () -> this.plugin.getReportService().openPage(returnPage).thenAccept(page ->
                nextTick(() -> TicketMenu.openOpen(player, page))
            ),
            () -> this.plugin.getReportService().openPage(returnPage).thenAccept(page ->
                nextTick(() -> TicketMenu.openOpen(player, page))
            )
        );
    }

    private void handleLookupOverviewClick(final Player player, final LookupMenus.OverviewHolder holder, final int slot) {
        if (slot == 13) {
            this.plugin.getSanctionService().history(holder.targetUuid(), holder.targetName(), 54).thenAccept(history ->
                nextTick(() -> HistoryMenu.open(player, holder.targetName(), history))
            );
            return;
        }
        if (slot == 15) {
            this.plugin.getLookupService().page(holder.targetUuid(), holder.targetName(), 0).thenAccept(page ->
                nextTick(() -> LookupMenus.openSessions(player, holder.targetUuid(), holder.targetName(), page))
            );
        }
    }

    private void handleLookupSessionsClick(final Player player, final LookupMenus.SessionsHolder holder, final int slot) {
        if (slot == 45 && holder.page() > 0) {
            this.plugin.getLookupService().page(holder.targetUuid(), holder.targetName(), holder.page() - 1).thenAccept(page ->
                nextTick(() -> LookupMenus.openSessions(player, holder.targetUuid(), holder.targetName(), page))
            );
            return;
        }
        if (slot == 49) {
            openLookupOverview(player, holder.targetUuid(), holder.targetName());
            return;
        }
        if (slot == 53 && holder.hasNext()) {
            this.plugin.getLookupService().page(holder.targetUuid(), holder.targetName(), holder.page() + 1).thenAccept(page ->
                nextTick(() -> LookupMenus.openSessions(player, holder.targetUuid(), holder.targetName(), page))
            );
        }
    }

    private void openLookupOverview(final Player player, final java.util.UUID targetUuid, final String targetName) {
        final java.util.concurrent.CompletableFuture<PlayerProfile> profileFuture =
            this.plugin.getSanctionService().playerProfile(targetUuid, targetName);
        final java.util.concurrent.CompletableFuture<SessionSummary> summaryFuture =
            this.plugin.getLookupService().summary(targetUuid, targetName);
        profileFuture.thenCombine(summaryFuture, java.util.AbstractMap.SimpleEntry::new).thenAccept(entry ->
            nextTick(() -> LookupMenus.openOverview(player, targetUuid, targetName, entry.getKey(), entry.getValue()))
        );
    }

    private void sync(final Runnable runnable) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, runnable);
    }

    private void nextTick(final Runnable runnable) {
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, runnable, 1L);
    }
}
