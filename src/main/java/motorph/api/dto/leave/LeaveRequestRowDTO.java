package motorph.api.dto.leave;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import motorph.domain.value.RequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Wire copy of the backend's LeaveRequestRowDTO for the admin leave table. */
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

    @JsonCreator
    public LeaveRequestRowDTO(@JsonProperty("leaveRequestId") int leaveRequestId,
                              @JsonProperty("employeeId") int employeeId,
                              @JsonProperty("employeeFullName") String employeeFullName,
                              @JsonProperty("leaveTypeName") String leaveTypeName,
                              @JsonProperty("startDate") LocalDate startDate,
                              @JsonProperty("endDate") LocalDate endDate,
                              @JsonProperty("leaveHours") double leaveHours,
                              @JsonProperty("reason") String reason,
                              @JsonProperty("status") RequestStatus status,
                              @JsonProperty("dateSubmitted") LocalDateTime dateSubmitted,
                              @JsonProperty("approverAccountId") Integer approverAccountId,
                              @JsonProperty("remarks") String remarks,
                              @JsonProperty("dateReviewed") LocalDateTime dateReviewed) {
        this.leaveRequestId = leaveRequestId;
        this.employeeId = employeeId;
        this.employeeFullName = employeeFullName;
        this.leaveTypeName = leaveTypeName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.leaveHours = leaveHours;
        this.reason = reason;
        this.status = status;
        this.dateSubmitted = dateSubmitted;
        this.approverAccountId = approverAccountId;
        this.remarks = remarks;
        this.dateReviewed = dateReviewed;
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
