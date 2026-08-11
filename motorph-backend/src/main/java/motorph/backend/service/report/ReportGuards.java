package motorph.backend.service.report;

import motorph.backend.exception.EmptyReportException;

import java.util.Collection;

/**
 * Shared precondition for report generation. Jasper happily renders a
 * header-only PDF from an empty dataset, which reaches the user as a blank
 * document; every report checks here first so the client can say "nothing to
 * show" instead.
 */
final class ReportGuards {

    // utility class — no instances
    private ReportGuards() {
    }

    static void requireRows(Collection<?> rows, String message) {
        if (rows == null || rows.isEmpty()) {
            throw new EmptyReportException(message);
        }
    }
}
