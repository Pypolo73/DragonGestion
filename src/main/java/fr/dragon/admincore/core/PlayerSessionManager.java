package fr.dragon.admincore.core;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class PlayerSessionManager {

    private final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();

    public PlayerSession get(final UUID uuid) {
        return this.sessions.computeIfAbsent(uuid, ignored -> new PlayerSession(uuid, "unknown"));
    }

    public PlayerSession get(final Player player) {
        return this.sessions.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerSession(player.getUniqueId(), player.getName()));
    }

    public Collection<PlayerSession> getAll() {
        return this.sessions.values();
    }

    public Collection<PlayerSession> getOnlineStaffMembers() {
        return this.sessions.values().stream().filter(PlayerSession::isInStaffMode).toList();
    }

    public PlayerSession getOrCreate(final Player player) {
        return get(player);
    }

    public void updateName(final Player player) {
        this.sessions.compute(player.getUniqueId(), (ignored, existing) -> existing == null ? new PlayerSession(player.getUniqueId(), player.getName()) : existing);
    }

    public void remove(final UUID uuid) {
        this.sessions.remove(uuid);
    }
}
