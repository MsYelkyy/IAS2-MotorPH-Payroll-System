package motorph.ui.auth;

import motorph.application.dto.login.LoginResultDTO;
import motorph.application.service.UserAccountService;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** Handles forced password change when user logs in with a temporary password. */
public class ForceChangePasswordController {

    private UserAccountService userAccountService;
    private LoginController loginController;
    private LoginResultDTO pendingResult;

    private boolean newPasswordVisible     = false;
    private boolean confirmPasswordVisible = false;

    @FXML private PasswordField newPasswordField;
    @FXML private TextField     newPasswordTextField;
    @FXML private Button        newPasswordToggleBtn;

    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField     confirmPasswordTextField;
    @FXML private Button        confirmPasswordToggleBtn;

    @FXML private Label forceChangeMessageLabel;

    @FXML
    private void initialize() {
        userAccountService = EmployeeUIServiceFactory.getInstance().getUserAccountService();
    }

    // setters called by LoginController during wiring
    public void setLoginController(LoginController loginController) {
        this.loginController = loginController;
    }

    // stores the pending login result and resets the form
    public void setPendingResult(LoginResultDTO result) {
        this.pendingResult = result;
        clearForm();
    }

    // submits the new password to the service
    @FXML
    private void handleUpdatePassword() {
        UiMessageUtil.clearMessage(forceChangeMessageLabel);

        if (pendingResult == null) {
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
            userAccountService.completeTemporaryPasswordChange(
                    pendingResult.getUserAccountId(),
                    newPassword,
                    confirmPassword
            );

            pendingResult = null;

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
        pendingResult = null;
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
