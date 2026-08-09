package motorph.backend.dto.auth;

/** Wire request body for POST /api/v1/auth/refresh. */
public record RefreshRequest(String refreshToken) {
}
