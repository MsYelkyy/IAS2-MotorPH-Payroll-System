package motorph.ui.auth;

import motorph.application.dto.login.LoginResultDTO;
import motorph.application.session.UserSession;
import motorph.domain.value.Role;
import motorph.api.ApiClientFactory;
import motorph.api.ApiException;
import motorph.api.AuthApiClient;
import motorph.api.dto.auth.MeResponse;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.List;

/** Handles OTP verification after successful credential validation. */
public class OtpController {

    private AuthApiClient authApiClient;
    private LoginController loginController;
    private String pendingChallengeId;

    @FXML
    private TextField otpField;

    @FXML
    private Label otpMessageLabel;

    // initializes OTP dependencies
    @FXML
    private void initialize() {
        authApiClient = ApiClientFactory.getInstance().getAuthApiClient();
    }

    // setters
    public void setLoginController(LoginController loginController) {
        this.loginController = loginController;
    }

    // stores the opaque login challenge issued by the backend for the OTP step
    public void setPendingChallenge(String challengeId) {
        this.pendingChallengeId = challengeId;
    }

    // shows OTP message and clears old input
    public void showWithMessage(String message) {
        otpField.clear();
        UiValidationUtil.clearInvalid(otpField);
        UiMessageUtil.setSuccess(otpMessageLabel, message);
    }

    // verifies OTP, completes login, and loads the account's current
    // role/employee info from the backend (never trusted from the client)
    @FXML
    private void handleOtpVerification() {
        UiValidationUtil.clearInvalid(otpField);

        if (!hasPendingChallenge()) {
            return;
        }

        String otp = UiValidationUtil.getText(otpField);

        try {
            authApiClient.verifyOtp(pendingChallengeId, otp);
            pendingChallengeId = null;

            MeResponse me = authApiClient.me();
            LoginResultDTO result = toLoginResultDTO(me);

            UserSession.getInstance().login(result);

            if (loginController != null) {
                loginController.onLoginSuccess(result);
            }
        } catch (Exception e) {
            UiValidationUtil.markInvalid(otpField);
            String message = e instanceof ApiException apiException
                    && apiException.getHttpStatus() == 401
                    && "AUTHENTICATION_FAILED".equals(apiException.getCode())
                    && "Login challenge is invalid or expired.".equals(apiException.getMessage())
                    ? "Your verification session has expired. Please log in again."
                    : UiMessageUtil.toUserMessage(e);
            UiMessageUtil.setError(otpMessageLabel, message);
        }
    }

    // returns to the login card
    @FXML
    private void backToLogin() {
        pendingChallengeId = null;

        if (loginController != null) {
            loginController.showLoginCard();
        }
    }

    // checks if OTP verification has a pending challenge
    private boolean hasPendingChallenge() {
        if (pendingChallengeId != null) {
            return true;
        }

        UiMessageUtil.setError(otpMessageLabel, "Please sign in again before verifying OTP.");

        if (loginController != null) {
            loginController.showLoginCard();
        }

        return false;
    }

    // adapts the backend's /me response into the desktop's existing
    // LoginResultDTO shape so LoginController.onLoginSuccess needs no changes
    private LoginResultDTO toLoginResultDTO(MeResponse me) {
        List<Role> roles = me.roles().stream().map(Role::fromValue).toList();
        Role activeRole = Role.fromValue(me.activeRole());

        return new LoginResultDTO(
                me.userAccountId(),
                me.employeeId(),
                me.companyEmail(),
                roles,
                activeRole,
                true,
                false,
                roles.size() > 1,
                "Login successful."
        );
    }
}
