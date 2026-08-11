package motorph.application.service;

import motorph.application.dto.auth.EmailPreviewDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Stores the latest email preview for QA testing.
 */
public class EmailPreviewService {

    private EmailPreviewDTO latestEmail;
    private final List<Consumer<EmailPreviewDTO>> listeners = new ArrayList<>();

    // stores the latest email preview
    public void savePreview(EmailPreviewDTO emailPreview) {
        if (emailPreview == null) {
            throw new IllegalArgumentException("Email preview is required.");
        }

        latestEmail = emailPreview;
        notifyListeners(emailPreview);
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

    // registers a listener that receives future email previews
    public void addListener(Consumer<EmailPreviewDTO> listener) {
        if (listener == null || listeners.contains(listener)) {
            return;
        }

        listeners.add(listener);
    }

    // removes a previously registered listener
    public void removeListener(Consumer<EmailPreviewDTO> listener) {
        listeners.remove(listener);
    }

    // notifies listeners without exposing the mutable listener collection
    private void notifyListeners(EmailPreviewDTO emailPreview) {
        for (Consumer<EmailPreviewDTO> listener : List.copyOf(listeners)) {
            listener.accept(emailPreview);
        }
    }
}
