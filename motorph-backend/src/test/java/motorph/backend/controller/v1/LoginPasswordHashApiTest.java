package motorph.backend.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import motorph.backend.configuration.ClockConfig;
import motorph.backend.domain.TotpCredential;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.ChallengePurpose;
import motorph.backend.domain.value.Role;
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.TotpCredentialRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.security.JwtAuthenticationFilter;
import motorph.backend.security.JwtTokenProvider;
import motorph.backend.security.LoginRateLimiter;
import motorph.backend.security.PasswordHasher;
import motorph.backend.security.SecurityConfig;
import motorph.backend.security.TokenIssuanceService;
import motorph.backend.service.AuthenticationService;
import motorph.backend.service.LoginChallengeService;
import motorph.backend.service.PasswordRecoveryService;
import motorph.backend.service.PermissionService;
import motorph.backend.service.TotpService;
import motorph.backend.service.UserAccessLogService;
import motorph.backend.service.UserAccountService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Password login through the real controller, validation, exception handler and
 * {@link AuthenticationService}, with real password hashing. Covers an account that still has an
 * old-format hash and one that has the new versioned hash, and checks that no password, hash or
 * salt reaches the response, the console log (at DEBUG) or the audit log.
 */
@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        CurrentUserProvider.class,
        AuthenticationService.class,
        ClockConfig.class
})
@TestPropertySource(properties = {
        "motorph.jwt.secret=control-three-test-signing-secret-with-at-least-32-bytes",
        "logging.level.motorph=TRACE",
        "logging.level.org.springframework.web=DEBUG",
        "logging.level.org.springframework.security=DEBUG"
})
@ExtendWith(OutputCaptureExtension.class)
class LoginPasswordHashApiTest {

    private static final int USER_ACCOUNT_ID = 12;
    private static final int EMPLOYEE_ID = 101;
    private static final String EMAIL = "employee@motorph.com";
    private static final String PASSWORD = "Correct-Horse-9";
    private static final String LOGIN = "/api/v1/auth/login";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private TotpCredentialRepository totpCredentialRepository;
    @MockitoBean private TokenRevocationRepository tokenRevocationRepository;
    @MockitoBean private UserAccessLogService userAccessLogService;
    @MockitoBean private LoginChallengeService loginChallengeService;
    @MockitoBean private LoginRateLimiter loginRateLimiter;
    @MockitoBean private TokenIssuanceService tokenIssuanceService;
    @MockitoBean private TotpService totpService;
    @MockitoBean private PasswordRecoveryService passwordRecoveryService;
    @MockitoBean private UserAccountService userAccountService;
    @MockitoBean private PermissionService permissionService;

    private String salt;
    private String legacyHash;
    private String currentHash;

    @BeforeEach
    void setUp() throws Exception {
        salt = PasswordHasher.generateSalt();
        legacyHash = legacyHash(PASSWORD, salt);
        currentHash = PasswordHasher.hash(PASSWORD, salt);

        when(userAccountRepository.resetFailedPasswordAttempts(USER_ACCOUNT_ID)).thenReturn(true);
        when(loginChallengeService.issue(USER_ACCOUNT_ID, ChallengePurpose.TOTP_SETUP)).thenReturn("setup-challenge");
        when(loginChallengeService.issue(USER_ACCOUNT_ID, ChallengePurpose.TOTP_LOGIN)).thenReturn("login-challenge");
    }

    // The stored hash format

    @Test
    @DisplayName("Legacy hash: a correct password signs in and the stored hash is upgraded to the stronger format")
    void legacyHashLogsInAndIsUpgraded(CapturedOutput output) throws Exception {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(legacyHash, false, true));

        String body = login(PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TOTP_SETUP_REQUIRED"))
                .andExpect(jsonPath("$.challengeId").value("setup-challenge"))
                .andReturn().getResponse().getContentAsString();

        ArgumentCaptor<String> newHash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> newSalt = ArgumentCaptor.forClass(String.class);
        verify(userAccountRepository).updatePasswordHash(eq(USER_ACCOUNT_ID), newHash.capture(), newSalt.capture());

        assertThat(newHash.getValue()).startsWith("pbkdf2_sha256$600000$");
        assertThat(newSalt.getValue()).isNotEqualTo(salt);
        assertThat(PasswordHasher.verify(PASSWORD, newSalt.getValue(), newHash.getValue())).isTrue();
        assertThat(PasswordHasher.needsRehash(newHash.getValue())).isFalse();

        assertNoSecretsAnywhere(body, output, PASSWORD, legacyHash, salt, newHash.getValue(), newSalt.getValue());
    }

