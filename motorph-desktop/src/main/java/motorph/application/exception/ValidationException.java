package motorph.application.exception;

/**
 * Represents an error caused by invalid input or failed business validation.
 */
public class ValidationException extends RuntimeException {

    // constructors
    public ValidationException(String message) {
        super(message);
    }

    // constructors
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}