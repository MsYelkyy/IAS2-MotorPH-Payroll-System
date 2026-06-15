package motorph.application.service.report;



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

    // builds organization payroll report parameters
    private Map<String, Object> buildOrganizationPayrollParameters(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("REPORT_TITLE", "Organizational Payroll Report");
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

    // builds department payroll report parameters
    private Map<String, Object> buildDepartmentPayrollParameters(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("REPORT_TITLE", "Department Payroll Report");
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

    // validates department id input
    private void validateDepartmentId(Integer departmentId) {
        if (departmentId == null || departmentId <= 0) {
            throw new ValidationException("Department is required.");
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