package motorph.backend.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;

/**
 * Generates and verifies 6-digit TOTP codes based on RFC 6238.
 * The codes change every 30 seconds and work with common authenticator apps.
 *
 * <p>The {@link Clock} is provided separately so the time can be controlled during testing.
 */
@Component
public class TotpCodeService {

    public static final String ISSUER = "MotorPH";
    public static final int DIGITS = 6;
    public static final int PERIOD_SECONDS = 30;

    // Accepts a code from the previous or next 30-second step in case the device clock is slightly out of sync.
    public static final int ALLOWED_STEP_DRIFT = 1;

    private static final int SECRET_BYTES = 20;

    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final TimeBasedOneTimePasswordGenerator generator;

    public TotpCodeService(Clock clock) {
        this.clock = clock;
        this.generator = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(PERIOD_SECONDS), DIGITS);
    }

    // A new 160-bit secret, the size RFC 4226 recommends for HMAC-SHA1.
    public byte[] generateSecret() {
        byte[] secret = new byte[SECRET_BYTES];
        secureRandom.nextBytes(secret);
        return secret;
    }

    // Returns the secret in Base32 format for manual entry in an authenticator app.
    public String toBase32(byte[] secret) {
        return new Base32().encodeToString(secret).replace("=", "");
    }

    // Creates the TOTP setup link that is placed inside the QR code.
    public String buildOtpAuthUri(String accountEmail, byte[] secret) {
        return "otpauth://totp/" + encode(ISSUER) + ":" + encode(accountEmail)
                + "?secret=" + toBase32(secret)
                + "&issuer=" + encode(ISSUER)
                + "&algorithm=SHA1"
                + "&digits=" + DIGITS
                + "&period=" + PERIOD_SECONDS;
    }

    public long currentTimeStep() {
        return timeStepOf(clock.instant());
    }

    /**
     * Checks the current TOTP code and nearby time steps to allow for small clock differences.
     * Codes that were already used are skipped so they cannot be accepted again.
     */
    public OptionalLong findMatchingTimeStep(byte[] secret, String submittedCode, Long lastUsedTimeStep) {
        if (submittedCode == null || !submittedCode.matches("\\d{" + DIGITS + "}")) {
            return OptionalLong.empty();
        }

        byte[] submitted = submittedCode.getBytes(StandardCharsets.US_ASCII);
        long current = currentTimeStep();
        OptionalLong match = OptionalLong.empty();

        for (long step = current - ALLOWED_STEP_DRIFT; step <= current + ALLOWED_STEP_DRIFT; step++) {
            byte[] expected = codeForStep(secret, step).getBytes(StandardCharsets.US_ASCII);
            boolean equal = MessageDigest.isEqual(submitted, expected);
            boolean unused = lastUsedTimeStep == null || step > lastUsedTimeStep;

            if (equal && unused && match.isEmpty()) {
                match = OptionalLong.of(step);
            }
        }

        return match;
    }

    // Generates the TOTP code for a specific time step, mainly for testing.
    public String codeForStep(byte[] secret, long step) {
        try {
            int code = generator.generateOneTimePassword(
                    new SecretKeySpec(secret, generator.getAlgorithm()),
                    Instant.ofEpochSecond(step * PERIOD_SECONDS));

            return String.format("%0" + DIGITS + "d", code);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Invalid authenticator secret.", e);
        }
    }

    private long timeStepOf(Instant instant) {
        return Math.floorDiv(instant.getEpochSecond(), (long) PERIOD_SECONDS);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
