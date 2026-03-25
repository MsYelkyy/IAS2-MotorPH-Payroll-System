package motorph.domain.model;

import java.util.Date;

import motorph.domain.value.LeaveStatus;

/**
 * Represents a leave request with date range and reason.
 */
public class LeaveRequest extends Request {

    private final Date from;
    private final Date to;
    private final String reason;
    private final String leaveType;

    // constructors
    public LeaveRequest(String requestId, int employeeId, Date requestDate,
                        Date from, Date to, String reason, String leaveType) {

        this(requestId, employeeId, requestDate,
                from, to, reason, leaveType,
                LeaveStatus.PENDING, null, null);
    }

    // constructors
    public LeaveRequest(String requestId, int employeeId, Date requestDate,
                        Date from, Date to, String reason, String leaveType,
                        LeaveStatus status, String reviewedBy, Date dateReviewed) {

        super(requestId, employeeId, requestDate, status, reviewedBy, dateReviewed);

        this.from = copyDate(requireNonNull(from, "From date is required"));
        this.to = copyDate(requireNonNull(to, "To date is required"));

        if (this.from.after(this.to)) {
            throw new IllegalArgumentException("From date cannot be after To date.");
        }

        this.reason = requireNonBlank(reason, "Reason is required");
        this.leaveType = requireNonBlank(leaveType, "Leave type is required");
    }

    // getters
    public Date getFromDate() {
        return copyDate(from);
    }

    public Date getToDate() {
        return copyDate(to);
    }

    public String getReason() {
        return reason;
    }

    public String getLeaveType() {
        return leaveType;
    }
}