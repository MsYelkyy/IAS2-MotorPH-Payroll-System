package motorph.backend.dto.payroll;

import java.time.LocalDate;

/**
 * One row in an employee's payroll history. {@code basicPay} equals {@code grossPay} because {@code
 * payroll_result} does not store basic pay separately; the full breakdown is only on the payslip.
 */
public class PayslipSummaryDTO {

    private final int payrunId;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final double grossPay;
    private final double deductions;
    private final double netPay;

    public PayslipSummaryDTO(int payrunId, LocalDate periodStart, LocalDate periodEnd,
                             double grossPay, double deductions, double netPay) {
        this.payrunId = payrunId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.grossPay = grossPay;
        this.deductions = deductions;
        this.netPay = netPay;
    }

    public int getPayrunId() {
        return payrunId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public double getGrossPay() {
        return grossPay;
    }

    public double getDeductions() {
        return deductions;
    }

    public double getNetPay() {
        return netPay;
    }
}
