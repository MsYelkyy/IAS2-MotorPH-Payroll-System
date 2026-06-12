package motorph.ui.auth;

import motorph.application.dto.login.LoginResultDTO;
import motorph.application.service.AuthenticationService;
import motorph.application.session.UserSession;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Handles OTP verification after successful credential validation. */
public class OtpController {

    private AuthenticationService authenticationService;
    private LoginController loginController;
    private LoginResultDTO pendingLoginResult;

    @FXML
    private TextField otpField;

    @FXML
    private Label otpMessageLabel;

    // initializes OTP dependencies
    @FXML
    private void initialize() {
        authenticationService = EmployeeUIServiceFactory.getInstance().getAuthenticationService();
    }

    // setters
    public void setLoginController(LoginController loginController) {
        this.loginController = loginController;
    }

    // stores pending login result for OTP verification
    public void setPendingLoginResult(LoginResultDTO result) {
        this.pendingLoginResult = result;
    }

    // shows OTP message and clears old input
    public void showWithMessage(String message) {
        otpField.clear();
        UiValidationUtil.clearInvalid(otpField);
        UiMessageUtil.setSuccess(otpMessageLabel, message);
    }

    // verifies OTP and completes login
    @FXML
    private void handleOtpVerification() {
        UiValidationUtil.clearInvalid(otpField);

        if (!hasPendingLogin()) {
            return;
        }

        String otp = UiValidationUtil.getText(otpField);

        try {
            LoginResultDTO result = authenticationService.verifyLoginOtp(
                    pendingLoginResult.getUserAccountId(),
                    otp
            );

            UserSession.getInstance().login(result);
            pendingLoginResult = null;

            if (loginController != null) {
                loginController.onLoginSuccess(result);
            }
        } catch (Exception e) {
            UiValidationUtil.markInvalid(otpField);
            UiMessageUtil.setError(otpMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    // returns to the login card
    @FXML
    private void backToLogin() {
        pendingLoginResult = null;

        if (loginController != null) {
            loginController.showLoginCard();
        }
    }

    // checks if OTP verification has a pending login
    private boolean hasPendingLogin() {
        if (pendingLoginResult != null && pendingLoginResult.isOtpRequired()) {
            return true;
        }

        UiMessageUtil.setError(otpMessageLabel, "Please sign in again before verifying OTP.");

        if (loginController != null) {
            loginController.showLoginCard();
        }

        return false;
    }
}