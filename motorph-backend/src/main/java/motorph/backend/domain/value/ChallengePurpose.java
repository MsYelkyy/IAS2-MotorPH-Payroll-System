package motorph.backend.domain.value;

/**
 * Defines the action allowed for a login challenge.
 * A challenge can only be used for its assigned purpose.
 */
public enum ChallengePurpose {

    // Replacing an IT-issued temporary password during login.
    TEMPORARY_PASSWORD,

    // First-time authenticator enrolment after a correct password.
    TOTP_SETUP,

    // Entering the authenticator code after a correct password.
    TOTP_LOGIN,

    // Entering the authenticator code to start a Forgot Password reset.
    PASSWORD_RECOVERY
}
