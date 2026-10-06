package motorph.backend.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TotpSecretCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII));
    private static final byte[] SECRET = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    private final TotpSecretCipher cipher = new TotpSecretCipher(KEY);

    @Test
    @DisplayName("The stored form is not the secret, uses a fresh nonce each time, and decrypts back")
    void encryptsWithAFreshNonce() {
        TotpSecretCipher.EncryptedSecret first = cipher.encrypt(SECRET, "ctx");
        TotpSecretCipher.EncryptedSecret second = cipher.encrypt(SECRET, "ctx");

        assertThat(first.nonce()).hasSize(12);
        assertThat(first.ciphertext()).isNotEqualTo(SECRET).hasSize(SECRET.length + 16);
        assertThat(new String(first.ciphertext(), StandardCharsets.ISO_8859_1))
                .doesNotContain(new String(SECRET, StandardCharsets.ISO_8859_1));
        assertThat(first.nonce()).isNotEqualTo(second.nonce());
        assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
        assertThat(cipher.decrypt(first.ciphertext(), first.nonce(), "ctx")).isEqualTo(SECRET);
    }

    @Test
    @DisplayName("A value moved to another account fails to decrypt")
    void bindsTheSecretToItsContext() {
        TotpSecretCipher.EncryptedSecret stored = cipher.encrypt(SECRET, "motorph-totp-credential:7");

        assertThatThrownBy(() -> cipher.decrypt(stored.ciphertext(), stored.nonce(), "motorph-totp-credential:8"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Tampered ciphertext and a different key are both detected")
    void detectsTamperingAndWrongKey() {
        TotpSecretCipher.EncryptedSecret stored = cipher.encrypt(SECRET, "ctx");
        byte[] tampered = Arrays.copyOf(stored.ciphertext(), stored.ciphertext().length);
        tampered[0] ^= 1;

        assertThatThrownBy(() -> cipher.decrypt(tampered, stored.nonce(), "ctx"))
                .isInstanceOf(IllegalStateException.class);

        String otherKey = Base64.getEncoder().encodeToString(new byte[32]);
        assertThatThrownBy(() -> new TotpSecretCipher(otherKey).decrypt(stored.ciphertext(), stored.nonce(), "ctx"))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest(name = "key \"{0}\" stops the backend from starting")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "not-base64!!", "c2hvcnQ=", "replace-with-base64-of-32-random-bytes"})
    @DisplayName("A missing, malformed or wrong-length key fails startup with a clear message that never echoes the value")
    void rejectsInvalidKeys(String key) {
        assertThatThrownBy(() -> new TotpSecretCipher(key))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MOTORPH_TOTP_ENCRYPTION_KEY")
                .hasMessageContaining("32 random bytes")
                .hasMessageNotContaining("not-base64")
                .hasMessageNotContaining("c2hvcnQ=");
    }
}
