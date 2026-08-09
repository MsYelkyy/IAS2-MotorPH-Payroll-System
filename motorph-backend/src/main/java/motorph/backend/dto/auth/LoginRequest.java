package motorph.backend.dto.auth;

/** Wire request body for POST /api/v1/auth/login. */
public record LoginRequest(String companyEmail, String password) {
}
