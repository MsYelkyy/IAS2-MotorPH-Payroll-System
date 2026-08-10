package motorph.backend.dto.payroll;

import motorph.backend.domain.value.PayrollType;

import java.time.LocalDate;

/**
 * Carries one completed payrun's summary for the completed payroll landing tab.
 * A completed payrun already has generated payroll results.
 */
public class CompletedPayrunSummaryDTO {

    private final int payrunId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDate payDate;
    private final PayrollType payrollType;
    private final int employeesPaid;
    private final double totalGrossPay;
    private final double totalDeductions;
    private final double totalNetPay;

    // constructors
    public CompletedPayrunSummaryDTO(int payrunId,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     LocalDate payDate,
                                     PayrollType payrollType,
                                     int employeesPaid,
                                     double totalGrossPay,
                                     double totalDeductions,
                                     double totalNetPay) {
        this.payrunId = payrunId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.payDate = payDate;
        this.payrollType = payrollType;
        this.employeesPaid = employeesPaid;
        this.totalGrossPay = totalGrossPay;
        this.totalDeductions = totalDeductions;
        this.totalNetPay = totalNetPay;
    }

    public int getPayrunId() {
        return payrunId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public PayrollType getPayrollType() {
        return payrollType;
    }

    public int getEmployeesPaid() {
        return employeesPaid;
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
}
