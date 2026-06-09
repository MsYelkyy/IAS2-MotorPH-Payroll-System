package motorph.application.service;

import motorph.application.dto.auth.EmailPreviewDTO;

/**
 * Stores the latest email preview for QA testing.
 */
public class EmailPreviewService {

    private static EmailPreviewDTO latestEmail;

    // stores the latest email preview
    public void savePreview(EmailPreviewDTO emailPreview) {
        if (emailPreview == null) {
            throw new IllegalArgumentException("Email preview is required.");
        }

        latestEmail = emailPreview;
    }

    // returns the latest email preview
    public EmailPreviewDTO getLatestPreview() {
        return latestEmail;
    }

    // checks if a preview exists
    public boolean hasPreview() {
        return latestEmail != null;
    }

    // clears the stored preview
    public void clearPreview() {
        latestEmail = null;
    }
}