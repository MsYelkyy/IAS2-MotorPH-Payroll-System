package motorph.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import motorph.backend.controller.v1.AuthController;
import motorph.backend.controller.v1.UserAccountController;
import motorph.backend.domain.LoginChallenge;
import motorph.backend.domain.TotpCredential;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.domain.value.Role;
import motorph.backend.dto.auth.ForgotPasswordRequest;
import motorph.backend.dto.auth.ForgotPasswordResponse;
import motorph.backend.dto.auth.LoginRequest;
import motorph.backend.dto.auth.LoginResponse;
import motorph.backend.dto.auth.PasswordRecoveryVerifyResponse;
import motorph.backend.dto.auth.ResetPasswordRequest;
import motorph.backend.dto.auth.TokenResponse;
import motorph.backend.dto.auth.TotpCodeRequest;
import motorph.backend.dto.auth.TotpSetupResponse;
import motorph.backend.dto.auth.TotpSetupStartRequest;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.exception.OtpVerificationException;
import motorph.backend.exception.TooManyRequestsException;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.LoginChallengeRepository;
import motorph.backend.repository.PasswordRecoveryTokenRepository;
import motorph.backend.repository.PermissionRepository;
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.TotpCredentialRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.security.JwtTokenProvider;
import motorph.backend.security.LoginRateLimiter;
import motorph.backend.security.PasswordHasher;
import motorph.backend.security.TokenIssuanceService;
import motorph.backend.security.TotpSecretCipher;

import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Drives the real authentication, TOTP, challenge, recovery and token services against in-memory
 * repositories and a clock the tests move by hand, so expiry, time-step and replay behaviour is
 * checked without waiting or a database.
 */
class TotpAuthenticationFlowTest {

