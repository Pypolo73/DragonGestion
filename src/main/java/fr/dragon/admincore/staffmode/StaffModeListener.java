package fr.dragon.admincore.staffmode;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

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

    @EventHandler(ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent event) {
        if (!this.plugin.getStaffModeService().isInStaffMode(event.getPlayer().getUniqueId())) {
            return;
        }
        final ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null || item.getItemMeta().displayName() == null) {
            return;
        }
        final String itemName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        if ("Vanish".equalsIgnoreCase(itemName)) {
            this.plugin.getVanishService().toggle(event.getPlayer());
        } else if ("Quitter".equalsIgnoreCase(itemName)) {
            this.plugin.getStaffModeService().toggleStaffMode(event.getPlayer());
        } else if ("Freeze".equalsIgnoreCase(itemName)) {
            final Entity target = event.getPlayer().getTargetEntity(6);
            if (target instanceof Player targetedPlayer) {
                this.plugin.getStaffModeService().toggleFreeze(targetedPlayer);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(final PlayerDropItemEvent event) {
        if (this.plugin.getStaffModeService().isInStaffMode(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(final EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && this.plugin.getStaffModeService().isInStaffMode(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
