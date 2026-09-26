package motorph.backend.service;

import motorph.backend.domain.PayrollResult;
import motorph.backend.domain.Payrun;
import motorph.backend.dto.payroll.PayslipReportDTO;
import motorph.backend.dto.payroll.PayslipSummaryDTO;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.ValidationException;
import motorph.backend.reporting.JasperReportGenerator;
import motorph.backend.repository.PayrollResultRepository;
import motorph.backend.repository.PayrunRepository;
import motorph.backend.repository.report.PayrollReportRepository;

import net.sf.jasperreports.engine.JasperPrint;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Payslip history and PDF generation for employees.
 */
@Service
public class PayslipService {

    private static final String PAYSLIP_REPORT_TEMPLATE = "/motorph/reports/payroll/payslip_report.jrxml";

    private final PayrunRepository payrunRepository;
    private final PayrollResultRepository payrollResultRepository;
    private final PayrollReportRepository payrollReportRepository;
    private final JasperReportGenerator jasperReportGenerator;

    public PayslipService(PayrunRepository payrunRepository,
                          PayrollResultRepository payrollResultRepository,
                          PayrollReportRepository payrollReportRepository,
                          JasperReportGenerator jasperReportGenerator) {
        this.payrunRepository = payrunRepository;
        this.payrollResultRepository = payrollResultRepository;
        this.payrollReportRepository = payrollReportRepository;
        this.jasperReportGenerator = jasperReportGenerator;
    }

    /**
     * Returns the employee's payroll history, newest first.
     */
    public List<PayslipSummaryDTO> getPayslipSummaries(int employeeId) {
        validateEmployeeId(employeeId);

        List<PayrollResult> results = payrollResultRepository.findByEmployeeId(employeeId);
        results.sort(Comparator.comparing(PayrollResult::getGeneratedAt).reversed());

        List<PayslipSummaryDTO> summaries = new ArrayList<>();

        for (PayrollResult result : results) {
            Payrun payrun = payrunRepository.findById(result.getPayrunId());

            if (payrun == null) {
                continue;
            }

            summaries.add(new PayslipSummaryDTO(
                    result.getPayrunId(),
                    payrun.getStartDate(),
                    payrun.getEndDate(),
                    result.getGrossPay(),
                    result.getTotalDeductions(),
                    result.getNetPay()
            ));
        }

        return summaries;
    }

    /**
     * Returns the payslip data used to build the PDF.
     */
    public PayslipReportDTO getPayslipReport(int employeeId, int payrunId) {
        validateEmployeeId(employeeId);
        validatePayrunId(payrunId);

        PayslipReportDTO payslip = payrollReportRepository.findPayslipReport(employeeId, payrunId);

        if (payslip == null) {
            throw new EntityNotFoundException("Payslip not found.");
        }

        return payslip;
    }

    public byte[] generatePayslipPdf(int employeeId, int payrunId) {
        PayslipReportDTO payslip = getPayslipReport(employeeId, payrunId);

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                PAYSLIP_REPORT_TEMPLATE,
                buildPayslipParameters(payslip),
                List.of(payslip)
        );

        return jasperReportGenerator.exportToPdfBytes(jasperPrint);
    }

    private Map<String, Object> buildPayslipParameters(PayslipReportDTO payslip) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Employee Payslip Report");
        parameters.put("START_DATE", payslip.getStartDate());
        parameters.put("END_DATE", payslip.getEndDate());
        return parameters;
    }

    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee is required.");
        }
    }

    private void validatePayrunId(int payrunId) {
        if (payrunId <= 0) {
            throw new ValidationException("Payrun is required.");
        }
    }
}
