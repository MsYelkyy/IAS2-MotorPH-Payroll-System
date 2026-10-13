package motorph.backend.dto;

import motorph.backend.dto.auth.ChangePasswordRequest;
import motorph.backend.dto.auth.CompleteTemporaryPasswordRequest;
import motorph.backend.dto.auth.LoginRequest;
import motorph.backend.dto.auth.LogoutRequest;
import motorph.backend.dto.auth.RefreshRequest;
import motorph.backend.dto.auth.ResetPasswordRequest;
import motorph.backend.dto.auth.TotpCodeRequest;
import motorph.backend.dto.auth.TotpSetupStartRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring logs a request body with its toString() when web DEBUG logging is on, so a request that
 * carries a password, token, challenge or code must not print it.
 */
class AuthRequestToStringTest {

    private static final String SECRET = "S3cret-Value-XYZ";

    static Stream<Object> secretCarryingRequests() {
        return Stream.of(
                new LoginRequest("manuel.garcia@motorph.com", SECRET),
                new ChangePasswordRequest(SECRET, SECRET + "-new"),
                new RefreshRequest(SECRET),
                new LogoutRequest(SECRET),
                new CompleteTemporaryPasswordRequest(SECRET, SECRET + "-new", SECRET + "-confirm"),
                new ResetPasswordRequest(SECRET, SECRET + "-new", SECRET + "-confirm"),
                new TotpCodeRequest(SECRET, "123456"),
                new TotpSetupStartRequest(SECRET));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("A request that carries a secret does not print it")
    @MethodSource("secretCarryingRequests")
    void toStringDoesNotPrintSecrets(Object request) {
        assertThat(request.toString()).doesNotContain(SECRET).doesNotContain("123456");
    }

    @Test
    @DisplayName("The login request still shows the email, which helps with troubleshooting, but never the password")
    void loginRequestKeepsTheEmailOnly() {
        assertThat(new LoginRequest("manuel.garcia@motorph.com", SECRET).toString())
                .contains("manuel.garcia@motorph.com")
                .doesNotContain(SECRET);
    }
}
