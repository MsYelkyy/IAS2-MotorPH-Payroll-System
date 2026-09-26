package motorph.api.dto.overtime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import motorph.domain.value.RequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Client-side copy of the backend's {@code OvertimeRequest}, flattened into one class. The
 * backend's {@code Request}/{@code OvertimeRequest} split exists for server-side validation and
 * is not needed on the client.
 */
public class OvertimeRequestDTO {

    private final Integer requestId;
    private final int employeeId;
    private final LocalDateTime requestDate;
    private final RequestStatus status;
    private final Integer approverAccountId;
    private final String remarks;
    private final LocalDateTime dateReviewed;
    private final int attendanceId;
    private final int overtimeTypeId;
    private final LocalDate overtimeDate;
    private final double requestedHours;
    private final String reason;

    @JsonCreator
    public OvertimeRequestDTO(@JsonProperty("requestId") Integer requestId,
                              @JsonProperty("employeeId") int employeeId,
                              @JsonProperty("requestDate") LocalDateTime requestDate,
                              @JsonProperty("status") RequestStatus status,
                              @JsonProperty("approverAccountId") Integer approverAccountId,
                              @JsonProperty("remarks") String remarks,
                              @JsonProperty("dateReviewed") LocalDateTime dateReviewed,
                              @JsonProperty("attendanceId") int attendanceId,
                              @JsonProperty("overtimeTypeId") int overtimeTypeId,
                              @JsonProperty("overtimeDate") LocalDate overtimeDate,
                              @JsonProperty("requestedHours") double requestedHours,
                              @JsonProperty("reason") String reason) {
        this.requestId = requestId;
        this.employeeId = employeeId;
        this.requestDate = requestDate;
        this.status = status;
        this.approverAccountId = approverAccountId;
        this.remarks = remarks;
        this.dateReviewed = dateReviewed;
        this.attendanceId = attendanceId;
        this.overtimeTypeId = overtimeTypeId;
        this.overtimeDate = overtimeDate;
        this.requestedHours = requestedHours;
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

    public int getAttendanceId() {
        return attendanceId;
    }

    public int getOvertimeTypeId() {
        return overtimeTypeId;
    }

    public LocalDate getOvertimeDate() {
        return overtimeDate;
    }

    public double getRequestedHours() {
        return requestedHours;
    }

    public String getReason() {
        return reason;
    }

    public boolean hasApprovalDetails() {
        return approverAccountId != null && dateReviewed != null;
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
