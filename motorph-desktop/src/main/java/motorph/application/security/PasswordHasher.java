package motorph.application.security;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Handles password hashing, salt generation, and password verification for user authentication.
 */
public final class PasswordHasher {

    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 60000;
    private static final int KEY_LENGTH = 128;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private PasswordHasher() {
        // constructors
    }

    // Generates a random salt for password hashing.
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Hashes a plain password using the provided Base64 salt.
    public static String hash(String password, String saltBase64) {
        validatePassword(password);
        validateSalt(saltBase64);

        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();

            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash password.", e);
        }
    }

    // Verifies if the input password matches the stored hash.
    public static boolean verify(String inputPassword, String saltBase64, String storedHash) {
        validatePassword(inputPassword);
        validateSalt(saltBase64);
        validateStoredHash(storedHash);

        String computedHash = hash(inputPassword, saltBase64);
        return computedHash.equals(storedHash);
    }

    // Validates that the password is not null or blank.
    private static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
    }

    // Validates that the salt is not null or blank.
    private static void validateSalt(String saltBase64) {
        if (saltBase64 == null || saltBase64.isBlank()) {
            throw new IllegalArgumentException("Password salt is required.");
        }
    }

    // Validates that the stored hash is not null or blank.
    private static void validateStoredHash(String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            throw new IllegalArgumentException("Stored password hash is required.");
        }
    }
}