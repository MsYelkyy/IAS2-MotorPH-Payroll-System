package motorph.backend.dto.auth;

/** Wire response body carrying newly issued tokens. */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
