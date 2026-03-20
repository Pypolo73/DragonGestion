package fr.dragon.admincore.lookup;

import fr.dragon.admincore.core.AdminCorePlugin;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LookupService {

    public static final int PAGE_SIZE = 28;

    private final AdminCorePlugin plugin;
    private final SessionRepository sessionRepository;

    public LookupService(final AdminCorePlugin plugin, final SessionRepository sessionRepository) {
        this.plugin = plugin;
        this.sessionRepository = sessionRepository;
    }

    public CompletableFuture<Void> recordJoin(final UUID uuid, final String ip, final String name) {
        final String server = this.plugin.getConfigLoader().config().getString(
            "network.server_name",
            this.plugin.getConfigLoader().config().getString("general.server-name", "paper-1")
        );
        return this.sessionRepository.recordJoin(uuid, ip, name, server, Instant.now());
    }

    public CompletableFuture<Void> recordQuit(final UUID uuid) {
        return this.sessionRepository.recordQuit(uuid, Instant.now());
    }

    public CompletableFuture<SessionSummary> summary(final UUID uuid, final String name) {
        return this.sessionRepository.summary(uuid, name);
    }

    public CompletableFuture<SessionRepository.SessionPage> page(final UUID uuid, final String name, final int page) {
        return this.sessionRepository.page(uuid, name, page, PAGE_SIZE);
    }
}
