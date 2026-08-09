package motorph.api.dto.systemconfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/** Wire copy of the backend's PhilHealthRateDTO. */
public class PhilHealthRateDTO {
    private final int    philhealthRateId;
    private final int    deductionTypeId;
    private final double premiumRate;
    private final double salaryFloor;
    private final double salaryCeiling;
    private final double employeeShareRatio;
    private final double employerShareRatio;
    private final LocalDate effectiveStartDate;
    private final LocalDate effectiveEndDate;

    @JsonCreator
    public PhilHealthRateDTO(@JsonProperty("philhealthRateId") int philhealthRateId,
                              @JsonProperty("deductionTypeId") int deductionTypeId,
                              @JsonProperty("premiumRate") double premiumRate,
                              @JsonProperty("salaryFloor") double salaryFloor,
                              @JsonProperty("salaryCeiling") double salaryCeiling,
                              @JsonProperty("employeeShareRatio") double employeeShareRatio,
                              @JsonProperty("employerShareRatio") double employerShareRatio,
                              @JsonProperty("effectiveStartDate") LocalDate effectiveStartDate,
                              @JsonProperty("effectiveEndDate") LocalDate effectiveEndDate) {
        this.philhealthRateId    = philhealthRateId;
        this.deductionTypeId     = deductionTypeId;
        this.premiumRate         = premiumRate;
        this.salaryFloor         = salaryFloor;
        this.salaryCeiling       = salaryCeiling;
        this.employeeShareRatio  = employeeShareRatio;
        this.employerShareRatio  = employerShareRatio;
        this.effectiveStartDate  = effectiveStartDate;
        this.effectiveEndDate    = effectiveEndDate;
    }

    public int    getPhilhealthRateId()   { return philhealthRateId; }
    public int    getDeductionTypeId()    { return deductionTypeId; }
    public double getPremiumRate()        { return premiumRate; }
    public double getSalaryFloor()        { return salaryFloor; }
    public double getSalaryCeiling()      { return salaryCeiling; }
    public double getEmployeeShareRatio() { return employeeShareRatio; }
    public double getEmployerShareRatio() { return employerShareRatio; }
    public LocalDate getEffectiveStartDate() { return effectiveStartDate; }
    public LocalDate getEffectiveEndDate()   { return effectiveEndDate; }
}
