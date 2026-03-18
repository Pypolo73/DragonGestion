package fr.dragon.admincore.core.compat;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.sanctions.CreateSanctionRequest;
import fr.dragon.admincore.sanctions.SanctionScope;
import fr.dragon.admincore.sanctions.SanctionService;
import fr.dragon.admincore.sanctions.SanctionType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.shortninja.staffplusplus.ban.BanFilters;
import net.shortninja.staffplusplus.ban.BanService;
import net.shortninja.staffplusplus.ban.IBan;
import net.shortninja.staffplusplus.session.SppPlayer;
import org.bukkit.command.CommandSender;

public final class StaffPlusPlusBanServiceAdapter implements BanService {

    private final AdminCorePlugin plugin;
    private final SanctionService sanctions;

    public StaffPlusPlusBanServiceAdapter(final AdminCorePlugin plugin, final SanctionService sanctions) {
        this.plugin = plugin;
        this.sanctions = sanctions;
    }

    @Override
    public void unban(final CommandSender sender, final SppPlayer target, final String reason, final boolean silent) {
        this.sanctions.revoke(SanctionType.BAN, target.getId(), target.getUsername());
    }

    @Override
    public void unban(final SppPlayer target, final int id, final String reason) {
        this.sanctions.revoke(SanctionType.BAN, target.getId(), target.getUsername());
    }

    @Override
    public long getTotalBanCount() {
        return this.sanctions.allSanctions().join().stream().filter(record -> record.type() == SanctionType.BAN).count();
    }

    @Override
    public long getActiveBanCount() {
        return this.sanctions.allSanctions().join().stream().filter(record -> record.type() == SanctionType.BAN && record.active()).count();
    }

    @Override
    public void permBan(final CommandSender sender, final SppPlayer target, final String reason, final String template, final boolean silent) {
        permBan(sender, target, reason, silent);
    }

    @Override
    public void permBan(final CommandSender sender, final SppPlayer target, final String reason, final boolean silent) {
        this.sanctions.create(new CreateSanctionRequest(
            target.getId(),
            target.getUsername(),
            sender instanceof org.bukkit.entity.Player player ? player.getUniqueId() : null,
            sender.getName(),
            SanctionType.BAN,
            reason,
            null,
            SanctionScope.PLAYER,
            target.getId().toString()
        ));
    }

    @Override
    public void tempBan(final CommandSender sender, final SppPlayer target, final Long duration, final String reason, final String template, final boolean silent) {
        tempBan(sender, target, duration, reason, silent);
    }

    @Override
    public void tempBan(final CommandSender sender, final SppPlayer target, final Long duration, final String reason, final boolean silent) {
        this.sanctions.create(new CreateSanctionRequest(
            target.getId(),
            target.getUsername(),
            sender instanceof org.bukkit.entity.Player player ? player.getUniqueId() : null,
            sender.getName(),
            SanctionType.BAN,
            reason,
            Instant.now().plusMillis(duration),
            SanctionScope.PLAYER,
            target.getId().toString()
        ));
    }

    @Override
    public void extendBan(final CommandSender sender, final SppPlayer target, final long duration) {
    }

    @Override
    public void reduceBan(final CommandSender sender, final SppPlayer target, final long duration) {
    }

    @Override
    public Optional<? extends IBan> getBanByBannedUuid(final UUID uuid) {
        return this.sanctions.getCachedBan(uuid).map(record -> new BanView(record, this.plugin));
    }

    @Override
    public IBan getActiveById(final int id) {
        return this.sanctions.allSanctions().join().stream()
            .filter(record -> record.id() == id && record.type() == SanctionType.BAN && record.active())
            .findFirst()
            .map(record -> (IBan) new BanView(record, this.plugin))
            .orElse(null);
    }

    @Override
    public IBan getById(final int id) {
        return this.sanctions.allSanctions().join().stream()
            .filter(record -> record.id() == id && record.type() == SanctionType.BAN)
            .findFirst()
            .map(record -> (IBan) new BanView(record, this.plugin))
            .orElse(null);
    }

    @Override
    public List<? extends IBan> getAllPaged(final int page, final int pageSize) {
        return this.sanctions.allSanctions().join().stream()
            .filter(record -> record.type() == SanctionType.BAN)
            .skip((long) Math.max(0, page - 1) * pageSize)
            .limit(pageSize)
            .map(record -> (IBan) new BanView(record, this.plugin))
            .toList();
    }

    @Override
    public long getBanCount(final BanFilters filters) {
        return getTotalBanCount();
    }
}
