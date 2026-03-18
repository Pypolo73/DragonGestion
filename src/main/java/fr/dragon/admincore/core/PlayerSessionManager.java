package fr.dragon.admincore.core;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.shortninja.staffplusplus.session.IPlayerSession;
import net.shortninja.staffplusplus.session.SessionManager;
import org.bukkit.entity.Player;

public final class PlayerSessionManager implements SessionManager {

    private final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();

    @Override
    public IPlayerSession get(final UUID uuid) {
        return this.sessions.computeIfAbsent(uuid, ignored -> new PlayerSession(uuid, "unknown"));
    }

    @Override
    public IPlayerSession get(final Player player) {
        return this.sessions.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerSession(player.getUniqueId(), player.getName()));
    }

    @Override
    public Collection<? extends IPlayerSession> getAll() {
        return this.sessions.values();
    }

    @Override
    public Collection<? extends IPlayerSession> getOnlineStaffMembers() {
        return this.sessions.values().stream().filter(PlayerSession::isInStaffMode).toList();
    }

    public PlayerSession getOrCreate(final Player player) {
        return (PlayerSession) get(player);
    }

    public void updateName(final Player player) {
        this.sessions.compute(player.getUniqueId(), (ignored, existing) -> existing == null ? new PlayerSession(player.getUniqueId(), player.getName()) : existing);
    }

    public void remove(final UUID uuid) {
        this.sessions.remove(uuid);
    }
}
