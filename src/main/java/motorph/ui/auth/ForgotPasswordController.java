package motorph.ui.auth;

import motorph.api.ApiClientFactory;
import motorph.api.AuthApiClient;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Submits a password reset request to IT for approval. */
public class ForgotPasswordController {

    private AuthApiClient authApiClient;
    private LoginController loginController;

    @FXML
    private VBox forgotEmailCard;

    @FXML
    private TextField resetEmailField;

    @FXML
    private Label forgotEmailMessageLabel;

    @FXML
    private void initialize() {
        authApiClient = ApiClientFactory.getInstance().getAuthApiClient();
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
            authApiClient.forgotPassword(email);
        } catch (Exception e) {
            // only reachable for transport/rate-limit failures: the endpoint
            // deliberately succeeds whether or not the address has an account
            UiMessageUtil.setError(forgotEmailMessageLabel, UiMessageUtil.toUserMessage(e));
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
}
