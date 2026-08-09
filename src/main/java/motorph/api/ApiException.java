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

    public ApiException(int httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
