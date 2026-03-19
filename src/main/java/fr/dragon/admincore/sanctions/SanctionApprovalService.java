package fr.dragon.admincore.sanctions;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.StaffRole;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SanctionApprovalService {

    private final AdminCorePlugin plugin;
    private final AtomicInteger ids = new AtomicInteger(0);
    private final Map<Integer, PendingSanctionRequest> pending = new ConcurrentHashMap<>();

    public SanctionApprovalService(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    public PendingSanctionRequest submit(
        final UUID requesterUuid,
        final String requesterName,
        final CreateSanctionRequest request,
        final java.time.Duration duration,
        final StaffRole requiredApproverRole
    ) {
        final PendingSanctionRequest pendingRequest = new PendingSanctionRequest(
            this.ids.incrementAndGet(),
            requesterUuid,
            requesterName,
            request,
            duration,
            requiredApproverRole
        );
        this.pending.put(pendingRequest.id(), pendingRequest);
        notifyApprovers(pendingRequest);
        return pendingRequest;
    }

    public Optional<PendingSanctionRequest> approve(final int id, final StaffRole approverRole) {
        final PendingSanctionRequest request = this.pending.get(id);
        if (request == null || !approverRole.atLeast(request.requiredApproverRole())) {
            return Optional.empty();
        }
        this.pending.remove(id);
        this.plugin.getSanctionService().create(request.request()).whenComplete((record, throwable) ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (throwable != null) {
                    this.pending.putIfAbsent(request.id(), request);
                    return;
                }
                final Player target = record.targetUuid() == null ? null : Bukkit.getPlayer(record.targetUuid());
                if (target != null) {
                    this.plugin.getStaffAccessService().refresh(target);
                }
                final Player requester = request.requesterUuid() == null ? null : Bukkit.getPlayer(request.requesterUuid());
                if (requester != null) {
                    requester.sendMessage(this.plugin.getMessageFormatter().message(
                        "admin.approval-approved",
                        this.plugin.getMessageFormatter().text("id", Integer.toString(request.id())),
                        this.plugin.getMessageFormatter().text("target", request.request().targetName())
                    ));
                }
            })
        );
        return Optional.of(request);
    }

    public Optional<PendingSanctionRequest> deny(final int id, final StaffRole approverRole) {
        final PendingSanctionRequest request = this.pending.remove(id);
        if (request == null || !approverRole.atLeast(request.requiredApproverRole())) {
            return Optional.empty();
        }
        final Player requester = request.requesterUuid() == null ? null : Bukkit.getPlayer(request.requesterUuid());
        if (requester != null) {
            requester.sendMessage(this.plugin.getMessageFormatter().message(
                "admin.approval-denied",
                this.plugin.getMessageFormatter().text("id", Integer.toString(request.id())),
                this.plugin.getMessageFormatter().text("target", request.request().targetName())
            ));
        }
        return Optional.of(request);
    }

    public List<PendingSanctionRequest> pending(final StaffRole approverRole) {
        return this.pending.values().stream()
            .filter(request -> approverRole.atLeast(request.requiredApproverRole()))
            .sorted(Comparator.comparingInt(PendingSanctionRequest::id))
            .toList();
    }

    private void notifyApprovers(final PendingSanctionRequest request) {
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (!this.plugin.getStaffAccessService().canApprove(online, request.requiredApproverRole())) {
                continue;
            }
            online.sendMessage(this.plugin.getMessageFormatter().message(
                "admin.approval-notify",
                this.plugin.getMessageFormatter().text("id", Integer.toString(request.id())),
                this.plugin.getMessageFormatter().text("requester", request.requesterName()),
                this.plugin.getMessageFormatter().text("type", request.request().type().name()),
                this.plugin.getMessageFormatter().text("target", request.request().targetName()),
                this.plugin.getMessageFormatter().text("duration", request.duration() == null ? "Permanent" : fr.dragon.admincore.util.TimeParser.format(request.duration())),
                this.plugin.getMessageFormatter().text("reason", request.request().reason()),
                this.plugin.getMessageFormatter().text("approverRole", request.requiredApproverRole().displayName())
            ));
        }
    }
}
