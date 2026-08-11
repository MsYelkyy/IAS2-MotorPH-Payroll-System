package motorph.api.dto.salary;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import motorph.domain.value.SalaryType;

import java.time.LocalDate;

/**
 * Wire copy of the backend's SalaryHistory domain model, mirroring the old
 * motorph.domain.model.SalaryHistory getter names.
 */
public class SalaryHistoryDTO {

    private final Integer salaryHistoryId;
    private final int employeeId;
    private final SalaryType salaryType;
    private final Double basicSalary;
    private final Double hourlyRate;
    private final LocalDate startDate;
    private final LocalDate endDate;

    @JsonCreator
    public SalaryHistoryDTO(@JsonProperty("salaryHistoryId") Integer salaryHistoryId,
                            @JsonProperty("employeeId") int employeeId,
                            @JsonProperty("salaryType") SalaryType salaryType,
                            @JsonProperty("basicSalary") Double basicSalary,
                            @JsonProperty("hourlyRate") Double hourlyRate,
                            @JsonProperty("startDate") LocalDate startDate,
                            @JsonProperty("endDate") LocalDate endDate) {
        this.salaryHistoryId = salaryHistoryId;
        this.employeeId = employeeId;
        this.salaryType = salaryType;
        this.basicSalary = basicSalary;
        this.hourlyRate = hourlyRate;
        this.startDate = startDate;
        this.endDate = endDate;
    }

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

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public double getGrossSemiMonthly() {
        if (salaryType != SalaryType.MONTHLY || basicSalary == null) {
            return 0.0;
        }
        return basicSalary / 2;
    }

    public boolean isEffectiveOn(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        boolean startsOnOrBefore = !startDate.isAfter(date);
        boolean hasNoEndDate = endDate == null;
        boolean endsOnOrAfter = hasNoEndDate || !endDate.isBefore(date);
        return startsOnOrBefore && endsOnOrAfter;
    }

    public boolean isCurrent() {
        return isEffectiveOn(LocalDate.now());
    }
}
