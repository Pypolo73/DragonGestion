package fr.dragon.admincore.staffmode;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public record StaffModeSnapshot(
    ItemStack[] contents,
    ItemStack[] armor,
    ItemStack offHand,
    GameMode gameMode,
    boolean allowFlight,
    boolean flying,
    Location location,
    boolean wasVanished
) {
}
