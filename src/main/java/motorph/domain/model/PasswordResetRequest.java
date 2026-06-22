package motorph.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;
import motorph.domain.value.PasswordResetStatus;

/**
 * Represents a password reset request and maps to the password_reset_request table.
 */
public class PasswordResetRequest {

    private Integer resetRequestId;
    private int userAccountId;
    private PasswordResetStatus requestStatus;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private Integer resolvedByEmployeeId;

    // constructors
    public PasswordResetRequest(Integer resetRequestId,
                                int userAccountId,
                                PasswordResetStatus requestStatus,
                                LocalDateTime requestedAt,
                                LocalDateTime completedAt,
                                LocalDateTime cancelledAt,
                                Integer resolvedByEmployeeId) {

        setResetRequestId(resetRequestId);
        setUserAccountId(userAccountId);
        setRequestStatus(requestStatus);
        setRequestedAt(requestedAt);
        setCompletedAt(completedAt);
        setCancelledAt(cancelledAt);
        setResolvedByEmployeeId(resolvedByEmployeeId);
    }

    public PasswordResetRequest(int userAccountId) {
        this(
                null,
                userAccountId,
                PasswordResetStatus.PENDING,
                LocalDateTime.now(),
                null,
                null,
                null
        );
    }

    // getters
    public Integer getResetRequestId() {
        return resetRequestId;
    }

    public int getUserAccountId() {
        return userAccountId;
    }

    public PasswordResetStatus getRequestStatus() {
        return requestStatus;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public Integer getResolvedByEmployeeId() {
        return resolvedByEmployeeId;
    }

    // checks if the request is pending
    public boolean isPending() {
        return requestStatus == PasswordResetStatus.PENDING;
    }

    // checks if the request is completed
    public boolean isCompleted() {
        return requestStatus == PasswordResetStatus.COMPLETED;
    }

    // checks if the request is expired
    public boolean isExpired() {
        return requestStatus == PasswordResetStatus.EXPIRED;
    }

    // checks if the request is cancelled
    public boolean isCancelled() {
        return requestStatus == PasswordResetStatus.CANCELLED;
    }

    // marks the request as completed
    public void markCompleted() {
        if (!isPending()) {
            throw new IllegalStateException("Only pending password reset requests can be completed.");
        }

        this.requestStatus = PasswordResetStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    // marks the request as expired
    public void markExpired() {
        if (!isPending()) {
            throw new IllegalStateException("Only pending password reset requests can be expired.");
        }

        this.requestStatus = PasswordResetStatus.EXPIRED;
    }

    // marks the request as cancelled
    public void markCancelled() {
        if (!isPending()) {
            throw new IllegalStateException("Only pending password reset requests can be cancelled.");
        }

        this.requestStatus = PasswordResetStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    // marks the request as manually resolved
    public void markManuallyResolved(int resolvedByEmployeeId) {
        setResolvedByEmployeeId(resolvedByEmployeeId);
        markCompleted();
    }

    // setters
    public void setResetRequestId(Integer resetRequestId) {
        if (resetRequestId != null && resetRequestId <= 0) {
            throw new IllegalArgumentException("Reset request ID must be positive.");
        }

        this.resetRequestId = resetRequestId;
    }

    public void setUserAccountId(int userAccountId) {
        if (userAccountId <= 0) {
            throw new IllegalArgumentException("User account ID must be positive.");
        }

        this.userAccountId = userAccountId;
    }

    public void setRequestStatus(PasswordResetStatus requestStatus) {
        this.requestStatus = Objects.requireNonNull(requestStatus, "Password reset status is required.");
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = Objects.requireNonNull(requestedAt, "Requested date and time is required.");
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public void setResolvedByEmployeeId(Integer resolvedByEmployeeId) {
        if (resolvedByEmployeeId != null && resolvedByEmployeeId <= 0) {
            throw new IllegalArgumentException("Resolved by employee ID must be positive.");
        }

        this.resolvedByEmployeeId = resolvedByEmployeeId;
    }

    // returns the password reset request summary
    @Override
    public String toString() {
        return "PasswordResetRequest{"
                + "resetRequestId=" + resetRequestId
                + ", userAccountId=" + userAccountId
                + ", requestStatus=" + requestStatus
                + ", requestedAt=" + requestedAt
                + ", completedAt=" + completedAt
                + ", cancelledAt=" + cancelledAt
                + ", resolvedByEmployeeId=" + resolvedByEmployeeId
                + '}';
    }
}