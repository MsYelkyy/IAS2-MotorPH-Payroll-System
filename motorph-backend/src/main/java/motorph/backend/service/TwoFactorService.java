package motorph.backend.service;

import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.ValidationException;
import motorph.backend.security.PasswordHasher;
import motorph.backend.domain.TwoFactorCode;
import motorph.backend.domain.value.OtpPurpose;
import motorph.backend.repository.TwoFactorCodeRepository;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Generates and verifies login OTPs. Codes are stored hashed, expire after 10 minutes, and stop
 * working after 5 failed attempts.
 */
@Service
public class TwoFactorService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    public static int getOtpLength() {
        return OTP_LENGTH;
    }

    public static int getOtpExpiryMinutes() {
        return OTP_EXPIRY_MINUTES;
    }

    public static int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    private final TwoFactorCodeRepository twoFactorCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public TwoFactorService(TwoFactorCodeRepository twoFactorCodeRepository) {
        this.twoFactorCodeRepository = twoFactorCodeRepository;
    }

    /**
     * Creates a login OTP, expiring any earlier one, and returns the raw code for delivery.
     */
    public String createLoginOtp(int userAccountId) {
        validateUserAccountId(userAccountId);

        twoFactorCodeRepository.expireOldCodes(userAccountId, OtpPurpose.LOGIN_2FA);

        String rawOtp = generateOtp();
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(rawOtp, salt);

        TwoFactorCode code = new TwoFactorCode(
                userAccountId,
                null,
                salt + ":" + hash,
                OtpPurpose.LOGIN_2FA,
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)
        );

        twoFactorCodeRepository.save(code);

        return rawOtp;
    }

    /**
     * Verifies a login OTP and marks it as used. Each wrong code counts as a failed attempt.
     */
    public void verifyLoginOtp(int userAccountId, String rawOtp) {
        validateUserAccountId(userAccountId);
        validateRawOtp(rawOtp);

        TwoFactorCode code = twoFactorCodeRepository.findLatestValidCode(
                userAccountId,
                OtpPurpose.LOGIN_2FA
        );

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

    private boolean matches(String rawOtp, String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            throw new IllegalStateException("Stored OTP hash is missing.");
        }

        String[] parts = storedValue.split(":");

        if (parts.length != 2) {
            throw new IllegalStateException("Stored OTP hash format is invalid.");
        }

        return PasswordHasher.verify(rawOtp, parts[0], parts[1]);
    }

    private String generateOtp() {
        int min = (int) Math.pow(10, OTP_LENGTH - 1);
        int max = (int) Math.pow(10, OTP_LENGTH) - 1;
        int code = secureRandom.nextInt(max - min + 1) + min;

        return String.valueOf(code);
    }

    private void validateUserAccountId(int userAccountId) {
        if (userAccountId <= 0) {
            throw new ValidationException("User account ID is required.");
        }
    }

    private void validateRawOtp(String rawOtp) {
        if (rawOtp == null || rawOtp.isBlank()) {
            throw new ValidationException("OTP code is required.");
        }

        if (!rawOtp.matches("\\d{" + OTP_LENGTH + "}")) {
            throw new ValidationException("OTP code must be " + OTP_LENGTH + " digits.");
        }
    }
}
