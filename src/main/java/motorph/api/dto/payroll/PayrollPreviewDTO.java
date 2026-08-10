package motorph.api.dto.payroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/** Wire copy of the backend's payroll.PayrollPreviewDTO. */
public class PayrollPreviewDTO {
    private final int employeeId;
    private final String fullName;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final double totalHours;
    private final double grossPay;
    private final double totalDeductions;
    private final double netPay;

    @JsonCreator
    public PayrollPreviewDTO(@JsonProperty("employeeId") int employeeId,
                             @JsonProperty("fullName") String fullName,
                             @JsonProperty("periodStart") LocalDate periodStart,
                             @JsonProperty("periodEnd") LocalDate periodEnd,
                             @JsonProperty("totalHours") double totalHours,
                             @JsonProperty("grossPay") double grossPay,
                             @JsonProperty("totalDeductions") double totalDeductions,
                             @JsonProperty("netPay") double netPay) {
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
