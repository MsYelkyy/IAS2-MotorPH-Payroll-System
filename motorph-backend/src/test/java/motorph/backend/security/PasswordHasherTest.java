package motorph.backend.security;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHasherTest {

    private static final String PASSWORD = "CorrectPassw0rd";

    @Test
    void newHashesUseTheVersionedFormatWithCurrentIterations() {
        String hash = PasswordHasher.hash(PASSWORD, PasswordHasher.generateSalt());

        assertThat(hash).startsWith("pbkdf2_sha256$600000$");
        assertThat(PasswordHasher.needsRehash(hash)).isFalse();
    }

    @Test
    void newHashVerifiesOnlyTheCorrectPassword() {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(PASSWORD, salt);

        assertThat(PasswordHasher.verify(PASSWORD, salt, hash)).isTrue();
        assertThat(PasswordHasher.verify("WrongPassw0rd", salt, hash)).isFalse();
    }

    @Test
    void sameStoredHashFailsWithADifferentSalt() {
        String hash = PasswordHasher.hash(PASSWORD, PasswordHasher.generateSalt());

        assertThat(PasswordHasher.verify(PASSWORD, PasswordHasher.generateSalt(), hash)).isFalse();
    }

    @Test
    void legacyHashStillVerifiesAndIsMarkedForUpgrade() throws Exception {
        String salt = PasswordHasher.generateSalt();
        String legacy = legacyHash(PASSWORD, salt);

        assertThat(PasswordHasher.verify(PASSWORD, salt, legacy)).isTrue();
        assertThat(PasswordHasher.verify("WrongPassw0rd", salt, legacy)).isFalse();
        assertThat(PasswordHasher.needsRehash(legacy)).isTrue();
    }

    @Test
    void malformedVersionedHashIsRejectedWithoutThrowing() {
        String salt = PasswordHasher.generateSalt();

        assertThat(PasswordHasher.verify(PASSWORD, salt, "pbkdf2_sha256$notanumber$abc")).isFalse();
        assertThat(PasswordHasher.verify(PASSWORD, salt, "pbkdf2_sha256$600000")).isFalse();
    }

    @Test
    void missingInputsAreRejected() {
        assertThatThrownBy(() -> PasswordHasher.hash(" ", PasswordHasher.generateSalt()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PasswordHasher.verify(PASSWORD, "", "hash"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PasswordHasher.needsRehash(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static String legacyHash(String password, String saltBase64) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), Base64.getDecoder().decode(saltBase64), 60_000, 128);
        byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();

        return Base64.getEncoder().encodeToString(hash);
    }
}
