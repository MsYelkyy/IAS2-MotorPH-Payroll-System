package motorph.api.dto.payroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/** Wire copy of the backend's payroll.PayrollSummaryDTO. */
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

    @JsonCreator
    public PayrollSummaryDTO(@JsonProperty("payrollResultId") int payrollResultId,
                             @JsonProperty("employeeId") int employeeId,
                             @JsonProperty("payrunId") int payrunId,
                             @JsonProperty("fullName") String fullName,
                             @JsonProperty("payDate") LocalDate payDate,
                             @JsonProperty("periodStart") LocalDate periodStart,
                             @JsonProperty("periodEnd") LocalDate periodEnd,
                             @JsonProperty("grossPay") double grossPay,
                             @JsonProperty("totalDeductions") double totalDeductions,
                             @JsonProperty("netPay") double netPay) {
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

    public int getPayrollResultId() {
        return payrollResultId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public int getPayrunId() {
        return payrunId;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getPayDate() {
        return payDate;
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

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNetPay() {
        return netPay;
    }
}
