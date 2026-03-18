package fr.dragon.admincore.core.compat;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.sanctions.SanctionRecord;
import fr.dragon.admincore.util.TimeParser;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import net.shortninja.staffplusplus.appeals.AppealableType;
import net.shortninja.staffplusplus.appeals.IAppeal;
import net.shortninja.staffplusplus.ban.IBan;

public final class BanView implements IBan {

    private final SanctionRecord sanction;
    private final String serverName;
    private IAppeal appeal;

    public BanView(final SanctionRecord sanction, final AdminCorePlugin plugin) {
        this.sanction = sanction;
        this.serverName = plugin.getConfigLoader().config().getString("general.server-name", plugin.getServer().getName());
    }

    @Override
    public int getId() {
        return (int) this.sanction.id();
    }

    @Override
    public Long getCreationTimestamp() {
        return this.sanction.createdAt().toEpochMilli();
    }

    @Override
    public ZonedDateTime getCreationDate() {
        return ZonedDateTime.ofInstant(this.sanction.createdAt(), ZoneId.systemDefault());
    }

    @Override
    public String getReason() {
        return this.sanction.reason();
    }

    @Override
    public String getTargetName() {
        return this.sanction.targetName();
    }

    @Override
    public UUID getTargetUuid() {
        return this.sanction.targetUuid();
    }

    @Override
    public String getIssuerName() {
        return this.sanction.actorName();
    }

    @Override
    public UUID getIssuerUuid() {
        return this.sanction.actorUuid();
    }

    @Override
    public String getUnbannedByName() {
        return null;
    }

    @Override
    public UUID getUnbannedByUuid() {
        return null;
    }

    @Override
    public Long getEndTimestamp() {
        return this.sanction.expiresAt() == null ? null : this.sanction.expiresAt().toEpochMilli();
    }

    @Override
    public ZonedDateTime getEndDate() {
        return this.sanction.expiresAt() == null ? null : ZonedDateTime.ofInstant(this.sanction.expiresAt(), ZoneId.systemDefault());
    }

    @Override
    public String getUnbanReason() {
        return null;
    }

    @Override
    public String getHumanReadableDuration() {
        return this.sanction.expiresAt() == null ? "Permanent" : TimeParser.formatRemaining(this.sanction.expiresAt());
    }

    @Override
    public String getServerName() {
        return this.serverName;
    }

    @Override
    public boolean isSilentBan() {
        return false;
    }

    @Override
    public boolean isSilentUnban() {
        return false;
    }

    @Override
    public Optional<String> getTemplate() {
        return Optional.empty();
    }

    @Override
    public Optional<? extends IAppeal> getAppeal() {
        return Optional.ofNullable(this.appeal);
    }

    @Override
    public void setAppeal(final IAppeal appeal) {
        this.appeal = appeal;
    }

    @Override
    public AppealableType getType() {
        return AppealableType.BAN;
    }
}
