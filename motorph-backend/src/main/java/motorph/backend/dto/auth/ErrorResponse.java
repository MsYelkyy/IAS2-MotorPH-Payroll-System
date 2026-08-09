package motorph.backend.dto.auth;

import java.util.List;

/**
 * Stable error contract - `code` is the public API surface (documented string
 * enum), never a backend Java exception class name. Clients switch on `code`,
 * not on `message` (which may change wording) or on any Java type.
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
