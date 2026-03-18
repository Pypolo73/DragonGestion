package fr.dragon.admincore.core.compat;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.sanctions.SanctionService;
import fr.dragon.admincore.sanctions.SanctionType;
import java.util.List;
import net.shortninja.staffplusplus.mute.IMute;
import net.shortninja.staffplusplus.mute.MuteFilters;
import net.shortninja.staffplusplus.mute.MuteService;

public final class StaffPlusPlusMuteServiceAdapter implements MuteService {

    private final AdminCorePlugin plugin;
    private final SanctionService sanctions;

    public StaffPlusPlusMuteServiceAdapter(final AdminCorePlugin plugin, final SanctionService sanctions) {
        this.plugin = plugin;
        this.sanctions = sanctions;
    }

    @Override
    public long getTotalMuteCount() {
        return this.sanctions.allSanctions().join().stream().filter(record -> record.type() == SanctionType.MUTE).count();
    }

    @Override
    public long getActiveMuteCount() {
        return this.sanctions.allSanctions().join().stream().filter(record -> record.type() == SanctionType.MUTE && record.active()).count();
    }

    @Override
    public List<? extends IMute> getAllPaged(final int page, final int pageSize) {
        return this.sanctions.allSanctions().join().stream()
            .filter(record -> record.type() == SanctionType.MUTE)
            .skip((long) Math.max(0, page - 1) * pageSize)
            .limit(pageSize)
            .map(record -> (IMute) new MuteView(record, this.plugin))
            .toList();
    }

    @Override
    public long getMuteCount(final MuteFilters filters) {
        return getTotalMuteCount();
    }
}
