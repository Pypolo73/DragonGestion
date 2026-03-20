package fr.dragon.admincore.alerts;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.database.PlayerRepository;
import fr.dragon.admincore.database.SanctionRepository;
import fr.dragon.admincore.sanctions.SanctionRecord;
import fr.dragon.admincore.sanctions.SanctionType;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public final class AlertManager implements Listener {

    private final AdminCorePlugin plugin;
    private final AlertRepository alertRepository;
    private final SanctionRepository sanctionRepository;
    private final PlayerRepository playerRepository;

    public AlertManager(
        final AdminCorePlugin plugin,
        final AlertRepository alertRepository,
        final SanctionRepository sanctionRepository,
        final PlayerRepository playerRepository
    ) {
        this.plugin = plugin;
        this.alertRepository = alertRepository;
        this.sanctionRepository = sanctionRepository;
        this.playerRepository = playerRepository;
    }

    public void handleWarnApplied(final SanctionRecord record) {
        if (record.type() != SanctionType.WARN || record.targetUuid() == null) {
            return;
        }
        final int threshold = this.plugin.getConfigLoader().config().getInt("alerts.warns_threshold", 3);
        if (threshold <= 0) {
            return;
        }
        final int windowMinutes = Math.max(1, this.plugin.getConfigLoader().config().getInt("alerts.warns_window_minutes", 60));
        final Instant since = Instant.now().minus(Duration.ofMinutes(windowMinutes));
        this.sanctionRepository.countRecentActiveWarnings(record.targetUuid(), record.targetName(), since)
            .thenCompose(count -> maybeTrigger(
                count >= threshold,
                AlertType.WARNS_THRESHOLD,
                record.targetUuid(),
                since,
                "Warns actifs: " + count + " en " + windowMinutes + "m pour " + record.targetName()
            ));
    }

    public void handleConnectionRecorded(final UUID targetUuid, final String targetName, final String ip) {
        if (targetUuid == null || ip == null || ip.isBlank()) {
            return;
        }
        final int altsThreshold = this.plugin.getConfigLoader().config().getInt("alerts.alts_threshold", 3);
        final int connectionsThreshold = this.plugin.getConfigLoader().config().getInt("alerts.connections_threshold", 5);
        final int connectionsWindowMinutes = Math.max(1, this.plugin.getConfigLoader().config().getInt("alerts.connections_window_minutes", 15));
        final Instant since = Instant.now().minus(Duration.ofMinutes(connectionsWindowMinutes));
        if (altsThreshold > 0) {
            this.playerRepository.countDistinctAccountsOnIp(ip).thenCompose(count -> maybeTrigger(
                count >= altsThreshold,
                AlertType.ALTS_THRESHOLD,
                targetUuid,
                since,
                "Comptes detectes sur " + ip + ": " + count + " pour " + targetName
            ));
        }
        if (connectionsThreshold > 0) {
            this.playerRepository.countRecentConnections(targetUuid, since).thenCompose(count -> maybeTrigger(
                count >= connectionsThreshold,
                AlertType.CONNECTIONS_THRESHOLD,
                targetUuid,
                since,
                "Connexions recentes: " + count + " en " + connectionsWindowMinutes + "m pour " + targetName
            ));
        }
    }

    private CompletableFuture<Void> maybeTrigger(
        final boolean conditionMet,
        final AlertType type,
        final UUID targetUuid,
        final Instant windowSince,
        final String details
    ) {
        if (!conditionMet) {
            return CompletableFuture.completedFuture(null);
        }
        return this.alertRepository.existsRecent(type, targetUuid, windowSince).thenCompose(exists -> {
            if (exists) {
                return CompletableFuture.completedFuture(null);
            }
            return this.alertRepository.insert(type, targetUuid, details).thenRun(() ->
                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> notifyStaff(type, details))
            );
        }).exceptionally(throwable -> null);
    }

    private void notifyStaff(final AlertType type, final String details) {
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(PermissionService.ALERTS)) {
                continue;
            }
            online.sendMessage(this.plugin.getMessageFormatter().message(
                "alerts.triggered",
                this.plugin.getMessageFormatter().text("type", readable(type)),
                this.plugin.getMessageFormatter().text("details", details)
            ));
            online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.0f);
        }
    }

    private String readable(final AlertType type) {
        return switch (type) {
            case WARNS_THRESHOLD -> "warns";
            case ALTS_THRESHOLD -> "alts";
            case CONNECTIONS_THRESHOLD -> "connexions";
        };
    }
}
