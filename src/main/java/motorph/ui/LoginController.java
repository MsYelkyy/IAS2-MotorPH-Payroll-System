package motorph.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import motorph.ui.dialog.EmailPreviewDialog;
import motorph.application.dto.login.LoginRequestDTO;
import motorph.application.dto.login.LoginResultDTO;
import motorph.application.exception.AuthenticationException;
import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.application.service.LoginService;
import motorph.application.service.LoginServiceImpl;

/**
 * Controller for the MotorPH login screen.
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label usernameErrorLabel;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private Label messageLabel;

    private LoginService loginService;

    // initializes the login screen and required services
    @FXML
    private void initialize() {
        clearErrors();
        loginService = new LoginServiceImpl();
    }

    // handles login validation and OTP request
    @FXML
    private void handleLogin() {
        clearErrors();

        String companyEmail = getText(usernameField);
        String password = getText(passwordField);

        if (!validateLoginInput(companyEmail, password)) {
            return;
        }

        try {
            LoginRequestDTO request = new LoginRequestDTO(companyEmail, password);
            LoginResultDTO result = loginService.authenticate(request);

            if (result.isOtpRequired()) {
                showEmailPreviewIfAvailable();
                openOtpScreen(result.getUserAccountId());
                return;
            }

            messageLabel.setText(result.getMessage());

        } catch (AuthenticationException
                 | ValidationException
                 | BusinessRuleException
                 | EntityNotFoundException e) {

            messageLabel.setText(e.getMessage());

        } catch (RuntimeException e) {
            messageLabel.setText("Login failed. Please try again.");
            e.printStackTrace();
        }
    }

    // handles forgot password request
    @FXML
    private void handleForgotPassword() {
        messageLabel.setText("Password reset request feature is not connected yet.");
    }

    // validates login form input
    private boolean validateLoginInput(String companyEmail, String password) {
        boolean valid = true;

        if (companyEmail.isBlank()) {
            usernameErrorLabel.setText("Company email is required.");
            valid = false;
        }

        if (password.isBlank()) {
            passwordErrorLabel.setText("Password is required.");
            valid = false;
        }

        return valid;
    }

    // shows the latest email preview when available
    private void showEmailPreviewIfAvailable() {
        if (loginService == null || !loginService.hasEmailPreview()) {
            return;
        }

        EmailPreviewDialog.show(loginService.getLatestEmailPreview());
    }

    // opens the OTP verification screen
    private void openOtpScreen(Integer userAccountId) {
        if (userAccountId == null || userAccountId <= 0) {
            messageLabel.setText("Invalid user account for OTP verification.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/motorph/ui/otp.fxml"));
            Parent root = loader.load();

            OtpController otpController = loader.getController();
            otpController.setLoginVerificationData(loginService, userAccountId);

            Stage stage = getCurrentStage();
            stage.setScene(new Scene(root));
            stage.setTitle("MotorPH OTP Verification");
            stage.show();

        } catch (Exception e) {
            messageLabel.setText("Failed to open OTP screen.");
            e.printStackTrace();
        }
    }

    // gets the current stage
    private Stage getCurrentStage() {
        return (Stage) usernameField.getScene().getWindow();
    }

    // gets trimmed text from a text field
    private String getText(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    // clears all login error messages
    private void clearErrors() {
        usernameErrorLabel.setText("");
        passwordErrorLabel.setText("");
        messageLabel.setText("");
    }
}