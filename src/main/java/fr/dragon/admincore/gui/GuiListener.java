package fr.dragon.admincore.gui;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.database.PlayerProfile;
import fr.dragon.admincore.chat.ChatHistoryEntry;
import fr.dragon.admincore.dialog.PlayerSearchDialog;
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
        if (holder instanceof AdminCoreMenuContext.OnlineHolder onlineHolder) {
            event.setCancelled(true);
            handleOnlineClick(player, onlineHolder.players(), event.getSlot());
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
            final String input = response.getText("query") == null ? "" : response.getText("query").trim();
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
        }));
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
        this.plugin.getSanctionService().history(Bukkit.getOfflinePlayer(targetName).getUniqueId(), targetName, 54).thenAccept(history ->
            nextTick(() -> HistoryMenu.open(player, targetName, history))
        );
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
        this.plugin.getSanctionService().history(Bukkit.getOfflinePlayer(targetName).getUniqueId(), targetName, 54).thenAccept(history ->
            nextTick(() -> HistoryMenu.open(player, targetName, history))
        );
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

    private void sync(final Runnable runnable) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, runnable);
    }

    private void nextTick(final Runnable runnable) {
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, runnable, 1L);
    }
}
