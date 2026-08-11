package motorph.api.dto.systemconfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/** Wire copy of the backend's SssRateDTO. */
public class SssRateDTO {
    private final int    sssRateId;
    private final int    deductionTypeId;
    private final double minSalary;
    private final double maxSalary;
    private final double employeeShare;
    private final double employerShare;
    private final LocalDate effectiveStartDate;
    private final LocalDate effectiveEndDate;

    @JsonCreator
    public SssRateDTO(@JsonProperty("sssRateId") int sssRateId,
                      @JsonProperty("deductionTypeId") int deductionTypeId,
                      @JsonProperty("minSalary") double minSalary,
                      @JsonProperty("maxSalary") double maxSalary,
                      @JsonProperty("employeeShare") double employeeShare,
                      @JsonProperty("employerShare") double employerShare,
                      @JsonProperty("effectiveStartDate") LocalDate effectiveStartDate,
                      @JsonProperty("effectiveEndDate") LocalDate effectiveEndDate) {
        this.sssRateId          = sssRateId;
        this.deductionTypeId    = deductionTypeId;
        this.minSalary          = minSalary;
        this.maxSalary          = maxSalary;
        this.employeeShare      = employeeShare;
        this.employerShare      = employerShare;
        this.effectiveStartDate = effectiveStartDate;
        this.effectiveEndDate   = effectiveEndDate;
    }

    public int    getSssRateId()        { return sssRateId; }
    public int    getDeductionTypeId()  { return deductionTypeId; }
    public double getMinSalary()        { return minSalary; }
    public double getMaxSalary()        { return maxSalary; }
    public double getEmployeeShare()    { return employeeShare; }
    public double getEmployerShare()    { return employerShare; }
    public LocalDate getEffectiveStartDate() { return effectiveStartDate; }
    public LocalDate getEffectiveEndDate()   { return effectiveEndDate; }
}
