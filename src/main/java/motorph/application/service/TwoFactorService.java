package motorph.application.service;

import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.ValidationException;
import motorph.application.security.PasswordHasher;
import motorph.domain.model.TwoFactorCode;
import motorph.domain.value.OtpPurpose;
import motorph.infrastructure.repository.TwoFactorCodeRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Handles OTP generation and verification for two-factor authentication.
 */
public class TwoFactorService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final TwoFactorCodeRepository twoFactorCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    // constructors
    public TwoFactorService(TwoFactorCodeRepository twoFactorCodeRepository) {
        if (twoFactorCodeRepository == null) {
            throw new IllegalArgumentException("TwoFactorCodeRepository is required.");
        }

        this.twoFactorCodeRepository = twoFactorCodeRepository;
    }

    // Creates a new OTP and returns the raw code for sending.
    public String createOtp(int userAccountId, Integer resetRequestId, OtpPurpose purpose) {
        validateUserAccountId(userAccountId);
        validateResetRequestForPurpose(resetRequestId, purpose);
        validatePurpose(purpose);

        twoFactorCodeRepository.expireOldCodes(userAccountId, purpose);

        String rawOtp = generateOtp();
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(rawOtp, salt);

        TwoFactorCode code = new TwoFactorCode(
                userAccountId,
                resetRequestId,
                salt + ":" + hash,
                purpose,
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)
        );

        twoFactorCodeRepository.save(code);
        return rawOtp;
    }

    // Verifies a login OTP.
    public void verifyLoginOtp(int userAccountId, String rawOtp) {
        validateUserAccountId(userAccountId);
        validateRawOtp(rawOtp);

        TwoFactorCode code = twoFactorCodeRepository.findLatestValidCode(
                userAccountId,
                OtpPurpose.LOGIN_2FA
        );

        verifyCodeOrThrow(code, rawOtp);
    }

    // Verifies a password reset OTP.
    public void verifyPasswordResetOtp(int resetRequestId, String rawOtp) {
        validateResetRequestId(resetRequestId);
        validateRawOtp(rawOtp);

        TwoFactorCode code = twoFactorCodeRepository.findLatestValidByResetRequestId(resetRequestId);
        verifyCodeOrThrow(code, rawOtp);
    }

    // Verifies the OTP record and marks it as used.
    private void verifyCodeOrThrow(TwoFactorCode code, String rawOtp) {
        if (code == null) {
            throw new BusinessRuleException("OTP is invalid or expired.");
        }

        if (code.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new BusinessRuleException("OTP attempt limit reached.");
        }

        if (!matches(rawOtp, code.getOtpCodeHash())) {
            twoFactorCodeRepository.incrementAttemptCount(code.getTwoFactorCodeId());

            if (code.getAttemptCount() + 1 >= MAX_ATTEMPTS) {
                throw new BusinessRuleException("OTP attempt limit reached.");
            }

            throw new BusinessRuleException("Invalid OTP code.");
        }

        twoFactorCodeRepository.markAsUsed(code.getTwoFactorCodeId());
    }

    // Checks if the raw OTP matches the stored hash.
    private boolean matches(String rawOtp, String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            throw new IllegalStateException("Stored OTP hash is missing.");
        }

        String[] parts = storedValue.split(":");

        if (parts.length != 2) {
            throw new IllegalStateException("Stored OTP hash format is invalid.");
        }

        String salt = parts[0];
        String hash = parts[1];

        return PasswordHasher.verify(rawOtp, salt, hash);
    }

    // Generates a numeric OTP.
    private String generateOtp() {
        int min = (int) Math.pow(10, OTP_LENGTH - 1);
        int max = (int) Math.pow(10, OTP_LENGTH) - 1;
        int code = secureRandom.nextInt(max - min + 1) + min;

        return String.valueOf(code);
    }

    // Validates user account id input.
    private void validateUserAccountId(int userAccountId) {
        if (userAccountId <= 0) {
            throw new ValidationException("User account ID is required.");
        }
    }

    // Validates reset request id input.
    private void validateResetRequestId(int resetRequestId) {
        if (resetRequestId <= 0) {
            throw new ValidationException("Reset request ID is required.");
        }
    }

    // Validates reset request id based on OTP purpose.
    private void validateResetRequestForPurpose(Integer resetRequestId, OtpPurpose purpose) {
        if (purpose == OtpPurpose.PASSWORD_RESET && (resetRequestId == null || resetRequestId <= 0)) {
            throw new ValidationException("Reset request ID is required for password reset OTP.");
        }

        if (purpose == OtpPurpose.LOGIN_2FA && resetRequestId != null) {
            throw new ValidationException("Reset request ID must be empty for login OTP.");
        }
    }

    // Validates OTP purpose input.
    private void validatePurpose(OtpPurpose purpose) {
        if (purpose == null) {
            throw new ValidationException("OTP purpose is required.");
        }
    }

    // Validates raw OTP input.
    private void validateRawOtp(String rawOtp) {
        if (rawOtp == null || rawOtp.isBlank()) {
            throw new ValidationException("OTP code is required.");
        }

        if (!rawOtp.matches("\\d{" + OTP_LENGTH + "}")) {
            throw new ValidationException("OTP code must be " + OTP_LENGTH + " digits.");
        }
    }
}