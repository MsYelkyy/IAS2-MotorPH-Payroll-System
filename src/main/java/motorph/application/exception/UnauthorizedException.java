package motorph.application.exception;

/**
 * Represents an error when a user tries to perform an unauthorized action.
 */
public class UnauthorizedException extends RuntimeException {

    // constructors
    public UnauthorizedException(String message) {
        super(message);
    }

    // constructors
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}