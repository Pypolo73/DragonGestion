package fr.dragon.admincore.teleportation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.time.Instant;
import java.util.UUID;

public record TeleportData(
    long id,
    UUID owner,
    String name,
    String world,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    boolean isPublic,
    Instant createdAt,
    String description
) {
    public static TeleportData home(UUID owner, String name, Location loc) {
        return new TeleportData(
            0, owner, name,
            loc.getWorld().getName(),
            loc.getX(), loc.getY(), loc.getZ(),
            loc.getYaw(), loc.getPitch(),
            false, Instant.now(), ""
        );
    }

    public static TeleportData warp(String name, Location loc, String description) {
        return new TeleportData(
            0, null, name,
            loc.getWorld().getName(),
            loc.getX(), loc.getY(), loc.getZ(),
            loc.getYaw(), loc.getPitch(),
            false, Instant.now(), description
        );
    }

    public static TeleportData spawn(Location loc) {
        return new TeleportData(
            0, null, "spawn",
            loc.getWorld().getName(),
            loc.getX(), loc.getY(), loc.getZ(),
            loc.getYaw(), loc.getPitch(),
            false, Instant.now(), ""
        );
    }

    public Location toLocation(org.bukkit.Server server) {
        World worldObj = server.getWorld(this.world);
        if (worldObj == null) {
            worldObj = server.getWorlds().get(0);
        }
        return new Location(worldObj, x, y, z, yaw, pitch);
    }

    public String getLocationString() {
        return String.format("%s (%.1f, %.1f, %.1f)", world, x, y, z);
    }

    public Component toComponent(NamedTextColor color) {
        return Component.text(name, color)
            .append(Component.text(" - ", NamedTextColor.GRAY))
            .append(Component.text(getLocationString(), NamedTextColor.DARK_GRAY));
    }
}
