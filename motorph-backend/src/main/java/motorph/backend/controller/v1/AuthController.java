package motorph.backend.controller.v1;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.ChallengePurpose;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.auth.ChangePasswordRequest;
import motorph.backend.dto.auth.CompleteTemporaryPasswordRequest;
import motorph.backend.dto.auth.ForgotPasswordRequest;
import motorph.backend.dto.auth.ForgotPasswordResponse;
import motorph.backend.dto.auth.LoginRequest;
import motorph.backend.dto.auth.LoginResponse;
import motorph.backend.dto.auth.LogoutRequest;
import motorph.backend.dto.auth.MeResponse;
import motorph.backend.dto.auth.PasswordRecoveryVerifyResponse;
import motorph.backend.dto.auth.RefreshRequest;
import motorph.backend.dto.auth.ResetPasswordRequest;
import motorph.backend.dto.auth.TokenResponse;
import motorph.backend.dto.auth.TotpCodeRequest;
import motorph.backend.dto.auth.TotpSetupResponse;
import motorph.backend.dto.auth.TotpSetupStartRequest;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.AuthenticatedPrincipal;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.security.JwtTokenProvider;
import motorph.backend.security.LoginRateLimiter;
import motorph.backend.security.TokenIssuanceService;
import motorph.backend.service.AuthenticationService;
import motorph.backend.service.LoginAttemptOutcome;
import motorph.backend.service.LoginChallengeService;
import motorph.backend.service.PasswordRecoveryService;
import motorph.backend.service.PermissionService;
import motorph.backend.service.TotpCodeService;
import motorph.backend.service.TotpService;
import motorph.backend.service.UserAccountService;
import motorph.backend.service.UserAccessLogService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication endpoints: login with authenticator-app (TOTP) verification, Forgot Password with
 * TOTP, token refresh, logout, and password changes. A correct password returns only an opaque
 * challenge ID; tokens are issued after the authenticator code is accepted. Roles are never stored in
 * tokens; they are loaded from the database on each request.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final LoginChallengeService loginChallengeService;
    private final TokenIssuanceService tokenIssuanceService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccountRepository userAccountRepository;
    private final UserAccessLogService userAccessLogService;
    private final LoginRateLimiter loginRateLimiter;
    private final CurrentUserProvider currentUserProvider;
    private final TotpService totpService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final UserAccountService userAccountService;
    private final PermissionService permissionService;

    public AuthController(AuthenticationService authenticationService,
                          LoginChallengeService loginChallengeService,
                          TokenIssuanceService tokenIssuanceService,
                          JwtTokenProvider jwtTokenProvider,
                          UserAccountRepository userAccountRepository,
                          UserAccessLogService userAccessLogService,
                          LoginRateLimiter loginRateLimiter,
                          CurrentUserProvider currentUserProvider,
                          TotpService totpService,
                          PasswordRecoveryService passwordRecoveryService,
                          UserAccountService userAccountService,
                          PermissionService permissionService) {
        this.authenticationService = authenticationService;
        this.loginChallengeService = loginChallengeService;
        this.tokenIssuanceService = tokenIssuanceService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userAccountRepository = userAccountRepository;
        this.userAccessLogService = userAccessLogService;
        this.loginRateLimiter = loginRateLimiter;
        this.currentUserProvider = currentUserProvider;
        this.totpService = totpService;
        this.passwordRecoveryService = passwordRecoveryService;
        this.userAccountService = userAccountService;
        this.permissionService = permissionService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        loginRateLimiter.checkAndRecord(clientKey(httpRequest, request.companyEmail()));

        LoginAttemptOutcome outcome = authenticationService.authenticate(request.companyEmail(), request.password());
        String maskedEmail = maskEmail(outcome.getCompanyEmail());

        /** Reuse the challenge so the client can finish the next step 
         * without ever receiving the user account ID. No token is issued here.
        */
        return switch (outcome.getType()) {
            case TEMPORARY_PASSWORD_CHANGE_REQUIRED -> new LoginResponse(
                    LoginResponse.TEMPORARY_PASSWORD_CHANGE_REQUIRED,
                    loginChallengeService.issue(outcome.getUserAccountId(), ChallengePurpose.TEMPORARY_PASSWORD),
                    maskedEmail, outcome.getMessage());
            case TOTP_SETUP_REQUIRED -> new LoginResponse(
                    LoginResponse.TOTP_SETUP_REQUIRED,
                    loginChallengeService.issue(outcome.getUserAccountId(), ChallengePurpose.TOTP_SETUP),
                    maskedEmail, outcome.getMessage());
            case TOTP_REQUIRED -> new LoginResponse(
                    LoginResponse.TOTP_REQUIRED,
                    loginChallengeService.issue(outcome.getUserAccountId(), ChallengePurpose.TOTP_LOGIN),
                    maskedEmail, outcome.getMessage());
        };
    }

    /**
     * Generates the authenticator secret for a first-time enrolment and returns the QR code. Nothing
     * is enabled until the confirmation below succeeds.
     */
    @PostMapping("/totp/setup/start")
    public TotpSetupResponse startTotpSetup(@RequestBody @Valid TotpSetupStartRequest request, HttpServletRequest httpRequest) {
        loginRateLimiter.checkAndRecord(clientKey(httpRequest, request.challengeId()));

        TotpService.EnrollmentDetails details = totpService.startEnrollment(request.challengeId());

        return new TotpSetupResponse(
                Base64.getEncoder().encodeToString(details.qrCodePng()),
                details.manualEntryKey(),
                TotpCodeService.ISSUER,
                details.accountLabel(),
                TotpCodeService.DIGITS,
                TotpCodeService.PERIOD_SECONDS,
                loginChallengeService.lifetimeOf(ChallengePurpose.TOTP_SETUP).toSeconds()
        );
    }

    /**
     * Confirms enrolment with one valid code, enables TOTP, and completes the login.
     */
    @PostMapping("/totp/setup/confirm")
    public TokenResponse confirmTotpSetup(@RequestBody @Valid TotpCodeRequest request, HttpServletRequest httpRequest) {
        loginRateLimiter.checkAndRecord(clientKey(httpRequest, request.challengeId()));

        UserAccount account = totpService.confirmEnrollment(request.challengeId(), request.code());

        return completeLogin(account);
    }

    /**
     * Verifies the authenticator code for a login and, only then, issues the token pair.
     */
    @PostMapping("/totp/verify")
    public TokenResponse verifyTotp(@RequestBody @Valid TotpCodeRequest request, HttpServletRequest httpRequest) {
        loginRateLimiter.checkAndRecord(clientKey(httpRequest, request.challengeId()));

        UserAccount account = totpService.verifyLogin(request.challengeId(), request.code());

        return completeLogin(account);
    }

    /**
     * Starts a password recovery. The response is the same whether or not the email belongs to an
     * account, so this endpoint cannot be used to find out which company emails exist. A user
     * without an authenticator cannot recover this way and must ask an administrator.
     */
    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@RequestBody @Valid ForgotPasswordRequest request,
                                                 HttpServletRequest httpRequest) {
        loginRateLimiter.checkAndRecord(clientKey(httpRequest, request.companyEmail()));

        PasswordRecoveryService.RecoveryStart start = passwordRecoveryService.start(request.companyEmail());

        return new ForgotPasswordResponse(start.challengeId(), start.expiresInSeconds(),
                "If the account can be recovered, enter the code from your authenticator app. "
                        + "If you cannot access your authenticator app, contact an administrator.");
    }

    /**
     * Verifies the authenticator code for a recovery and returns the single-use reset token.
     */
    @PostMapping("/forgot-password/verify")
    public PasswordRecoveryVerifyResponse verifyPasswordRecovery(@RequestBody @Valid TotpCodeRequest request,
                                                                 HttpServletRequest httpRequest) {
        loginRateLimiter.checkAndRecord(clientKey(httpRequest, request.challengeId()));

        PasswordRecoveryService.RecoveryToken token =
                passwordRecoveryService.verifyCode(request.challengeId(), request.code());

        return new PasswordRecoveryVerifyResponse(token.resetToken(), token.expiresInSeconds());
    }

    /**
     * Sets the new password with the reset token and signs the user out of every session.
     */
    @PostMapping("/forgot-password/reset")
    public void resetPassword(@RequestBody @Valid ResetPasswordRequest request, HttpServletRequest httpRequest) {
        loginRateLimiter.checkAndRecord(clientKey(httpRequest, request.resetToken()));

        passwordRecoveryService.resetPassword(request.resetToken(), request.newPassword(), request.confirmPassword());
    }

    /**
     * Replaces a temporary password during login, before any token has been issued. The account
     * comes from the single-use challenge issued by {@code /login}, never from the request body.
     */
    @PostMapping("/complete-temporary-password")
    public void completeTemporaryPassword(@RequestBody @Valid CompleteTemporaryPasswordRequest request,
                                           HttpServletRequest httpRequest) {
        loginRateLimiter.checkAndRecord(clientKey(httpRequest, request.challengeId()));

        int userAccountId = loginChallengeService.resolveAndConsume(
                request.challengeId(), ChallengePurpose.TEMPORARY_PASSWORD);

        userAccountService.completeTemporaryPasswordChange(
                userAccountId, request.newPassword(), request.confirmPassword());
    }

    /**
     * Changes the caller's own password. The account is taken from the access token.
     */
    @PostMapping("/change-password")
    public void changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        permissionService.requirePermission(PermissionKey.CHANGE_OWN_PASSWORD);

        userAccountService.changeOwnPassword(
                currentUserProvider.getCurrentUserAccountId(),
                request.currentPassword(),
                request.newPassword());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody @Valid RefreshRequest request) {
        return tokenIssuanceService.refresh(request.refreshToken());
    }

    /**
     * Revokes the caller's access token and, if supplied, their refresh token. Requires
     * authentication so the logout is attributed to the right user in the audit log.
     */
    @PostMapping("/logout")
    public void logout(@RequestBody(required = false) LogoutRequest request) {
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();

        tokenIssuanceService.revoke(principal.jti(), principal.userAccountId(), principal.tokenExpiresAt());

        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            revokeRefreshTokenBestEffort(request.refreshToken().trim());
        }

        userAccessLogService.logLogout(principal.userAccountId());
    }

    @GetMapping("/me")
    public MeResponse me() {
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();

        UserAccount account = userAccountRepository.findById(principal.userAccountId());

        if (account == null) {
            throw new EntityNotFoundException("User account not found.");
        }

        List<String> roles = account.getRoles().stream().map(Enum::name).collect(Collectors.toList());

        return new MeResponse(
                account.getUserAccountId(),
                account.getEmployeeId(),
                account.getCompanyEmail(),
                roles,
                account.getActiveRole().name()
        );
    }

    /**
     * Revokes the refresh token sent with a logout request. An invalid or expired token is ignored,
     * since it can no longer be used anyway.
     */
    private void revokeRefreshTokenBestEffort(String refreshToken) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);

            if (jwtTokenProvider.isRefreshToken(claims)) {
                int userAccountId = Integer.parseInt(claims.getSubject());
                tokenIssuanceService.revoke(claims.getId(), userAccountId, claims.getExpiration().toInstant());
            }
        } catch (JwtException | IllegalArgumentException e) {
            // Already invalid or expired; nothing to revoke.
        }
    }

    /**
     * This method is for token-issuance boundary.
     * It is called only after successful TOTP enrolment confirmation or TOTP login verification
     */
    private TokenResponse completeLogin(UserAccount account) {
        authenticationService.completeLogin(account);

        return tokenIssuanceService.issueTokenPair(account.getUserAccountId(), account.getEmployeeId());
    }

    private String clientKey(HttpServletRequest request, String secondaryKey) {
        String ip = request.getRemoteAddr();
        return ip + "|" + (secondaryKey == null ? "" : secondaryKey.trim().toLowerCase());
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');

        if (at <= 1) {
            return "***" + email.substring(at);
        }

        return email.charAt(0) + "***" + email.substring(at - 1);
    }
}
