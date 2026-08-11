package motorph.application.exception;

/**
 * Represents an error when a business rule is violated.
 */
public class BusinessRuleException extends RuntimeException {

    // constructors
    public BusinessRuleException(String message) {
        super(message);
    }

    // constructors
    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}