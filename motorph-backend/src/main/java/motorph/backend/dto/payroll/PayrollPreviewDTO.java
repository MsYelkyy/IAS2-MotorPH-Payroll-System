package motorph.backend.dto.payroll;

import java.time.LocalDate;

/** Carries a non-persisted payroll preview computed before generation. */
public class PayrollPreviewDTO {

    private final int employeeId;
    private final String fullName;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final double totalHours;
    private final double grossPay;
    private final double totalDeductions;
    private final double netPay;

    // constructors
    public PayrollPreviewDTO(int employeeId,
                             String fullName,
                             LocalDate periodStart,
                             LocalDate periodEnd,
                             double totalHours,
                             double grossPay,
                             double totalDeductions,
                             double netPay) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalHours = totalHours;
        this.grossPay = grossPay;
        this.totalDeductions = totalDeductions;
        this.netPay = netPay;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public double getTotalHours() {
        return totalHours;
    }

    public double getGrossPay() {
        return grossPay;
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNetPay() {
        return netPay;
    }
}
