package motorph.ui.auth;

import motorph.api.ApiClientFactory;
import motorph.api.AuthApiClient;
import motorph.api.dto.auth.ForgotPasswordResponse;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Starts a Forgot Password recovery: the email step, before the authenticator code is asked for. */
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

        ForgotPasswordResponse response;

        try {
            response = authApiClient.forgotPassword(email);
        } catch (Exception e) {
            UiMessageUtil.setError(forgotEmailMessageLabel, UiMessageUtil.toUserMessage(e));
            return;
        }

        // the next screen looks the same for every address, so it reveals nothing about the account
        if (loginController != null) {
            loginController.onRecoveryStarted(response.challengeId());
        }
    }

    @FXML
    private void backToLogin() {
        if (loginController != null) {
            loginController.showLoginCard();
        }
    }
}
