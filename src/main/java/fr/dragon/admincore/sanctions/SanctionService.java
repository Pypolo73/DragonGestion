package fr.dragon.admincore.sanctions;

import fr.dragon.admincore.database.NoteRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SanctionService {

    CompletableFuture<SanctionRecord> create(CreateSanctionRequest request);

    CompletableFuture<Integer> revoke(SanctionType type, UUID targetUuid, String targetName);

    CompletableFuture<Optional<SanctionRecord>> findActiveBan(UUID targetUuid, String targetName, String ip);

    CompletableFuture<Optional<SanctionRecord>> findActiveMute(UUID targetUuid, String targetName, String ip);

    Optional<SanctionRecord> getCachedMute(UUID uuid);

    Optional<SanctionRecord> getCachedBan(UUID uuid);

    CompletableFuture<List<SanctionRecord>> history(UUID targetUuid, String targetName, int limit);

    CompletableFuture<List<SanctionRecord>> activeWarnings(UUID targetUuid, String targetName);

    CompletableFuture<Integer> clearWarnings(UUID targetUuid, String targetName);

    CompletableFuture<Void> recordPlayer(UUID uuid, String name, String ip);

    CompletableFuture<Void> addNote(UUID targetUuid, String targetName, UUID actorUuid, String actorName, String note);

    CompletableFuture<List<NoteRepository.NoteEntry>> notes(UUID targetUuid, String targetName);

    CompletableFuture<List<String>> findAlts(UUID targetUuid, String targetName);

    CompletableFuture<Optional<String>> findLatestIp(String targetName);

    CompletableFuture<List<String>> findAccountsByIp(String ip);

    CompletableFuture<List<SanctionRecord>> allSanctions();

    CompletableFuture<Path> exportCsv(Path directory);
}
