package motorph.backend.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import motorph.backend.domain.value.RequestStatus;
import motorph.backend.domain.value.RequestType;

/**
 * Represents a leave request and maps to the leave_request table.
 */
public class LeaveRequest extends Request {

    private final int leaveTypeId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final double leaveHours;
    private final String reason;

    public LeaveRequest(
            Integer leaveRequestId,
            int employeeId,
            int leaveTypeId,
            LocalDate startDate,
            LocalDate endDate,
            double leaveHours,
            String reason,
            LocalDateTime dateSubmitted) {

        this(
                leaveRequestId,
                employeeId,
                leaveTypeId,
                startDate,
                endDate,
                leaveHours,
                reason,
                RequestStatus.PENDING,
                dateSubmitted,
                null,
                null,
                null
        );
    }

    public LeaveRequest(
            Integer leaveRequestId,
            int employeeId,
            int leaveTypeId,
            LocalDate startDate,
            LocalDate endDate,
            double leaveHours,
            String reason,
            RequestStatus status,
            LocalDateTime dateSubmitted,
            Integer approvedBy,
            String remarks,
            LocalDateTime dateReviewed) {

        super(
                leaveRequestId,
                employeeId,
                dateSubmitted,
                status,
                approvedBy,
                remarks,
                dateReviewed
        );

        this.leaveTypeId = validatePositive(leaveTypeId, "Leave type ID must be positive.");
        this.startDate = requireNonNull(startDate, "Start date is required.");
        this.endDate = requireNonNull(endDate, "End date is required.");
        validateDateRange(this.startDate, this.endDate);
        this.leaveHours = validateLeaveHours(leaveHours);
        this.reason = requireNonBlank(reason, "Reason is required.");
    }

    @Override
    public RequestType getRequestType() {
        return RequestType.LEAVE;
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

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
    }

    private double validateLeaveHours(double leaveHours) {
        if (leaveHours <= 0) {
            throw new IllegalArgumentException("Leave hours must be greater than zero.");
        }
        return leaveHours;
    }
}
