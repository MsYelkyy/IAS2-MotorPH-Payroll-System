package motorph.domain.model;

import java.time.LocalDate;
import motorph.domain.value.SalaryType;

/**
 * Represents an employee salary record and maps to the salary_history table.
 */
public class SalaryHistory {

    private Integer salaryHistoryId;
    private int employeeId;
    private SalaryType salaryType;
    private Double basicSalary;
    private Double hourlyRate;
    private LocalDate startDate;
    private LocalDate endDate;

    // constructors
    public SalaryHistory(
            Integer salaryHistoryId,
            int employeeId,
            SalaryType salaryType,
            Double basicSalary,
            Double hourlyRate,
            LocalDate startDate,
            LocalDate endDate) {

        setSalaryHistoryId(salaryHistoryId);
        setEmployeeId(employeeId);
        setSalaryType(salaryType);
        setBasicSalary(basicSalary);
        setHourlyRate(hourlyRate);
        setStartDate(startDate);
        setEndDate(endDate);
        validateSalaryType();
        validateDateRange();
    }

    // getters
    public Integer getSalaryHistoryId() {
        return salaryHistoryId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public SalaryType getSalaryType() {
        return salaryType;
    }

    public Double getBasicSalary() {
        return basicSalary;
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }

    // returns the semi-monthly salary
    public double getGrossSemiMonthly() {
        if (salaryType != SalaryType.MONTHLY || basicSalary == null) {
            return 0.0;
        }
        return basicSalary / 2;
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

    public void setSalaryType(SalaryType salaryType) {
        if (salaryType == null) {
            throw new IllegalArgumentException("Salary type is required.");
        }
        this.salaryType = salaryType;
    }

    public void setBasicSalary(Double basicSalary) {
        if (basicSalary != null && basicSalary < 0) {
            throw new IllegalArgumentException("Basic salary cannot be negative.");
        }
        this.basicSalary = basicSalary;
    }

    public void setHourlyRate(Double hourlyRate) {
        if (hourlyRate != null && hourlyRate < 0) {
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

    // validates the salary type values
    private void validateSalaryType() {
        if (salaryType == SalaryType.MONTHLY && (basicSalary == null || basicSalary <= 0)) {
            throw new IllegalArgumentException("Monthly employees must have a basic salary.");
        }

        if (salaryType == SalaryType.HOURLY && (hourlyRate == null || hourlyRate <= 0)) {
            throw new IllegalArgumentException("Hourly employees must have an hourly rate.");
        }
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
                + ", salaryType=" + salaryType
                + ", basicSalary=" + basicSalary
                + ", hourlyRate=" + hourlyRate
                + ", startDate=" + startDate
                + ", endDate=" + endDate
                + '}';
    }
}