package motorph.backend.service.report;

import motorph.backend.dto.report.audit.AuditReportDTO;
import motorph.backend.exception.ValidationException;
import motorph.backend.reporting.JasperReportGenerator;
import motorph.backend.repository.report.AuditReportRepository;

import net.sf.jasperreports.engine.JasperPrint;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static motorph.backend.service.report.ReportGuards.requireRows;

@Service
public class AuditReportService {

    private static final String AUDIT_REPORT = "/motorph/reports/audit/audit_report.jrxml";

    private final AuditReportRepository auditReportRepository;
    private final JasperReportGenerator jasperReportGenerator;

    public AuditReportService(AuditReportRepository auditReportRepository, JasperReportGenerator jasperReportGenerator) {
        this.auditReportRepository = auditReportRepository;
        this.jasperReportGenerator = jasperReportGenerator;
    }

    public List<AuditReportDTO> getAuditReport(Integer userAccountId, String action, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return auditReportRepository.findAuditReport(userAccountId, action, startDate, endDate);
    }

    public byte[] generateAuditReportPdf(Integer userAccountId, String action, LocalDate startDate, LocalDate endDate) {
        List<AuditReportDTO> data = getAuditReport(userAccountId, action, startDate, endDate);
        requireRows(data, "No audit log entries match the selected filters.");

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                AUDIT_REPORT,
                buildReportParameters(startDate, endDate),
                data
        );

        return jasperReportGenerator.exportToPdfBytes(jasperPrint);
    }

    private Map<String, Object> buildReportParameters(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Audit Trail Report");
        parameters.put("START_DATE", startDate);
        parameters.put("END_DATE", endDate);
        return parameters;
    }

    // dates are optional here, but must be paired if either is present
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw new ValidationException("Both start and end date are required together.");
        }
        if (startDate != null && endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
    }
}
