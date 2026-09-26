package motorph.backend.dto.auth;

/** Request body for {@code POST /api/v1/auth/refresh}. */
public record RefreshRequest(String refreshToken) {
}
