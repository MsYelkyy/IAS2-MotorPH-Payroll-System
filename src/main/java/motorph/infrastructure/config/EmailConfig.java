package motorph.infrastructure.config;

/**
 * Stores email sender configuration for preview-based demo delivery.
 */
public class EmailConfig {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String senderEmail;
    private final boolean authEnabled;
    private final boolean startTlsEnabled;

    // constructors
    public EmailConfig(String host,
                       int port,
                       String username,
                       String password,
                       String senderEmail,
                       boolean authEnabled,
                       boolean startTlsEnabled) {

        this.host = requireNonBlank(host, "SMTP host is required.");
        this.port = validatePort(port);
        this.username = requireNonBlank(username, "SMTP username is required.");
        this.password = requireNonBlank(password, "SMTP password is required.");
        this.senderEmail = requireNonBlank(senderEmail, "Sender email is required.");
        this.authEnabled = authEnabled;
        this.startTlsEnabled = startTlsEnabled;
    }

    // compatibility alias for old callers; demo builds now use in-app previews
    @Deprecated
    public static EmailConfig mailtrapSandbox() {
        return previewOnly();
    }

    // creates preview-only email configuration for in-app demo delivery
    public static EmailConfig previewOnly() {
        return new EmailConfig(
                "preview.local",
                25,
                "preview",
                "preview",
                "no-reply@motorph.com",
                false,
                false
        );
    }

    // getters
    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public boolean isStartTlsEnabled() {
        return startTlsEnabled;
    }

    // validates smtp port
    private int validatePort(int port) {
        if (port <= 0) {
            throw new IllegalArgumentException("SMTP port must be positive.");
        }

        return port;
    }

    // validates required text
    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}
