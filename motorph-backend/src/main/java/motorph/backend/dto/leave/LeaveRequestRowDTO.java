package motorph.backend.dto.leave;

import motorph.backend.domain.value.RequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Carries a flattened leave request record for display in the admin leave table.
 */
public class LeaveRequestRowDTO {

    private final int leaveRequestId;
    private final int employeeId;
    private final String employeeFullName;
    private final String leaveTypeName;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final double leaveHours;
    private final String reason;
    private final RequestStatus status;
    private final LocalDateTime dateSubmitted;
    private final Integer approverAccountId;
    private final String remarks;
    private final LocalDateTime dateReviewed;

    public LeaveRequestRowDTO(
            int leaveRequestId,
            int employeeId,
            String employeeFullName,
            String leaveTypeName,
            LocalDate startDate,
            LocalDate endDate,
            double leaveHours,
            String reason,
            RequestStatus status,
            LocalDateTime dateSubmitted,
            Integer approverAccountId,
            String remarks,
            LocalDateTime dateReviewed) {

        this.leaveRequestId   = leaveRequestId;
        this.employeeId       = employeeId;
        this.employeeFullName = employeeFullName;
        this.leaveTypeName    = leaveTypeName;
        this.startDate        = startDate;
        this.endDate          = endDate;
        this.leaveHours       = leaveHours;
        this.reason           = reason;
        this.status           = status;
        this.dateSubmitted    = dateSubmitted;
        this.approverAccountId = approverAccountId;
        this.remarks          = remarks;
        this.dateReviewed     = dateReviewed;
    }

    public int getLeaveRequestId()      { return leaveRequestId; }
    public int getEmployeeId()          { return employeeId; }
    public String getEmployeeFullName() { return employeeFullName; }
    public String getLeaveTypeName()    { return leaveTypeName; }
    public LocalDate getStartDate()     { return startDate; }
    public LocalDate getEndDate()       { return endDate; }
    public double getLeaveHours()       { return leaveHours; }
    public String getReason()           { return reason; }
    public RequestStatus getStatus()    { return status; }
    public LocalDateTime getDateSubmitted()  { return dateSubmitted; }
    public Integer getApproverAccountId()    { return approverAccountId; }
    public String getRemarks()               { return remarks; }
    public LocalDateTime getDateReviewed()   { return dateReviewed; }
}
