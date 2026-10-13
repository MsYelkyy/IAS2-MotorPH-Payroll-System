package motorph.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Temporary email preview used only when real SMTP delivery is not configured.
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
