package motorph.domain.model;

import motorph.domain.contract.Approvable;
import motorph.domain.value.LeaveStatus;

import java.util.Date;
import java.util.Objects;

/**
 * Represents a base request with approval workflow.
 */
public abstract class Request implements Approvable {

    private final String requestId;
    private final int employeeId;
    private final Date requestDate;

    private LeaveStatus status;
    private String reviewedBy;
    private Date dateReviewed;

    // constructors
    protected Request(String requestId, int employeeId, Date requestDate,
                      LeaveStatus status, String reviewedBy, Date dateReviewed) {

        this.requestId = requireNonBlank(requestId, "Request ID is required");
        this.employeeId = validateEmployeeId(employeeId);
        this.requestDate = copyDate(requireNonNull(requestDate, "Request date is required"));

        this.status = requireNonNull(status, "Status is required");

        this.reviewedBy = (reviewedBy == null) ? null : requireNonBlank(reviewedBy, "ReviewedBy is required");
        this.dateReviewed = copyDate(dateReviewed);
    }

    // getters
    public String getRequestId() {
        return requestId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public Date getRequestDate() {
        return copyDate(requestDate);
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public Date getDateReviewed() {
        return copyDate(dateReviewed);
    }

    // approves request
    @Override
    public void approve(String reviewedBy) {
        this.status = LeaveStatus.APPROVED;
        this.reviewedBy = requireNonBlank(reviewedBy, "ReviewedBy is required");
        this.dateReviewed = new Date();
    }

    // rejects request
    @Override
    public void reject(String reviewedBy) {
        this.status = LeaveStatus.REJECTED;
        this.reviewedBy = requireNonBlank(reviewedBy, "ReviewedBy is required");
        this.dateReviewed = new Date();
    }

    // validates employee id
    protected int validateEmployeeId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }
        return id;
    }

    // validates string not blank
    protected static String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    // validates not null
    protected static <T> T requireNonNull(T value, String message) {
        return Objects.requireNonNull(value, message);
    }

    // copies date safely
    protected static Date copyDate(Date date) {
        return date == null ? null : new Date(date.getTime());
    }
}