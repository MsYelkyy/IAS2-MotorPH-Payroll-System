package motorph.application.dto.report.payroll;

import java.time.LocalDate;
import java.util.List;

/**
 * Holds the read-only aggregates shown on the Payroll Reporting Dashboard.
 * All values come from vw_payroll_summary_monthly for one payroll month.
 */
public class PayrollDashboardDTO {

    // KPI cards
    private final int totalEmployees;
    private final double totalGrossPay;
    private final double totalDeductions;
    private final double totalNetPay;
    private final String payrollMonthLabel;   // e.g. "June 2024"
    private final LocalDate mostRecentPayDate; // latest month_end for the month
    private final int payrollCutoffs;

    // chart data
    private final List<NetPayPoint> netPayByMonth;
    private final DeductionBreakdown deductionBreakdown;
    private final List<DepartmentCost> departmentCosts;

    // constructors
    public PayrollDashboardDTO(int totalEmployees,
                               double totalGrossPay,
                               double totalDeductions,
                               double totalNetPay,
                               String payrollMonthLabel,
                               LocalDate mostRecentPayDate,
                               int payrollCutoffs,
                               List<NetPayPoint> netPayByMonth,
                               DeductionBreakdown deductionBreakdown,
                               List<DepartmentCost> departmentCosts) {

        this.totalEmployees = totalEmployees;
        this.totalGrossPay = totalGrossPay;
        this.totalDeductions = totalDeductions;
        this.totalNetPay = totalNetPay;
        this.payrollMonthLabel = payrollMonthLabel;
        this.mostRecentPayDate = mostRecentPayDate;
        this.payrollCutoffs = payrollCutoffs;
        this.netPayByMonth = netPayByMonth;
        this.deductionBreakdown = deductionBreakdown;
        this.departmentCosts = departmentCosts;
    }

    // getters
    public int getTotalEmployees() {
        return totalEmployees;
    }

    public double getTotalGrossPay() {
        return totalGrossPay;
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getTotalNetPay() {
        return totalNetPay;
    }

    public String getPayrollMonthLabel() {
        return payrollMonthLabel;
    }

    public LocalDate getMostRecentPayDate() {
        return mostRecentPayDate;
    }

    public int getPayrollCutoffs() {
        return payrollCutoffs;
    }

    public List<NetPayPoint> getNetPayByMonth() {
        return netPayByMonth;
    }

    public DeductionBreakdown getDeductionBreakdown() {
        return deductionBreakdown;
    }

    public List<DepartmentCost> getDepartmentCosts() {
        return departmentCosts;
    }

    // one point on the net-pay trend line (one payroll month)
    public static class NetPayPoint {
        private final String monthLabel;
        private final double netPay;

        public NetPayPoint(String monthLabel, double netPay) {
            this.monthLabel = monthLabel;
            this.netPay = netPay;
        }

        public String getMonthLabel() {
            return monthLabel;
        }

        public double getNetPay() {
            return netPay;
        }
    }

    // totals for the deduction donut chart
    public static class DeductionBreakdown {
        private final double sss;
        private final double philhealth;
        private final double pagibig;
        private final double withholdingTax;

        public DeductionBreakdown(double sss, double philhealth, double pagibig, double withholdingTax) {
            this.sss = sss;
            this.philhealth = philhealth;
            this.pagibig = pagibig;
            this.withholdingTax = withholdingTax;
        }

        public double getSss() {
            return sss;
        }

        public double getPhilhealth() {
            return philhealth;
        }

        public double getPagibig() {
            return pagibig;
        }

        public double getWithholdingTax() {
            return withholdingTax;
        }

        public double getTotal() {
            return sss + philhealth + pagibig + withholdingTax;
        }
    }

    // one bar on the payroll-cost-by-department chart
    public static class DepartmentCost {
        private final String departmentName;
        private final double netPay;

        public DepartmentCost(String departmentName, double netPay) {
            this.departmentName = departmentName;
            this.netPay = netPay;
        }

        public String getDepartmentName() {
            return departmentName;
        }

        public double getNetPay() {
            return netPay;
        }
    }
}
