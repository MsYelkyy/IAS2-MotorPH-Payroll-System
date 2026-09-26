package motorph.backend.exception;

/**
 * Thrown when a report query returns no rows. Kept separate from {@link ValidationException}
 * because the request itself is valid; the client should say there is nothing to show instead of
 * opening an empty PDF.
 */
public class EmptyReportException extends RuntimeException {

    public EmptyReportException(String message) {
        super(message);
    }
}
