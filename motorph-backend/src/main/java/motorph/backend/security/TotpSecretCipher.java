package motorph.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Encrypts TOTP secrets with AES-256-GCM before they are stored. The secret cannot be hashed
 * because the backend needs it to check codes. The key comes from the
 * {@code MOTORPH_TOTP_ENCRYPTION_KEY} environment variable and is validated at startup, so a missing
 * or malformed key stops the backend instead of failing later at the first login.
 *
 * <p>Every encryption uses a fresh random nonce and binds the ciphertext to a context string (the
 * user account or enrolment challenge it belongs to), so a stored value copied onto another row
 * fails to decrypt.
 */
@Component
public class TotpSecretCipher {

    private static final int KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /** Ciphertext (with the authentication tag appended) and the nonce it was encrypted with. */
    public record EncryptedSecret(byte[] ciphertext, byte[] nonce) {

        @Override
        public boolean equals(Object other) {
            return other instanceof EncryptedSecret that
                    && Arrays.equals(ciphertext, that.ciphertext)
                    && Arrays.equals(nonce, that.nonce);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(ciphertext) + Arrays.hashCode(nonce);
        }

        @Override
        public String toString() {
            return "EncryptedSecret[ciphertext=***, nonce=***]";
        }
    }

    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public TotpSecretCipher(@Value("${motorph.totp.encryption-key:}") String base64Key) {
        this.key = parseKey(base64Key);
    }

    public EncryptedSecret encrypt(byte[] plaintext, String context) {
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return new EncryptedSecret(cipher.doFinal(plaintext), nonce);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt the authenticator secret.", e);
        }
    }

    public byte[] decrypt(byte[] ciphertext, byte[] nonce, String context) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException e) {
            // Wrong key, tampered data, or a value moved to another account. Never include the data.
            throw new IllegalStateException("Failed to decrypt the authenticator secret.", e);
        }
    }

    private static SecretKey parseKey(String base64Key) {
        String message = "MOTORPH_TOTP_ENCRYPTION_KEY must be set to the Base64 text of exactly 32 random bytes "
                + "(generate one with: openssl rand -base64 32). Refusing to start without a valid key.";

        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(message);
        }

        byte[] decoded;

        try {
            decoded = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(message);
        }

        if (decoded.length != KEY_BYTES) {
            throw new IllegalStateException(message);
        }

        return new SecretKeySpec(decoded, "AES");
    }
}
