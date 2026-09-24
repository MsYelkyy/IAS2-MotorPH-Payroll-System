package motorph.backend.dto.payroll;

import motorph.backend.domain.value.PayrollType;

import java.time.LocalDate;

/**
 * Carries one pending payrun's summary for the pending payroll landing tab.
 * A pending payrun has no generated payroll results yet.
 */
public class PendingPayrunSummaryDTO {

    private final int payrunId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDate payDate;
    private final PayrollType payrollType;
    private final int totalEmployees;
    private final int readyEmployees;
    private final int incompleteEmployees;
    private final int noAttendanceEmployees;

    public PendingPayrunSummaryDTO(int payrunId,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   LocalDate payDate,
                                   PayrollType payrollType,
                                   int totalEmployees,
                                   int readyEmployees,
                                   int incompleteEmployees,
                                   int noAttendanceEmployees) {
        this.payrunId = payrunId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.payDate = payDate;
        this.payrollType = payrollType;
        this.totalEmployees = totalEmployees;
        this.readyEmployees = readyEmployees;
        this.incompleteEmployees = incompleteEmployees;
        this.noAttendanceEmployees = noAttendanceEmployees;
    }

    public int getPayrunId() {
        return payrunId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public PayrollType getPayrollType() {
        return payrollType;
    }

    public int getTotalEmployees() {
        return totalEmployees;
    }

    public int getReadyEmployees() {
        return readyEmployees;
    }

    public int getIncompleteEmployees() {
        return incompleteEmployees;
    }

    public int getNoAttendanceEmployees() {
        return noAttendanceEmployees;
    }
}
