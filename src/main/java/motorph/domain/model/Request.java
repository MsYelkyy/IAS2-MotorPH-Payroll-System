package motorph.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import motorph.domain.contract.Approvable;
import motorph.domain.value.RequestStatus;
import motorph.domain.value.RequestType;

/**
 * Represents a base employee request with approval details.
 */
public abstract class Request implements Approvable {

    private final Integer requestId;
    private final int employeeId;
    private final LocalDateTime requestDate;

    private RequestStatus status;
    private Integer approverAccountId;
    private String remarks;
    private LocalDateTime dateReviewed;

    // constructors
    protected Request(
            Integer requestId,
            int employeeId,
            LocalDateTime requestDate,
            RequestStatus status,
            Integer approverAccountId,
            String remarks,
            LocalDateTime dateReviewed) {

        this.requestId = validateNullableId(requestId, "Request ID must be positive.");
        this.employeeId = validatePositive(employeeId, "Employee ID must be positive.");
        this.requestDate = requireNonNull(requestDate, "Request date is required.");
        this.status = requireNonNull(status, "Status is required.");
        this.approverAccountId = validateNullableId(approverAccountId, "Approver account ID must be positive.");
        this.remarks = clean(remarks);
        this.dateReviewed = dateReviewed;
    }

    // returns the specific type of this request
    public abstract RequestType getRequestType();

    // getters
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

    // approves the request
    @Override
    public void approve(int approverAccountId, String remarks) {
        this.status = RequestStatus.APPROVED;
        this.approverAccountId = validatePositive(approverAccountId, "Approver account ID must be positive.");
        this.remarks = clean(remarks);
        this.dateReviewed = LocalDateTime.now();
    }

    // rejects the request
    @Override
    public void reject(int approverAccountId, String remarks) {
        this.status = RequestStatus.REJECTED;
        this.approverAccountId = validatePositive(approverAccountId, "Approver account ID must be positive.");
        this.remarks = clean(remarks);
        this.dateReviewed = LocalDateTime.now();
    }

    // cancels the request
    public void cancel() {
        cancel(null);
    }

    // overload: cancels the request and records a reason
    public void cancel(String reason) {
        this.status = RequestStatus.CANCELLED;
        this.remarks = clean(reason);
        this.dateReviewed = LocalDateTime.now();
    }

    // checks if the request is pending
    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    // checks if the request is approved
    public boolean isApproved() {
        return status == RequestStatus.APPROVED;
    }

    // checks if the request is rejected
    public boolean isRejected() {
        return status == RequestStatus.REJECTED;
    }

    // checks if the request is cancelled
    public boolean isCancelled() {
        return status == RequestStatus.CANCELLED;
    }

    // validates a required positive number
    protected int validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // validates an optional positive number
    protected Integer validateNullableId(Integer value, String message) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // validates required text
    protected static String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    // validates required object
    protected static <T> T requireNonNull(T value, String message) {
        return Objects.requireNonNull(value, message);
    }

    // cleans optional text
    protected static String clean(String value) {
        return value == null ? null : value.trim();
    }
}