package motorph.ui.auth;

import motorph.api.ApiException;
import motorph.application.dto.login.LoginResultDTO;
import motorph.application.session.UserSession;
import motorph.domain.value.Role;
import motorph.api.ApiClientFactory;
import motorph.api.AuthApiClient;
import motorph.api.dto.auth.LoginResponse;
import motorph.api.dto.auth.MeResponse;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.NavigationUtil;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Manages the sign-in page and the screens that follow the password: authenticator enrolment, the
 * authenticator code, Forgot Password and the new-password screens. The dashboard opens only after the
 * backend has accepted the authenticator code and issued tokens.
 */
public class LoginController {

    private static final String ACCOUNT_LOCKED_MESSAGE =
            "Your account has been locked after multiple failed sign-in attempts. Please contact an administrator.";

    private AuthApiClient authApiClient;
    private boolean passwordVisible = false;

    @FXML private StackPane rootPane;
    @FXML private VBox loginCard;
    @FXML private VBox totpCard;
    @FXML private VBox totpSetupCard;
    @FXML private VBox forgotCard;
    @FXML private VBox recoveryCard;
    @FXML private VBox resetPasswordCard;
    @FXML private VBox forceChangeCard;
    @FXML private TotpVerifyController totpCardController;
    @FXML private TotpSetupController totpSetupCardController;
    @FXML private ForgotPasswordController forgotCardController;
    @FXML private TotpVerifyController recoveryCardController;
    @FXML private ForceChangePasswordController resetPasswordCardController;
    @FXML private ForceChangePasswordController forceChangeCardController;
    @FXML private TextField companyEmailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button passwordToggleBtn;
    @FXML private Label loginMessageLabel;

    // initializes login page dependencies and controls
    @FXML
    private void initialize() {
        authApiClient = ApiClientFactory.getInstance().getAuthApiClient();

        wireSubControllers();
        loginMessageLabel.managedProperty().bind(loginMessageLabel.textProperty().isNotEmpty());

        companyEmailField.textProperty().addListener((obs, oldValue, newValue) ->
                UiMessageUtil.clearMessage(loginMessageLabel));

        passwordField.textProperty().addListener((obs, oldValue, newValue) ->
                UiMessageUtil.clearMessage(loginMessageLabel));

        passwordTextField.textProperty().addListener((obs, oldValue, newValue) ->
                UiMessageUtil.clearMessage(loginMessageLabel));

        // pressing Enter in any login field submits the form
        companyEmailField.setOnAction(event -> handleLogin());
        passwordField.setOnAction(event -> handleLogin());
        passwordTextField.setOnAction(event -> handleLogin());

        showLoginCard();
    }

    // navigates to the highest role workspace after successful login
    public void onLoginSuccess(LoginResultDTO result) {
        List<Role> roles = result.getRoles();

        if (roles.contains(Role.ADMIN)) {
            CurrentUserSession.switchRole(Role.ADMIN);
            NavigationUtil.goToAdminApp(rootPane.getScene());
            return;
        }

        if (roles.contains(Role.HR)) {
            CurrentUserSession.switchRole(Role.HR);
            NavigationUtil.goToHrApp(rootPane.getScene());
            return;
        }

        if (roles.contains(Role.IT)) {
            CurrentUserSession.switchRole(Role.IT);
            NavigationUtil.goToITApp(rootPane.getScene());
            return;
        }

        if (roles.contains(Role.FINANCE)) {
            CurrentUserSession.switchRole(Role.FINANCE);
            NavigationUtil.goToFinanceApp(rootPane.getScene());
            return;
        }

        NavigationUtil.goToEmployeeApp(rootPane.getScene());
    }

    // loads the signed-in account from the backend and opens its workspace; called once the backend
    // has accepted the authenticator code and the tokens are held
    public void finishLogin() {
        MeResponse me = authApiClient.me();
        List<Role> roles = me.roles().stream().map(Role::fromValue).toList();
        Role activeRole = Role.fromValue(me.activeRole());

        LoginResultDTO result = new LoginResultDTO(
                me.userAccountId(),
                me.employeeId(),
                me.companyEmail(),
                roles,
                activeRole,
                true,
                false,
                roles.size() > 1,
                "Login successful."
        );

        UserSession.getInstance().login(result);
        onLoginSuccess(result);
    }

    // Opens the authenticator code screen for password recovery without revealing whether the account exists.
    public void onRecoveryStarted(String challengeId) {
        recoveryCardController.showForRecovery(challengeId);
        showCard(recoveryCard);
    }

    // opens the new-password screen once the authenticator code was accepted for a recovery
    public void onRecoveryVerified(String resetToken) {
        resetPasswordCardController.setPendingResetToken(resetToken);
        showCard(resetPasswordCard);
    }

    // returns to login after forced password change and shows a confirmation message
    public void onForceChangePasswordComplete(String successMessage) {
        showLoginCard();
        UiMessageUtil.setInlineSuccess(loginMessageLabel, successMessage);
    }

