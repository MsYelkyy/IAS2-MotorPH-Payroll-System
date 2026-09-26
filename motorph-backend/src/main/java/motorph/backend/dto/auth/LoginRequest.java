package motorph.backend.dto.auth;

/** Request body for {@code POST /api/v1/auth/login}. */
public record LoginRequest(String companyEmail, String password) {
}
