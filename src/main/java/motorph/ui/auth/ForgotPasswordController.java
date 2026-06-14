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

/** Handles forgot password requests for IT-assisted password reset. */
public class ForgotPasswordController {

    private PasswordResetService passwordResetService;
    private LoginController loginController;

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
            passwordResetService.requestPasswordReset(email);
            UiMessageUtil.setSuccess(
                    forgotEmailMessageLabel,
                    "Password reset request submitted. Please wait for IT to process your request."
            );
            resetEmailField.clear();
        } catch (Exception e) {
            if (isDatabaseUnavailable(e)) {
                UiValidationUtil.clearInvalid(resetEmailField);
                UiMessageUtil.setError(forgotEmailMessageLabel, "Password reset needs MySQL. Start the database and try again.");
                return;
            }

            UiValidationUtil.markInvalid(resetEmailField);
            UiMessageUtil.setError(forgotEmailMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleVerifyResetOtp() {
        UiMessageUtil.setError(
                forgotEmailMessageLabel,
                "Password reset requests must be processed by IT."
        );
        showStep(forgotEmailCard);
    }

    @FXML
    private void handleCompleteReset() {
        UiMessageUtil.setError(
                forgotEmailMessageLabel,
                "Password reset requests must be processed by IT."
        );
        showStep(forgotEmailCard);
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