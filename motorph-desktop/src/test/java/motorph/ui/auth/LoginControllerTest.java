package motorph.ui.auth;

import motorph.api.ApiException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginControllerTest {

    @Test
    void lockedAccountShowsSupportMessage() {
        ApiException failure = new ApiException(
                401, "AUTHENTICATION_FAILED", "Invalid email or password.", "ACCOUNT_LOCKED");

        assertEquals(
                "Your account has been locked after multiple failed sign-in attempts. Please contact an administrator.",
                LoginController.loginErrorMessage(failure));
    }

    @Test
    void incorrectPasswordKeepsGenericMessage() {
        ApiException failure = new ApiException(401, "AUTHENTICATION_FAILED", "Invalid email or password.");

        assertEquals("Invalid email or password.", LoginController.loginErrorMessage(failure));
    }
}
