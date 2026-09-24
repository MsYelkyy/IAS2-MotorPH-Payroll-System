package motorph.backend.service.report;

import motorph.backend.dto.report.payroll.OrganizationPayrollReportDTO;
import motorph.backend.dto.report.payroll.PayrollSummaryReportDTO;
import motorph.backend.exception.ValidationException;
import motorph.backend.reporting.JasperReportGenerator;
import motorph.backend.repository.report.PayrollReportRepository;

import net.sf.jasperreports.engine.JasperPrint;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static motorph.backend.service.report.ReportGuards.requireRows;

/**
 * Organization-wide and per-department payroll reports. Individual payslips are handled by {@code
 * PayslipService}.
 */
@Service
public class PayrollReportService {

    private static final String ORGANIZATION_PAYROLL_REPORT = "/motorph/reports/payroll/organization_payroll_report.jrxml";
    private static final String PAYROLL_SUMMARY_REPORT = "/motorph/reports/payroll/payroll_summary_report.jrxml";

    private final PayrollReportRepository payrollReportRepository;
    private final JasperReportGenerator jasperReportGenerator;

    public PayrollReportService(PayrollReportRepository payrollReportRepository, JasperReportGenerator jasperReportGenerator) {
        this.payrollReportRepository = payrollReportRepository;
        this.jasperReportGenerator = jasperReportGenerator;
    }

    public List<OrganizationPayrollReportDTO> getOrganizationPayrollReport(Integer departmentId, String departmentName,
                                                                           Integer employeeId, Integer payrunId,
                                                                           LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return payrollReportRepository.findOrganizationPayrollReport(departmentId, departmentName, employeeId, payrunId, startDate, endDate);
    }

    public byte[] generateOrganizationPayrollReportPdf(Integer departmentId, String departmentName, Integer employeeId,
                                                        Integer payrunId, LocalDate startDate, LocalDate endDate) {
        List<OrganizationPayrollReportDTO> data =
                getOrganizationPayrollReport(departmentId, departmentName, employeeId, payrunId, startDate, endDate);
        requireRows(data, "No payroll records match the selected filters.");

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                ORGANIZATION_PAYROLL_REPORT,
                buildOrganizationPayrollParameters(departmentName, startDate, endDate),
                data
        );

        return jasperReportGenerator.exportToPdfBytes(jasperPrint);
    }

    public List<PayrollSummaryReportDTO> getPayrollSummaryReport(Integer departmentId, Integer payrunId,
                                                                  LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return payrollReportRepository.findPayrollSummaryReport(departmentId, payrunId, startDate, endDate);
    }

    public byte[] generatePayrollSummaryReportPdf(Integer departmentId, Integer payrunId,
                                                   LocalDate startDate, LocalDate endDate) {
        List<PayrollSummaryReportDTO> data = getPayrollSummaryReport(departmentId, payrunId, startDate, endDate);
        requireRows(data, "No payroll records match the selected filters.");

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                PAYROLL_SUMMARY_REPORT,
                buildPayrollSummaryParameters(startDate, endDate),
                data
        );

        return jasperReportGenerator.exportToPdfBytes(jasperPrint);
    }

    private Map<String, Object> buildOrganizationPayrollParameters(String departmentName, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Monthly Payroll Summary Report");
        parameters.put("DEPARTMENT_NAME", departmentName);
        parameters.put("START_DATE", startDate);
        parameters.put("END_DATE", endDate);
        return parameters;
    }

    private Map<String, Object> buildPayrollSummaryParameters(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Payroll Summary Report");
        parameters.put("START_DATE", startDate);
        parameters.put("END_DATE", endDate);
        return parameters;
    }

    /**
     * Dates are optional, but if one is given the other must be too.
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw new ValidationException("Both start and end date are required together.");
        }
        if (startDate != null && endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
    }
}
