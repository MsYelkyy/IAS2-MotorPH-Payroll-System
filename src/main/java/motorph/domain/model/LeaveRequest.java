package motorph.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import motorph.domain.value.RequestStatus;

/**
 * Represents a leave request and maps to the leave_request table.
 */
public class LeaveRequest extends Request {

    private final int leaveTypeId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final double leaveHours;
    private final String reason;

    // constructors
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

    // constructors
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

    // getters
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

    // checks if the leave request covers one day
    public boolean isSingleDay() {
        return startDate.equals(endDate);
    }

    // checks if the leave request covers multiple days
    public boolean isMultipleDays() {
        return endDate.isAfter(startDate);
    }

    // validates the leave date range
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
    }

    // validates leave hours
    private double validateLeaveHours(double leaveHours) {
        if (leaveHours <= 0) {
            throw new IllegalArgumentException("Leave hours must be greater than zero.");
        }
        return leaveHours;
    }

    // returns the leave request summary
    @Override
    public String toString() {
        return "LeaveRequest{"
                + "leaveRequestId=" + getRequestId()
                + ", employeeId=" + getEmployeeId()
                + ", leaveTypeId=" + leaveTypeId
                + ", startDate=" + startDate
                + ", endDate=" + endDate
                + ", leaveHours=" + leaveHours
                + ", status=" + getStatus()
                + ", approvedBy=" + getApproverAccountId()
                + '}';
    }
}