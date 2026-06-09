package motorph.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import motorph.application.dto.login.LoginResultDTO;
import motorph.application.exception.AuthenticationException;
import motorph.application.exception.ValidationException;
import motorph.application.service.LoginService;
import motorph.application.session.UserSession;

/**
 * Controller for verifying login OTP before completing authentication.
 */
public class OtpController {

    @FXML
    private TextField otpField;

    @FXML
    private Label otpErrorLabel;

    @FXML
    private Label messageLabel;

    private LoginService loginService;
    private int userAccountId;

    // initializes the OTP screen
    @FXML
    private void initialize() {
        clearErrors();
    }

    // receives login data from the login screen
    public void setLoginVerificationData(LoginService loginService, int userAccountId) {
        if (loginService == null) {
            throw new IllegalArgumentException("LoginService is required.");
        }

        if (userAccountId <= 0) {
            throw new IllegalArgumentException("User account ID is required.");
        }

        this.loginService = loginService;
        this.userAccountId = userAccountId;
    }

    // verifies the entered OTP
    @FXML
    private void handleVerifyOtp() {
        clearErrors();

        String rawOtp = getText(otpField);

        if (rawOtp.isBlank()) {
            otpErrorLabel.setText("OTP code is required.");
            return;
        }

        try {
            LoginResultDTO result = loginService.verifyLoginOtp(userAccountId, rawOtp);

            if (result.isAuthenticated()) {
                UserSession.getInstance().login(result);
                messageLabel.setText(result.getMessage());

                // TODO: Navigate to dashboard based on UserSession.getInstance().getActiveRole()
            }

        } catch (AuthenticationException | ValidationException e) {
            messageLabel.setText(e.getMessage());

        } catch (RuntimeException e) {
            messageLabel.setText("OTP verification failed. Please try again.");
            e.printStackTrace();
        }
    }

    // gets trimmed text from a text field
    private String getText(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    // clears OTP error messages
    private void clearErrors() {
        otpErrorLabel.setText("");
        messageLabel.setText("");
    }
}