package fr.dragon.admincore.staffmode;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.core.StaffActionType;
import fr.dragon.admincore.gui.ActiveSanctionsMenu;
import fr.dragon.admincore.gui.AdminCoreMenuContext;
import fr.dragon.admincore.gui.ChatHistoryMenu;
import fr.dragon.admincore.gui.HistoryMenu;
import fr.dragon.admincore.gui.TicketMenu;
import fr.dragon.admincore.inventory.InventoryMenuContext;
import fr.dragon.admincore.lookup.LookupMenus;
import org.bukkit.Location;
import org.bukkit.block.Block;
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
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
        if (tool == StaffTool.TELEPORT) {
            handleTeleportInteract(event);
            return;
        }
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
        if (tool == StaffTool.TELEPORT) {
            if (event.getPlayer().isSneaking()) {
                pullTarget(event.getPlayer(), target);
            } else {
                teleportToTarget(event.getPlayer(), target);
            }
            return;
        }
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
        if (tool == StaffTool.TELEPORT) {
            if (event.getPlayer().isSneaking()) {
                pullTarget(event.getPlayer(), target);
            } else {
                teleportToTarget(event.getPlayer(), target);
            }
            return;
        }
        handleTool(event.getPlayer(), tool, target);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeldItemChange(final PlayerItemHeldEvent event) {
        if (!this.plugin.getStaffModeService().isInStaffMode(event.getPlayer().getUniqueId())) {
            return;
        }
        this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
            this.plugin.getStaffModeService().refreshMonitor(event.getPlayer())
        );
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (!this.plugin.getStaffModeService().isInStaffMode(event.getPlayer().getUniqueId())) {
            return;
        }
        if (this.plugin.getStaffModeService().isObservationMode(event.getPlayer().getUniqueId())) {
            event.setCancelled(false);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(final BlockPlaceEvent event) {
        if (!this.plugin.getStaffModeService().isInStaffMode(event.getPlayer().getUniqueId())) {
            return;
        }
        if (this.plugin.getStaffModeService().isObservationMode(event.getPlayer().getUniqueId())) {
            event.setCancelled(false);
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
            final boolean vanished = this.plugin.getVanishService().toggle(player);
            this.plugin.getStaffActionLogger().log(
                player,
                StaffActionType.VANISH_TOGGLE,
                player.getUniqueId(),
                player.getName(),
                vanished ? "Vanish active via staffmode" : "Vanish desactive via staffmode"
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
        if (tool == StaffTool.MONITOR) {
            this.plugin.getStaffModeService().refreshMonitor(player);
            return;
        }
        if (target == null) {
            return;
        }
        if (tool == StaffTool.FREEZE) {
            final boolean frozen = this.plugin.getStaffModeService().toggleFreeze(target);
            this.plugin.getStaffActionLogger().log(
                player,
                StaffActionType.FREEZE_TOGGLE,
                target.getUniqueId(),
                target.getName(),
                frozen ? "Freeze active via item staff" : "Freeze retire via item staff"
            );
        } else if (tool == StaffTool.RIDE) {
            target.addPassenger(player);
        }
    }

    private void handleTeleportInteract(final PlayerInteractEvent event) {
        if (event.getAction().isLeftClick() && event.getPlayer().isSneaking()) {
            final Block block = event.getPlayer().getTargetBlockExact(50);
            if (block == null) {
                event.getPlayer().sendMessage(this.plugin.getMessageFormatter().message("staffmode.teleport-no-target"));
                return;
            }
            teleportToGround(event.getPlayer(), block.getLocation().add(0.5D, 1.0D, 0.5D));
            return;
        }
        if (event.getAction().isRightClick()) {
            final Player target = targetInSight(event.getPlayer());
            if (target == null) {
                event.getPlayer().sendMessage(this.plugin.getMessageFormatter().message("staffmode.teleport-no-target"));
                return;
            }
            if (event.getPlayer().isSneaking()) {
                pullTarget(event.getPlayer(), target);
            } else {
                teleportToTarget(event.getPlayer(), target);
            }
        }
    }

    private void teleportToTarget(final Player staff, final Player target) {
        ensureVanished(staff);
        staff.teleport(target.getLocation());
        this.plugin.getStaffActionLogger().log(
            staff,
            StaffActionType.TP_TO,
            target.getUniqueId(),
            target.getName(),
            formatLocation("Vers joueur", target.getLocation())
        );
    }

    private void pullTarget(final Player staff, final Player target) {
        target.teleport(staff.getLocation());
        this.plugin.getStaffActionLogger().log(
            staff,
            StaffActionType.TP_PULL,
            target.getUniqueId(),
            target.getName(),
            formatLocation("Vers staff", staff.getLocation())
        );
    }

    private void teleportToGround(final Player staff, final Location location) {
        ensureVanished(staff);
        staff.teleport(location);
        this.plugin.getStaffActionLogger().log(
            staff,
            StaffActionType.TP_GROUND,
            staff.getUniqueId(),
            staff.getName(),
            formatLocation("Bloc vise", location)
        );
    }

    private void ensureVanished(final Player player) {
        if (!this.plugin.getVanishService().isVanished(player.getUniqueId())) {
            this.plugin.getVanishService().setVanished(player, true);
        }
    }

    private String formatLocation(final String prefix, final Location location) {
        return prefix + " " + location.getWorld().getName() + " "
            + location.getBlockX() + " "
            + location.getBlockY() + " "
            + location.getBlockZ();
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
            || holder instanceof TicketMenu.OpenHolder
            || holder instanceof TicketMenu.HistoryHolder
            || holder instanceof LookupMenus.OverviewHolder
            || holder instanceof LookupMenus.SessionsHolder
            || holder instanceof InventoryMenuContext.SelectorHolder
            || holder instanceof InventoryMenuContext.ReadOnlyHolder
            || holder instanceof InventoryMenuContext.EditHolder
            || holder instanceof InventoryMenuContext.BackupsHolder
            || holder instanceof InventoryMenuContext.ConfirmHolder;
    }
}
