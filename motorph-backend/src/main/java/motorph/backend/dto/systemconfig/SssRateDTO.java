package motorph.backend.dto.systemconfig;

import java.time.LocalDate;

public class SssRateDTO {
    private final int    sssRateId;
    private final int    deductionTypeId;
    private final double minSalary;
    private final double maxSalary;
    private final double employeeShare;
    private final double employerShare;
    private final LocalDate effectiveStartDate;
    private final LocalDate effectiveEndDate;

    public SssRateDTO(int sssRateId, int deductionTypeId,
                      double minSalary, double maxSalary,
                      double employeeShare, double employerShare,
                      LocalDate effectiveStartDate, LocalDate effectiveEndDate) {
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
