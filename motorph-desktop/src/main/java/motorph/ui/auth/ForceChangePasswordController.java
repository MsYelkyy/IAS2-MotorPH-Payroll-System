package motorph.ui.auth;

import motorph.api.ApiClientFactory;
import motorph.api.AuthApiClient;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Sets a new password. Serves two paths: the forced change after logging in with a temporary password,
 * and the last step of Forgot Password, where the single-use reset token from the authenticator check
 * authorises the change.
 */
public class ForceChangePasswordController {

    private AuthApiClient authApiClient;
    private LoginController loginController;
    private String pendingChallengeId;
    private String pendingResetToken;

    private boolean newPasswordVisible     = false;
    private boolean confirmPasswordVisible = false;

    @FXML private PasswordField newPasswordField;
    @FXML private TextField     newPasswordTextField;
    @FXML private Button        newPasswordToggleBtn;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField     confirmPasswordTextField;
    @FXML private Button        confirmPasswordToggleBtn;

    @FXML private Label forceChangeMessageLabel;
    @FXML private Label eyebrowLabel;
    @FXML private Label headingLabel;
    @FXML private Label subheadingLabel;
    @FXML private Button updateButton;

    @FXML
    private void initialize() {
        authApiClient = ApiClientFactory.getInstance().getAuthApiClient();
    }

    // setters called by LoginController during wiring
    public void setLoginController(LoginController loginController) {
        this.loginController = loginController;
    }

    // stores the opaque challenge login issued for this forced change
    public void setPendingChallenge(String challengeId) {
        this.pendingChallengeId = challengeId;
        this.pendingResetToken = null;
        eyebrowLabel.setText("SECURE YOUR ACCOUNT");
        headingLabel.setText("Create a new password");
        subheadingLabel.setText("Choose a new password before continuing to your account.");
        clearForm();
    }

    // stores the single-use token the backend issued after the authenticator code was accepted
    public void setPendingResetToken(String resetToken) {
        this.pendingResetToken = resetToken;
        this.pendingChallengeId = null;
        eyebrowLabel.setText("ACCOUNT RECOVERY");
        headingLabel.setText("Choose a new password");
        subheadingLabel.setText("Your identity is confirmed. Choose a new password for your account.");
        clearForm();
    }

    // submits the new password to the service
    @FXML
    private void handleUpdatePassword() {
        UiMessageUtil.clearMessage(forceChangeMessageLabel);

        if (pendingChallengeId == null && pendingResetToken == null) {
            UiMessageUtil.setError(forceChangeMessageLabel, "Session expired. Please sign in again.");
            return;
        }

        String newPassword = newPasswordVisible
                ? UiValidationUtil.getText(newPasswordTextField)
                : UiValidationUtil.getText(newPasswordField);

        String confirmPassword = confirmPasswordVisible
                ? UiValidationUtil.getText(confirmPasswordTextField)
                : UiValidationUtil.getText(confirmPasswordField);

        try {
            if (pendingResetToken != null) {
                authApiClient.resetPassword(pendingResetToken, newPassword, confirmPassword);
            } else {
                authApiClient.completeTemporaryPassword(pendingChallengeId, newPassword, confirmPassword);
            }

            pendingChallengeId = null;
            pendingResetToken = null;
            clearForm();

            if (loginController != null) {
                loginController.onForceChangePasswordComplete(
                        "Password updated. Please sign in with your new password."
                );
            }
        } catch (Exception e) {
            UiMessageUtil.setError(forceChangeMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    // toggles new password field visibility
    @FXML
    private void toggleNewPassword() {
        newPasswordVisible = !newPasswordVisible;

        if (newPasswordVisible) {
            newPasswordTextField.setText(newPasswordField.getText());
            newPasswordTextField.setVisible(true);
            newPasswordTextField.setManaged(true);
            newPasswordField.setVisible(false);
            newPasswordField.setManaged(false);
            newPasswordToggleBtn.setText("Hide");
            return;
        }

        newPasswordField.setText(newPasswordTextField.getText());
        newPasswordField.setVisible(true);
        newPasswordField.setManaged(true);
        newPasswordTextField.setVisible(false);
        newPasswordTextField.setManaged(false);
        newPasswordToggleBtn.setText("Show");
    }

    // toggles confirm password field visibility
    @FXML
    private void toggleConfirmPassword() {
        confirmPasswordVisible = !confirmPasswordVisible;

        if (confirmPasswordVisible) {
            confirmPasswordTextField.setText(confirmPasswordField.getText());
            confirmPasswordTextField.setVisible(true);
            confirmPasswordTextField.setManaged(true);
            confirmPasswordField.setVisible(false);
            confirmPasswordField.setManaged(false);
            confirmPasswordToggleBtn.setText("Hide");
            return;
        }

        confirmPasswordField.setText(confirmPasswordTextField.getText());
        confirmPasswordField.setVisible(true);
        confirmPasswordField.setManaged(true);
        confirmPasswordTextField.setVisible(false);
        confirmPasswordTextField.setManaged(false);
        confirmPasswordToggleBtn.setText("Show");
    }

    // clears state and returns to the login card
    @FXML
    private void backToLogin() {
        pendingChallengeId = null;
        pendingResetToken = null;
        clearForm();

        if (loginController != null) {
            loginController.showLoginCard();
        }
    }

    private void clearForm() {
        newPasswordVisible     = false;
        confirmPasswordVisible = false;

        if (newPasswordField != null)        { newPasswordField.clear(); newPasswordField.setVisible(true); newPasswordField.setManaged(true); }
        if (newPasswordTextField != null)    { newPasswordTextField.clear(); newPasswordTextField.setVisible(false); newPasswordTextField.setManaged(false); }
        if (newPasswordToggleBtn != null)    { newPasswordToggleBtn.setText("Show"); }

        if (confirmPasswordField != null)    { confirmPasswordField.clear(); confirmPasswordField.setVisible(true); confirmPasswordField.setManaged(true); }
        if (confirmPasswordTextField != null){ confirmPasswordTextField.clear(); confirmPasswordTextField.setVisible(false); confirmPasswordTextField.setManaged(false); }
        if (confirmPasswordToggleBtn != null){ confirmPasswordToggleBtn.setText("Show"); }

        if (forceChangeMessageLabel != null) { UiMessageUtil.clearMessage(forceChangeMessageLabel); }
    }
}
