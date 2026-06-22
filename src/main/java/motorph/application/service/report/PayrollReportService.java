package motorph.application.service.report;

import motorph.application.dto.report.payroll.OrganizationPayrollReportDTO;
import motorph.application.dto.report.payroll.PayrollSummaryReportDTO;
import motorph.application.dto.report.payroll.PayslipReportDTO;
import motorph.application.exception.ValidationException;
import motorph.infrastructure.repository.report.PayrollReportRepository;
import motorph.infrastructure.reporting.JasperReportGenerator;

import net.sf.jasperreports.engine.JasperPrint;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles payroll report generation and export.
 */
public class PayrollReportService {

    private static final String ORGANIZATION_PAYROLL_REPORT =
            "/motorph/reports/payroll/organization_payroll_report.jrxml";

    private static final String PAYROLL_SUMMARY_REPORT =
            "/motorph/reports/payroll/payroll_summary_report.jrxml";

    private static final String PAYSLIP_REPORT =
            "/motorph/reports/payroll/payslip_report.jrxml";

    private final PayrollReportRepository payrollReportRepository;
    private final JasperReportGenerator jasperReportGenerator;

    // constructors
    public PayrollReportService(PayrollReportRepository payrollReportRepository,
                                JasperReportGenerator jasperReportGenerator) {

        this.payrollReportRepository = require(
                payrollReportRepository,
                "PayrollReportRepository is required."
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

        return payrollReportRepository.findOrganizationPayrollReport(
                departmentId,
                employeeId,
                payrunId,
                startDate,
                endDate
        );
    }

    // returns payroll summary report data
    public List<PayrollSummaryReportDTO> getPayrollSummaryReport(
            Integer departmentId,
            Integer payrunId,
            LocalDate startDate,
            LocalDate endDate) {

        validateDateRange(startDate, endDate);

        return payrollReportRepository.findPayrollSummaryReport(
                departmentId,
                payrunId,
                startDate,
                endDate
        );
    }

    // returns payslip report data
    public PayslipReportDTO getPayslipReport(int employeeId, int payrunId) {
        validateEmployeeId(employeeId);
        validatePayrunId(payrunId);

        PayslipReportDTO payslip = payrollReportRepository.findPayslipReport(employeeId, payrunId);

        if (payslip == null) {
            throw new ValidationException("Payslip report data not found.");
        }

        return payslip;
    }

    // previews payslip report
    public void previewPayslipReport(int employeeId, int payrunId) {
        PayslipReportDTO payslip = getPayslipReport(employeeId, payrunId);

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                PAYSLIP_REPORT,
                buildPayslipParameters(payslip),
                List.of(payslip)
        );

        jasperReportGenerator.showPreview(jasperPrint);
    }

    // previews payroll summary report
    public void previewPayrollSummaryReport(Integer departmentId,
                                            Integer payrunId,
                                            LocalDate startDate,
                                            LocalDate endDate) {

        List<PayrollSummaryReportDTO> reportData = getPayrollSummaryReport(
                departmentId,
                payrunId,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                PAYROLL_SUMMARY_REPORT,
                buildPayrollSummaryParameters(startDate, endDate),
                reportData
        );

        jasperReportGenerator.showPreview(jasperPrint);
    }

    // previews the organization payroll report
    public void previewOrganizationPayrollReport(Integer departmentId,
                                                 String departmentName,
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
                buildOrganizationPayrollParameters(departmentName, startDate, endDate),
                reportData
        );

        jasperReportGenerator.showPreview(jasperPrint);
    }

    // exports payslip report to PDF
    public void exportPayslipReportToPdf(int employeeId, int payrunId, String outputPath) {
        validateOutputPath(outputPath);

        PayslipReportDTO payslip = getPayslipReport(employeeId, payrunId);

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                PAYSLIP_REPORT,
                buildPayslipParameters(payslip),
                List.of(payslip)
        );

        jasperReportGenerator.exportToPdf(jasperPrint, outputPath);
    }

    // exports payroll summary report to PDF
    public void exportPayrollSummaryReportToPdf(Integer departmentId,
                                                Integer payrunId,
                                                LocalDate startDate,
                                                LocalDate endDate,
                                                String outputPath) {

        validateOutputPath(outputPath);

        List<PayrollSummaryReportDTO> reportData = getPayrollSummaryReport(
                departmentId,
                payrunId,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                PAYROLL_SUMMARY_REPORT,
                buildPayrollSummaryParameters(startDate, endDate),
                reportData
        );

        jasperReportGenerator.exportToPdf(jasperPrint, outputPath);
    }

    // exports the organization payroll report to PDF
    public void exportOrganizationPayrollReportToPdf(Integer departmentId,
                                                     String departmentName,
                                                     Integer employeeId,
                                                     Integer payrunId,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     String outputPath) {

        validateOutputPath(outputPath);

        List<OrganizationPayrollReportDTO> reportData = getOrganizationPayrollReport(
                departmentId,
                employeeId,
                payrunId,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                ORGANIZATION_PAYROLL_REPORT,
                buildOrganizationPayrollParameters(departmentName, startDate, endDate),
                reportData
        );

        jasperReportGenerator.exportToPdf(jasperPrint, outputPath);
    }

    // builds organization payroll report parameters
    private Map<String, Object> buildOrganizationPayrollParameters(String departmentName,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Monthly Payroll Summary Report");
        parameters.put("DEPARTMENT_NAME", departmentName);
        parameters.put("START_DATE", startDate);
        parameters.put("END_DATE", endDate);
        return parameters;
    }

    // builds payroll summary report parameters
    private Map<String, Object> buildPayrollSummaryParameters(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Payroll Summary Report");
        parameters.put("START_DATE", startDate);
        parameters.put("END_DATE", endDate);
        return parameters;
    }

    // builds payslip report parameters
    private Map<String, Object> buildPayslipParameters(PayslipReportDTO payslip) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Employee Payslip Report");
        parameters.put("START_DATE", payslip.getStartDate());
        parameters.put("END_DATE", payslip.getEndDate());
        return parameters;
    }

    // validates employee id input
    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee is required.");
        }
    }

    // validates payrun id input
    private void validatePayrunId(int payrunId) {
        if (payrunId <= 0) {
            throw new ValidationException("Payrun is required.");
        }
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

    // validates output path input
    private void validateOutputPath(String outputPath) {
        if (outputPath == null || outputPath.isBlank()) {
            throw new ValidationException("Output path is required.");
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
