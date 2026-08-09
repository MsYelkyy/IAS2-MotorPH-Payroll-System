package motorph.backend.domain;

import java.time.LocalDateTime;
import java.util.Objects;

import motorph.backend.domain.contract.Approvable;
import motorph.backend.domain.value.RequestStatus;
import motorph.backend.domain.value.RequestType;

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

    public abstract RequestType getRequestType();

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

    @Override
    public void approve(int approverAccountId, String remarks) {
        this.status = RequestStatus.APPROVED;
        this.approverAccountId = validatePositive(approverAccountId, "Approver account ID must be positive.");
        this.remarks = clean(remarks);
        this.dateReviewed = LocalDateTime.now();
    }

    @Override
    public void reject(int approverAccountId, String remarks) {
        this.status = RequestStatus.REJECTED;
        this.approverAccountId = validatePositive(approverAccountId, "Approver account ID must be positive.");
        this.remarks = clean(remarks);
        this.dateReviewed = LocalDateTime.now();
    }

    public void cancel() {
        cancel(null);
    }

    public void cancel(String reason) {
        this.status = RequestStatus.CANCELLED;
        this.remarks = clean(reason);
        this.dateReviewed = LocalDateTime.now();
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

    protected int validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    protected Integer validateNullableId(Integer value, String message) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    protected static String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    protected static <T> T requireNonNull(T value, String message) {
        return Objects.requireNonNull(value, message);
    }

    protected static String clean(String value) {
        return value == null ? null : value.trim();
    }
}
