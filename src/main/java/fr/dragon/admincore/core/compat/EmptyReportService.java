package fr.dragon.admincore.core.compat;

import java.util.List;
import net.shortninja.staffplusplus.reports.IReport;
import net.shortninja.staffplusplus.reports.ReportFilters;
import net.shortninja.staffplusplus.reports.ReportService;

public final class EmptyReportService implements ReportService {

    @Override
    public long getReportCount(final ReportFilters filters) {
        return 0;
    }

    @Override
    public List<? extends IReport> findReports(final ReportFilters filters, final int page, final int pageSize) {
        return List.of();
    }
}
