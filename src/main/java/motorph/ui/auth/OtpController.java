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

/** Handles OTP verification after a successful credential check. */
public class OtpController {

    private AuthenticationService authenticationService;
    private LoginController loginController;
    private LoginResultDTO pendingLoginResult;
    private boolean offlinePreviewMode = false;

    @FXML
    private TextField otpField;

    @FXML
    private Label otpMessageLabel;

    @FXML
    private void initialize() {
        authenticationService = EmployeeUIServiceFactory.getInstance().getAuthenticationService();
    }

    // stores the parent login controller used for navigation callbacks
    public void setLoginController(LoginController loginController) {
        this.loginController = loginController;
    }

    // stores the pending login result that holds the account ID for OTP verification
    public void setPendingLoginResult(LoginResultDTO result) {
        this.pendingLoginResult = result;
        this.offlinePreviewMode = false;
    }

    // enables offline preview mode where any 6-digit OTP is accepted
    public void setOfflinePreviewMode(boolean enabled) {
        this.offlinePreviewMode = enabled;
        this.pendingLoginResult = null;
    }

    // shows the OTP card with an initial status message
    public void showWithMessage(String message) {
        otpField.clear();
        UiValidationUtil.clearInvalid(otpField);
        UiMessageUtil.setSuccess(otpMessageLabel, message);
    }

    @FXML
    private void handleOtpVerification() {
        UiValidationUtil.clearInvalid(otpField);
        String otp = UiValidationUtil.getText(otpField);

        if (offlinePreviewMode) {
            if (!otp.matches("\\d{6}")) {
                UiValidationUtil.markInvalid(otpField);
                UiMessageUtil.setError(otpMessageLabel, "Enter a 6-digit OTP code.");
                return;
            }
            offlinePreviewMode = false;
            UserSession.getInstance().clear();
            if (loginController != null) {
                loginController.onLoginSuccess(null);
            }
            return;
        }

        if (pendingLoginResult == null || !pendingLoginResult.isOtpRequired()) {
            UiMessageUtil.setError(otpMessageLabel, "Please sign in again before verifying OTP.");
            if (loginController != null) {
                loginController.showLoginCard();
            }
            return;
        }

        try {
            LoginResultDTO result = authenticationService.verifyLoginOtp(
                    pendingLoginResult.getUserAccountId(), otp);
            UserSession.getInstance().login(result);
            pendingLoginResult = null;
            if (loginController != null) {
                loginController.onLoginSuccess(result);
            }
        } catch (Exception e) {
            UiValidationUtil.markInvalid(otpField);
            UiMessageUtil.setError(otpMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void backToLogin() {
        if (loginController != null) {
            loginController.showLoginCard();
        }
    }

    // extracts a user-friendly message from an exception
    private String userMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return "Unable to complete the request. Please try again.";
        if (isDatabaseUnavailable(e)) return "Database connection unavailable. Start MySQL and try again.";
        if (msg.startsWith("Failed to ")) return "Unable to complete the request. Please check the database connection.";
        return msg;
    }

    // checks whether the exception signals a database connectivity failure
    private boolean isDatabaseUnavailable(Exception e) {
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && (msg.contains("Failed to connect to the database")
                    || msg.contains("Communications link failure")
                    || msg.contains("Connection refused")
                    || msg.contains("No suitable driver"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
