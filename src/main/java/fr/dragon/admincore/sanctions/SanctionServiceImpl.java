package fr.dragon.admincore.sanctions;

import fr.dragon.admincore.database.DatabaseManager;
import fr.dragon.admincore.database.NoteRepository;
import fr.dragon.admincore.database.PlayerProfile;
import fr.dragon.admincore.database.PlayerRepository;
import fr.dragon.admincore.database.SanctionRepository;
import fr.dragon.admincore.util.ConfigLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SanctionServiceImpl implements SanctionService {

    private final DatabaseManager databaseManager;
    private final SanctionRepository sanctionRepository;
    private final PlayerRepository playerRepository;
    private final NoteRepository noteRepository;
    private final ConfigLoader configLoader;
    private final Clock clock;
    private final Map<UUID, SanctionRecord> muteCache = new ConcurrentHashMap<>();
    private final Map<UUID, SanctionRecord> banCache = new ConcurrentHashMap<>();

    public SanctionServiceImpl(
        final DatabaseManager databaseManager,
        final SanctionRepository sanctionRepository,
        final PlayerRepository playerRepository,
        final NoteRepository noteRepository,
        final ConfigLoader configLoader
    ) {
        this.databaseManager = databaseManager;
        this.sanctionRepository = sanctionRepository;
        this.playerRepository = playerRepository;
        this.noteRepository = noteRepository;
        this.configLoader = configLoader;
        this.clock = Clock.systemUTC();
    }

    @Override
    public CompletableFuture<SanctionRecord> create(final CreateSanctionRequest request) {
        final CompletableFuture<Integer> cleanup = switch (request.type()) {
            case BAN, MUTE -> revoke(request.type(), request.targetUuid(), request.targetName());
            default -> CompletableFuture.completedFuture(0);
        };
        return cleanup
            .thenCompose(ignored -> createLinkedIpSanction(request))
            .thenCompose(ignored -> this.sanctionRepository.insert(request))
            .thenApply(record -> {
                cache(record);
                return record;
            });
    }

    @Override
    public CompletableFuture<Integer> revoke(final SanctionType type, final UUID targetUuid, final String targetName) {
        if (type == SanctionType.BAN && targetUuid != null) {
            this.banCache.remove(targetUuid);
        }
        if (type == SanctionType.MUTE && targetUuid != null) {
            this.muteCache.remove(targetUuid);
        }
        return this.sanctionRepository.deactivate(type, targetUuid, targetName).thenCompose(updated -> {
            if (type != SanctionType.BAN || targetUuid == null) {
                return CompletableFuture.completedFuture(updated);
            }
            return this.sanctionRepository.deactivateLinkedIp(type, targetUuid)
                .thenApply(linkedUpdated -> updated + linkedUpdated);
        });
    }

    @Override
    public CompletableFuture<Optional<SanctionRecord>> findActiveBan(final UUID targetUuid, final String targetName, final String ip) {
        return this.sanctionRepository.findActive(targetUuid, targetName, ip, EnumSet.of(SanctionType.BAN)).thenApply(this::validateActive);
    }

    @Override
    public CompletableFuture<Optional<SanctionRecord>> findActiveMute(final UUID targetUuid, final String targetName, final String ip) {
        return this.sanctionRepository.findActive(targetUuid, targetName, ip, EnumSet.of(SanctionType.MUTE)).thenApply(this::validateActive);
    }

    @Override
    public Optional<SanctionRecord> getCachedMute(final UUID uuid) {
        return validateCache(this.muteCache, uuid);
    }

    @Override
    public Optional<SanctionRecord> getCachedBan(final UUID uuid) {
        return validateCache(this.banCache, uuid);
    }

    @Override
    public CompletableFuture<List<SanctionRecord>> history(final UUID targetUuid, final String targetName, final int limit) {
        return this.sanctionRepository.history(targetUuid, targetName, limit);
    }

    @Override
    public CompletableFuture<List<SanctionRecord>> activeWarnings(final UUID targetUuid, final String targetName) {
        return this.sanctionRepository.activeWarnings(targetUuid, targetName);
    }

    @Override
    public CompletableFuture<Integer> clearWarnings(final UUID targetUuid, final String targetName) {
        return this.sanctionRepository.clearWarnings(targetUuid, targetName);
    }

    @Override
    public CompletableFuture<Void> recordPlayer(final UUID uuid, final String name, final String ip, final String clientBrand, final int level) {
        return this.playerRepository.upsert(uuid, name, ip, clientBrand, level);
    }

    @Override
    public CompletableFuture<Void> addNote(
        final UUID targetUuid,
        final String targetName,
        final UUID actorUuid,
        final String actorName,
        final String note
    ) {
        return this.noteRepository.insert(targetUuid, targetName, actorUuid, actorName, note);
    }

    @Override
    public CompletableFuture<List<NoteRepository.NoteEntry>> notes(final UUID targetUuid, final String targetName) {
        return this.noteRepository.findAll(targetUuid, targetName);
    }

    @Override
    public CompletableFuture<List<String>> findAlts(final UUID targetUuid, final String targetName) {
        return this.playerRepository.findAlts(targetUuid, targetName);
    }

    @Override
    public CompletableFuture<Optional<String>> findLatestIp(final String targetName) {
        return this.playerRepository.findLatestIp(targetName);
    }

    @Override
    public CompletableFuture<List<String>> findAccountsByIp(final String ip) {
        return this.playerRepository.findAccountsByIp(ip);
    }

    @Override
    public CompletableFuture<List<SanctionRecord>> allSanctions() {
        return this.sanctionRepository.allSanctions();
    }

    @Override
    public CompletableFuture<List<SanctionRecord>> recentActiveSanctions(final int limit) {
        return this.sanctionRepository.recentActive(limit).thenApply(records ->
            records.stream().filter(record -> record.isActive(this.clock)).limit(limit).toList()
        );
    }

    @Override
    public CompletableFuture<List<String>> searchPlayerNames(final String query, final int limit) {
        return this.playerRepository.searchNames(query, limit);
    }

    @Override
    public CompletableFuture<List<PlayerProfile>> listProfiles(final int offset, final int limit) {
        return this.playerRepository.listProfiles(offset, limit);
    }

    @Override
    public CompletableFuture<PlayerProfile> playerProfile(final UUID targetUuid, final String targetName) {
        return this.playerRepository.profile(targetUuid, targetName);
    }

    @Override
    public CompletableFuture<Path> exportCsv(final Path directory) {
        return this.sanctionRepository.allSanctions().thenCompose(sanctions -> this.noteRepository.findAll(null, "__never__").handle((notes, ignored) -> List.<NoteRepository.NoteEntry>of())
            .thenCompose(ignoredNotes -> CompletableFuture.supplyAsync(() -> {
                try {
                    Files.createDirectories(directory);
                    final Path sanctionsFile = directory.resolve("sanctions.csv");
                    final StringBuilder sanctionsCsv = new StringBuilder("id,target,actor,type,reason,createdAt,expiresAt,active,scope,scopeValue\n");
                    for (final SanctionRecord sanction : sanctions) {
                        sanctionsCsv.append(sanction.id()).append(',')
                            .append(escape(sanction.targetName())).append(',')
                            .append(escape(sanction.actorName())).append(',')
                            .append(sanction.type().name()).append(',')
                            .append(escape(sanction.reason())).append(',')
                            .append(sanction.createdAt()).append(',')
                            .append(sanction.expiresAt()).append(',')
                            .append(sanction.active()).append(',')
                            .append(sanction.scope().name()).append(',')
                            .append(escape(sanction.scopeValue()))
                            .append('\n');
                    }
                    Files.writeString(sanctionsFile, sanctionsCsv);
                    return sanctionsFile;
                } catch (final IOException exception) {
                    throw new IllegalStateException("Export CSV impossible", exception);
                }
            }, this.databaseManager.executor())));
    }

    private Optional<SanctionRecord> validateActive(final Optional<SanctionRecord> record) {
        if (record.isEmpty()) {
            return Optional.empty();
        }
        if (!record.get().isActive(this.clock)) {
            return Optional.empty();
        }
        cache(record.get());
        return record;
    }

    private void cache(final SanctionRecord record) {
        if (record.targetUuid() == null) {
            return;
        }
        if (record.type() == SanctionType.BAN && record.isActive(this.clock)) {
            this.banCache.put(record.targetUuid(), record);
        }
        if (record.type() == SanctionType.MUTE && record.isActive(this.clock)) {
            this.muteCache.put(record.targetUuid(), record);
        }
    }

    private Optional<SanctionRecord> validateCache(final Map<UUID, SanctionRecord> cache, final UUID uuid) {
        final SanctionRecord record = cache.get(uuid);
        if (record == null) {
            return Optional.empty();
        }
        if (!record.isActive(this.clock)) {
            cache.remove(uuid);
            return Optional.empty();
        }
        return Optional.of(record);
    }

    private String escape(final String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private CompletableFuture<Void> createLinkedIpSanction(final CreateSanctionRequest request) {
        if (request.type() != SanctionType.BAN || request.scope() != SanctionScope.PLAYER) {
            return CompletableFuture.completedFuture(null);
        }
        final boolean enabled = shouldLinkIpBan(request.expiresAt());
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }
        return this.playerRepository.findLatestIp(request.targetName()).thenCompose(optionalIp -> {
            if (optionalIp.isEmpty() || optionalIp.get().isBlank() || request.targetUuid() == null) {
                return CompletableFuture.completedFuture(null);
            }
            final String ip = optionalIp.get();
            final CreateSanctionRequest linkedRequest = new CreateSanctionRequest(
                null,
                ip,
                request.actorUuid(),
                request.actorName(),
                request.type(),
                request.reason(),
                request.expiresAt(),
                SanctionScope.IP,
                ip
            );
            return this.sanctionRepository.insert(linkedRequest, request.targetUuid()).thenApply(ignored -> null);
        });
    }

    private boolean shouldLinkIpBan(final Instant expiresAt) {
        if (expiresAt == null) {
            return this.configLoader.config().getBoolean("sanctions.link-ip-on-ban", true);
        }
        return this.configLoader.config().getBoolean("sanctions.link-ip-on-tempban", true);
    }
}
