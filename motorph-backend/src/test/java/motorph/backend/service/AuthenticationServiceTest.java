package motorph.backend.service;

import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.exception.OtpVerificationException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.UserAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

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
                .hasMessage("Account is locked.");

        verify(userAccessLogService).logLoginFailed(EMAIL, "Login failed because account is locked.");
        verifyNoMoreInteractions(userAccessLogService);
        verifyNoInteractions(twoFactorService);
    }

    private UserAccount account(boolean locked) {
        return new UserAccount(
                ACCOUNT_ID, 101, EMAIL, EMAIL, "hash", "salt",
                List.of(Role.EMPLOYEE), locked, true, null, false, null);
    }
}
