package motorph.backend.service;

import motorph.backend.domain.TotpCredential;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.repository.TotpCredentialRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.PasswordHasher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static motorph.backend.repository.UserAccountRepository.FailedPasswordAttemptResult.ATTEMPT_RECORDED;
import static motorph.backend.repository.UserAccountRepository.FailedPasswordAttemptResult.LOCK_APPLIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * The password check decides the next step and never completes a login by itself, and a locked
 * account is audited once however far the login got.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticationServiceTest {

    private static final int ACCOUNT_ID = 12;
    private static final String EMAIL = "juan.cruz@motorph.com";
    private static final String PASSWORD = "CorrectPassw0rd";

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private TotpCredentialRepository totpCredentialRepository;
    @Mock private UserAccessLogService userAccessLogService;

    @InjectMocks private AuthenticationService authenticationService;

    private String salt;
    private String hash;

    @BeforeEach
    void setUp() {
        salt = PasswordHasher.generateSalt();
        hash = PasswordHasher.hash(PASSWORD, salt);
        when(userAccountRepository.resetFailedPasswordAttempts(ACCOUNT_ID)).thenReturn(true);
    }

    @Test
    @DisplayName("A correct password without an enrolled authenticator asks for enrolment")
    void passwordOkWithoutTotpRequiresSetup() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(false, false));
        when(totpCredentialRepository.findByUserAccountId(ACCOUNT_ID)).thenReturn(null);

        LoginAttemptOutcome outcome = authenticationService.authenticate(EMAIL, PASSWORD);

        assertThat(outcome.getType()).isEqualTo(LoginAttemptOutcome.Type.TOTP_SETUP_REQUIRED);
        assertThat(outcome.getUserAccountId()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    @DisplayName("A correct password with an enabled authenticator asks for the code")
    void passwordOkWithTotpRequiresCode() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(false, false));
        when(totpCredentialRepository.findByUserAccountId(ACCOUNT_ID)).thenReturn(
                new TotpCredential(ACCOUNT_ID, new byte[36], new byte[12], true, LocalDateTime.now(), 5L));

        assertThat(authenticationService.authenticate(EMAIL, PASSWORD).getType())
                .isEqualTo(LoginAttemptOutcome.Type.TOTP_REQUIRED);
    }

    @Test
    @DisplayName("An incorrect password increments the targeted account failure count")
    void incorrectPasswordIncrementsAccountCount() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(false, false));
        when(userAccountRepository.recordFailedPasswordAttempt(ACCOUNT_ID, 5)).thenReturn(ATTEMPT_RECORDED);

        assertThatThrownBy(() -> authenticationService.authenticate(EMAIL, "WrongPassw0rd"))
                .isInstanceOfSatisfying(AuthenticationException.class, failure -> {
                    assertThat(failure.getMessage()).isEqualTo("Invalid email or password.");
                    assertThat(failure.getReason()).isNull();
                });

        verify(userAccountRepository).recordFailedPasswordAttempt(ACCOUNT_ID, 5);
        verify(userAccessLogService).logLoginFailed(EMAIL, "Login failed because password did not match.");
        verify(userAccessLogService, never()).logAutomaticAccountLock(anyInt());
        verify(userAccountRepository, never()).resetFailedPasswordAttempts(ACCOUNT_ID);
    }

    @Test
    @DisplayName("An unknown email keeps the generic authentication response without a lock reason")
    void unknownEmailDoesNotReturnLockReason() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> authenticationService.authenticate(EMAIL, "WrongPassw0rd"))
                .isInstanceOfSatisfying(AuthenticationException.class, failure -> {
                    assertThat(failure.getMessage()).isEqualTo("Invalid email or password.");
                    assertThat(failure.getReason()).isNull();
                });

        verify(userAccountRepository, never()).recordFailedPasswordAttempt(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Reaching the configured threshold applies and audits an automatic lock")
    void thresholdFailureAppliesAutomaticLock() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(false, false));
        when(userAccountRepository.recordFailedPasswordAttempt(ACCOUNT_ID, 5)).thenReturn(LOCK_APPLIED);

        assertThatThrownBy(() -> authenticationService.authenticate(EMAIL, "WrongPassw0rd"))
                .isInstanceOfSatisfying(AuthenticationException.class, failure -> {
                    assertThat(failure.getMessage()).isEqualTo("Invalid email or password.");
                    assertThat(failure.getReason()).isEqualTo(AuthenticationException.Reason.ACCOUNT_LOCKED);
                });

        verify(userAccessLogService).logAutomaticAccountLock(ACCOUNT_ID);
    }

    @ParameterizedTest(name = "A persistently locked account rejects password: {0}")
    @ValueSource(strings = {PASSWORD, "WrongPassw0rd"})
    @DisplayName("A persistently locked account returns the lock reason without checking the password")
    void lockedAccountRejectsLoginWithoutCheckingPassword(String submittedPassword) {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(true, false));

        assertThatThrownBy(() -> authenticationService.authenticate(EMAIL, submittedPassword))
                .isInstanceOfSatisfying(AuthenticationException.class, failure -> {
                    assertThat(failure.getMessage()).isEqualTo("Invalid email or password.");
                    assertThat(failure.getReason()).isEqualTo(AuthenticationException.Reason.ACCOUNT_LOCKED);
                });

        verify(userAccountRepository, never()).recordFailedPasswordAttempt(anyInt(), anyInt());
        verify(userAccountRepository, never()).resetFailedPasswordAttempts(ACCOUNT_ID);
    }

    @Test
    @DisplayName("A correct password clears the failure counter before continuing")
    void correctPasswordResetsFailureCounter() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(false, false));

        authenticationService.authenticate(EMAIL, PASSWORD);

        verify(userAccountRepository).resetFailedPasswordAttempts(ACCOUNT_ID);
        verify(userAccountRepository, never()).recordFailedPasswordAttempt(anyInt(), anyInt());
    }

    @Test
    @DisplayName("A correct password on an older hash upgrades the stored hash")
    void correctPasswordUpgradesAnOlderHash() throws Exception {
        String legacy = legacyHash(PASSWORD, salt);
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(accountWithHash(legacy));

        authenticationService.authenticate(EMAIL, PASSWORD);

        ArgumentCaptor<String> newHash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> newSalt = ArgumentCaptor.forClass(String.class);
        verify(userAccountRepository).updatePasswordHash(eq(ACCOUNT_ID), newHash.capture(), newSalt.capture());
        assertThat(PasswordHasher.needsRehash(newHash.getValue())).isFalse();
        assertThat(PasswordHasher.verify(PASSWORD, newSalt.getValue(), newHash.getValue())).isTrue();
    }

    @Test
    @DisplayName("A current hash is not rewritten on login")
    void currentHashIsNotRewritten() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(false, false));

        authenticationService.authenticate(EMAIL, PASSWORD);

        verify(userAccountRepository, never()).updatePasswordHash(anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("A wrong password never upgrades the hash")
    void wrongPasswordDoesNotUpgradeTheHash() throws Exception {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(accountWithHash(legacyHash(PASSWORD, salt)));

        assertThatThrownBy(() -> authenticationService.authenticate(EMAIL, "WrongPassw0rd"))
                .isInstanceOf(AuthenticationException.class);

        verify(userAccountRepository, never()).updatePasswordHash(anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("A credential that is not enabled does not count as enrolled")
    void disabledCredentialRequiresSetup() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(false, false));
        when(totpCredentialRepository.findByUserAccountId(ACCOUNT_ID)).thenReturn(
                new TotpCredential(ACCOUNT_ID, new byte[36], new byte[12], false, null, null));

        assertThat(authenticationService.authenticate(EMAIL, PASSWORD).getType())
                .isEqualTo(LoginAttemptOutcome.Type.TOTP_SETUP_REQUIRED);
    }

    @Test
    @DisplayName("A temporary password is sent to the forced change before any authenticator step")
    void temporaryPasswordComesFirst() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(false, true));

        assertThat(authenticationService.authenticate(EMAIL, PASSWORD).getType())
                .isEqualTo(LoginAttemptOutcome.Type.TEMPORARY_PASSWORD_CHANGE_REQUIRED);
    }

    @Test
    @DisplayName("A wrong password is audited as LOGIN_FAILED and gives the generic message")
    void wrongPasswordIsAudited() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(false, false));

        assertThatThrownBy(() -> authenticationService.authenticate(EMAIL, "WrongPassw0rd"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid email or password.");

        verify(userAccessLogService).logLoginFailed(EMAIL, "Login failed because password did not match.");
    }

    @Test
    @DisplayName("A locked account is audited once as LOGIN_FAILED and never reaches the authenticator step")
    void lockedAccountIsNotAuditedTwice() {
        when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(account(true, false));

        assertThatThrownBy(() -> authenticationService.requireLoginEligible(ACCOUNT_ID))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid email or password.");

        verify(userAccessLogService).logLoginFailed(EMAIL, "Login failed because account is locked.");
        verifyNoMoreInteractions(userAccessLogService);
    }

    @Test
    @DisplayName("Completing a login records the time and audits LOGIN_SUCCESS")
    void completeLoginIsAudited() {
        UserAccount account = account(false, false);

        authenticationService.completeLogin(account);

        verify(userAccountRepository).updateLastLogin(ACCOUNT_ID);
        verify(userAccessLogService).logLoginSuccess(ACCOUNT_ID, "Login successful.");
        verifyNoMoreInteractions(userAccessLogService);
    }

    private UserAccount account(boolean locked, boolean mustChangePassword) {
        return new UserAccount(
                ACCOUNT_ID, 101, EMAIL, EMAIL, hash, salt,
                List.of(Role.EMPLOYEE), locked, true, null,
                mustChangePassword, mustChangePassword ? LocalDateTime.now().plusHours(1) : null);
    }

    private UserAccount accountWithHash(String storedHash) {
        return new UserAccount(
                ACCOUNT_ID, 101, EMAIL, EMAIL, storedHash, salt,
                List.of(Role.EMPLOYEE), false, true, null, false, null);
    }

    private static String legacyHash(String password, String saltBase64) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), Base64.getDecoder().decode(saltBase64), 60_000, 128);
        byte[] bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();

        return Base64.getEncoder().encodeToString(bytes);
    }
}
