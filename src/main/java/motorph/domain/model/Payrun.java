package motorph.domain.model;

import java.time.LocalDate;
import motorph.domain.value.PayrollType;

/**
 * Represents a payroll batch and maps to the payrun table in the database.
 */
public class Payrun {

    private Integer payrunId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate payDate;
    private PayrollType payrollType;

    // constructors
    public Payrun(
            Integer payrunId,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate payDate,
            PayrollType payrollType) {

        setPayrunId(payrunId);
        setStartDate(startDate);
        setEndDate(endDate);
        setPayDate(payDate);
        setPayrollType(payrollType);
        validateDateRange();
    }

    // getters
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

    // checks if the payrun covers a date
    public boolean includesDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    // checks if the payrun is released
    public boolean isReleasedOn(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        return !payDate.isAfter(date);
    }

    // setters
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

    // validates the payrun date range
    private void validateDateRange() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
    }

    // returns the payrun summary
    @Override
    public String toString() {
        return "Payrun{"
                + "payrunId=" + payrunId
                + ", startDate=" + startDate
                + ", endDate=" + endDate
                + ", payDate=" + payDate
                + ", payrollType=" + payrollType
                + '}';
    }
}