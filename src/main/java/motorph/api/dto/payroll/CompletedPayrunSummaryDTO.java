package motorph.api.dto.payroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import motorph.domain.value.PayrollType;

import java.time.LocalDate;

/** Wire copy of the backend's payroll.CompletedPayrunSummaryDTO. */
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

    @JsonCreator
    public CompletedPayrunSummaryDTO(@JsonProperty("payrunId") int payrunId,
                                     @JsonProperty("startDate") LocalDate startDate,
                                     @JsonProperty("endDate") LocalDate endDate,
                                     @JsonProperty("payDate") LocalDate payDate,
                                     @JsonProperty("payrollType") PayrollType payrollType,
                                     @JsonProperty("employeesPaid") int employeesPaid,
                                     @JsonProperty("totalGrossPay") double totalGrossPay,
                                     @JsonProperty("totalDeductions") double totalDeductions,
                                     @JsonProperty("totalNetPay") double totalNetPay) {
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
