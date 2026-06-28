package motorph.application.exception;

/**
 * Represents an error when a database operation fails.
 */
public class DataAccessException extends RuntimeException {

    // constructors
    public DataAccessException(String message) {
        super(message);
    }

    // constructors
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
