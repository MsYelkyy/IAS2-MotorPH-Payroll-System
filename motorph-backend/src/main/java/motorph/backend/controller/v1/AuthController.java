package motorph.backend.controller.v1;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import motorph.backend.domain.UserAccount;
import motorph.backend.dto.auth.LoginRequest;
import motorph.backend.dto.auth.LoginResponse;
import motorph.backend.dto.auth.LogoutRequest;
import motorph.backend.dto.auth.MeResponse;
import motorph.backend.dto.auth.RefreshRequest;
import motorph.backend.dto.auth.TokenResponse;
import motorph.backend.dto.auth.VerifyOtpRequest;
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
import motorph.backend.service.UserAccessLogService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 1 auth boundary. Login never returns account internals before OTP
 * verification (challenge-based flow); logout is authenticated so it is
 * auditable and can revoke the presented token(s); roles are never embedded
 * in a token and are always resolved fresh from the DB (see /me).
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

    public AuthController(AuthenticationService authenticationService,
                          LoginChallengeService loginChallengeService,
                          TokenIssuanceService tokenIssuanceService,
                          JwtTokenProvider jwtTokenProvider,
                          UserAccountRepository userAccountRepository,
                          UserAccessLogService userAccessLogService,
                          LoginRateLimiter loginRateLimiter,
                          CurrentUserProvider currentUserProvider) {
        this.authenticationService = authenticationService;
        this.loginChallengeService = loginChallengeService;
        this.tokenIssuanceService = tokenIssuanceService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userAccountRepository = userAccountRepository;
        this.userAccessLogService = userAccessLogService;
        this.loginRateLimiter = loginRateLimiter;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        loginRateLimiter.checkAndRecord(clientKey(httpRequest, request.companyEmail()));

        LoginAttemptOutcome outcome = authenticationService.authenticate(request.companyEmail(), request.password());
        String maskedEmail = maskEmail(outcome.getCompanyEmail());

        if (outcome.getType() == LoginAttemptOutcome.Type.TEMPORARY_PASSWORD_CHANGE_REQUIRED) {
            return new LoginResponse(null, maskedEmail, true, outcome.getMessage(), null);
        }

        String challengeId = loginChallengeService.issue(outcome.getUserAccountId());
        return new LoginResponse(challengeId, maskedEmail, false, outcome.getMessage(), outcome.getOtpPreview());
    }

    @PostMapping("/verify-otp")
    public TokenResponse verifyOtp(@RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest) {
        loginRateLimiter.checkAndRecord(clientKey(httpRequest, request.challengeId()));

        int userAccountId = loginChallengeService.resolveAndConsume(request.challengeId());
        UserAccount account = authenticationService.verifyLoginOtp(userAccountId, request.otp());

        return tokenIssuanceService.issueTokenPair(account.getUserAccountId(), account.getEmployeeId());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody RefreshRequest request) {
        return tokenIssuanceService.refresh(request.refreshToken());
    }

    // authenticated: only a caller holding a still-valid access token can log out with it,
    // and the acting identity for the audit log always comes from that token, never the body
    @PostMapping("/logout")
    public void logout(@RequestBody(required = false) LogoutRequest request) {
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();

        tokenIssuanceService.revoke(principal.jti(), principal.userAccountId(), principal.tokenExpiresAt());

        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            revokeRefreshTokenBestEffort(request.refreshToken());
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

    // revokes the paired refresh token if the client supplied one; a malformed/expired
    // refresh token here is not fatal to logout itself, so failures are swallowed
    private void revokeRefreshTokenBestEffort(String refreshToken) {
        try {
            Claims claims = jwtTokenProvider.parseClaims(refreshToken);

            if (jwtTokenProvider.isRefreshToken(claims)) {
                int userAccountId = Integer.parseInt(claims.getSubject());
                tokenIssuanceService.revoke(claims.getId(), userAccountId, claims.getExpiration().toInstant());
            }
        } catch (JwtException | IllegalArgumentException e) {
            // token already invalid/expired - nothing to revoke
        }
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
