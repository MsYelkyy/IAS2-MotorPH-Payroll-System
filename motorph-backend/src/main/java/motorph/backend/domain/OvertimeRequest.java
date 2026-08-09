package motorph.backend.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import motorph.backend.domain.value.RequestStatus;
import motorph.backend.domain.value.RequestType;

/**
 * Represents an overtime request and maps to the overtime_request table.
 */
public class OvertimeRequest extends Request {

    private final int attendanceId;
    private final int overtimeTypeId;
    private final LocalDate overtimeDate;
    private final double requestedHours;
    private final String reason;

    public OvertimeRequest(
            Integer overtimeRequestId,
            int employeeId,
            int attendanceId,
            int overtimeTypeId,
            LocalDate overtimeDate,
            double requestedHours,
            String reason,
            LocalDateTime dateSubmitted) {

        this(
                overtimeRequestId,
                employeeId,
                attendanceId,
                overtimeTypeId,
                overtimeDate,
                requestedHours,
                reason,
                RequestStatus.PENDING,
                dateSubmitted,
                null,
                null,
                null
        );
    }

    public OvertimeRequest(
            Integer overtimeRequestId,
            int employeeId,
            int attendanceId,
            int overtimeTypeId,
            LocalDate overtimeDate,
            double requestedHours,
            String reason,
            RequestStatus status,
            LocalDateTime dateSubmitted,
            Integer approvedBy,
            String remarks,
            LocalDateTime dateReviewed) {

        super(
                overtimeRequestId,
                employeeId,
                dateSubmitted,
                status,
                approvedBy,
                remarks,
                dateReviewed
        );

        this.attendanceId = validatePositive(attendanceId, "Attendance ID must be positive.");
        this.overtimeTypeId = validatePositive(overtimeTypeId, "Overtime type ID must be positive.");
        this.overtimeDate = requireNonNull(overtimeDate, "Overtime date is required.");
        this.requestedHours = validateRequestedHours(requestedHours);
        this.reason = requireNonBlank(reason, "Reason is required.");
    }

    @Override
    public RequestType getRequestType() {
        return RequestType.OVERTIME;
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
        return getApproverAccountId() != null && getDateReviewed() != null;
    }

    private double validateRequestedHours(double requestedHours) {
        if (requestedHours <= 0) {
            throw new IllegalArgumentException("Requested overtime hours must be greater than zero.");
        }
        return requestedHours;
    }
}
