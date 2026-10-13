package motorph.api.dto.auth;

/**
 * Authenticator code submitted for login, setup, or password recovery.
 * The values are hidden from logs to prevent the code from being exposed.
 */
public record TotpCodeRequest(String challengeId, String code) {

    @Override
    public String toString() {
        return "TotpCodeRequest[***]";
    }
}
