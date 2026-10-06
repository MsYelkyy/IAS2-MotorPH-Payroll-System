package motorph.backend.exception;

/**
 * Represents a rejected authenticator (TOTP) code: wrong, out of date, or too many attempts.
 */
public class OtpVerificationException extends BusinessRuleException {

    public enum Reason {
        INVALID_CODE,
        EXPIRED_OR_UNAVAILABLE,
        ATTEMPT_LIMIT_REACHED
    }

    private final Reason reason;

    public OtpVerificationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
