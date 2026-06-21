package motorph.application.dto.payroll;

import motorph.domain.value.PayrollType;
import java.time.LocalDate;

public class PayrunCreateDTO {

    private final LocalDate   startDate;
    private final LocalDate   endDate;
    private final LocalDate   payDate;
    private final PayrollType payrollType;

    public PayrunCreateDTO(LocalDate startDate, LocalDate endDate,
                           LocalDate payDate, PayrollType payrollType) {
        this.startDate   = startDate;
        this.endDate     = endDate;
        this.payDate     = payDate;
        this.payrollType = payrollType;
    }

    public LocalDate   getStartDate()     { return startDate; }
    public LocalDate   getEndDate()       { return endDate; }
    public LocalDate   getPayDate()       { return payDate; }
    public PayrollType getPayrollType()   { return payrollType; }
}
