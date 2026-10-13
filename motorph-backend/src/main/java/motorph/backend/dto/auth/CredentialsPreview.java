package motorph.backend.dto.auth;

/**
 * Development stand-in for an email containing login credentials. It is returned in the API
 * response only while preview mode is enabled, because the backend has no mail integration yet.
 */
public record CredentialsPreview(
        String senderEmail,
        String recipientEmail,
        String subject,
        String body,
        String companyEmail,
        String temporaryPassword
) {
    @Override
    public String toString() {
        return "CredentialsPreview[companyEmail=" + companyEmail + ", temporaryPassword=***, body=***]";
    }
}
