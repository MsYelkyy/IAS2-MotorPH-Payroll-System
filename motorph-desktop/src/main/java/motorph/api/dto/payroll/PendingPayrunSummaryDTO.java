package motorph.api.dto.payroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import motorph.domain.value.PayrollType;

import java.time.LocalDate;

/** Wire copy of the backend's payroll.PendingPayrunSummaryDTO. */
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

    @JsonCreator
    public PendingPayrunSummaryDTO(@JsonProperty("payrunId") int payrunId,
                                   @JsonProperty("startDate") LocalDate startDate,
                                   @JsonProperty("endDate") LocalDate endDate,
                                   @JsonProperty("payDate") LocalDate payDate,
                                   @JsonProperty("payrollType") PayrollType payrollType,
                                   @JsonProperty("totalEmployees") int totalEmployees,
                                   @JsonProperty("readyEmployees") int readyEmployees,
                                   @JsonProperty("incompleteEmployees") int incompleteEmployees,
                                   @JsonProperty("noAttendanceEmployees") int noAttendanceEmployees) {
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
