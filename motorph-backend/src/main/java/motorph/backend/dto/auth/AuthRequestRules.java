package motorph.backend.dto.auth;

/**
 * Shape rules shared by the authentication request DTOs, so a challenge ID or reset token is checked
 * the same way on every endpoint. The server issues both as URL-safe Base64 (43 characters), and
 * the decoy ID returned for an unknown email has the same shape, so these rules cannot be used to
 * tell a real account from a fake one.
 */
final class AuthRequestRules {

    static final String URL_SAFE_ID = "[A-Za-z0-9_-]+";
    static final int MAX_ID_LENGTH = 128;

    private AuthRequestRules() {
    }
}
