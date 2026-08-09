package motorph.api.dto.systemconfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/** Wire copy of the backend's WithholdingTaxBracketDTO. */
public class WithholdingTaxBracketDTO {
    private final int    taxBracketId;
    private final int    deductionTypeId;
    private final double minSalary;
    private final double maxSalary;
    private final double baseTax;
    private final double taxRate;
    private final double excessOver;
    private final LocalDate effectiveStartDate;
    private final LocalDate effectiveEndDate;

    @JsonCreator
    public WithholdingTaxBracketDTO(@JsonProperty("taxBracketId") int taxBracketId,
                                    @JsonProperty("deductionTypeId") int deductionTypeId,
                                    @JsonProperty("minSalary") double minSalary,
                                    @JsonProperty("maxSalary") double maxSalary,
                                    @JsonProperty("baseTax") double baseTax,
                                    @JsonProperty("taxRate") double taxRate,
                                    @JsonProperty("excessOver") double excessOver,
                                    @JsonProperty("effectiveStartDate") LocalDate effectiveStartDate,
                                    @JsonProperty("effectiveEndDate") LocalDate effectiveEndDate) {
        this.taxBracketId       = taxBracketId;
        this.deductionTypeId    = deductionTypeId;
        this.minSalary          = minSalary;
        this.maxSalary          = maxSalary;
        this.baseTax            = baseTax;
        this.taxRate            = taxRate;
        this.excessOver         = excessOver;
        this.effectiveStartDate = effectiveStartDate;
        this.effectiveEndDate   = effectiveEndDate;
    }

    public int    getTaxBracketId()      { return taxBracketId; }
    public int    getDeductionTypeId()   { return deductionTypeId; }
    public double getMinSalary()         { return minSalary; }
    public double getMaxSalary()         { return maxSalary; }
    public double getBaseTax()           { return baseTax; }
    public double getTaxRate()           { return taxRate; }
    public double getExcessOver()        { return excessOver; }
    public LocalDate getEffectiveStartDate() { return effectiveStartDate; }
    public LocalDate getEffectiveEndDate()   { return effectiveEndDate; }
}
