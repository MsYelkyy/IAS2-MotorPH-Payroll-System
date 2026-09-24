package motorph.backend.service.report;

import motorph.backend.exception.EmptyReportException;

import java.util.Collection;

/**
 * Shared checks for report generation. Jasper renders a header-only PDF from an empty dataset, so
 * every report checks here first and the client can report that there is nothing to show.
 */
final class ReportGuards {

    private ReportGuards() {
    }

    static void requireRows(Collection<?> rows, String message) {
        if (rows == null || rows.isEmpty()) {
            throw new EmptyReportException(message);
        }
    }
}
