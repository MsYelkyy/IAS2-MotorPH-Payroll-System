package motorph.api;

import motorph.api.dto.auth.ChangePasswordRequest;
import motorph.api.dto.auth.CompleteTemporaryPasswordRequest;
import motorph.api.dto.auth.ForgotPasswordRequest;
import motorph.api.dto.auth.LoginRequest;
import motorph.api.dto.auth.LoginResponse;
import motorph.api.dto.auth.LogoutRequest;
import motorph.api.dto.auth.MeResponse;
import motorph.api.dto.auth.TokenResponse;
import motorph.api.dto.auth.VerifyOtpRequest;

/**
 * Thin wrapper mirroring the backend's /api/v1/auth endpoints. Method shapes
 * intentionally resemble the old desktop AuthenticationService so call sites
 * in ui/auth/** change minimally.
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

    // verifies the OTP and stores the returned token pair
    public TokenResponse verifyOtp(String challengeId, String otp) {
        TokenResponse tokens = httpApiClient.post(
                "/api/v1/auth/verify-otp",
                new VerifyOtpRequest(challengeId, otp),
                TokenResponse.class
        );

        tokenHolder.store(tokens.accessToken(), tokens.refreshToken(), tokens.expiresIn());
        return tokens;
    }

    // asks IT to issue a temporary password; the backend answers the same way
    // whether or not the address maps to an eligible account, so a success
    // here is not confirmation that the account exists
    public void forgotPassword(String companyEmail) {
        httpApiClient.postNoContent("/api/v1/auth/forgot-password", new ForgotPasswordRequest(companyEmail));
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

    // DB-loaded, for UI display only - never treat as an authorization source
    public MeResponse me() {
        return httpApiClient.get("/api/v1/auth/me", MeResponse.class);
    }

    // revokes the held tokens server-side, then clears them locally regardless of outcome
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
