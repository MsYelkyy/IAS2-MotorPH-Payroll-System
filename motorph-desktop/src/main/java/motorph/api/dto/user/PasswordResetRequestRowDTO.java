package motorph.api.dto.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import motorph.domain.value.PasswordResetStatus;

import java.time.LocalDateTime;

/**
 * Wire copy of the backend's PasswordResetRequestRowDTO, keeping the getter
 * names the IT password reset table already binds to.
 */
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

    @JsonCreator
    public PasswordResetRequestRowDTO(@JsonProperty("resetRequestId") int resetRequestId,
                                      @JsonProperty("userAccountId") int userAccountId,
                                      @JsonProperty("employeeName") String employeeName,
                                      @JsonProperty("companyEmail") String companyEmail,
                                      @JsonProperty("status") PasswordResetStatus status,
                                      @JsonProperty("requestedAt") LocalDateTime requestedAt,
                                      @JsonProperty("resolvedBy") String resolvedBy,
                                      @JsonProperty("completedAt") LocalDateTime completedAt,
                                      @JsonProperty("cancelledAt") LocalDateTime cancelledAt) {
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
