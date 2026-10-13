package motorph.api;

/**
 * Thrown by HttpApiClient for any non-2xx backend response. Carries the
 * backend's stable `code` alongside a human-readable message; existing UI
 * catch blocks keep working unchanged since they only read getMessage()
 * (see UiMessageUtil.toUserMessage).
 */
public class ApiException extends RuntimeException {

    private final String code;
    private final int httpStatus;
    private final String reason;

    public ApiException(int httpStatus, String code, String message) {
        this(httpStatus, code, message, null);
    }

    public ApiException(int httpStatus, String code, String message, String reason) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
        this.reason = reason;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getReason() {
        return reason;
    }
}
