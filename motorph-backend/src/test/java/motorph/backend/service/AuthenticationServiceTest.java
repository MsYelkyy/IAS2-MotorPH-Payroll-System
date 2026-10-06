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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                .hasMessage("Account is locked.");

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
}
