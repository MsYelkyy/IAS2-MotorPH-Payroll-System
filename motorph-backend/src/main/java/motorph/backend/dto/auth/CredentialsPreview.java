package motorph.backend.dto.auth;

/**
 * Dev-only stand-in for a sent email, returned inline on the employee-create
 * response when no real SMTP integration exists yet (see
 * EmployeeCredentialsDeliveryService). Same rationale as OtpPreview.
 */
public record CredentialsPreview(
        String senderEmail,
        String recipientEmail,
        String subject,
        String body,
        String companyEmail,
        String temporaryPassword
) {
}
