package motorph.api.dto.salary;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * Client-side copy of the backend's {@code EmployeeBenefit}.
 */
public class EmployeeBenefitDTO {

    private final Integer employeeBenefitId;
    private final int employeeId;
    private final int benefitTypeId;
    private final double amount;
    private final LocalDate effectiveStartDate;
    private final LocalDate effectiveEndDate;

    @JsonCreator
    public EmployeeBenefitDTO(@JsonProperty("employeeBenefitId") Integer employeeBenefitId,
                              @JsonProperty("employeeId") int employeeId,
                              @JsonProperty("benefitTypeId") int benefitTypeId,
                              @JsonProperty("amount") double amount,
                              @JsonProperty("effectiveStartDate") LocalDate effectiveStartDate,
                              @JsonProperty("effectiveEndDate") LocalDate effectiveEndDate) {
        this.employeeBenefitId = employeeBenefitId;
        this.employeeId = employeeId;
        this.benefitTypeId = benefitTypeId;
        this.amount = amount;
        this.effectiveStartDate = effectiveStartDate;
        this.effectiveEndDate = effectiveEndDate;
    }

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

    public boolean isEffectiveOn(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        boolean startsOnOrBefore = !effectiveStartDate.isAfter(date);
        boolean hasNoEndDate = effectiveEndDate == null;
        boolean endsOnOrAfter = hasNoEndDate || !effectiveEndDate.isBefore(date);
        return startsOnOrBefore && endsOnOrAfter;
    }

    public boolean isCurrent() {
        return isEffectiveOn(LocalDate.now());
    }
}
