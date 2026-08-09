package motorph.backend.exception;

/**
 * Represents a rate-limit rejection on a public authentication endpoint.
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
