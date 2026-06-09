package motorph.application.service;

import motorph.application.dto.report.OrganizationPayrollReportDTO;
import motorph.application.exception.ValidationException;
import motorph.infrastructure.repository.OrganizationPayrollReportRepository;
import motorph.infrastructure.reporting.JasperReportGenerator;

import net.sf.jasperreports.engine.JasperPrint;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles admin report generation workflows.
 */
public class AdminReportService {

    private static final String ORGANIZATION_PAYROLL_REPORT =
            "/motorph/reports/organization_payroll_report.jrxml";

    private final OrganizationPayrollReportRepository organizationPayrollReportRepository;
    private final JasperReportGenerator jasperReportGenerator;

    // constructors
    public AdminReportService(OrganizationPayrollReportRepository organizationPayrollReportRepository,
                              JasperReportGenerator jasperReportGenerator) {

        this.organizationPayrollReportRepository = require(
                organizationPayrollReportRepository,
                "OrganizationPayrollReportRepository is required."
        );

        this.jasperReportGenerator = require(
                jasperReportGenerator,
                "JasperReportGenerator is required."
        );
    }

    // returns organization payroll report data
    public List<OrganizationPayrollReportDTO> getOrganizationPayrollReport(
            Integer departmentId,
            Integer employeeId,
            Integer payrunId,
            LocalDate startDate,
            LocalDate endDate) {

        validateDateRange(startDate, endDate);

        return organizationPayrollReportRepository.findOrganizationPayrollReport(
                departmentId,
                employeeId,
                payrunId,
                startDate,
                endDate
        );
    }

    // previews the organization payroll report
    public void previewOrganizationPayrollReport(Integer departmentId,
                                                 Integer employeeId,
                                                 Integer payrunId,
                                                 LocalDate startDate,
                                                 LocalDate endDate) {

        List<OrganizationPayrollReportDTO> reportData = getOrganizationPayrollReport(
                departmentId,
                employeeId,
                payrunId,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                ORGANIZATION_PAYROLL_REPORT,
                buildReportParameters(startDate, endDate),
                reportData
        );

        jasperReportGenerator.showPreview(jasperPrint);
    }

    // exports the organization payroll report to PDF
    public void exportOrganizationPayrollReportToPdf(Integer departmentId,
                                                     Integer employeeId,
                                                     Integer payrunId,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     String outputPath) {

        if (outputPath == null || outputPath.isBlank()) {
            throw new ValidationException("Output path is required.");
        }

        List<OrganizationPayrollReportDTO> reportData = getOrganizationPayrollReport(
                departmentId,
                employeeId,
                payrunId,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                ORGANIZATION_PAYROLL_REPORT,
                buildReportParameters(startDate, endDate),
                reportData
        );

        jasperReportGenerator.exportToPdf(jasperPrint, outputPath);
    }

    // builds report parameters for JasperReports
    private Map<String, Object> buildReportParameters(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("REPORT_TITLE", "Organization Payroll Report");
        parameters.put("START_DATE", startDate);
        parameters.put("END_DATE", endDate);

        return parameters;
    }

    // validates date range input
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            throw new ValidationException("Both start date and end date are required when filtering by date.");
        }

        if (startDate != null && endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
    }

    // validates required dependency
    private <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}