package motorph.domain.model;

import java.time.LocalDate;

/**
 * Represents an employee salary record and maps to the salary_history table in the database.
 */
public class SalaryHistory {

    private Integer salaryHistoryId;
    private int employeeId;
    private double basicSalary;
    private double grossSemiMonthly;
    private double hourlyRate;
    private LocalDate startDate;
    private LocalDate endDate;

    // constructors
    public SalaryHistory(
            Integer salaryHistoryId,
            int employeeId,
            double basicSalary,
            double grossSemiMonthly,
            double hourlyRate,
            LocalDate startDate,
            LocalDate endDate) {

        setSalaryHistoryId(salaryHistoryId);
        setEmployeeId(employeeId);
        setBasicSalary(basicSalary);
        setGrossSemiMonthly(grossSemiMonthly);
        setHourlyRate(hourlyRate);
        setStartDate(startDate);
        setEndDate(endDate);
        validateDateRange();
    }

    // getters
    public Integer getSalaryHistoryId() {
        return salaryHistoryId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    public double getGrossSemiMonthly() {
        return grossSemiMonthly;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    // checks if the salary record is active on a date
    public boolean isEffectiveOn(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }

        boolean startsOnOrBefore = !startDate.isAfter(date);
        boolean hasNoEndDate = endDate == null;
        boolean endsOnOrAfter = hasNoEndDate || !endDate.isBefore(date);

        return startsOnOrBefore && endsOnOrAfter;
    }

    // checks if the salary record is currently active
    public boolean isCurrent() {
        return isEffectiveOn(LocalDate.now());
    }

    // setters
    public void setSalaryHistoryId(Integer salaryHistoryId) {
        if (salaryHistoryId != null && salaryHistoryId <= 0) {
            throw new IllegalArgumentException("Salary history ID must be positive.");
        }
        this.salaryHistoryId = salaryHistoryId;
    }

    public void setEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }
        this.employeeId = employeeId;
    }

    public void setBasicSalary(double basicSalary) {
        if (basicSalary < 0) {
            throw new IllegalArgumentException("Basic salary cannot be negative.");
        }
        this.basicSalary = basicSalary;
    }

    public void setGrossSemiMonthly(double grossSemiMonthly) {
        if (grossSemiMonthly < 0) {
            throw new IllegalArgumentException("Gross semi-monthly salary cannot be negative.");
        }
        this.grossSemiMonthly = grossSemiMonthly;
    }

    public void setHourlyRate(double hourlyRate) {
        if (hourlyRate < 0) {
            throw new IllegalArgumentException("Hourly rate cannot be negative.");
        }
        this.hourlyRate = hourlyRate;
    }

    public void setStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required.");
        }
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        validateDateRange();
    }

    // validates the salary date range
    private void validateDateRange() {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
    }

    // returns the salary history summary
    @Override
    public String toString() {
        return "SalaryHistory{"
                + "salaryHistoryId=" + salaryHistoryId
                + ", employeeId=" + employeeId
                + ", basicSalary=" + basicSalary
                + ", grossSemiMonthly=" + grossSemiMonthly
                + ", hourlyRate=" + hourlyRate
                + ", startDate=" + startDate
                + ", endDate=" + endDate
                + '}';
    }
}