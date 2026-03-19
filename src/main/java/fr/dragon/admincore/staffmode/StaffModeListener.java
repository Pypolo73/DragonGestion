package fr.dragon.admincore.staffmode;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.gui.ActiveSanctionsMenu;
import fr.dragon.admincore.gui.AdminCoreMenuContext;
import fr.dragon.admincore.gui.ChatHistoryMenu;
import fr.dragon.admincore.gui.HistoryMenu;
import fr.dragon.admincore.inventory.InventoryMenuContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;

public final class StaffModeListener implements Listener {

    private final AdminCorePlugin plugin;

    public StaffModeListener(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onMove(final PlayerMoveEvent event) {
        if (!this.plugin.getStaffModeService().isFrozen(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
            || event.getFrom().getBlockY() != event.getTo().getBlockY()
            || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(final PlayerCommandPreprocessEvent event) {
        final String lower = event.getMessage().toLowerCase();
        if (this.plugin.getStaffModeService().isFrozen(event.getPlayer().getUniqueId())
            && !event.getPlayer().hasPermission(PermissionService.FREEZE_BYPASS)
            && this.plugin.getConfigLoader().config().getStringList("staff.freeze-allowed-commands").stream().noneMatch(lower::startsWith)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(this.plugin.getMessageFormatter().message("staffmode.freeze-blocked"));
            return;
        }
        if (lower.startsWith("/msg ") || lower.startsWith("/tell ") || lower.startsWith("/w ") || lower.startsWith("/r ") || lower.startsWith("/reply ")) {
            for (final var spyUuid : this.plugin.getChatService().getSpyEnabled()) {
                final Player spy = this.plugin.getServer().getPlayer(spyUuid);
                if (spy != null && !spy.equals(event.getPlayer())) {
                    spy.sendMessage(this.plugin.getMessageFormatter().message(
                        "staffmode.spy-format",
                        this.plugin.getMessageFormatter().text("player", event.getPlayer().getName()),
                        this.plugin.getMessageFormatter().text("message", event.getMessage())
                    ));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(final PlayerInteractEvent event) {
        if (!this.plugin.getStaffModeService().isInStaffMode(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() == org.bukkit.event.block.Action.PHYSICAL) {
            return;
        }
        final StaffTool tool = tool(event.getPlayer().getInventory().getItemInMainHand());
        if (tool == null) {
            return;
        }
        event.setCancelled(true);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        handleTool(event.getPlayer(), tool, targetInSight(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractEntity(final PlayerInteractEntityEvent event) {
        if (!this.plugin.getStaffModeService().isInStaffMode(event.getPlayer().getUniqueId())) {
            return;
        }
        final StaffTool tool = tool(event.getPlayer().getInventory().getItemInMainHand());
        if (tool == null || !(event.getRightClicked() instanceof Player target)) {
            return;
        }
        event.setCancelled(true);
        handleTool(event.getPlayer(), tool, target);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractAtEntity(final PlayerInteractAtEntityEvent event) {
        if (!this.plugin.getStaffModeService().isInStaffMode(event.getPlayer().getUniqueId())) {
            return;
        }
        final StaffTool tool = tool(event.getPlayer().getInventory().getItemInMainHand());
        if (tool == null || !(event.getRightClicked() instanceof Player target)) {
            return;
        }
        event.setCancelled(true);
        handleTool(event.getPlayer(), tool, target);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(final PlayerDropItemEvent event) {
        if (this.plugin.getStaffModeService().isInStaffMode(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(final PlayerSwapHandItemsEvent event) {
        if (this.plugin.getStaffModeService().isInStaffMode(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !this.plugin.getStaffModeService().isInStaffMode(player.getUniqueId())) {
            return;
        }
        final InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        if (isManagedGui(topHolder)) {
            if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
                event.setCancelled(true);
            }
            return;
        }
        if (this.plugin.getStaffModeService().isObservationMode(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player
            && this.plugin.getStaffModeService().isInStaffMode(player.getUniqueId())
            && !this.plugin.getStaffModeService().isObservationMode(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(final EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && this.plugin.getStaffModeService().isInStaffMode(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private StaffTool tool(final ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        final String toolName = item.getItemMeta().getPersistentDataContainer().get(StaffModeServiceImpl.TOOL_KEY, PersistentDataType.STRING);
        if (toolName == null) {
            return null;
        }
        return StaffTool.valueOf(toolName);
    }

    private Player targetInSight(final Player player) {
        final Entity target = player.getTargetEntity(8);
        return target instanceof Player targetedPlayer ? targetedPlayer : null;
    }

    private void handleTool(final Player player, final StaffTool tool, final Player target) {
        if (tool == StaffTool.VANISH) {
            this.plugin.getVanishService().toggle(player);
            return;
        }
        if (tool == StaffTool.MENU) {
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () ->
                fr.dragon.admincore.gui.AdminCoreMenus.openMain(player), 1L
            );
            return;
        }
        if (tool == StaffTool.SPECTATOR) {
            final boolean enabled = this.plugin.getStaffModeService().toggleObservationMode(player);
            player.sendMessage(this.plugin.getMessageFormatter().message(enabled ? "staffmode.observe-enabled" : "staffmode.observe-disabled"));
            return;
        }
        if (tool == StaffTool.HISTORY) {
            if (target == null) {
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () ->
                    fr.dragon.admincore.gui.AdminCoreMenus.openMain(player), 1L
                );
                return;
            }
            this.plugin.getSanctionService().history(target.getUniqueId(), target.getName(), 54).thenAccept(history ->
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () ->
                    HistoryMenu.open(player, target.getName(), history), 1L
                )
            );
            return;
        }
        if (tool == StaffTool.CHAT_LOGS) {
            final int limit = Math.max(1, this.plugin.getConfigLoader().config().getInt(
                "chat.clear-message-history-limit",
                this.plugin.getConfigLoader().config().getInt("chat.history-limit", 100)
            ));
            if (target == null) {
                this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () ->
                    ChatHistoryMenu.open(player, 0, this.plugin.getChatService().recentMessages(limit)), 1L
                );
                return;
            }
            this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () ->
                ChatHistoryMenu.openForAuthor(player, target.getName(), 0, this.plugin.getChatService().recentMessagesByAuthor(target.getName(), limit)), 1L
            );
            return;
        }
        if (tool == StaffTool.EXIT) {
            this.plugin.getStaffModeService().toggleStaffMode(player);
            return;
        }
        if (target == null) {
            return;
        }
        if (tool == StaffTool.FREEZE) {
            this.plugin.getStaffModeService().toggleFreeze(target);
        } else if (tool == StaffTool.RIDE) {
            target.addPassenger(player);
        }
    }

    private boolean isManagedGui(final InventoryHolder holder) {
        return holder instanceof HistoryMenu.Holder
            || holder instanceof ActiveSanctionsMenu.Holder
            || holder instanceof AdminCoreMenuContext.MainHolder
            || holder instanceof AdminCoreMenuContext.ActiveHolder
            || holder instanceof AdminCoreMenuContext.SearchModeHolder
            || holder instanceof AdminCoreMenuContext.SearchResultsHolder
            || holder instanceof AdminCoreMenuContext.ProfileListHolder
            || holder instanceof AdminCoreMenuContext.RecentHolder
            || holder instanceof AdminCoreMenuContext.OnlineHolder
            || holder instanceof ChatHistoryMenu.Holder
            || holder instanceof InventoryMenuContext.SelectorHolder
            || holder instanceof InventoryMenuContext.ReadOnlyHolder
            || holder instanceof InventoryMenuContext.EditHolder
            || holder instanceof InventoryMenuContext.BackupsHolder
            || holder instanceof InventoryMenuContext.ConfirmHolder;
    }
}
