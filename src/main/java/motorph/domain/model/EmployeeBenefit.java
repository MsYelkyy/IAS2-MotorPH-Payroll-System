package motorph.domain.model;

import java.time.LocalDate;

/**
 * Represents an employee benefit record and maps to the employee_benefit table in the database.
 */
public class EmployeeBenefit {

    private Integer employeeBenefitId;
    private int employeeId;
    private int benefitTypeId;
    private double amount;
    private LocalDate effectiveStartDate;
    private LocalDate effectiveEndDate;

    // constructors
    public EmployeeBenefit(
            Integer employeeBenefitId,
            int employeeId,
            int benefitTypeId,
            double amount,
            LocalDate effectiveStartDate,
            LocalDate effectiveEndDate) {

        setEmployeeBenefitId(employeeBenefitId);
        setEmployeeId(employeeId);
        setBenefitTypeId(benefitTypeId);
        setAmount(amount);
        setEffectiveStartDate(effectiveStartDate);
        setEffectiveEndDate(effectiveEndDate);
        validateDateRange();
    }

    // getters
    public Integer getEmployeeBenefitId() {
        return employeeBenefitId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public int getBenefitTypeId() {
        return benefitTypeId;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDate getEffectiveStartDate() {
        return effectiveStartDate;
    }

    public LocalDate getEffectiveEndDate() {
        return effectiveEndDate;
    }

    // checks if the benefit is active on a date
    public boolean isEffectiveOn(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        boolean startsOnOrBefore = !effectiveStartDate.isAfter(date);
        boolean hasNoEndDate = effectiveEndDate == null;
        boolean endsOnOrAfter = hasNoEndDate || !effectiveEndDate.isBefore(date);

        return startsOnOrBefore && endsOnOrAfter;
    }

    // checks if the benefit is currently active
    public boolean isCurrent() {
        return isEffectiveOn(LocalDate.now());
    }

    // setters
    public void setEmployeeBenefitId(Integer employeeBenefitId) {
        if (employeeBenefitId != null && employeeBenefitId <= 0) {
            throw new IllegalArgumentException("Employee benefit ID must be positive.");
        }
        this.employeeBenefitId = employeeBenefitId;
    }

    public void setEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }
        this.employeeId = employeeId;
    }

    public void setBenefitTypeId(int benefitTypeId) {
        if (benefitTypeId <= 0) {
            throw new IllegalArgumentException("Benefit type ID must be positive.");
        }
        this.benefitTypeId = benefitTypeId;
    }

    public void setAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Benefit amount cannot be negative.");
        }
        this.amount = amount;
    }

    public void setEffectiveStartDate(LocalDate effectiveStartDate) {
        if (effectiveStartDate == null) {
            throw new IllegalArgumentException("Effective start date is required.");
        }
        this.effectiveStartDate = effectiveStartDate;
    }

    public void setEffectiveEndDate(LocalDate effectiveEndDate) {
        this.effectiveEndDate = effectiveEndDate;
        validateDateRange();
    }

    // validates the benefit date range
    private void validateDateRange() {
        if (effectiveStartDate != null
                && effectiveEndDate != null
                && effectiveEndDate.isBefore(effectiveStartDate)) {
            throw new IllegalArgumentException("Effective end date cannot be before effective start date.");
        }
    }

    // returns the employee benefit summary
    @Override
    public String toString() {
        return "EmployeeBenefit{"
                + "employeeBenefitId=" + employeeBenefitId
                + ", employeeId=" + employeeId
                + ", benefitTypeId=" + benefitTypeId
                + ", amount=" + amount
                + ", effectiveStartDate=" + effectiveStartDate
                + ", effectiveEndDate=" + effectiveEndDate
                + '}';
    }
}