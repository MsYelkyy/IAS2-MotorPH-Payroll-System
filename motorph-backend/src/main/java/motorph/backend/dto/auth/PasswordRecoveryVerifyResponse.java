package motorph.backend.dto.auth;

/** Response after a recovery code is verified. Contains a one-time token used to change the password. */
public record PasswordRecoveryVerifyResponse(String resetToken, long expiresInSeconds) {

    @Override
    public String toString() {
        return "PasswordRecoveryVerifyResponse[resetToken=***]";
    }
}
