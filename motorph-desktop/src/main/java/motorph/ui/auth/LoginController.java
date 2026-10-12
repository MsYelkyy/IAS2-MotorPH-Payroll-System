package motorph.ui.auth;

import motorph.api.ApiException;
import motorph.application.dto.auth.EmailPreviewDTO;
import motorph.application.dto.auth.EmailPreviewType;
import motorph.application.dto.login.LoginResultDTO;
import motorph.domain.value.Role;
import motorph.api.ApiClientFactory;
import motorph.api.AuthApiClient;
import motorph.api.dto.auth.LoginResponse;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.NavigationUtil;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import java.time.LocalDateTime;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Manages employee login page actions and navigation. */
public class LoginController {

    private static final String ACCOUNT_LOCKED_MESSAGE =
            "Your account has been locked after multiple failed sign-in attempts. Please contact an administrator.";

    private AuthApiClient authApiClient;
    private boolean passwordVisible = false;

    @FXML private StackPane rootPane;
    @FXML private VBox loginCard;
    @FXML private VBox otpCard;
    @FXML private VBox forgotCard;
    @FXML private VBox forceChangeCard;
    @FXML private OtpController otpCardController;
    @FXML private ForgotPasswordController forgotCardController;
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

    // returns to login after password reset
    public void onPasswordResetComplete(String successMessage) {
        showLoginCard();
        UiMessageUtil.setInlineSuccess(loginMessageLabel, successMessage);
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

            if (response.temporaryPasswordChangeRequired()) {
                // login stops here on a temporary password; the challenge lets the
                // change be completed server-side without a token or an account ID
                forceChangeCardController.setPendingChallenge(response.challengeId());
                showCard(forceChangeCard);
                return;
            }

            otpCardController.setPendingChallenge(response.challengeId());
            showCard(otpCard);
            otpCardController.showWithMessage("OTP sent to " + response.maskedEmail() + ".");

            // No real SMTP is wired up on the backend yet (Phase 1) - when it says so,
            // show the OTP the same way the app always has: via the existing in-app
            // email preview dialog (see Main.java's global EmailPreviewService listener).
            if (response.otpPreview() != null) {
                showOtpPreview(response.otpPreview());
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

    // feeds the backend's dev-only OTP preview into the app's existing
    // EmailPreviewService, which Main.java's global listener renders via
    // EmailPreviewDialog - unchanged from how this always worked pre-migration
    private void showOtpPreview(motorph.api.dto.auth.OtpPreview otpPreview) {
        EmailPreviewDTO preview = new EmailPreviewDTO(
                EmailPreviewType.LOGIN_OTP,
                otpPreview.senderEmail(),
                otpPreview.recipientEmail(),
                otpPreview.subject(),
                otpPreview.body(),
                "OTP Code",
                otpPreview.otpCode(),
                LocalDateTime.now()
        );

        EmployeeUIServiceFactory.getInstance().getEmailPreviewService().savePreview(preview);
    }

    // shows the selected auth card
    private void showCard(VBox cardToShow) {
        for (VBox card : List.of(loginCard, otpCard, forgotCard, forceChangeCard)) {
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
        if (otpCardController != null) {
            otpCardController.setLoginController(this);
        }

        if (forgotCardController != null) {
            forgotCardController.setLoginController(this);
        }

        if (forceChangeCardController != null) {
            forceChangeCardController.setLoginController(this);
        }
    }

}
