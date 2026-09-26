package motorph.backend.dto.auth;

import java.util.List;

/**
 * Error body returned by every endpoint. Clients should branch on {@code code}, which is stable,
 * and not on {@code message}, whose wording may change.
 */
public record ErrorResponse(
        String code,
        String message,
        List<String> fieldErrors
) {
    public ErrorResponse(String code, String message) {
        this(code, message, List.of());
    }
}
