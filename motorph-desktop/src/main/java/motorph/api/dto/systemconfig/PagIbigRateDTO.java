package motorph.api.dto.systemconfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/** Wire copy of the backend's PagIbigRateDTO. */
public class PagIbigRateDTO {
    private final int    pagibigRateId;
    private final int    deductionTypeId;
    private final double minSalary;
    private final double maxSalary;
    private final double employeeShareRate;
    private final double employerShareRate;
    private final Double maxEmployeeShare;
    private final Double maxEmployerShare;
    private final LocalDate effectiveStartDate;
    private final LocalDate effectiveEndDate;

    @JsonCreator
    public PagIbigRateDTO(@JsonProperty("pagibigRateId") int pagibigRateId,
                          @JsonProperty("deductionTypeId") int deductionTypeId,
                          @JsonProperty("minSalary") double minSalary,
                          @JsonProperty("maxSalary") double maxSalary,
                          @JsonProperty("employeeShareRate") double employeeShareRate,
                          @JsonProperty("employerShareRate") double employerShareRate,
                          @JsonProperty("maxEmployeeShare") Double maxEmployeeShare,
                          @JsonProperty("maxEmployerShare") Double maxEmployerShare,
                          @JsonProperty("effectiveStartDate") LocalDate effectiveStartDate,
                          @JsonProperty("effectiveEndDate") LocalDate effectiveEndDate) {
        this.pagibigRateId      = pagibigRateId;
        this.deductionTypeId    = deductionTypeId;
        this.minSalary          = minSalary;
        this.maxSalary          = maxSalary;
        this.employeeShareRate  = employeeShareRate;
        this.employerShareRate  = employerShareRate;
        this.maxEmployeeShare   = maxEmployeeShare;
        this.maxEmployerShare   = maxEmployerShare;
        this.effectiveStartDate = effectiveStartDate;
        this.effectiveEndDate   = effectiveEndDate;
    }

    public int    getPagibigRateId()       { return pagibigRateId; }
    public int    getDeductionTypeId()     { return deductionTypeId; }
    public double getMinSalary()           { return minSalary; }
    public double getMaxSalary()           { return maxSalary; }
    public double getEmployeeShareRate()   { return employeeShareRate; }
    public double getEmployerShareRate()   { return employerShareRate; }
    public Double getMaxEmployeeShare()    { return maxEmployeeShare; }
    public Double getMaxEmployerShare()    { return maxEmployerShare; }
    public LocalDate getEffectiveStartDate() { return effectiveStartDate; }
    public LocalDate getEffectiveEndDate()   { return effectiveEndDate; }
}
