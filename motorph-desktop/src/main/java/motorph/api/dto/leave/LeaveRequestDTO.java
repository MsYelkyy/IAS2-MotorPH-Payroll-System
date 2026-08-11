package motorph.api.dto.leave;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import motorph.domain.value.RequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Wire copy of the backend's LeaveRequest domain model (flattened - the
 * Request/LeaveRequest inheritance split was for server-side validation, not
 * needed on the wire). Mirrors the old motorph.domain.model.LeaveRequest and
 * its Request base class getter names.
 */
public class LeaveRequestDTO {

    private final Integer requestId;
    private final int employeeId;
    private final LocalDateTime requestDate;
    private final RequestStatus status;
    private final Integer approverAccountId;
    private final String remarks;
    private final LocalDateTime dateReviewed;
    private final int leaveTypeId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final double leaveHours;
    private final String reason;

    @JsonCreator
    public LeaveRequestDTO(@JsonProperty("requestId") Integer requestId,
                           @JsonProperty("employeeId") int employeeId,
                           @JsonProperty("requestDate") LocalDateTime requestDate,
                           @JsonProperty("status") RequestStatus status,
                           @JsonProperty("approverAccountId") Integer approverAccountId,
                           @JsonProperty("remarks") String remarks,
                           @JsonProperty("dateReviewed") LocalDateTime dateReviewed,
                           @JsonProperty("leaveTypeId") int leaveTypeId,
                           @JsonProperty("startDate") LocalDate startDate,
                           @JsonProperty("endDate") LocalDate endDate,
                           @JsonProperty("leaveHours") double leaveHours,
                           @JsonProperty("reason") String reason) {
        this.requestId = requestId;
        this.employeeId = employeeId;
        this.requestDate = requestDate;
        this.status = status;
        this.approverAccountId = approverAccountId;
        this.remarks = remarks;
        this.dateReviewed = dateReviewed;
        this.leaveTypeId = leaveTypeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.leaveHours = leaveHours;
        this.reason = reason;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Integer getApproverAccountId() {
        return approverAccountId;
    }

    public String getRemarks() {
        return remarks;
    }

    public LocalDateTime getDateReviewed() {
        return dateReviewed;
    }

    public int getLeaveTypeId() {
        return leaveTypeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public double getLeaveHours() {
        return leaveHours;
    }

    public String getReason() {
        return reason;
    }

    public boolean isSingleDay() {
        return startDate.equals(endDate);
    }

    public boolean isMultipleDays() {
        return endDate.isAfter(startDate);
    }

    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    public boolean isApproved() {
        return status == RequestStatus.APPROVED;
    }

    public boolean isRejected() {
        return status == RequestStatus.REJECTED;
    }

    public boolean isCancelled() {
        return status == RequestStatus.CANCELLED;
    }
}
