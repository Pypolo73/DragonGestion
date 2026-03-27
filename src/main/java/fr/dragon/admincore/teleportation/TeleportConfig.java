package fr.dragon.admincore.teleportation;

import java.util.List;
import java.util.Map;

public record TeleportConfig(
    HomeConfig home,
    WarpConfig warp,
    SpawnConfig spawn,
    RtpConfig rtp,
    TpaConfig tpa,
    BackConfig back,
    DatabaseConfig database
) {
    public record HomeConfig(
        int maxHomes,
        int warmup,
        int cooldown,
        String defaultName
    ) {}

    public record WarpConfig(
        int maxWarps,
        int warmup,
        int cooldown
    ) {}

    public record SpawnConfig(
        int warmup,
        boolean setAsDefaultSpawn,
        boolean teleportNewPlayers
    ) {}

    public record RtpConfig(
        boolean enabled,
        int maxAttempts,
        int minDistance,
        int maxDistance,
        int minHeight,
        List<String> allowedWorlds,
        List<String> restrictedWorlds,
        int cooldown,
        int warmup
    ) {}

    public record TpaConfig(
        int requestTimeout,
        int warmup,
        boolean allowIgnore
    ) {}

    public record BackConfig(
        boolean enabled,
        boolean saveOnDeath,
        boolean saveOnTeleport,
        List<String> restrictedWorlds
    ) {}

    public record DatabaseConfig(
        String type,
        Map<String, Object> mysql,
        Map<String, Object> sqlite
    ) {}
}
