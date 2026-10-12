package motorph.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Error body returned by every endpoint. Clients should branch on {@code code}, which is stable,
 * and not on {@code message}, whose wording may change.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        List<String> fieldErrors,
        String reason
) {
    public ErrorResponse(String code, String message) {
        this(code, message, List.of(), null);
    }

    public ErrorResponse(String code, String message, List<String> fieldErrors) {
        this(code, message, fieldErrors, null);
    }
}
