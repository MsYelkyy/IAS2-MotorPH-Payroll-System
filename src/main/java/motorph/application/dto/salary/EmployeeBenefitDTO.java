package motorph.application.dto.salary;

import java.time.LocalDate;

public class EmployeeBenefitDTO {

    private final int       employeeId;
    private final int       benefitTypeId;  // 1=Rice Subsidy, 2=Phone Allowance, 3=Clothing Allowance
    private final double    amount;
    private final LocalDate startDate;
    private final LocalDate endDate;        // null = open-ended

    public EmployeeBenefitDTO(int employeeId, int benefitTypeId, double amount,
                              LocalDate startDate, LocalDate endDate) {
        this.employeeId    = employeeId;
        this.benefitTypeId = benefitTypeId;
        this.amount        = amount;
        this.startDate     = startDate;
        this.endDate       = endDate;
    }

    public int       getEmployeeId()    { return employeeId; }
    public int       getBenefitTypeId() { return benefitTypeId; }
    public double    getAmount()        { return amount; }
    public LocalDate getStartDate()     { return startDate; }
    public LocalDate getEndDate()       { return endDate; }
}
