package motorph.api.dto.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * Wire copy of the backend's PayrollDashboardDTO, mirroring the old
 * motorph.application.dto.report.payroll.PayrollDashboardDTO getter names
 * (including its nested NetPayPoint/DeductionBreakdown/DepartmentCost types)
 * so FinanceDashboardController needed no changes beyond the service call.
 */
public class PayrollDashboardDTO {

    private final int totalEmployees;
    private final double totalGrossPay;
    private final double totalDeductions;
    private final double totalNetPay;
    private final String payrollMonthLabel;
    private final LocalDate mostRecentPayDate;
    private final int payrollCutoffs;
    private final List<NetPayPoint> netPayByMonth;
    private final DeductionBreakdown deductionBreakdown;
    private final List<DepartmentCost> departmentCosts;

    @JsonCreator
    public PayrollDashboardDTO(@JsonProperty("totalEmployees") int totalEmployees,
                               @JsonProperty("totalGrossPay") double totalGrossPay,
                               @JsonProperty("totalDeductions") double totalDeductions,
                               @JsonProperty("totalNetPay") double totalNetPay,
                               @JsonProperty("payrollMonthLabel") String payrollMonthLabel,
                               @JsonProperty("mostRecentPayDate") LocalDate mostRecentPayDate,
                               @JsonProperty("payrollCutoffs") int payrollCutoffs,
                               @JsonProperty("netPayByMonth") List<NetPayPoint> netPayByMonth,
                               @JsonProperty("deductionBreakdown") DeductionBreakdown deductionBreakdown,
                               @JsonProperty("departmentCosts") List<DepartmentCost> departmentCosts) {
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

    public static class NetPayPoint {
        private final String monthLabel;
        private final double netPay;

        @JsonCreator
        public NetPayPoint(@JsonProperty("monthLabel") String monthLabel, @JsonProperty("netPay") double netPay) {
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

    public static class DeductionBreakdown {
        private final double sss;
        private final double philhealth;
        private final double pagibig;
        private final double withholdingTax;

        @JsonCreator
        public DeductionBreakdown(@JsonProperty("sss") double sss,
                                  @JsonProperty("philhealth") double philhealth,
                                  @JsonProperty("pagibig") double pagibig,
                                  @JsonProperty("withholdingTax") double withholdingTax) {
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

    public static class DepartmentCost {
        private final String departmentName;
        private final double netPay;

        @JsonCreator
        public DepartmentCost(@JsonProperty("departmentName") String departmentName, @JsonProperty("netPay") double netPay) {
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
