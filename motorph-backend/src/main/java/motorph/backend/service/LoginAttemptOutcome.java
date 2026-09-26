package motorph.backend.service;

import motorph.backend.dto.auth.OtpPreview;

/**
 * Result of a password check, used only inside the backend. {@code AuthController} converts it into
 * the public {@code LoginResponse}, so it can safely carry the user account ID.
 */
public final class LoginAttemptOutcome {

    public enum Type {
        OTP_SENT,
        TEMPORARY_PASSWORD_CHANGE_REQUIRED
    }

    private final Type type;
    private final int userAccountId;
    private final String companyEmail;
    private final String message;
    private final OtpPreview otpPreview;

    public LoginAttemptOutcome(Type type, int userAccountId, String companyEmail, String message) {
        this(type, userAccountId, companyEmail, message, null);
    }

    public LoginAttemptOutcome(Type type, int userAccountId, String companyEmail, String message, OtpPreview otpPreview) {
        this.type = type;
        this.userAccountId = userAccountId;
        this.companyEmail = companyEmail;
        this.message = message;
        this.otpPreview = otpPreview;
    }

    public OtpPreview getOtpPreview() {
        return otpPreview;
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