    @Test
    @DisplayName("New hash: a correct password signs in and the stored hash is left alone")
    void currentHashLogsInWithoutRewrite(CapturedOutput output) throws Exception {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(currentHash, false, true));

        String body = login(PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TOTP_SETUP_REQUIRED"))
                .andReturn().getResponse().getContentAsString();

        verify(userAccountRepository, never()).updatePasswordHash(anyInt(), anyString(), anyString());

        assertNoSecretsAnywhere(body, output, PASSWORD, currentHash, salt);
    }

    @ParameterizedTest(name = "{0} hash")
    @DisplayName("Either hash format asks an enrolled user for the authenticator code")
    @ValueSource(strings = {"legacy", "current"})
    void enrolledUserIsAskedForTheCode(String format) throws Exception {
        TotpCredential enrolled = mock(TotpCredential.class);
        when(enrolled.isEnabled()).thenReturn(true);
        when(totpCredentialRepository.findByUserAccountId(USER_ACCOUNT_ID)).thenReturn(enrolled);
        when(userAccountRepository.findByCompanyEmail(EMAIL))
                .thenReturn(account("legacy".equals(format) ? legacyHash : currentHash, false, true));

        login(PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TOTP_REQUIRED"))
                .andExpect(jsonPath("$.challengeId").value("login-challenge"));
    }

    @ParameterizedTest(name = "{0} hash")
    @DisplayName("A wrong password is refused with the generic 401, counts as a failure and never upgrades the hash")
    @ValueSource(strings = {"legacy", "current"})
    void wrongPasswordIsRefused(String format, CapturedOutput output) throws Exception {
        when(userAccountRepository.findByCompanyEmail(EMAIL))
                .thenReturn(account("legacy".equals(format) ? legacyHash : currentHash, false, true));

        String body = login("Wrong-Password-1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password."))
                .andExpect(jsonPath("$.challengeId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        verify(userAccountRepository).recordFailedPasswordAttempt(eq(USER_ACCOUNT_ID), anyInt());
        verify(userAccountRepository, never()).updatePasswordHash(anyInt(), anyString(), anyString());
        verifyNoInteractions(loginChallengeService);

        assertNoSecretsAnywhere(body, output, "Wrong-Password-1", PASSWORD, legacyHash, currentHash, salt);
    }

    @Test
    @DisplayName("A locked account is refused with 401 and the lock reason, and its hash is not touched")
    void lockedAccountIsRefused() throws Exception {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(legacyHash, true, true));

        login(PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.reason").value("ACCOUNT_LOCKED"));

        verify(userAccountRepository, never()).updatePasswordHash(anyInt(), anyString(), anyString());
        verifyNoInteractions(loginChallengeService);
    }

    @Test
    @DisplayName("An inactive account is refused with 401, and its hash is not touched")
    void inactiveAccountIsRefused() throws Exception {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(legacyHash, false, false));

        login(PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is inactive."));

        verify(userAccountRepository, never()).updatePasswordHash(anyInt(), anyString(), anyString());
        verifyNoInteractions(loginChallengeService);
    }

    @Test
    @DisplayName("An unknown email gets the same generic 401 as a wrong password")
    void unknownEmailIsGeneric() throws Exception {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(null);

        login(PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    // Helpers

    private ResultActions login(String password) throws Exception {
        return mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("companyEmail", EMAIL, "password", password))));
    }

    /**
     * Neither the response, the console log nor any audit-log call may contain a password, a stored
     * hash or a salt.
     */
    private void assertNoSecretsAnywhere(String responseBody, CapturedOutput output, String... secrets) {
        List<String> auditArguments = Mockito.mockingDetails(userAccessLogService).getInvocations().stream()
                .flatMap(invocation -> Stream.of(invocation.getArguments()))
                .map(String::valueOf)
                .collect(Collectors.toList());

        assertThat(responseBody).doesNotContain(secrets);
        assertThat(output.getAll()).doesNotContain(secrets);
        assertThat(String.join("\n", auditArguments)).doesNotContain(secrets);
    }

    private UserAccount account(String hash, boolean locked, boolean active) {
        return new UserAccount(
                USER_ACCOUNT_ID, EMPLOYEE_ID, EMAIL, EMAIL, hash, salt,
                List.of(Role.EMPLOYEE), locked, active, null, false, null);
    }

    /**
     * The format before the upgrade: 60,000 iterations, a 128-bit key, no prefix.
     */
    private static String legacyHash(String password, String saltBase64) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(saltBase64), 60_000, 128);

        return Base64.getEncoder().encodeToString(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());
    }
}
