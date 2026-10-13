package motorph.backend.dto.auth;

/**
 * Request body for {@code POST /api/v1/auth/logout}. The refresh token is optional, but clients
 * should send it so it is revoked together with the access token.
 */
public record LogoutRequest(String refreshToken) {

    @Override
    public String toString() {
        return "LogoutRequest[refreshToken=***]";
    }
}
