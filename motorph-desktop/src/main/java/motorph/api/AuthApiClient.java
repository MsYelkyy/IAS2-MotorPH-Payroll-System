package motorph.api;

import motorph.api.dto.auth.ChangePasswordRequest;
import motorph.api.dto.auth.CompleteTemporaryPasswordRequest;
import motorph.api.dto.auth.ForgotPasswordRequest;
import motorph.api.dto.auth.ForgotPasswordResponse;
import motorph.api.dto.auth.LoginRequest;
import motorph.api.dto.auth.LoginResponse;
import motorph.api.dto.auth.LogoutRequest;
import motorph.api.dto.auth.MeResponse;
import motorph.api.dto.auth.PasswordRecoveryVerifyResponse;
import motorph.api.dto.auth.ResetPasswordRequest;
import motorph.api.dto.auth.TokenResponse;
import motorph.api.dto.auth.TotpCodeRequest;
import motorph.api.dto.auth.TotpSetupResponse;
import motorph.api.dto.auth.TotpSetupStartRequest;

/**
 * Connects the desktop app to the backend authentication endpoints.
 * The methods are kept similar to the old AuthenticationService to reduce changes in the UI code.
 */
public final class AuthApiClient {

    private final HttpApiClient httpApiClient;
    private final AuthTokenHolder tokenHolder;

    public AuthApiClient(HttpApiClient httpApiClient, AuthTokenHolder tokenHolder) {
        this.httpApiClient = httpApiClient;
        this.tokenHolder = tokenHolder;
    }

    public LoginResponse login(String companyEmail, String password) {
        return httpApiClient.post("/api/v1/auth/login", new LoginRequest(companyEmail, password), LoginResponse.class);
    }

    // Gets the authenticator secret for first-time setup.
    public TotpSetupResponse startTotpSetup(String challengeId) {
        return httpApiClient.post("/api/v1/auth/totp/setup/start",
                new TotpSetupStartRequest(challengeId), TotpSetupResponse.class);
    }

    // Confirms authenticator setup and stores the returned login tokens.
    public TokenResponse confirmTotpSetup(String challengeId, String code) {
        TokenResponse tokens = httpApiClient.post("/api/v1/auth/totp/setup/confirm",
                new TotpCodeRequest(challengeId, code), TokenResponse.class);

        tokenHolder.store(tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn());
        return tokens;
    }

    // verifies the authenticator code for a login and stores the returned token pair
    public TokenResponse verifyTotp(String challengeId, String code) {
        TokenResponse tokens = httpApiClient.post("/api/v1/auth/totp/verify",
                new TotpCodeRequest(challengeId, code), TokenResponse.class);

        tokenHolder.store(tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn());
        return tokens;
    }

    // Starts password recovery without revealing whether the account exists.
    public ForgotPasswordResponse forgotPassword(String companyEmail) {
        return httpApiClient.post("/api/v1/auth/forgot-password",
                new ForgotPasswordRequest(companyEmail), ForgotPasswordResponse.class);
    }

    // checks the authenticator code for a recovery and returns the single-use reset token
    public PasswordRecoveryVerifyResponse verifyRecoveryCode(String challengeId, String code) {
        return httpApiClient.post("/api/v1/auth/forgot-password/verify",
                new TotpCodeRequest(challengeId, code), PasswordRecoveryVerifyResponse.class);
    }

    // sets the new password with the reset token; the backend signs the user out everywhere
    public void resetPassword(String resetToken, String newPassword, String confirmPassword) {
        httpApiClient.postNoContent("/api/v1/auth/forgot-password/reset",
                new ResetPasswordRequest(resetToken, newPassword, confirmPassword));
    }

    // finishes a forced temporary-password change; the challenge came from login
    public void completeTemporaryPassword(String challengeId, String newPassword, String confirmPassword) {
        httpApiClient.postNoContent("/api/v1/auth/complete-temporary-password",
                new CompleteTemporaryPasswordRequest(challengeId, newPassword, confirmPassword));
    }

    // changes the signed-in user own password; the backend acts on the token holder
    public void changePassword(String currentPassword, String newPassword) {
        httpApiClient.postNoContent("/api/v1/auth/change-password",
                new ChangePasswordRequest(currentPassword, newPassword));
    }

    // Loaded from the database for display only, not for permission checks.
    public MeResponse me() {
        return httpApiClient.get("/api/v1/auth/me", MeResponse.class);
    }

    // Revokes the current tokens and clears them from the client.
    public void logout() {
        try {
            if (tokenHolder.isAuthenticated()) {
                httpApiClient.postNoContent("/api/v1/auth/logout", new LogoutRequest(tokenHolder.getRefreshToken()));
            }
        } finally {
            tokenHolder.clear();
        }
    }
}
