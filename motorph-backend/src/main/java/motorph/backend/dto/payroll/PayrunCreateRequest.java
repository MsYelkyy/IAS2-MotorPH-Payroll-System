package motorph.backend.dto.payroll;

import motorph.backend.domain.value.PayrollType;

import java.time.LocalDate;

/** Request body to create a new payrun. */
public class PayrunCreateRequest {

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate payDate;
    private PayrollType payrollType;

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public void setPayDate(LocalDate payDate) {
        this.payDate = payDate;
    }

    public PayrollType getPayrollType() {
        return payrollType;
    }

    public void setPayrollType(PayrollType payrollType) {
        this.payrollType = payrollType;
    }
}