    // shows the login card
    public void showLoginCard() {
        clearLoginState();
        showCard(loginCard);
    }

    // handles login credential submission
    @FXML
    private void handleLogin() {
        clearLoginValidationOnly();

        String email = UiValidationUtil.getText(companyEmailField);
        String password = passwordVisible
                ? UiValidationUtil.getText(passwordTextField)
                : UiValidationUtil.getText(passwordField);

        if (email.isBlank()) {
            UiValidationUtil.markInvalid(companyEmailField);
            UiMessageUtil.setInlineError(loginMessageLabel, "Please enter your company email.");
            return;
        }

        if (password.isBlank()) {
            UiValidationUtil.markInvalid(passwordVisible ? passwordTextField : passwordField);
            UiMessageUtil.setInlineError(loginMessageLabel, "Please enter your password.");
            return;
        }

        if (!UiValidationUtil.isMotorphEmail(email)) {
            UiValidationUtil.markInvalid(companyEmailField);
            UiMessageUtil.setInlineError(loginMessageLabel, "Please enter a valid MotorPH email.");
            return;
        }

        try {
            LoginResponse response = authApiClient.login(email, password);

            switch (response.status()) {
                case LoginResponse.TEMPORARY_PASSWORD_CHANGE_REQUIRED -> {
                    // the challenge lets the change be completed server-side without a token or an account ID
                    forceChangeCardController.setPendingChallenge(response.challengeId());
                    showCard(forceChangeCard);
                }
                case LoginResponse.TOTP_SETUP_REQUIRED -> {
                    showCard(totpSetupCard);
                    totpSetupCardController.begin(response.challengeId());
                }
                case LoginResponse.TOTP_REQUIRED -> {
                    totpCardController.showForLogin(response.challengeId());
                    showCard(totpCard);
                }
                default -> UiMessageUtil.setInlineError(loginMessageLabel,
                        "This version of the app cannot complete sign-in. Please update and try again.");
            }

        } catch (Exception e) {
            UiValidationUtil.markInvalid(companyEmailField);
            UiValidationUtil.markInvalid(passwordVisible ? passwordTextField : passwordField);
            UiMessageUtil.setInlineError(loginMessageLabel, loginErrorMessage(e));
        }
    }

    static String loginErrorMessage(Exception e) {
        if (e instanceof ApiException apiException
                && "ACCOUNT_LOCKED".equals(apiException.getReason())) {
            return ACCOUNT_LOCKED_MESSAGE;
        }

        return UiMessageUtil.toUserMessage(e);
    }

    // opens the forgot password card
    @FXML
    private void handleForgotPassword() {
        forgotCardController.showEmailStep();
        showCard(forgotCard);
    }

    // toggles password visibility
    @FXML
    private void togglePassword() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordToggleBtn.setText("Hide");
            return;
        }

        passwordField.setText(passwordTextField.getText());
        passwordField.setVisible(true);
        passwordField.setManaged(true);
        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);
        passwordToggleBtn.setText("Show");
    }

    // shows the selected auth card
    private void showCard(VBox cardToShow) {
        for (VBox card : List.of(loginCard, totpCard, totpSetupCard, forgotCard, recoveryCard,
                resetPasswordCard, forceChangeCard)) {
            boolean active = card == cardToShow;
            card.setVisible(active);
            card.setManaged(active);
        }
    }

    // clears login validation without clearing typed credentials
    private void clearLoginValidationOnly() {
        UiValidationUtil.clearInvalid(companyEmailField);
        UiValidationUtil.clearInvalid(passwordField);
        UiValidationUtil.clearInvalid(passwordTextField);
        UiMessageUtil.clearMessage(loginMessageLabel);
    }

    // clears login form state
    private void clearLoginState() {
        UiValidationUtil.clearInvalid(companyEmailField);
        UiValidationUtil.clearInvalid(passwordField);
        UiValidationUtil.clearInvalid(passwordTextField);
        UiMessageUtil.clearMessage(loginMessageLabel);

        companyEmailField.clear();
        passwordField.clear();
        passwordTextField.clear();

        passwordField.setVisible(true);
        passwordField.setManaged(true);
        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);

        passwordVisible = false;

        if (passwordToggleBtn != null) {
            passwordToggleBtn.setText("Show");
        }
    }

    // connects included controllers to this controller
    private void wireSubControllers() {
        if (totpCardController != null) {
            totpCardController.setLoginController(this);
        }

        if (totpSetupCardController != null) {
            totpSetupCardController.setLoginController(this);
        }

        if (recoveryCardController != null) {
            recoveryCardController.setLoginController(this);
        }

        if (resetPasswordCardController != null) {
            resetPasswordCardController.setLoginController(this);
        }

        if (forgotCardController != null) {
            forgotCardController.setLoginController(this);
        }

        if (forceChangeCardController != null) {
            forceChangeCardController.setLoginController(this);
        }
    }

}
