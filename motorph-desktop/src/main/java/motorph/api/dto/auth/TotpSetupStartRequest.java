package motorph.api.dto.auth;

/** Asks the backend to generate the authenticator secret for an enrolment challenge. */
public record TotpSetupStartRequest(String challengeId) {

    @Override
    public String toString() {
        return "TotpSetupStartRequest[***]";
    }
}
