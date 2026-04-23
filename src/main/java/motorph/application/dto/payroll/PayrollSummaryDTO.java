package motorph.application.dto.payroll;

import java.time.LocalDate;

/**
 * Carries payroll summary details for one generated payroll result.
 */
public class PayrollSummaryDTO {

    private final int payrollResultId;
    private final int employeeId;
    private final int payrunId;
    private final String fullName;
    private final LocalDate payDate;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final double grossPay;
    private final double totalDeductions;
    private final double netPay;

    // constructors
    public PayrollSummaryDTO(
            int payrollResultId,
            int employeeId,
            int payrunId,
            String fullName,
            LocalDate payDate,
            LocalDate periodStart,
            LocalDate periodEnd,
            double grossPay,
            double totalDeductions,
            double netPay) {

        this.payrollResultId = payrollResultId;
        this.employeeId = employeeId;
        this.payrunId = payrunId;
        this.fullName = fullName;
        this.payDate = payDate;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.grossPay = grossPay;
        this.totalDeductions = totalDeductions;
        this.netPay = netPay;
    }

    // getters
    public int getPayrollResultId() {
        return payrollResultId;
    }

    // getters
    public int getEmployeeId() {
        return employeeId;
    }

    // getters
    public int getPayrunId() {
        return payrunId;
    }

    // getters
    public String getFullName() {
        return fullName;
    }

    // getters
    public LocalDate getPayDate() {
        return payDate;
    }

    // getters
    public LocalDate getPeriodStart() {
        return periodStart;
    }

    // getters
    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    // getters
    public double getGrossPay() {
        return grossPay;
    }

    // getters
    public double getTotalDeductions() {
        return totalDeductions;
    }

    // getters
    public double getNetPay() {
        return netPay;
    }
}