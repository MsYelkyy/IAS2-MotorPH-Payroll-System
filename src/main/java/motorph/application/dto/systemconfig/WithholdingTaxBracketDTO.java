package motorph.application.dto.systemconfig;

import java.time.LocalDate;

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

    public WithholdingTaxBracketDTO(int taxBracketId, int deductionTypeId,
                                    double minSalary, double maxSalary,
                                    double baseTax, double taxRate, double excessOver,
                                    LocalDate effectiveStartDate, LocalDate effectiveEndDate) {
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
