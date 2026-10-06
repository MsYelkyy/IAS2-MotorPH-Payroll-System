package motorph.backend.dto.auth;

/**
 * Response for {@code POST /api/v1/auth/login}.
 * After the password is verified, the response tells the client what step comes next and provides a challenge ID.
 * No access tokens, user ID, or role information are returned at this stage.
 *
 * <p>Possible {@code status} values are {@code TOTP_REQUIRED},
 * {@code TOTP_SETUP_REQUIRED}, and {@code TEMPORARY_PASSWORD_CHANGE_REQUIRED}.
 */
public record LoginResponse(
        String status,
        String challengeId,
        String maskedEmail,
        String message
) {
    public static final String TOTP_REQUIRED = "TOTP_REQUIRED";
    public static final String TOTP_SETUP_REQUIRED = "TOTP_SETUP_REQUIRED";
    public static final String TEMPORARY_PASSWORD_CHANGE_REQUIRED = "TEMPORARY_PASSWORD_CHANGE_REQUIRED";
}
