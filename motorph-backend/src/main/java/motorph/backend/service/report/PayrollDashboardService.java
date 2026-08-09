package motorph.backend.service.report;

import motorph.backend.dto.report.payroll.PayrollDashboardDTO;
import motorph.backend.repository.report.PayrollDashboardRepository;

import org.springframework.stereotype.Service;

@Service
public class PayrollDashboardService {

    private static final int TREND_MONTH_LIMIT = 6;

    private final PayrollDashboardRepository repository;

    public PayrollDashboardService(PayrollDashboardRepository repository) {
        this.repository = repository;
    }

    public PayrollDashboardDTO getLatestDashboard() {
        String latestMonth = repository.findLatestPayrollMonth();

        if (latestMonth == null) {
            return null;
        }

        return getDashboard(latestMonth);
    }

    public PayrollDashboardDTO getDashboard(String payrollMonth) {
        PayrollDashboardDTO totals = repository.findMonthlyTotals(payrollMonth);

        if (totals == null) {
            return null;
        }

        return new PayrollDashboardDTO(
                totals.getTotalEmployees(),
                totals.getTotalGrossPay(),
                totals.getTotalDeductions(),
                totals.getTotalNetPay(),
                totals.getPayrollMonthLabel(),
                totals.getMostRecentPayDate(),
                totals.getPayrollCutoffs(),
                repository.findNetPayByMonth(TREND_MONTH_LIMIT),
                repository.findDeductionBreakdown(payrollMonth),
                repository.findDepartmentCosts(payrollMonth)
        );
    }
}
