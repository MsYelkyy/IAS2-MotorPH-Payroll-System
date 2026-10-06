package motorph.backend.dto.auth;

/** Request body for {@code POST /api/v1/auth/totp/setup/start}. */
public record TotpSetupStartRequest(String challengeId) {

    @Override
    public String toString() {
        return "TotpSetupStartRequest[challengeId=***]";
    }
}
