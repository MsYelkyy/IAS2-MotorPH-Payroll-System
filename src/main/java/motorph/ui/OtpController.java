package motorph.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import motorph.application.dto.login.LoginResultDTO;
import motorph.application.exception.AuthenticationException;
import motorph.application.exception.ValidationException;
import motorph.application.service.AuthenticationService;
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

    private AuthenticationService authenticationService;
    private int userAccountId;

    // initializes the OTP screen
    @FXML
    private void initialize() {
        otpErrorLabel.setText("");
        messageLabel.setText("");
    }

    // receives login data from the login screen
    public void setLoginVerificationData(AuthenticationService authenticationService, int userAccountId) {
        this.authenticationService = authenticationService;
        this.userAccountId = userAccountId;
    }

    // verifies the entered OTP
    @FXML
    private void handleVerifyOtp() {
        clearErrors();

        String rawOtp = otpField.getText();

        if (rawOtp == null || rawOtp.trim().isEmpty()) {
            otpErrorLabel.setText("OTP code is required.");
            return;
        }

        try {
            LoginResultDTO result = authenticationService.verifyLoginOtp(userAccountId, rawOtp.trim());

            if (result.isAuthenticated()) {
                UserSession.getInstance().login(result);

                messageLabel.setText(result.getMessage());

                // TODO: Navigate to dashboard based on UserSession.getInstance().getRole()
                // Example later:
                // openDashboard();
            }

        } catch (AuthenticationException | ValidationException e) {
            messageLabel.setText(e.getMessage());

        } catch (RuntimeException e) {
            messageLabel.setText("OTP verification failed. Please try again.");
            e.printStackTrace();
        }
    }

    // clears OTP error messages
    private void clearErrors() {
        otpErrorLabel.setText("");
        messageLabel.setText("");
    }
}