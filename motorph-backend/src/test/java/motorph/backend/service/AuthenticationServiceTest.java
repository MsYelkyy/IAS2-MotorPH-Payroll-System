package motorph.backend.service;

import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.dto.auth.OtpPreview;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.exception.OtpVerificationException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.PasswordHasher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static motorph.backend.repository.UserAccountRepository.FailedPasswordAttemptResult.ATTEMPT_RECORDED;
import static motorph.backend.repository.UserAccountRepository.FailedPasswordAttemptResult.LOCK_APPLIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies that failed OTP attempts are recorded without storing sensitive information.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticationServiceTest {

    private static final int ACCOUNT_ID = 12;
    private static final String EMAIL = "juan.cruz@motorph.com";
    private static final String SUBMITTED_OTP = "654321";

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private TwoFactorService twoFactorService;
    @Mock private OtpDeliveryService otpDeliveryService;
    @Mock private UserAccessLogService userAccessLogService;

    @InjectMocks private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(account(false));
    }

    @ParameterizedTest(name = "{0} is audited as OTP_FAILED")
    @CsvSource({
            "INVALID_CODE,            OTP verification failed: incorrect code.",
            "EXPIRED_OR_UNAVAILABLE,  OTP verification failed: code expired or unavailable.",
            "ATTEMPT_LIMIT_REACHED,   OTP verification failed: attempt limit reached."
    })
    void failedOtpIsAudited(OtpVerificationException.Reason reason, String expectedDetails) {
        OtpVerificationException failure = new OtpVerificationException(reason, "Message for the client.");
        doThrow(failure).when(twoFactorService).verifyLoginOtp(ACCOUNT_ID, SUBMITTED_OTP);

        assertThatThrownBy(() -> authenticationService.verifyLoginOtp(ACCOUNT_ID, SUBMITTED_OTP))
                .isSameAs(failure);

        verify(userAccessLogService).logEvent(ACCOUNT_ID, "OTP_FAILED", expectedDetails);
        verify(userAccessLogService, never()).logLoginSuccess(anyInt(), anyString());
        verify(userAccountRepository, never()).updateLastLogin(anyInt());
    }

    @ParameterizedTest(name = "{0}: the audit details contain no secret")
    @EnumSource(OtpVerificationException.Reason.class)
    void auditDetailsNeverContainSecrets(OtpVerificationException.Reason reason) {
        doThrow(new OtpVerificationException(reason, "Message for the client."))
                .when(twoFactorService).verifyLoginOtp(ACCOUNT_ID, SUBMITTED_OTP);

        assertThatThrownBy(() -> authenticationService.verifyLoginOtp(ACCOUNT_ID, SUBMITTED_OTP))
                .isInstanceOf(OtpVerificationException.class);

        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(userAccessLogService).logEvent(eq(ACCOUNT_ID), eq("OTP_FAILED"), details.capture());

        assertThat(details.getValue())
                .doesNotContain(SUBMITTED_OTP)
                .doesNotContain(EMAIL);
    }

    @Test
    @DisplayName("A correct OTP is audited only as LOGIN_SUCCESS, never as a failure")
    void successfulOtpIsNotAuditedAsFailure() {
        UserAccount account = authenticationService.verifyLoginOtp(ACCOUNT_ID, SUBMITTED_OTP);

        assertThat(account.getUserAccountId()).isEqualTo(ACCOUNT_ID);
        verify(userAccessLogService).logLoginSuccess(ACCOUNT_ID, "Login successful.");
        verifyNoMoreInteractions(userAccessLogService);
    }

    @Test
    @DisplayName("Valid company credentials start OTP delivery without recording a completed login")
    void validCredentialsStartOtpFlow() {
        String rawPassword = "CorrectHorseBatteryStaple!";
        String salt = PasswordHasher.generateSalt();
        UserAccount account = new UserAccount(
                ACCOUNT_ID, 101, EMAIL, EMAIL,
                PasswordHasher.hash(rawPassword, salt), salt,
                List.of(Role.EMPLOYEE), false, true, null, false, null);
        String rawOtp = "123456";
        OtpPreview preview = new OtpPreview("no-reply@motorph.com", EMAIL, "Verification", "Message", rawOtp);

        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account);
        when(employeeRepository.findPersonalEmailByEmployeeId(101)).thenReturn("juan.personal@example.com");
        when(twoFactorService.createLoginOtp(ACCOUNT_ID)).thenReturn(rawOtp);
        when(otpDeliveryService.sendLoginOtp(EMAIL, rawOtp)).thenReturn(preview);
        when(userAccountRepository.resetFailedPasswordAttempts(ACCOUNT_ID)).thenReturn(true);

        LoginAttemptOutcome outcome = authenticationService.authenticate("  " + EMAIL + "  ", rawPassword);

        assertThat(outcome.getType()).isEqualTo(LoginAttemptOutcome.Type.OTP_SENT);
        assertThat(outcome.getUserAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(outcome.getOtpPreview()).isSameAs(preview);
        verify(twoFactorService).createLoginOtp(ACCOUNT_ID);
        verify(otpDeliveryService).sendLoginOtp(EMAIL, rawOtp);
        verify(userAccountRepository).resetFailedPasswordAttempts(ACCOUNT_ID);
        verify(userAccountRepository, never()).updateLastLogin(anyInt());
        verify(userAccessLogService, never()).logLoginSuccess(anyInt(), anyString());
    }

    @Test
    @DisplayName("An incorrect password increments the targeted account failure count")
    void incorrectPasswordIncrementsAccountCount() {
        String correctPassword = "CorrectHorseBatteryStaple!";
        when(userAccountRepository.findByCompanyEmail(EMAIL))
                .thenReturn(passwordAccount(correctPassword, 0, false));
        when(userAccountRepository.recordFailedPasswordAttempt(ACCOUNT_ID, 5))
                .thenReturn(ATTEMPT_RECORDED);

        assertThatThrownBy(() -> authenticationService.authenticate(EMAIL, "WrongPassword!"))
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

        assertThatThrownBy(() -> authenticationService.authenticate(EMAIL, "WrongPassword!"))
                .isInstanceOfSatisfying(AuthenticationException.class, failure -> {
                    assertThat(failure.getMessage()).isEqualTo("Invalid email or password.");
                    assertThat(failure.getReason()).isNull();
                });

        verify(userAccountRepository, never()).recordFailedPasswordAttempt(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Reaching the configured threshold applies and audits an automatic lock")
    void thresholdFailureAppliesAutomaticLock() {
        String correctPassword = "CorrectHorseBatteryStaple!";
        when(userAccountRepository.findByCompanyEmail(EMAIL))
                .thenReturn(passwordAccount(correctPassword, 4, false));
        when(userAccountRepository.recordFailedPasswordAttempt(ACCOUNT_ID, 5))
                .thenReturn(LOCK_APPLIED);

        assertThatThrownBy(() -> authenticationService.authenticate(EMAIL, "WrongPassword!"))
                .isInstanceOfSatisfying(AuthenticationException.class, failure -> {
                    assertThat(failure.getMessage()).isEqualTo("Invalid email or password.");
                    assertThat(failure.getReason())
                            .isEqualTo(AuthenticationException.Reason.ACCOUNT_LOCKED);
                });

        verify(userAccountRepository).recordFailedPasswordAttempt(ACCOUNT_ID, 5);
        verify(userAccessLogService).logAutomaticAccountLock(ACCOUNT_ID);
    }

    @ParameterizedTest(name = "A persistently locked account rejects password: {0}")
    @ValueSource(strings = {"CorrectHorseBatteryStaple!", "WrongPassword!"})
    @DisplayName("A persistently locked account returns the lock reason without checking the password")
    void lockedAccountRejectsLoginWithoutCheckingPassword(String submittedPassword) {
        when(userAccountRepository.findByCompanyEmail(EMAIL))
                .thenReturn(passwordAccount("CorrectHorseBatteryStaple!", 5, true));

        assertThatThrownBy(() -> authenticationService.authenticate(EMAIL, submittedPassword))
                .isInstanceOfSatisfying(AuthenticationException.class, failure -> {
                    assertThat(failure.getMessage()).isEqualTo("Invalid email or password.");
                    assertThat(failure.getReason())
                            .isEqualTo(AuthenticationException.Reason.ACCOUNT_LOCKED);
                });

        verify(userAccountRepository, never()).recordFailedPasswordAttempt(anyInt(), anyInt());
        verify(userAccountRepository, never()).resetFailedPasswordAttempts(ACCOUNT_ID);
    }

    @Test
    @DisplayName("Correct password authentication resets the counter and continues to OTP")
    void correctPasswordResetsFailuresAndStartsOtpLogin() {
        String correctPassword = "CorrectHorseBatteryStaple!";
        String rawOtp = "123456";
        OtpPreview preview = new OtpPreview("no-reply@motorph.com", EMAIL, "Verification", "Message", rawOtp);
        when(userAccountRepository.findByCompanyEmail(EMAIL))
                .thenReturn(passwordAccount(correctPassword, 3, false));
        when(userAccountRepository.resetFailedPasswordAttempts(ACCOUNT_ID)).thenReturn(true);
        when(employeeRepository.findPersonalEmailByEmployeeId(101)).thenReturn("juan.personal@example.com");
        when(twoFactorService.createLoginOtp(ACCOUNT_ID)).thenReturn(rawOtp);
        when(otpDeliveryService.sendLoginOtp(EMAIL, rawOtp)).thenReturn(preview);

        LoginAttemptOutcome outcome = authenticationService.authenticate(EMAIL, correctPassword);

        assertThat(outcome.getType()).isEqualTo(LoginAttemptOutcome.Type.OTP_SENT);
        verify(userAccountRepository).resetFailedPasswordAttempts(ACCOUNT_ID);
        verify(twoFactorService).createLoginOtp(ACCOUNT_ID);
    }

    @Test
    @DisplayName("A malformed OTP is rejected as a validation error and is not an OTP attempt")
    void malformedOtpIsNotAuditedAsOtpFailure() {
        doThrow(new ValidationException("OTP code must be 6 digits."))
                .when(twoFactorService).verifyLoginOtp(ACCOUNT_ID, "12");

        assertThatThrownBy(() -> authenticationService.verifyLoginOtp(ACCOUNT_ID, "12"))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(userAccessLogService);
    }

    @Test
    @DisplayName("A locked account is audited once as LOGIN_FAILED and never reaches the OTP check")
    void lockedAccountIsNotAuditedTwice() {
        when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(account(true));

        assertThatThrownBy(() -> authenticationService.verifyLoginOtp(ACCOUNT_ID, SUBMITTED_OTP))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid email or password.");

        verify(userAccessLogService).logLoginFailed(EMAIL, "Login failed because account is locked.");
        verifyNoMoreInteractions(userAccessLogService);
        verifyNoInteractions(twoFactorService);
    }

    private UserAccount account(boolean locked) {
        return new UserAccount(
                ACCOUNT_ID, 101, EMAIL, EMAIL, "hash", "salt",
                List.of(Role.EMPLOYEE), locked, true, null, false, null);
    }

    private UserAccount passwordAccount(String password, int failedLoginCount, boolean locked) {
        String salt = PasswordHasher.generateSalt();
        return new UserAccount(
                ACCOUNT_ID, 101, EMAIL, EMAIL,
                PasswordHasher.hash(password, salt), salt,
                List.of(Role.EMPLOYEE), locked, true, null, false, null,
                failedLoginCount);
    }
}
