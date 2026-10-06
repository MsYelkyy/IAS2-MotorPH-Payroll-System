package motorph.backend.dto.auth;

/**
 * Authenticator code entered for a login, setup, or password recovery challenge.
 * The challenge is used to identify the account.
 */
public record TotpCodeRequest(String challengeId, String code) {

    @Override
    public String toString() {
        return "TotpCodeRequest[challengeId=***, code=***]";
    }
}
