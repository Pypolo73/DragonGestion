package fr.dragon.admincore.core.compat;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.sanctions.SanctionService;
import fr.dragon.admincore.sanctions.SanctionType;
import java.util.List;
import net.shortninja.staffplusplus.warnings.IWarning;
import net.shortninja.staffplusplus.warnings.WarningFilters;
import net.shortninja.staffplusplus.warnings.WarningService;

public final class StaffPlusPlusWarningServiceAdapter implements WarningService {

    private final AdminCorePlugin plugin;
    private final SanctionService sanctions;

    public StaffPlusPlusWarningServiceAdapter(final AdminCorePlugin plugin, final SanctionService sanctions) {
        this.plugin = plugin;
        this.sanctions = sanctions;
    }

    @Override
    public long getWarnCount(final WarningFilters filters) {
        return this.sanctions.allSanctions().join().stream().filter(record -> record.type() == SanctionType.WARN).count();
    }

    @Override
    public int getTotalScore(final String playerName) {
        return this.sanctions.allSanctions().join().stream()
            .filter(record -> record.type() == SanctionType.WARN && record.targetName().equalsIgnoreCase(playerName))
            .mapToInt(ignored -> 1)
            .sum();
    }

    @Override
    public List<? extends IWarning> findWarnings(final WarningFilters filters, final int page, final int pageSize) {
        return this.sanctions.allSanctions().join().stream()
            .filter(record -> record.type() == SanctionType.WARN)
            .skip((long) Math.max(0, page - 1) * pageSize)
            .limit(pageSize)
            .map(record -> (IWarning) new WarningView(record, this.plugin))
            .toList();
    }
}
