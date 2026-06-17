package motorph.ui.auth;

import motorph.application.service.PasswordResetService;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

/** Handles the three-step password reset flow: email → OTP → new password. */
public class ForgotPasswordController {

    private PasswordResetService passwordResetService;
    private LoginController loginController;
    private int pendingResetRequestId;

    @FXML
    private VBox forgotEmailCard;

    @FXML
    private VBox forgotOtpCard;

    @FXML
    private VBox forgotNewPasswordCard;

    @FXML
    private TextField resetEmailField;

    @FXML
    private TextField resetOtpField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label forgotEmailMessageLabel;

    @FXML
    private Label forgotOtpMessageLabel;

    @FXML
    private Label forgotNewPasswordMessageLabel;

    @FXML
    private void initialize() {
        passwordResetService = EmployeeUIServiceFactory.getInstance().getPasswordResetService();
    }

    // stores the parent login controller used for navigation callbacks
    public void setLoginController(LoginController loginController) {
        this.loginController = loginController;
    }

    // resets state and shows the email entry step
    public void showEmailStep() {
        pendingResetRequestId = 0;
        resetEmailField.clear();
        resetOtpField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
        clearAllMessages();
        showStep(forgotEmailCard);
    }

    @FXML
    private void handleSendResetOtp() {
        clearAllMessages();
        String email = UiValidationUtil.getText(resetEmailField);

        if (!UiValidationUtil.isMotorphEmail(email)) {
            UiValidationUtil.markInvalid(resetEmailField);
            UiMessageUtil.setError(forgotEmailMessageLabel, "Enter a valid MotorPH email address.");
            return;
        }

        try {
            pendingResetRequestId = passwordResetService.requestPasswordReset(email);
        } catch (Exception e) {
            if (isDatabaseUnavailable(e)) {
                UiValidationUtil.clearInvalid(resetEmailField);
                UiMessageUtil.setError(forgotEmailMessageLabel, "Password reset needs MySQL. Start the database and try again.");
                return;
            }
            UiValidationUtil.markInvalid(resetEmailField);
            UiMessageUtil.setError(forgotEmailMessageLabel, userMessage(e));
            return;
        }

        showStep(forgotOtpCard);
        UiMessageUtil.setSuccess(forgotOtpMessageLabel, "Reset OTP sent to your registered email address.");
    }

    @FXML
    private void handleVerifyResetOtp() {
        UiValidationUtil.clearInvalid(resetOtpField);

        if (pendingResetRequestId <= 0) {
            UiMessageUtil.setError(forgotOtpMessageLabel, "Please request a reset OTP first.");
            showStep(forgotEmailCard);
            return;
        }

        try {
            passwordResetService.verifyResetOtp(pendingResetRequestId, UiValidationUtil.getText(resetOtpField));
        } catch (Exception e) {
            UiValidationUtil.markInvalid(resetOtpField);
            UiMessageUtil.setError(forgotOtpMessageLabel, userMessage(e));
            return;
        }

        showStep(forgotNewPasswordCard);
        UiMessageUtil.setSuccess(forgotNewPasswordMessageLabel, "OTP verified. Enter your new password.");
    }

    @FXML
    private void handleCompleteReset() {
        UiValidationUtil.clearInvalid(newPasswordField);
        UiValidationUtil.clearInvalid(confirmPasswordField);

        String newPassword = UiValidationUtil.getText(newPasswordField);
        String confirmPassword = UiValidationUtil.getText(confirmPasswordField);
        boolean valid = true;

        if (newPassword.length() < 8) {
            UiValidationUtil.markInvalid(newPasswordField);
            valid = false;
        }

        if (!newPassword.equals(confirmPassword)) {
            UiValidationUtil.markInvalid(confirmPasswordField);
            valid = false;
        }

        if (!valid) {
            UiMessageUtil.setError(forgotNewPasswordMessageLabel, "Password must be 8+ characters and match confirmation.");
            return;
        }

        try {
            passwordResetService.resetPassword(pendingResetRequestId, newPassword, confirmPassword);
        } catch (Exception e) {
            UiMessageUtil.setError(forgotNewPasswordMessageLabel, userMessage(e));
            return;
        }

        pendingResetRequestId = 0;
        if (loginController != null) {
            loginController.onPasswordResetComplete("Password updated successfully. You can now sign in.");
        }
    }

    @FXML
    private void backToLogin() {
        if (loginController != null) {
            loginController.showLoginCard();
        }
    }

    @FXML
    private void backToEmailStep() {
        showStep(forgotEmailCard);
        clearAllMessages();
    }

    // shows only the given step card within this controller's root
    private void showStep(VBox stepToShow) {
        for (VBox card : List.of(forgotEmailCard, forgotOtpCard, forgotNewPasswordCard)) {
            boolean active = card == stepToShow;
            card.setVisible(active);
            card.setManaged(active);
        }
    }

    // clears all message labels across the three steps
    private void clearAllMessages() {
        UiMessageUtil.clearMessage(forgotEmailMessageLabel);
        UiMessageUtil.clearMessage(forgotOtpMessageLabel);
        UiMessageUtil.clearMessage(forgotNewPasswordMessageLabel);
        UiValidationUtil.clearInvalid(resetEmailField);
        UiValidationUtil.clearInvalid(resetOtpField);
        UiValidationUtil.clearInvalid(newPasswordField);
        UiValidationUtil.clearInvalid(confirmPasswordField);
    }

    // extracts a user-friendly message from an exception
    private String userMessage(Exception e) {
        String msg = e.getMessage();

        if (msg == null || msg.isBlank()) {
            return "Unable to complete the request. Please try again.";
        }

        if (isDatabaseUnavailable(e)) {
            return "Database connection unavailable. Start MySQL and try again.";
        }

        if (msg.startsWith("Failed to ")) {
            return "Unable to complete the request. Please check the database connection.";
        }

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
