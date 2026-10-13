package motorph.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hashes and verifies passwords using PBKDF2 with HMAC-SHA256 and a random salt per password.
 * New hashes are stored as {@code pbkdf2_sha256$<iterations>$<hash>}. A stored value without that
 * prefix is an older hash made with 60,000 iterations and is still accepted, so existing accounts
 * keep working until their hash is upgraded at the next successful login.
 */
public final class PasswordHasher {

    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH = 256;
    private static final int LEGACY_ITERATIONS = 60_000;
    private static final int LEGACY_KEY_LENGTH = 128;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2_sha256$";

    private PasswordHasher() {
    }

    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String saltBase64) {
        validatePassword(password);
        validateSalt(saltBase64);

        return PREFIX + ITERATIONS + "$" + derive(password, saltBase64, ITERATIONS, KEY_LENGTH);
    }

    public static boolean verify(String inputPassword, String saltBase64, String storedHash) {
        validatePassword(inputPassword);
        validateSalt(saltBase64);
        validateStoredHash(storedHash);

        String expected = storedHash;
        String computed;

        if (storedHash.startsWith(PREFIX)) {
            int separator = storedHash.indexOf('$', PREFIX.length());

            if (separator < 0) {
                return false;
            }

            int iterations;

            try {
                iterations = Integer.parseInt(storedHash.substring(PREFIX.length(), separator));
            } catch (NumberFormatException e) {
                return false;
            }

            expected = storedHash.substring(separator + 1);
            computed = derive(inputPassword, saltBase64, iterations, KEY_LENGTH);
        } else {
            computed = derive(inputPassword, saltBase64, LEGACY_ITERATIONS, LEGACY_KEY_LENGTH);
        }

        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.US_ASCII), expected.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * True when the stored hash was made with older parameters and should be replaced after a
     * successful verification.
     */
    public static boolean needsRehash(String storedHash) {
        validateStoredHash(storedHash);

        return !storedHash.startsWith(PREFIX + ITERATIONS + "$");
    }

    private static String derive(String password, String saltBase64, int iterations, int keyLength) {
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    iterations,
                    keyLength
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();

            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash password.", e);
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
    }

    private static void validateSalt(String saltBase64) {
        if (saltBase64 == null || saltBase64.isBlank()) {
            throw new IllegalArgumentException("Password salt is required.");
        }
    }

    private static void validateStoredHash(String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            throw new IllegalArgumentException("Stored password hash is required.");
        }
    }
}
