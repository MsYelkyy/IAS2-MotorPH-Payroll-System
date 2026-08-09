package motorph.backend.service;

import motorph.backend.dto.auth.OtpPreview;

/**
 * Internal, non-wire result of a password-verification attempt. AuthController
 * maps this into the public {@code LoginResponse} - this type never leaves the
 * backend, so it is free to carry the userAccountId that the wire contract must not.
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
