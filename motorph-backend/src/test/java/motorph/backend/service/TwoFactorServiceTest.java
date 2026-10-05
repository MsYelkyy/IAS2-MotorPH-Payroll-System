package motorph.backend.service;

import motorph.backend.domain.TwoFactorCode;
import motorph.backend.domain.value.OtpPurpose;
import motorph.backend.exception.OtpVerificationException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.TwoFactorCodeRepository;
import motorph.backend.security.PasswordHasher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Each way a login OTP check can fail carries a fixed reason, which is what the audit log records
 * (Control 5.4). The client-facing messages and the existing attempt counting are unchanged.
 */
class TwoFactorServiceTest {

    private static final int ACCOUNT_ID = 12;
    private static final int CODE_ID = 5;
    private static final String CORRECT_OTP = "123456";
    private static final String WRONG_OTP = "654321";

    private TwoFactorCodeRepository repository;
    private TwoFactorService service;
    private String storedHash;

    @BeforeEach
    void setUp() {
        repository = mock(TwoFactorCodeRepository.class);
        service = new TwoFactorService(repository);

        String salt = PasswordHasher.generateSalt();
        storedHash = salt + ":" + PasswordHasher.hash(CORRECT_OTP, salt);
    }

    @Test
    @DisplayName("No valid code on file is reported as expired or unavailable")
    void missingCodeIsExpiredOrUnavailable() {
        when(repository.findLatestValidCode(ACCOUNT_ID, OtpPurpose.LOGIN_2FA)).thenReturn(null);

        assertFailure(CORRECT_OTP, OtpVerificationException.Reason.EXPIRED_OR_UNAVAILABLE, "OTP is invalid or expired.");

        verify(repository, never()).incrementAttemptCount(anyInt());
        verify(repository, never()).markAsUsed(anyInt());
    }

    @Test
    @DisplayName("A code that has used all its attempts is reported as the attempt limit, even for the right code")
    void exhaustedCodeIsAttemptLimit() {
        when(repository.findLatestValidCode(ACCOUNT_ID, OtpPurpose.LOGIN_2FA)).thenReturn(code(5));

        assertFailure(CORRECT_OTP, OtpVerificationException.Reason.ATTEMPT_LIMIT_REACHED, "OTP attempt limit reached.");

        verify(repository, never()).incrementAttemptCount(anyInt());
        verify(repository, never()).markAsUsed(anyInt());
    }

    @Test
    @DisplayName("A wrong code with attempts left is an invalid code and counts as an attempt")
    void wrongCodeIsInvalidCode() {
        when(repository.findLatestValidCode(ACCOUNT_ID, OtpPurpose.LOGIN_2FA)).thenReturn(code(0));

        assertFailure(WRONG_OTP, OtpVerificationException.Reason.INVALID_CODE, "Invalid OTP code.");

        verify(repository).incrementAttemptCount(CODE_ID);
        verify(repository, never()).markAsUsed(anyInt());
    }

    @Test
    @DisplayName("A wrong code that uses the last attempt is reported as the attempt limit")
    void lastWrongAttemptIsAttemptLimit() {
        when(repository.findLatestValidCode(ACCOUNT_ID, OtpPurpose.LOGIN_2FA)).thenReturn(code(4));

        assertFailure(WRONG_OTP, OtpVerificationException.Reason.ATTEMPT_LIMIT_REACHED, "OTP attempt limit reached.");

        verify(repository).incrementAttemptCount(CODE_ID);
    }

    @Test
    @DisplayName("The right code is accepted and marked as used")
    void correctCodeIsAccepted() {
        when(repository.findLatestValidCode(ACCOUNT_ID, OtpPurpose.LOGIN_2FA)).thenReturn(code(2));

        assertThatCode(() -> service.verifyLoginOtp(ACCOUNT_ID, CORRECT_OTP)).doesNotThrowAnyException();

        verify(repository).markAsUsed(CODE_ID);
        verify(repository, never()).incrementAttemptCount(anyInt());
    }

    @Test
    @DisplayName("A malformed code is a validation error and is checked against nothing")
    void malformedCodeIsAValidationError() {
        assertThatThrownBy(() -> service.verifyLoginOtp(ACCOUNT_ID, "12"))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(repository);
    }

    private void assertFailure(String submittedOtp, OtpVerificationException.Reason reason, String message) {
        assertThatThrownBy(() -> service.verifyLoginOtp(ACCOUNT_ID, submittedOtp))
                .isInstanceOfSatisfying(OtpVerificationException.class,
                        failure -> assertThat(failure.getReason()).isEqualTo(reason))
                .hasMessage(message);
    }

    private TwoFactorCode code(int attemptCount) {
        return new TwoFactorCode(CODE_ID, ACCOUNT_ID, null, storedHash, OtpPurpose.LOGIN_2FA,
                LocalDateTime.now().plusMinutes(10), false, null, attemptCount, LocalDateTime.now());
    }
}
