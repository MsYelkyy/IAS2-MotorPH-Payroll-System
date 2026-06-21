package motorph.application.dto.it;

import motorph.domain.value.PasswordResetStatus;

import java.time.LocalDateTime;

/** Carries password reset request details for IT screens. */
public class PasswordResetRequestRowDTO {

    private final int resetRequestId;
    private final int userAccountId;
    private final String employeeName;
    private final String companyEmail;
    private final PasswordResetStatus status;
    private final LocalDateTime requestedAt;
    private final String resolvedBy;
    private final LocalDateTime completedAt;
    private final LocalDateTime cancelledAt;

    // constructors
    public PasswordResetRequestRowDTO(
            int resetRequestId,
            int userAccountId,
            String employeeName,
            String companyEmail,
            PasswordResetStatus status,
            LocalDateTime requestedAt,
            String resolvedBy,
            LocalDateTime completedAt,
            LocalDateTime cancelledAt) {

        this.resetRequestId = resetRequestId;
        this.userAccountId = userAccountId;
        this.employeeName = employeeName;
        this.companyEmail = companyEmail;
        this.status = status;
        this.requestedAt = requestedAt;
        this.resolvedBy = resolvedBy;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
    }

    // getters
    public int getResetRequestId() {
        return resetRequestId;
    }

    public int getUserAccountId() {
        return userAccountId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public PasswordResetStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}