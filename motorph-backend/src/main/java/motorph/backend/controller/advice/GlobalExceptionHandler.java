package motorph.backend.controller.advice;

import motorph.backend.dto.auth.ErrorResponse;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.DataAccessException;
import motorph.backend.exception.EmptyReportException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.TooManyRequestsException;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.exception.ValidationException;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.UserAccessLogService;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Translates exceptions into the {@link ErrorResponse} format. Clients only ever see the stable
 * {@code code} values, never Java exception class names, so the exception hierarchy can change
 * without breaking them.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final UserAccessLogService userAccessLogService;
    private final CurrentUserProvider currentUserProvider;

    public GlobalExceptionHandler(UserAccessLogService userAccessLogService,
                                  CurrentUserProvider currentUserProvider) {
        this.userAccessLogService = userAccessLogService;
        this.currentUserProvider = currentUserProvider;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        return respond(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e, HttpServletRequest request) {
        logAccessDenied(request);
        return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleIllegalArgument(RuntimeException e) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", e.getMessage());
    }

    /**
     * Handles a required request parameter that was not sent, such as a report's start or end date.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", e.getParameterName() + " is required.");
    }

    /**
     * Handles a request parameter that could not be converted to its type, such as a malformed
     * date.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", e.getName() + " is invalid.");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
    }

    /**
     * Handles a report that matched no rows, so the client can show a message instead of a blank
     * PDF.
     */
    @ExceptionHandler(EmptyReportException.class)
    public ResponseEntity<ErrorResponse> handleEmptyReport(EmptyReportException e) {
        return respond(HttpStatus.NOT_FOUND, "NO_REPORT_DATA", e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException e) {
        return respond(HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION", e.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException e) {
        return respond(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", e.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException e) {
        log.error("Data access failure", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An internal error occurred.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An internal error occurred.");
    }

    /**
     * Logs access denied events for authenticated users.
     * Only the request method and route are recorded.
     * Logging errors do not affect the 403 response.
     */
    private void logAccessDenied(HttpServletRequest request) {
        try {
            int actorUserAccountId = currentUserProvider.getCurrentUserAccountId();

            userAccessLogService.logEvent(actorUserAccountId, UserAccessLogService.ACTION_ACCESS_DENIED,
                    "Access denied: " + request.getMethod() + " " + routeOf(request));
        } catch (UnauthorizedException noAuthenticatedUser) {
            // No authenticated actor, so there is nobody to attribute the denial to.
        } catch (RuntimeException auditFailure) {
            log.error("Failed to write ACCESS_DENIED audit entry", auditFailure);
        }
    }

    /**
     * Returns the route template without including IDs or other path values.
     */
    private String routeOf(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        return pattern instanceof String route ? route : "(unmatched route)";
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}
