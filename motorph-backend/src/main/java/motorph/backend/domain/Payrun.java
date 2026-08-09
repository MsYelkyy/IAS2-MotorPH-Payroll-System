package motorph.backend.domain;

import java.time.LocalDate;
import motorph.backend.domain.value.PayrollType;

/**
 * Represents a payroll batch and maps to the payrun table in the database.
 */
public class Payrun {

    private Integer payrunId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate payDate;
    private PayrollType payrollType;

    public Payrun(Integer payrunId, LocalDate startDate, LocalDate endDate,
                  LocalDate payDate, PayrollType payrollType) {
        setPayrunId(payrunId);
        setStartDate(startDate);
        setEndDate(endDate);
        setPayDate(payDate);
        setPayrollType(payrollType);
        validateDateRange();
    }

    public Integer getPayrunId() {
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

    public void setPayrunId(Integer payrunId) {
        if (payrunId != null && payrunId <= 0) {
            throw new IllegalArgumentException("Payrun ID must be positive.");
        }
        this.payrunId = payrunId;
    }

    public void setStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required.");
        }
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("End date is required.");
        }
        this.endDate = endDate;
        validateDateRange();
    }

    public void setPayDate(LocalDate payDate) {
        if (payDate == null) {
            throw new IllegalArgumentException("Pay date is required.");
        }
        this.payDate = payDate;
    }

    public void setPayrollType(PayrollType payrollType) {
        if (payrollType == null) {
            throw new IllegalArgumentException("Payroll type is required.");
        }
        this.payrollType = payrollType;
    }

    private void validateDateRange() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
    }
}
