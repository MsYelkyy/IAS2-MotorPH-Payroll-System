package motorph.ui.auth;

import motorph.application.service.PasswordResetService;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Submits a password reset request to IT for approval. */
public class ForgotPasswordController {

    private PasswordResetService passwordResetService;
    private LoginController loginController;

    @FXML
    private VBox forgotEmailCard;

    @FXML
    private TextField resetEmailField;

    @FXML
    private Label forgotEmailMessageLabel;

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
        UiMessageUtil.clearMessage(forgotEmailMessageLabel);
        UiValidationUtil.clearInvalid(resetEmailField);
    }

    @FXML
    private void handleSubmitRequest() {
        UiMessageUtil.clearMessage(forgotEmailMessageLabel);
        UiValidationUtil.clearInvalid(resetEmailField);
        String email = UiValidationUtil.getText(resetEmailField);

        if (!UiValidationUtil.isMotorphEmail(email)) {
            UiValidationUtil.markInvalid(resetEmailField);
            UiMessageUtil.setError(forgotEmailMessageLabel, "Enter a valid MotorPH email address.");
            return;
        }

        try {
            passwordResetService.requestPasswordReset(email);
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

        if (loginController != null) {
            loginController.onPasswordResetComplete(
                    "Request submitted. IT will email you a temporary password.");
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