    private static final int USER_ID = 1;
    private static final int ADMIN_ID = 2;
    private static final int IT_ID = 3;
    private static final String EMAIL = "juan.cruz@motorph.com";
    private static final String OLD_PASSWORD = "OldPassw0rd";
    private static final String NEW_PASSWORD = "BrandNewPass1";
    private static final String ENCRYPTION_KEY = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII));

    // Starts a little ahead of real time so tokens issued during the test (which use the system clock)
    // never carry a later issue time than the moment a test revokes sessions.
    private final MutableClock clock = new MutableClock(Instant.now().plusSeconds(5));

    private final FakeUsers users = new FakeUsers();
    private final FakeChallenges challenges = new FakeChallenges();
    private final FakeCredentials credentials = new FakeCredentials();
    private final FakeRecoveryTokens recoveryTokens = new FakeRecoveryTokens();
    private final FakeRevocations revocations = new FakeRevocations();
    private final List<AuditEntry> audit = new ArrayList<>();

    private final TotpCodeService codes = new TotpCodeService(clock);
    private final HttpServletRequest http = mock(HttpServletRequest.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final PermissionRepository permissionRepository = mock(PermissionRepository.class);

    private TokenIssuanceService tokenIssuance;
    private AuthController authController;
    private UserAccountController userAccountController;
    private UserAccountService userAccountService;
    private LoginRateLimiter rateLimiter;

    private record AuditEntry(int actor, String action, String details) {
        @Override
        public String toString() {
            return action + " | " + details;
        }
    }

    @BeforeEach
    void setUp() {
        when(http.getRemoteAddr()).thenReturn("127.0.0.1");

        users.add(account(USER_ID, EMAIL, OLD_PASSWORD, Role.EMPLOYEE));
        users.add(account(ADMIN_ID, "admin@motorph.com", OLD_PASSWORD, Role.ADMIN));
        users.add(account(IT_ID, "it.user@motorph.com", OLD_PASSWORD, Role.IT));

        UserAccessLogService logs = mock(UserAccessLogService.class, invocation -> {
            Object[] args = invocation.getArguments();

            switch (invocation.getMethod().getName()) {
                case "logEvent" -> audit.add(new AuditEntry((Integer) args[0], (String) args[1], (String) args[2]));
                case "logLoginSuccess" -> audit.add(new AuditEntry((Integer) args[0], "LOGIN_SUCCESS", (String) args[1]));
                case "logLoginFailed" -> audit.add(new AuditEntry(0, "LOGIN_FAILED", (String) args[1]));
                default -> { }
            }
            return null;
        });

        TotpSecretCipher cipher = new TotpSecretCipher(ENCRYPTION_KEY);
        LoginChallengeService challengeService = new LoginChallengeService(challenges, clock, 10, 10, 5);
        AuthenticationService authentication = new AuthenticationService(users, credentials, logs);
        TotpService totpService = new TotpService(challengeService, authentication, credentials, cipher, codes,
                new TotpQrCodeGenerator(), logs, clock);

        userAccountService = new UserAccountService(users, mock(EmployeeRepository.class),
                mock(EmployeeCredentialsDeliveryService.class), logs, credentials);
        PasswordRecoveryService recovery = new PasswordRecoveryService(users, challengeService, totpService,
                recoveryTokens, revocations, userAccountService, logs, clock, 10);

        JwtTokenProvider jwt = new JwtTokenProvider("a-test-signing-secret-of-at-least-32-bytes!!", 15, 1440);
        tokenIssuance = spy(new TokenIssuanceService(jwt, revocations));
        rateLimiter = new LoginRateLimiter(1000, 1);

        PermissionService permissionService =
                new PermissionService(permissionRepository, users, currentUser, logs);

        authController = new AuthController(authentication, challengeService, tokenIssuance, jwt, users, logs,
                rateLimiter, currentUser, totpService, recovery, userAccountService, permissionService);
        userAccountController = new UserAccountController(userAccountService, permissionService, currentUser);
    }

    // ---------------------------------------------------------------- login and enrolment

    @Test
    @DisplayName("A correct password never issues tokens; a user without an authenticator is sent to enrolment")
    void firstLoginReturnsSetupRequiredAndNoTokens() {
        LoginResponse response = login(EMAIL, OLD_PASSWORD);

        assertThat(response.status()).isEqualTo("TOTP_SETUP_REQUIRED");
        assertThat(response.challengeId()).isNotBlank();
        verify(tokenIssuance, never()).issueTokenPair(anyInt(), anyInt());
    }

    @Test
    @DisplayName("A wrong password is a generic failure and starts no challenge")
    void wrongPasswordStartsNothing() {
        assertThatThrownBy(() -> login(EMAIL, "WrongPassw0rd")).isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid email or password.");
        assertThatThrownBy(() -> login("nobody@motorph.com", OLD_PASSWORD)).isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid email or password.");

        assertThat(challenges.count()).isZero();
    }

    @Test
    @DisplayName("TOTP stays off until a valid confirmation code is entered, and restarting setup does not enable it")
    void totpIsNotEnabledUntilConfirmed() {
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();
        byte[] secret = secretFrom(authController.startTotpSetup(new TotpSetupStartRequest(challengeId), http));
        String wrong = wrongCode(secret);

        assertThat(credentials.stored(USER_ID)).isNull();

        assertThatThrownBy(() -> authController.confirmTotpSetup(new TotpCodeRequest(challengeId, wrong), http))
                .isInstanceOf(OtpVerificationException.class);

        assertThat(credentials.stored(USER_ID)).isNull();
        assertThat(login(EMAIL, OLD_PASSWORD).status()).isEqualTo("TOTP_SETUP_REQUIRED");
        verify(tokenIssuance, never()).issueTokenPair(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Confirming enrolment with a current code enables TOTP and then issues the tokens")
    void confirmingEnrollmentEnablesTotpAndIssuesTokens() {
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();
        TotpSetupResponse setup = authController.startTotpSetup(new TotpSetupStartRequest(challengeId), http);

        assertThat(setup.issuer()).isEqualTo("MotorPH");
        assertThat(setup.accountLabel()).isEqualTo(EMAIL);
        assertThat(setup.digits()).isEqualTo(6);
        assertThat(setup.periodSeconds()).isEqualTo(30);
        assertThat(Base64.getDecoder().decode(setup.qrCodePngBase64())).startsWith(new byte[] {(byte) 0x89, 'P', 'N', 'G'});

        byte[] secret = secretFrom(setup);
        TokenResponse tokens = authController.confirmTotpSetup(
                new TotpCodeRequest(challengeId, codeNow(secret)), http);

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(credentials.stored(USER_ID).isEnabled()).isTrue();
        assertThat(auditActions()).contains("TOTP_ENROLLMENT_STARTED", "TOTP_ENROLLMENT_COMPLETED", "LOGIN_SUCCESS");

        assertThat(login(EMAIL, OLD_PASSWORD).status()).isEqualTo("TOTP_REQUIRED");
    }

    @Test
    @DisplayName("A setup challenge cannot be reused after enrolment, so the secret cannot be fetched again")
    void setupChallengeIsSingleUse() {
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();
        TotpSetupResponse setup = authController.startTotpSetup(new TotpSetupStartRequest(challengeId), http);
        authController.confirmTotpSetup(new TotpCodeRequest(challengeId, codeNow(secretFrom(setup))), http);

        assertThatThrownBy(() -> authController.startTotpSetup(new TotpSetupStartRequest(challengeId), http))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("The setup challenge and its unconfirmed secret expire")
    void setupChallengeExpires() {
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();
        TotpSetupResponse setup = authController.startTotpSetup(new TotpSetupStartRequest(challengeId), http);
        String code = codeNow(secretFrom(setup));

        clock.advance(Duration.ofMinutes(11));

        assertThatThrownBy(() -> authController.confirmTotpSetup(new TotpCodeRequest(challengeId, code), http))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Login challenge is invalid or expired.");
        assertThat(credentials.stored(USER_ID)).isNull();
    }

    @Test
    @DisplayName("Starting setup again replaces the secret, so only the newest QR code can be confirmed")
    void restartingSetupReplacesTheSecret() {
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();
        byte[] first = secretFrom(authController.startTotpSetup(new TotpSetupStartRequest(challengeId), http));
        byte[] second = secretFrom(authController.startTotpSetup(new TotpSetupStartRequest(challengeId), http));

        assertThat(second).isNotEqualTo(first);
        assertThatThrownBy(() -> authController.confirmTotpSetup(
                new TotpCodeRequest(challengeId, codeNow(first)), http))
                .isInstanceOf(OtpVerificationException.class);
        assertThat(authController.confirmTotpSetup(
                new TotpCodeRequest(challengeId, codeNow(second)), http).accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("Confirming before the QR code was generated is refused")
    void confirmWithoutStartingSetupIsRefused() {
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();

        assertThatThrownBy(() -> authController.confirmTotpSetup(new TotpCodeRequest(challengeId, "123456"), http))
                .isInstanceOf(ValidationException.class);
    }

    // ---------------------------------------------------------------- normal login after enrolment

    @Test
    @DisplayName("Tokens are issued only after the authenticator code is verified")
    void loginIssuesTokensOnlyAfterTotp() {
        byte[] secret = enroll();
        verify(tokenIssuance).issueTokenPair(USER_ID, 101);

        clock.advance(Duration.ofSeconds(30));
        LoginResponse response = login(EMAIL, OLD_PASSWORD);

        assertThat(response.status()).isEqualTo("TOTP_REQUIRED");
        verify(tokenIssuance).issueTokenPair(USER_ID, 101);

        String wrong = wrongCode(secret);
        assertThatThrownBy(() -> authController.verifyTotp(new TotpCodeRequest(response.challengeId(), wrong), http))
                .isInstanceOf(OtpVerificationException.class);
        verify(tokenIssuance).issueTokenPair(USER_ID, 101);

        TokenResponse tokens = authController.verifyTotp(
                new TotpCodeRequest(response.challengeId(), codeNow(secret)), http);

        assertThat(tokens.refreshToken()).isNotBlank();
        verify(tokenIssuance, org.mockito.Mockito.times(2)).issueTokenPair(USER_ID, 101);
        assertThat(auditActions()).contains("TOTP_VERIFICATION_SUCCEEDED");
    }

    @Test
    @DisplayName("An incorrect code fails and reports the attempts that remain")
    void incorrectCodeFailsWithAttemptsRemaining() {
        byte[] secret = enroll();
        clock.advance(Duration.ofSeconds(30));
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();
        String wrong = wrongCode(secret);

        assertThatThrownBy(() -> authController.verifyTotp(new TotpCodeRequest(challengeId, wrong), http))
                .isInstanceOf(OtpVerificationException.class)
                .hasMessage("Incorrect code. 4 attempts remaining.");
        assertThat(auditActions()).contains("TOTP_VERIFICATION_FAILED");
    }

    @Test
    @DisplayName("A malformed code is a validation error and does not use up an attempt")
    void malformedCodeDoesNotCountAsAnAttempt() {
        enroll();
        clock.advance(Duration.ofSeconds(30));
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();

        assertThatThrownBy(() -> authController.verifyTotp(new TotpCodeRequest(challengeId, "12ab"), http))
                .isInstanceOf(ValidationException.class);

        assertThat(challenges.attempts(challengeId)).isZero();
    }

    @Test
    @DisplayName("A code from more than one step ago is rejected, however recently it was valid")
    void outOfWindowCodeIsRejected() {
        byte[] secret = enroll();
        clock.advance(Duration.ofSeconds(30));
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();
        String oldCode = codeNow(secret);

        clock.advance(Duration.ofSeconds(95));

        assertThatThrownBy(() -> authController.verifyTotp(new TotpCodeRequest(challengeId, oldCode), http))
                .isInstanceOf(OtpVerificationException.class);
    }

    @Test
    @DisplayName("A code from the adjacent step is accepted to allow for clock drift")
    void adjacentStepIsAccepted() {
        byte[] secret = enroll();
        clock.advance(Duration.ofSeconds(60));
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();
        String nextStepCode = codes.codeForStep(secret, codes.currentTimeStep() + 1);

        assertThat(authController.verifyTotp(new TotpCodeRequest(challengeId, nextStepCode), http).accessToken())
                .isNotBlank();
    }

    @Test
    @DisplayName("A time step that was already accepted cannot be used again")
    void acceptedTimeStepCannotBeReplayed() {
        byte[] secret = enroll();
        clock.advance(Duration.ofSeconds(30));

        String first = login(EMAIL, OLD_PASSWORD).challengeId();
        String code = codeNow(secret);
        authController.verifyTotp(new TotpCodeRequest(first, code), http);

        String second = login(EMAIL, OLD_PASSWORD).challengeId();
        assertThatThrownBy(() -> authController.verifyTotp(new TotpCodeRequest(second, code), http))
                .isInstanceOf(OtpVerificationException.class);

        clock.advance(Duration.ofSeconds(30));
        assertThat(authController.verifyTotp(new TotpCodeRequest(second, codeNow(secret)), http).accessToken())
                .isNotBlank();
    }

    @Test
    @DisplayName("The code that confirmed enrolment cannot be replayed to log in straight away")
    void enrolmentCodeCannotBeReplayedAtLogin() {
        byte[] secret = enroll();
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();

        assertThatThrownBy(() -> authController.verifyTotp(new TotpCodeRequest(challengeId, codeNow(secret)), http))
                .isInstanceOf(OtpVerificationException.class);
    }

    @Test
    @DisplayName("Five incorrect codes close the challenge; the correct code no longer works")
    void fiveIncorrectCodesBlockTheChallenge() {
        byte[] secret = enroll();
        clock.advance(Duration.ofSeconds(30));
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();
        String wrong = wrongCode(secret);

        for (int remaining = 4; remaining >= 1; remaining--) {
            int expected = remaining;
            assertThatThrownBy(() -> authController.verifyTotp(new TotpCodeRequest(challengeId, wrong), http))
                    .isInstanceOf(OtpVerificationException.class)
                    .hasMessageContaining(expected + (expected == 1 ? " attempt remaining" : " attempts remaining"));
        }

        assertThatThrownBy(() -> authController.verifyTotp(new TotpCodeRequest(challengeId, wrong), http))
                .isInstanceOfSatisfying(OtpVerificationException.class, e -> assertThat(e.getReason())
                        .isEqualTo(OtpVerificationException.Reason.ATTEMPT_LIMIT_REACHED));

        assertThatThrownBy(() -> authController.verifyTotp(new TotpCodeRequest(challengeId, codeNow(secret)), http))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("An expired challenge fails even with the correct code")
    void expiredChallengeFails() {
        byte[] secret = enroll();
        clock.advance(Duration.ofSeconds(30));
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();

        clock.advance(Duration.ofMinutes(6));

        assertThatThrownBy(() -> authController.verifyTotp(new TotpCodeRequest(challengeId, codeNow(secret)), http))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Login challenge is invalid or expired.");
    }

    @Test
    @DisplayName("A challenge issued for one step cannot be used for another")
    void challengePurposeIsEnforced() {
        enroll();
        clock.advance(Duration.ofSeconds(30));
        String loginChallenge = login(EMAIL, OLD_PASSWORD).challengeId();

        assertThatThrownBy(() -> authController.startTotpSetup(new TotpSetupStartRequest(loginChallenge), http))
                .isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(() -> authController.verifyPasswordRecovery(new TotpCodeRequest(loginChallenge, "123456"), http))
                .isInstanceOf(OtpVerificationException.class);
    }

    @Test
    @DisplayName("An account locked between the password and the code cannot finish the login")
    void accountLockedMidLoginIsRefused() {
        byte[] secret = enroll();
        clock.advance(Duration.ofSeconds(30));
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();

        users.byId(USER_ID).setLocked(true);

        assertThatThrownBy(() -> authController.verifyTotp(new TotpCodeRequest(challengeId, codeNow(secret)), http))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Account is locked.");
    }

    @Test
    @DisplayName("The authenticator endpoints share the login rate limit")
    void codeEntryIsRateLimited() {
        rateLimiter = new LoginRateLimiter(2, 1);
        AuthController limited = new AuthController(null, null, null, null, null, null, rateLimiter, null,
                null, null, null, null);

        TotpCodeRequest request = new TotpCodeRequest("same-challenge", "123456");

        try {
            limited.verifyTotp(request, http);
        } catch (NullPointerException ignored) {
            // the collaborators are absent on purpose; only the limiter runs first
        }
        try {
            limited.verifyTotp(request, http);
        } catch (NullPointerException ignored) {
            // as above
        }

        assertThatThrownBy(() -> limited.verifyTotp(request, http)).isInstanceOf(TooManyRequestsException.class);
    }

    // ---------------------------------------------------------------- secret protection

    @Test
    @DisplayName("The secret is stored AES-GCM encrypted, never in the clear")
    void secretIsEncryptedInStorage() {
        byte[] secret = enroll();

        TotpCredential stored = credentials.stored(USER_ID);
        String base32 = new Base32().encodeToString(secret).replace("=", "");

        assertThat(stored.getEncryptedSecret()).isNotEqualTo(secret);
        assertThat(new String(stored.getEncryptedSecret(), StandardCharsets.ISO_8859_1))
                .doesNotContain(new String(secret, StandardCharsets.ISO_8859_1))
                .doesNotContain(base32);
        assertThat(stored.getSecretNonce()).hasSize(12);
        assertThat(new TotpSecretCipher(ENCRYPTION_KEY).decrypt(stored.getEncryptedSecret(), stored.getSecretNonce(),
                "motorph-totp-credential:" + USER_ID)).isEqualTo(secret);
    }

    @Test
    @DisplayName("No response except the one-time setup response contains the secret, and nothing sensitive is audited")
    void secretAndCodesAreNotExposed() throws Exception {
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();
        TotpSetupResponse setup = authController.startTotpSetup(new TotpSetupStartRequest(challengeId), http);
        byte[] secret = secretFrom(setup);
        String base32 = new Base32().encodeToString(secret).replace("=", "");
        String confirmCode = codeNow(secret);

        ObjectMapper json = new ObjectMapper();
        List<String> bodies = new ArrayList<>();
        bodies.add(json.writeValueAsString(login(EMAIL, OLD_PASSWORD)));

        TokenResponse tokens = authController.confirmTotpSetup(new TotpCodeRequest(challengeId, confirmCode), http);
        bodies.add(json.writeValueAsString(tokens));

        clock.advance(Duration.ofSeconds(30));
        String loginCode = codeNow(secret);
        String loginChallenge = login(EMAIL, OLD_PASSWORD).challengeId();
        bodies.add(json.writeValueAsString(
                authController.verifyTotp(new TotpCodeRequest(loginChallenge, loginCode), http)));

        ForgotPasswordResponse forgot = authController.forgotPassword(new ForgotPasswordRequest(EMAIL), http);
        bodies.add(json.writeValueAsString(forgot));

        clock.advance(Duration.ofSeconds(30));
        String recoveryCode = codeNow(secret);
        PasswordRecoveryVerifyResponse verified = authController.verifyPasswordRecovery(
                new TotpCodeRequest(forgot.challengeId(), recoveryCode), http);
        bodies.add(json.writeValueAsString(verified));

        assertThat(json.writeValueAsString(setup)).contains(setup.manualEntryKey()).contains("qrCodePngBase64");
        for (String body : bodies) {
            assertThat(body).doesNotContain(base32).doesNotContain("secret");
        }

        authController.resetPassword(new ResetPasswordRequest(verified.resetToken(), NEW_PASSWORD, NEW_PASSWORD), http);

        List<String> sensitive = List.of(base32, confirmCode, loginCode, recoveryCode, verified.resetToken(),
                tokens.accessToken(), tokens.refreshToken(), challengeId, loginChallenge, forgot.challengeId(),
                NEW_PASSWORD, OLD_PASSWORD, ENCRYPTION_KEY);
        for (AuditEntry entry : audit) {
            for (String value : sensitive) {
                assertThat(entry.toString()).doesNotContain(value);
            }
        }
        assertThat(setup.toString()).doesNotContain(base32).doesNotContain(setup.manualEntryKey());
    }

    // ---------------------------------------------------------------- Forgot Password

    @Test
    @DisplayName("Forgot Password answers alike for an enrolled user, an unenrolled user and an unknown email")
    void forgotPasswordIsGeneric() {
        enroll();

        ForgotPasswordResponse enrolled = authController.forgotPassword(new ForgotPasswordRequest(EMAIL), http);
        ForgotPasswordResponse unenrolled = authController.forgotPassword(new ForgotPasswordRequest("it.user@motorph.com"), http);
        ForgotPasswordResponse unknown = authController.forgotPassword(new ForgotPasswordRequest("nobody@motorph.com"), http);

        assertThat(enrolled.message()).isEqualTo(unenrolled.message()).isEqualTo(unknown.message())
                .contains("contact an administrator");
        assertThat(enrolled.expiresInSeconds()).isEqualTo(unenrolled.expiresInSeconds()).isEqualTo(unknown.expiresInSeconds());
        assertThat(List.of(enrolled.challengeId(), unenrolled.challengeId(), unknown.challengeId()))
                .allSatisfy(id -> assertThat(id).isNotBlank().hasSize(43));
        // only the enrolled account gets a stored challenge (plus the one from enrolment itself)
        assertThat(challenges.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Recovery needs a valid enrolled authenticator: unenrolled and unknown accounts get the same refusal")
    void recoveryRequiresEnrolledTotp() {
        enroll();
        ForgotPasswordResponse unenrolled = authController.forgotPassword(new ForgotPasswordRequest("it.user@motorph.com"), http);
        ForgotPasswordResponse unknown = authController.forgotPassword(new ForgotPasswordRequest("nobody@motorph.com"), http);

        assertThatThrownBy(() -> authController.verifyPasswordRecovery(new TotpCodeRequest(unenrolled.challengeId(), "123456"), http))
                .isInstanceOf(OtpVerificationException.class).hasMessage(TotpService.RECOVERY_FAILURE_MESSAGE);
        assertThatThrownBy(() -> authController.verifyPasswordRecovery(new TotpCodeRequest(unknown.challengeId(), "123456"), http))
                .isInstanceOf(OtpVerificationException.class).hasMessage(TotpService.RECOVERY_FAILURE_MESSAGE);
    }

    @Test
    @DisplayName("Five wrong codes during recovery close it, and every failure reads the same")
    void recoveryBlocksAfterFiveWrongCodes() {
        byte[] secret = enroll();
        clock.advance(Duration.ofSeconds(30));
        ForgotPasswordResponse forgot = authController.forgotPassword(new ForgotPasswordRequest(EMAIL), http);
        String wrong = wrongCode(secret);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authController.verifyPasswordRecovery(new TotpCodeRequest(forgot.challengeId(), wrong), http))
                    .isInstanceOf(OtpVerificationException.class).hasMessage(TotpService.RECOVERY_FAILURE_MESSAGE);
        }

        assertThatThrownBy(() -> authController.verifyPasswordRecovery(new TotpCodeRequest(forgot.challengeId(), codeNow(secret)), http))
                .isInstanceOf(OtpVerificationException.class).hasMessage(TotpService.RECOVERY_FAILURE_MESSAGE);
    }

    @Test
    @DisplayName("A recovery challenge expires")
    void recoveryChallengeExpires() {
        byte[] secret = enroll();
        clock.advance(Duration.ofSeconds(30));
        ForgotPasswordResponse forgot = authController.forgotPassword(new ForgotPasswordRequest(EMAIL), http);
        String code = codeNow(secret);

        clock.advance(Duration.ofMinutes(6));

        assertThatThrownBy(() -> authController.verifyPasswordRecovery(new TotpCodeRequest(forgot.challengeId(), code), http))
                .isInstanceOf(OtpVerificationException.class);
    }

    @Test
    @DisplayName("The recovery code cannot be a code that was already used to sign in")
    void recoveryCodeCannotBeReplayed() {
        byte[] secret = enroll();
        clock.advance(Duration.ofSeconds(30));
        String code = codeNow(secret);
        authController.verifyTotp(new TotpCodeRequest(login(EMAIL, OLD_PASSWORD).challengeId(), code), http);

        ForgotPasswordResponse forgot = authController.forgotPassword(new ForgotPasswordRequest(EMAIL), http);

        assertThatThrownBy(() -> authController.verifyPasswordRecovery(new TotpCodeRequest(forgot.challengeId(), code), http))
                .isInstanceOf(OtpVerificationException.class);
    }

    @Test
    @DisplayName("Recovery with a valid code sets a new password, signs out old sessions, and is audited")
    void recoveryResetsThePassword() {
        byte[] secret = enroll();
        TokenResponse oldSession = tokenIssuance.issueTokenPair(USER_ID, 101);
        assertThat(tokenIssuance.refresh(oldSession.refreshToken()).accessToken()).isNotBlank();

        clock.advance(Duration.ofSeconds(30));
        ForgotPasswordResponse forgot = authController.forgotPassword(new ForgotPasswordRequest(EMAIL), http);
        PasswordRecoveryVerifyResponse verified = authController.verifyPasswordRecovery(
                new TotpCodeRequest(forgot.challengeId(), codeNow(secret)), http);

        authController.resetPassword(new ResetPasswordRequest(verified.resetToken(), NEW_PASSWORD, NEW_PASSWORD), http);

        assertThatThrownBy(() -> login(EMAIL, OLD_PASSWORD)).isInstanceOf(AuthenticationException.class);
        assertThat(login(EMAIL, NEW_PASSWORD).status()).isEqualTo("TOTP_REQUIRED");
        assertThat(PasswordHasher.verify(NEW_PASSWORD, users.byId(USER_ID).getPasswordSalt(),
                users.byId(USER_ID).getPasswordHash())).isTrue();

        assertThatThrownBy(() -> tokenIssuance.refresh(oldSession.refreshToken()))
                .isInstanceOf(AuthenticationException.class).hasMessage("Refresh token has been revoked.");
        assertThat(auditActions()).contains("PASSWORD_RESET_COMPLETED");
    }

    @Test
    @DisplayName("The reset token works once")
    void resetTokenIsSingleUse() {
        String token = recoveryToken(enroll());

        authController.resetPassword(new ResetPasswordRequest(token, NEW_PASSWORD, NEW_PASSWORD), http);

        assertThatThrownBy(() -> authController.resetPassword(new ResetPasswordRequest(token, "AnotherPass2", "AnotherPass2"), http))
                .isInstanceOf(AuthenticationException.class);
        assertThat(PasswordHasher.verify(NEW_PASSWORD, users.byId(USER_ID).getPasswordSalt(),
                users.byId(USER_ID).getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("The reset token expires")
    void resetTokenExpires() {
        String token = recoveryToken(enroll());

        clock.advance(Duration.ofMinutes(11));

        assertThatThrownBy(() -> authController.resetPassword(new ResetPasswordRequest(token, NEW_PASSWORD, NEW_PASSWORD), http))
                .isInstanceOf(AuthenticationException.class);
        assertThat(PasswordHasher.verify(OLD_PASSWORD, users.byId(USER_ID).getPasswordSalt(),
                users.byId(USER_ID).getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("A password that breaks the rules or does not match is refused without using up the token")
    void invalidNewPasswordKeepsTheToken() {
        String token = recoveryToken(enroll());

        assertThatThrownBy(() -> authController.resetPassword(new ResetPasswordRequest(token, "weak", "weak"), http))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> authController.resetPassword(new ResetPasswordRequest(token, NEW_PASSWORD, "Different1"), http))
                .isInstanceOf(ValidationException.class);

        authController.resetPassword(new ResetPasswordRequest(token, NEW_PASSWORD, NEW_PASSWORD), http);

        assertThat(PasswordHasher.verify(NEW_PASSWORD, users.byId(USER_ID).getPasswordSalt(),
                users.byId(USER_ID).getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("A guessed or blank reset token does nothing")
    void unknownResetTokenIsRefused() {
        assertThatThrownBy(() -> authController.resetPassword(new ResetPasswordRequest("made-up-token", NEW_PASSWORD, NEW_PASSWORD), http))
                .isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(() -> authController.resetPassword(new ResetPasswordRequest("  ", NEW_PASSWORD, NEW_PASSWORD), http))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @DisplayName("Only the reset token's SHA-256 is stored")
    void resetTokenIsStoredHashed() {
        String token = recoveryToken(enroll());

        assertThat(recoveryTokens.hashes()).hasSize(1).doesNotContain(token);
        assertThat(recoveryTokens.hashes().iterator().next()).matches("[0-9a-f]{64}");
    }

    // ---------------------------------------------------------------- administrator reset

    @Test
    @DisplayName("A caller whose role lacks the reset permission is refused and nothing changes")
    void nonAdminCannotResetTotp() {
        enroll();
        actAs(IT_ID, Role.IT);
        when(permissionRepository.roleHasPermission(Role.IT, PermissionKey.RESET_USER_TOTP.name())).thenReturn(false);

        assertThatThrownBy(() -> userAccountController.resetTotp(USER_ID)).isInstanceOf(UnauthorizedException.class);

        assertThat(credentials.stored(USER_ID)).isNotNull();
        assertThat(auditActions()).doesNotContain("TOTP_RESET_BY_ADMIN");
    }

    @Test
    @DisplayName("An administrator's reset is audited and forces enrolment at the next login")
    void adminResetForcesReEnrollment() {
        enroll();
        clock.advance(Duration.ofSeconds(30));
        assertThat(login(EMAIL, OLD_PASSWORD).status()).isEqualTo("TOTP_REQUIRED");

        actAs(ADMIN_ID, Role.ADMIN);
        when(permissionRepository.roleHasPermission(Role.ADMIN, PermissionKey.RESET_USER_TOTP.name())).thenReturn(true);

        userAccountController.resetTotp(USER_ID);

        assertThat(credentials.stored(USER_ID)).isNull();
        assertThat(audit).anySatisfy(entry -> {
            assertThat(entry.actor()).isEqualTo(ADMIN_ID);
            assertThat(entry.action()).isEqualTo("TOTP_RESET_BY_ADMIN");
        });
        assertThat(login(EMAIL, OLD_PASSWORD).status()).isEqualTo("TOTP_SETUP_REQUIRED");

        byte[] secret = enroll();
        assertThat(credentials.stored(USER_ID).isEnabled()).isTrue();
        assertThat(secret).hasSize(20);
    }

    // ---------------------------------------------------------------- helpers

    private LoginResponse login(String email, String password) {
        return authController.login(new LoginRequest(email, password), http);
    }

    /** Runs the whole first-login enrolment for the standard user and returns the secret. */
    private byte[] enroll() {
        String challengeId = login(EMAIL, OLD_PASSWORD).challengeId();
        TotpSetupResponse setup = authController.startTotpSetup(new TotpSetupStartRequest(challengeId), http);
        byte[] secret = secretFrom(setup);

        authController.confirmTotpSetup(new TotpCodeRequest(challengeId, codeNow(secret)), http);

        return secret;
    }

    /** Takes the standard user through recovery and returns the reset token. */
    private String recoveryToken(byte[] secret) {
        clock.advance(Duration.ofSeconds(30));
        ForgotPasswordResponse forgot = authController.forgotPassword(new ForgotPasswordRequest(EMAIL), http);

        return authController.verifyPasswordRecovery(
                new TotpCodeRequest(forgot.challengeId(), codeNow(secret)), http).resetToken();
    }

    private byte[] secretFrom(TotpSetupResponse setup) {
        return new Base32().decode(setup.manualEntryKey().replace(" ", ""));
    }

    /** A six-digit code that is not valid for the current step or its neighbours. */
    private String wrongCode(byte[] secret) {
        long step = codes.currentTimeStep();
        List<String> valid = List.of(codes.codeForStep(secret, step - 1), codes.codeForStep(secret, step),
                codes.codeForStep(secret, step + 1));

        for (int candidate = 0; ; candidate++) {
            String code = String.format("%06d", candidate);

            if (!valid.contains(code)) {
                return code;
            }
        }
    }

    private String codeNow(byte[] secret) {
        return codes.codeForStep(secret, codes.currentTimeStep());
    }

    private List<String> auditActions() {
        return audit.stream().map(AuditEntry::action).toList();
    }

    private void actAs(int userAccountId, Role role) {
        when(currentUser.getCurrentUserAccountId()).thenReturn(userAccountId);
        when(currentUser.getRequestedRole()).thenReturn(role);
    }

    private UserAccount account(int id, String email, String password, Role role) {
        String salt = PasswordHasher.generateSalt();

        return new UserAccount(id, 100 + id, email, email, PasswordHasher.hash(password, salt), salt,
                List.of(role), false, true, null, false, null);
    }

    // ---------------------------------------------------------------- in-memory repositories

    private static final class FakeUsers extends UserAccountRepository {
        private final Map<Integer, UserAccount> accounts = new HashMap<>();

        FakeUsers() {
            super(null);
        }

        void add(UserAccount account) {
            accounts.put(account.getUserAccountId(), account);
        }

        UserAccount byId(int id) {
            return accounts.get(id);
        }

        @Override
        public UserAccount findByCompanyEmail(String companyEmail) {
            return accounts.values().stream()
                    .filter(a -> a.getCompanyEmail().equals(companyEmail))
                    .findFirst().orElse(null);
        }

        @Override
        public UserAccount findById(Integer userAccountId) {
            return accounts.get(userAccountId);
        }

        @Override
        public void updateLastLogin(int userAccountId) {
            accounts.get(userAccountId).setLastLoginAt(LocalDateTime.now());
        }

        @Override
        public void updatePassword(int userAccountId, String hash, String salt) {
            UserAccount account = accounts.get(userAccountId);
            account.updatePasswordCredentials(hash, salt);
            account.setMustChangePassword(false);
        }
    }

    private static final class FakeChallenges extends LoginChallengeRepository {
        private final Map<String, LoginChallenge> rows = new HashMap<>();

        FakeChallenges() {
            super(null);
        }

        int count() {
            return rows.size();
        }

        int attempts(String challengeId) {
            return rows.get(challengeId).getAttemptCount();
        }

        @Override
        public void save(LoginChallenge challenge) {
            rows.put(challenge.getChallengeId(), challenge);
        }

        @Override
        public LoginChallenge findById(String challengeId) {
            return rows.get(challengeId);
        }

        @Override
        public boolean markConsumed(String challengeId) {
            LoginChallenge c = rows.get(challengeId);

            if (c == null || c.isConsumed()) {
                return false;
            }

            rows.put(challengeId, copy(c, true, c.getAttemptCount(), c.getPendingSecretEncrypted(), c.getPendingSecretNonce()));
            return true;
        }

        @Override
        public int incrementAttemptCount(String challengeId) {
            LoginChallenge c = rows.get(challengeId);
            rows.put(challengeId, copy(c, c.isConsumed(), c.getAttemptCount() + 1, c.getPendingSecretEncrypted(), c.getPendingSecretNonce()));
            return c.getAttemptCount() + 1;
        }

        @Override
        public void savePendingSecret(String challengeId, byte[] encryptedSecret, byte[] nonce) {
            LoginChallenge c = rows.get(challengeId);
            rows.put(challengeId, copy(c, c.isConsumed(), c.getAttemptCount(), encryptedSecret, nonce));
        }

        private LoginChallenge copy(LoginChallenge c, boolean consumed, int attempts, byte[] secret, byte[] nonce) {
            return new LoginChallenge(c.getChallengeId(), c.getUserAccountId(), c.getPurpose(), c.getExpiresAt(),
                    consumed, attempts, secret, nonce, c.getCreatedAt());
        }
    }

    private static final class FakeCredentials extends TotpCredentialRepository {
        private final Map<Integer, TotpCredential> rows = new HashMap<>();

        FakeCredentials() {
            super(null);
        }

        TotpCredential stored(int userAccountId) {
            return rows.get(userAccountId);
        }

        @Override
        public TotpCredential findByUserAccountId(int userAccountId) {
            return rows.get(userAccountId);
        }

        @Override
        public void saveEnabled(int userAccountId, byte[] encryptedSecret, byte[] nonce,
                                LocalDateTime enrolledAt, long acceptedTimeStep) {
            if (rows.containsKey(userAccountId)) {
                throw new IllegalStateException("duplicate enrolment");
            }

            rows.put(userAccountId, new TotpCredential(userAccountId, encryptedSecret, nonce, true, enrolledAt, acceptedTimeStep));
        }

        @Override
        public boolean advanceLastUsedTimeStep(int userAccountId, long timeStep) {
            TotpCredential c = rows.get(userAccountId);

            if (c.getLastUsedTimeStep() != null && c.getLastUsedTimeStep() >= timeStep) {
                return false;
            }

            rows.put(userAccountId, new TotpCredential(userAccountId, c.getEncryptedSecret(), c.getSecretNonce(),
                    c.isEnabled(), c.getEnrolledAt(), timeStep));
            return true;
        }

        @Override
        public boolean deleteByUserAccountId(int userAccountId) {
            return rows.remove(userAccountId) != null;
        }
    }

    private static final class FakeRecoveryTokens extends PasswordRecoveryTokenRepository {
        private record Row(int userAccountId, LocalDateTime expiresAt, boolean consumed) {
        }

        private final Map<String, Row> rows = new HashMap<>();

        FakeRecoveryTokens() {
            super(null);
        }

        java.util.Set<String> hashes() {
            return rows.keySet();
        }

        @Override
        public void save(String tokenHash, int userAccountId, LocalDateTime expiresAt, LocalDateTime createdAt) {
            rows.put(tokenHash, new Row(userAccountId, expiresAt, false));
        }

        @Override
        public OptionalInt consume(String tokenHash, LocalDateTime now) {
            Row row = rows.get(tokenHash);

            if (row == null || row.consumed() || !row.expiresAt().isAfter(now)) {
                return OptionalInt.empty();
            }

            rows.put(tokenHash, new Row(row.userAccountId(), row.expiresAt(), true));
            return OptionalInt.of(row.userAccountId());
        }

        @Override
        public void invalidateAllForUser(int userAccountId) {
            rows.replaceAll((hash, row) -> row.userAccountId() == userAccountId
                    ? new Row(row.userAccountId(), row.expiresAt(), true) : row);
        }
    }

    private static final class FakeRevocations extends TokenRevocationRepository {
        private final java.util.Set<String> revoked = new java.util.HashSet<>();
        private final Map<Integer, LocalDateTime> sessionsRevokedAt = new HashMap<>();

        FakeRevocations() {
            super(null);
        }

        @Override
        public void revoke(String jti, int userAccountId, LocalDateTime tokenExpiresAt) {
            revoked.add(jti);
        }

        @Override
        public boolean isRevoked(String jti) {
            return revoked.contains(jti);
        }

        @Override
        public void revokeAllSessions(int userAccountId, LocalDateTime revokedAt) {
            sessionsRevokedAt.put(userAccountId, revokedAt);
        }

        @Override
        public boolean isIssuedBeforeSessionRevocation(int userAccountId, Instant issuedAt) {
            LocalDateTime revokedAt = sessionsRevokedAt.get(userAccountId);

            return revokedAt != null
                    && TokenRevocationRepository.predatesRevocation(issuedAt, revokedAt.atZone(ZoneId.systemDefault()).toInstant());
        }
    }
}
