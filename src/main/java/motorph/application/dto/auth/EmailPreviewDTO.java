package motorph.application.dto.auth;

import java.time.LocalDateTime;

/**
 * Stores email details for QA email preview display.
 */
public class EmailPreviewDTO {

    private final String from;
    private final String to;
    private final String subject;
    private final String body;
    private final LocalDateTime sentAt;

    // constructors
    public EmailPreviewDTO(String from,
                           String to,
                           String subject,
                           String body,
                           LocalDateTime sentAt) {

        this.from = cleanRequired(from, "Sender email is required.");
        this.to = cleanRequired(to, "Recipient email is required.");
        this.subject = cleanRequired(subject, "Email subject is required.");
        this.body = cleanRequired(body, "Email body is required.");
        this.sentAt = sentAt == null ? LocalDateTime.now() : sentAt;
    }

    // getters
    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    // validates required text
    private String cleanRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}