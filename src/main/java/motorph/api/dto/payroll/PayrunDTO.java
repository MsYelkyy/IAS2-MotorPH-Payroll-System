package motorph.api.dto.payroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import motorph.domain.value.PayrollType;

import java.time.LocalDate;

/** Wire copy of the backend's domain.Payrun. */
public class PayrunDTO {
    private final int payrunId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDate payDate;
    private final PayrollType payrollType;

    @JsonCreator
    public PayrunDTO(@JsonProperty("payrunId") int payrunId,
                     @JsonProperty("startDate") LocalDate startDate,
                     @JsonProperty("endDate") LocalDate endDate,
                     @JsonProperty("payDate") LocalDate payDate,
                     @JsonProperty("payrollType") PayrollType payrollType) {
        this.payrunId = payrunId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.payDate = payDate;
        this.payrollType = payrollType;
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
}
