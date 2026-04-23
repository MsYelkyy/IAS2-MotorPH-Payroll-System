package motorph.application.exception;

/**
 * Represents an error that occurs during user authentication.
 */
public class AuthenticationException extends RuntimeException {

    // constructors
    public AuthenticationException(String message) {
        super(message);
    }

    // constructors
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}