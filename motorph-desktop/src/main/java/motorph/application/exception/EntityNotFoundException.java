package motorph.application.exception;

/**
 * Represents an error when a required entity cannot be found.
 */
public class EntityNotFoundException extends RuntimeException {

    // constructors
    public EntityNotFoundException(String message) {
        super(message);
    }

    // constructors
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}