package motorph.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the MotorPH login screen.
 * Handles user input validation and login-related UI actions.
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

    @FXML
    private void initialize() {
        usernameErrorLabel.setText("");
        passwordErrorLabel.setText("");
        messageLabel.setText("");
    }

    @FXML
    private void handleLogin() {

        clearErrors();

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        boolean valid = true;

        if (username.isBlank()) {
            usernameErrorLabel.setText("Company email is required.");
            valid = false;
        }

        if (password.isBlank()) {
            passwordErrorLabel.setText("Password is required.");
            valid = false;
        }

        if (!valid) {
            return;
        }

        /*
         * TODO:
         * Connect AuthenticationService here.
         *
         * Example later:
         *
         * LoginResultDTO result =
         * authenticationService.login(username, password);
         *
         * Then navigate based on role.
         */

        messageLabel.setText("Login service not connected yet.");
    }

    @FXML
    private void handleForgotPassword() {

        /*
         * TODO:
         * Open forgot password dialog/screen.
         */

        messageLabel.setText("Please contact IT/Admin.");
    }

    private void clearErrors() {
        usernameErrorLabel.setText("");
        passwordErrorLabel.setText("");
        messageLabel.setText("");
    }
}