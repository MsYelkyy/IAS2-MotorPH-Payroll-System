package motorph.backend.dto.auth;

/** Response body carrying a newly issued token pair. */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
