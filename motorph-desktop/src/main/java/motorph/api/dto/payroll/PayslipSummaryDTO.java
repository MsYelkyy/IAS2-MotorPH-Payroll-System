package motorph.api.dto.payroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * One row in the employee's payroll history table. Mirrors the old
 * PayrollRowItem view-model's getter names, including its basicPay==grossPay
 * simplification, so EmployeePayslipController's table wiring needed no changes.
 */
public record PayslipSummaryDTO(
        int payrunId,
        LocalDate periodStart,
        LocalDate periodEnd,
        double grossPay,
        double deductions,
        double netPay
) {

    @JsonCreator
    public PayslipSummaryDTO(@JsonProperty("payrunId") int payrunId,
                             @JsonProperty("periodStart") LocalDate periodStart,
                             @JsonProperty("periodEnd") LocalDate periodEnd,
                             @JsonProperty("grossPay") double grossPay,
                             @JsonProperty("deductions") double deductions,
                             @JsonProperty("netPay") double netPay) {
        this.payrunId = payrunId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.grossPay = grossPay;
        this.deductions = deductions;
        this.netPay = netPay;
    }

    // matches the old PayrollRowItem.getBasicPay(), which was itself an alias for grossPay
    public double getBasicPay() {
        return grossPay;
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
