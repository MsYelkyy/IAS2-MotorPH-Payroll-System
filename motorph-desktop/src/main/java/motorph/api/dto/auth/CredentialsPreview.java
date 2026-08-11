package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Dev-only stand-in for a sent email, present on EmployeeCreateResponse only
 * when the backend has no real SMTP integration wired up (see the backend's
 * EmployeeCredentialsDeliveryService). Mirrors OtpPreview's role for login.
 */
public record CredentialsPreview(
        String senderEmail,
        String recipientEmail,
        String subject,
        String body,
        String companyEmail,
        String temporaryPassword
) {

    @JsonCreator
    public CredentialsPreview(@JsonProperty("senderEmail") String senderEmail,
                              @JsonProperty("recipientEmail") String recipientEmail,
                              @JsonProperty("subject") String subject,
                              @JsonProperty("body") String body,
                              @JsonProperty("companyEmail") String companyEmail,
                              @JsonProperty("temporaryPassword") String temporaryPassword) {
        this.senderEmail = senderEmail;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.body = body;
        this.companyEmail = companyEmail;
        this.temporaryPassword = temporaryPassword;
    }
}
