package motorph.backend.service;

/**
 * Result of a password check used only inside the backend.
 * {@code AuthController} converts it into a {@code LoginResponse} before sending the response to the client.
 * Since it stays inside the backend, it can include the user account ID.
 */
public final class LoginAttemptOutcome {

    public enum Type {
        TOTP_REQUIRED,
        TOTP_SETUP_REQUIRED,
        TEMPORARY_PASSWORD_CHANGE_REQUIRED
    }

    private final Type type;
    private final int userAccountId;
    private final String companyEmail;
    private final String message;

    public LoginAttemptOutcome(Type type, int userAccountId, String companyEmail, String message) {
        this.type = type;
        this.userAccountId = userAccountId;
        this.companyEmail = companyEmail;
        this.message = message;
    }

    public Type getType() {
        return type;
    }

    public int getUserAccountId() {
        return userAccountId;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public String getMessage() {
        return message;
    }
}
