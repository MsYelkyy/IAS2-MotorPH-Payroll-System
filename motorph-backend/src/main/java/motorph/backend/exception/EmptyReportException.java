package motorph.backend.exception;

/**
 * Thrown when a report matched no rows. Kept distinct from ValidationException
 * because the request was perfectly valid - there is simply nothing to render,
 * and a client should say so rather than hand the user an empty PDF.
 */
public class EmptyReportException extends RuntimeException {

    public EmptyReportException(String message) {
        super(message);
    }
}
