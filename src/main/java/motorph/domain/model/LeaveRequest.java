package motorph.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import motorph.domain.value.LeaveStatus;

/**
 * Represents a leave request in the system.
 */
public class LeaveRequest extends Request {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final double leaveHours;
    private final String reason;
    private final int leaveTypeId;

    // constructors
    public LeaveRequest(
            Integer requestId,
            int employeeId,
            LocalDateTime requestDate,
            LocalDate startDate,
            LocalDate endDate,
            double leaveHours,
            String reason,
            int leaveTypeId) {

        this(
                requestId,
                employeeId,
                requestDate,
                startDate,
                endDate,
                leaveHours,
                reason,
                leaveTypeId,
                LeaveStatus.PENDING,
                null,
                null,
                null
        );
    }

    // constructors
    public LeaveRequest(
            Integer requestId,
            int employeeId,
            LocalDateTime requestDate,
            LocalDate startDate,
            LocalDate endDate,
            double leaveHours,
            String reason,
            int leaveTypeId,
            LeaveStatus status,
            Integer approverAccountId,
            String remarks,
            LocalDateTime dateReviewed) {

        super(
                requestId,
                employeeId,
                requestDate,
                status,
                approverAccountId,
                remarks,
                dateReviewed
        );

        this.startDate = requireNonNull(startDate, "Start date is required.");
        this.endDate = requireNonNull(endDate, "End date is required.");
        validateDateRange(this.startDate, this.endDate);
        this.leaveHours = validateLeaveHours(leaveHours);
        this.reason = requireNonBlank(reason, "Reason is required.");
        this.leaveTypeId = validatePositive(leaveTypeId, "Leave type ID must be positive.");
    }

    // getters
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

    public int getLeaveTypeId() {
        return leaveTypeId;
    }

    // validates the leave date range
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
    }

    // validates leave hours
    private double validateLeaveHours(double leaveHours) {
        if (leaveHours < 0) {
            throw new IllegalArgumentException("Leave hours cannot be negative.");
        }
        return leaveHours;
    }
}