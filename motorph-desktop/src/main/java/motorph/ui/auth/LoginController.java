package motorph.ui.auth;

import motorph.application.dto.auth.EmailPreviewDTO;
import motorph.application.dto.auth.EmailPreviewType;
import motorph.application.dto.login.LoginResultDTO;
import motorph.domain.value.Role;
import motorph.api.ApiClientFactory;
import motorph.api.AuthApiClient;
import motorph.api.dto.auth.LoginResponse;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.AnimationUtil;
import motorph.ui.util.NavigationUtil;
import motorph.ui.util.ThemeManager;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import java.time.LocalDateTime;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Manages employee login page actions and navigation. */
public class LoginController {

    private AuthApiClient authApiClient;
    private boolean passwordVisible = false;

    private ComboBox<String> languageCombo;
    private ComboBox<String> themeCombo;

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
    @FXML private HBox globalControlsHostLogin;
    @FXML private VBox sidePanel;
    @FXML private VBox floatingNetPayCard;
    @FXML private VBox floatingAttendanceCard;
    @FXML private VBox floatingLeaveCard;
    @FXML private VBox floatingPaydayCard;
    @FXML private VBox floatingPayslipCard;

    // initializes login page dependencies and controls
    @FXML
    private void initialize() {
        authApiClient = ApiClientFactory.getInstance().getAuthApiClient();

        buildGlobalControls();
        wireSubControllers();

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

        initSidePanel();

        ThemeManager.getInstance().applyCurrentTo(rootPane);

        showLoginCard();
        startLoginAnimations();
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
            UiMessageUtil.setInlineError(loginMessageLabel, UiMessageUtil.toUserMessage(e));
        }
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

    // toggles the side panel
    @FXML
    private void toggleSidePanel() {
        if (sidePanel == null) {
            return;
        }

        if (sidePanel.isVisible()) {
            AnimationUtil.slideOutToRight(sidePanel, 280, null);
            return;
        }

        AnimationUtil.slideInFromRight(sidePanel, 280);
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

    // validates login input fields
    private boolean isLoginInputValid(String email, String password) {
        boolean valid = true;

        if (!UiValidationUtil.isMotorphEmail(email)) {
            UiValidationUtil.markInvalid(companyEmailField);
            valid = false;
        }

        if (password.isEmpty()) {
            UiValidationUtil.markInvalid(passwordField);
            valid = false;
        }

        return valid;
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

    // builds global language and theme controls
    private void buildGlobalControls() {
        if (globalControlsHostLogin == null) {
            return;
        }

        languageCombo = new ComboBox<>();
        languageCombo.getStyleClass().add("global-combo");
        languageCombo.setMinWidth(150);
        languageCombo.setPrefWidth(150);
        languageCombo.setMaxWidth(150);
        languageCombo.getItems().setAll("English", "Filipino");
        languageCombo.setValue(ThemeManager.getInstance().getCurrentLanguage());
        languageCombo.setOnAction(e -> ThemeManager.getInstance().setLanguage(languageCombo.getValue(), rootPane));

        themeCombo = new ComboBox<>();
        themeCombo.getStyleClass().add("global-combo");
        themeCombo.setMinWidth(150);
        themeCombo.setPrefWidth(150);
        themeCombo.setMaxWidth(150);
        themeCombo.getItems().setAll("System", "Light", "Dark");
        themeCombo.setValue(ThemeManager.getInstance().getCurrentTheme());
        themeCombo.setOnAction(e -> ThemeManager.getInstance().setTheme(themeCombo.getValue(), rootPane));

        Button moreBtn = new Button("⋮");
        moreBtn.getStyleClass().add("more-button");
        moreBtn.setOnAction(e -> toggleSidePanel());

        globalControlsHostLogin.setPickOnBounds(false);
        globalControlsHostLogin.getChildren().setAll(languageCombo, themeCombo, moreBtn);

        for (Node node : globalControlsHostLogin.getParent().getChildrenUnmodifiable()) {
            if (node != globalControlsHostLogin && node instanceof javafx.scene.layout.Region region) {
                region.setPickOnBounds(false);
            }
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

    // hides the side panel on startup
    private void initSidePanel() {
        if (sidePanel != null) {
            sidePanel.setVisible(false);
            sidePanel.setManaged(false);
        }
    }

    // starts login animations
    private void startLoginAnimations() {
        AnimationUtil.fadeIn(loginCard, 220, 0);
        AnimationUtil.scaleIn(loginCard, 240, 0);

        AnimationUtil.floatNode(floatingNetPayCard, -8, 4.8, 0.1);
        AnimationUtil.floatNode(floatingAttendanceCard, 9, 5.6, 0.6);
        AnimationUtil.floatNode(floatingLeaveCard, -7, 5.2, 1.0);
        AnimationUtil.floatNode(floatingPaydayCard, 6, 6.0, 0.4);
        AnimationUtil.floatNode(floatingPayslipCard, -6, 5.8, 0.8);

        AnimationUtil.fadeIn(floatingNetPayCard, 420, 100);
        AnimationUtil.fadeIn(floatingAttendanceCard, 420, 100);
        AnimationUtil.fadeIn(floatingLeaveCard, 420, 100);
        AnimationUtil.fadeIn(floatingPaydayCard, 420, 100);
        AnimationUtil.fadeIn(floatingPayslipCard, 420, 100);
    }
}