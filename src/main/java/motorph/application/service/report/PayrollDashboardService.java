package motorph.application.service.report;

import motorph.application.dto.report.payroll.PayrollDashboardDTO;
import motorph.infrastructure.repository.report.PayrollDashboardRepository;

import java.util.List;

/**
 * Builds the Payroll Reporting Dashboard data from the monthly summary view.
 * Read-only: it only aggregates existing report data, no payroll changes.
 */
public class PayrollDashboardService {

    private static final int TREND_MONTH_LIMIT = 6;

    private final PayrollDashboardRepository payrollDashboardRepository;

    // constructors
    public PayrollDashboardService(PayrollDashboardRepository payrollDashboardRepository) {
        this.payrollDashboardRepository = require(
                payrollDashboardRepository,
                "PayrollDashboardRepository is required.");
    }

    // builds the dashboard for the latest payroll month in the view
    public PayrollDashboardDTO getLatestDashboard() {
        String latestMonth = payrollDashboardRepository.findLatestPayrollMonth();
        if (latestMonth == null) {
            return null;
        }
        return getDashboard(latestMonth);
    }

    // builds the full dashboard (KPIs + charts) for one payroll month
    public PayrollDashboardDTO getDashboard(String payrollMonth) {
        PayrollDashboardDTO totals = payrollDashboardRepository.findMonthlyTotals(payrollMonth);
        if (totals == null) {
            return null;
        }

        List<PayrollDashboardDTO.NetPayPoint> netPayByMonth =
                payrollDashboardRepository.findNetPayByMonth(TREND_MONTH_LIMIT);

        PayrollDashboardDTO.DeductionBreakdown deductions =
                payrollDashboardRepository.findDeductionBreakdown(payrollMonth);

        List<PayrollDashboardDTO.DepartmentCost> departmentCosts =
                payrollDashboardRepository.findDepartmentCosts(payrollMonth);

        // merge the KPI totals with the chart data into one dashboard object
        return new PayrollDashboardDTO(
                totals.getTotalEmployees(),
                totals.getTotalGrossPay(),
                totals.getTotalDeductions(),
                totals.getTotalNetPay(),
                totals.getPayrollMonthLabel(),
                totals.getMostRecentPayDate(),
                totals.getPayrollCutoffs(),
                netPayByMonth,
                deductions,
                departmentCosts);
    }

    // validates required dependency
    private <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
