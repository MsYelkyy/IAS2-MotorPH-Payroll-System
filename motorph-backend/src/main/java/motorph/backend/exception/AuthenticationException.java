package motorph.backend.exception;

/**
 * Represents an error that occurs during user authentication.
 */
public class AuthenticationException extends RuntimeException {

    public enum Reason {
        ACCOUNT_LOCKED
    }

    private final Reason reason;

    public AuthenticationException(String message) {
        super(message);
        this.reason = null;
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
        this.reason = null;
    }

    public AuthenticationException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
