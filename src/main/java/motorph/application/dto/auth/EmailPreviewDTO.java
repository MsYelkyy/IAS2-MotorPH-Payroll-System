package motorph.application.dto.auth;

import java.time.LocalDateTime;

/**
 * Stores email details for QA email preview display.
 */
public class EmailPreviewDTO {

    private final EmailPreviewType type;
    private final String from;
    private final String to;
    private final String subject;
    private final String body;
    private final String highlightLabel;
    private final String highlightValue;
    private final LocalDateTime sentAt;

    // constructors
    public EmailPreviewDTO(String from,
                           String to,
                           String subject,
                           String body,
                           LocalDateTime sentAt) {
        this(EmailPreviewType.LOGIN_OTP, from, to, subject, body, null, null, sentAt);
    }

    public EmailPreviewDTO(EmailPreviewType type,
                           String from,
                           String to,
                           String subject,
                           String body,
                           String highlightLabel,
                           String highlightValue,
                           LocalDateTime sentAt) {

        this.type = type == null ? EmailPreviewType.LOGIN_OTP : type;
        this.from = cleanRequired(from, "Sender email is required.");
        this.to = cleanRequired(to, "Recipient email is required.");
        this.subject = cleanRequired(subject, "Email subject is required.");
        this.body = cleanRequired(body, "Email body is required.");
        this.highlightLabel = cleanOptional(highlightLabel);
        this.highlightValue = cleanOptional(highlightValue);
        this.sentAt = sentAt == null ? LocalDateTime.now() : sentAt;
    }

    // getters
    public EmailPreviewType getType() {
        return type;
    }

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

    public String getHighlightLabel() {
        return highlightLabel;
    }

    public String getHighlightValue() {
        return highlightValue;
    }

    public boolean hasHighlight() {
        return highlightLabel != null && highlightValue != null;
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

    private String cleanOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}
