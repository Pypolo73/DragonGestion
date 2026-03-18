package fr.dragon.admincore.core.compat;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.sanctions.SanctionRecord;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import net.shortninja.staffplusplus.appeals.AppealableType;
import net.shortninja.staffplusplus.appeals.IAppeal;
import net.shortninja.staffplusplus.warnings.IWarning;

public final class WarningView implements IWarning {

    private final SanctionRecord sanction;
    private final String serverName;
    private IAppeal appeal;

    public WarningView(final SanctionRecord sanction, final AdminCorePlugin plugin) {
        this.sanction = sanction;
        this.serverName = plugin.getConfigLoader().config().getString("general.server-name", plugin.getServer().getName());
    }

    @Override
    public int getId() {
        return (int) this.sanction.id();
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
    public String getReason() {
        return this.sanction.reason();
    }

    @Override
    public int getScore() {
        return 1;
    }

    @Override
    public String getSeverity() {
        return "LOW";
    }

    @Override
    public ZonedDateTime getCreationDate() {
        return ZonedDateTime.ofInstant(this.sanction.createdAt(), ZoneId.systemDefault());
    }

    @Override
    public String getServerName() {
        return this.serverName;
    }

    @Override
    public String getActionableType() {
        return "warning";
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
        return AppealableType.WARNING;
    }
}
