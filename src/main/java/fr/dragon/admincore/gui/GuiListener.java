package fr.dragon.admincore.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class GuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof HistoryMenu.Holder
            || event.getInventory().getHolder() instanceof ActiveSanctionsMenu.Holder) {
            event.setCancelled(true);
        }
    }
}
