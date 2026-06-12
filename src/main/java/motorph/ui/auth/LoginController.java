package motorph.ui.auth;

import motorph.application.dto.login.LoginRequestDTO;
import motorph.application.dto.login.LoginResultDTO;
import motorph.application.service.AuthenticationService;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.util.AnimationUtil;
import motorph.ui.util.NavigationUtil;
import motorph.ui.util.ThemeManager;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

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

/** Manages the auth page: login form, card navigation, global controls, and login animations. */
public class LoginController {

    private AuthenticationService authenticationService;
    private boolean passwordVisible = false;

    // programmatic global controls inserted into the header host
    private ComboBox<String> languageCombo;
    private ComboBox<String> themeCombo;

    @FXML
    private StackPane rootPane;

    @FXML
    private VBox loginCard;

    @FXML
    private VBox otpCard;

    @FXML
    private VBox forgotCard;

    // controllers injected by fx:include (named as {fx:id}Controller)
    @FXML
    private OtpController otpCardController;

    @FXML
    private ForgotPasswordController forgotCardController;

    @FXML
    private TextField companyEmailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordTextField;

    @FXML
    private Button passwordToggleBtn;

    @FXML
    private Label loginMessageLabel;

    @FXML
    private HBox globalControlsHostLogin;

    @FXML
    private VBox sidePanel;

    // optional floating hero cards — null when not in FXML
    @FXML
    private VBox floatingNetPayCard;

    @FXML
    private VBox floatingAttendanceCard;

    @FXML
    private VBox floatingLeaveCard;

    @FXML
    private VBox floatingPaydayCard;

    @FXML
    private VBox floatingPayslipCard;

    @FXML
    private void initialize() {
        authenticationService = EmployeeUIServiceFactory.getInstance().getAuthenticationService();

        buildGlobalControls();
        wireSubControllers();
        initSidePanel();

        ThemeManager.getInstance().applyCurrentTo(rootPane);

        showLoginCard();
        startLoginAnimations();
    }

    // called by OtpController when login succeeds — navigates to the employee shell
    public void onLoginSuccess(LoginResultDTO result) {
        NavigationUtil.goToEmployeeApp(rootPane.getScene());
    }

    // called by ForgotPasswordController when reset is complete — shows login with message
    public void onPasswordResetComplete(String successMessage) {
        showLoginCard();
        UiMessageUtil.setSuccess(loginMessageLabel, successMessage);
    }

    // shows the login card and resets login form state
    public void showLoginCard() {
        clearLoginState();
        showCard(loginCard);
    }

    @FXML
    private void handleLogin() {
        clearLoginState();

        String email = UiValidationUtil.getText(companyEmailField);
        String password = passwordVisible
                ? UiValidationUtil.getText(passwordTextField)
                : UiValidationUtil.getText(passwordField);
        boolean valid = true;

        if (!UiValidationUtil.isMotorphEmail(email)) {
            UiValidationUtil.markInvalid(companyEmailField);
            valid = false;
        }

        if (password.isEmpty()) {
            UiValidationUtil.markInvalid(passwordField);
            valid = false;
        }

        if (!valid) {
            UiMessageUtil.setError(loginMessageLabel, "Enter your MotorPH email and password.");
            return;
        }

        try {
            LoginResultDTO result = authenticationService.authenticate(new LoginRequestDTO(email, password));
            otpCardController.setPendingLoginResult(result);
            showCard(otpCard);
            otpCardController.showWithMessage("OTP sent to your registered email address.");
        } catch (Exception e) {
            if (isDatabaseUnavailable(e)) {
                otpCardController.setOfflinePreviewMode(true);
                showCard(otpCard);
                otpCardController.showWithMessage("Database is offline. Preview mode enabled. Enter any 6-digit OTP.");
                return;
            }
            UiValidationUtil.markInvalid(companyEmailField);
            UiValidationUtil.markInvalid(passwordField);
            UiMessageUtil.setError(loginMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleForgotPassword() {
        forgotCardController.showEmailStep();
        showCard(forgotCard);
    }

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
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            passwordToggleBtn.setText("Show");
        }
    }

    @FXML
    private void toggleSidePanel() {
        if (sidePanel == null) return;
        if (sidePanel.isVisible()) {
            AnimationUtil.slideOutToRight(sidePanel, 280, null);
        } else {
            AnimationUtil.slideInFromRight(sidePanel, 280);
        }
    }

    // shows only the given card in the auth card stack
    private void showCard(VBox cardToShow) {
        for (VBox card : List.of(loginCard, otpCard, forgotCard)) {
            boolean active = card == cardToShow;
            card.setVisible(active);
            card.setManaged(active);
        }
    }

    // resets login form fields and clears validation state
    private void clearLoginState() {
        UiValidationUtil.clearInvalid(companyEmailField);
        UiValidationUtil.clearInvalid(passwordField);
        UiMessageUtil.clearMessage(loginMessageLabel);
        passwordField.setVisible(true);
        passwordField.setManaged(true);
        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);
        passwordTextField.clear();
        passwordVisible = false;
        if (passwordToggleBtn != null) passwordToggleBtn.setText("Show");
    }

    // builds and inserts the language/theme combo boxes into the header host
    private void buildGlobalControls() {
        if (globalControlsHostLogin == null) return;

        languageCombo = new ComboBox<>();
        languageCombo.getStyleClass().add("global-combo");
        languageCombo.setPrefWidth(140);
        languageCombo.getItems().setAll("English", "Filipino");
        languageCombo.setValue(ThemeManager.getInstance().getCurrentLanguage());
        languageCombo.setOnAction(e -> ThemeManager.getInstance().setLanguage(languageCombo.getValue(), rootPane));

        themeCombo = new ComboBox<>();
        themeCombo.getStyleClass().add("global-combo");
        themeCombo.setPrefWidth(120);
        themeCombo.getItems().setAll("System", "Light", "Dark");
        themeCombo.setValue(ThemeManager.getInstance().getCurrentTheme());
        themeCombo.setOnAction(e -> ThemeManager.getInstance().setTheme(themeCombo.getValue(), rootPane));

        Button moreBtn = new Button("⋮");
        moreBtn.getStyleClass().add("more-button");
        moreBtn.setOnAction(e -> toggleSidePanel());

        globalControlsHostLogin.setPickOnBounds(false);
        globalControlsHostLogin.getChildren().setAll(languageCombo, themeCombo, moreBtn);

        for (Node n : globalControlsHostLogin.getParent().getChildrenUnmodifiable()) {
            if (n != globalControlsHostLogin && n instanceof javafx.scene.layout.Region r) {
                r.setPickOnBounds(false);
            }
        }
    }

    // wires sub-controller back-references so they can call back to this controller
    private void wireSubControllers() {
        if (otpCardController != null) otpCardController.setLoginController(this);
        if (forgotCardController != null) forgotCardController.setLoginController(this);
    }

    // hides the side panel on startup
    private void initSidePanel() {
        if (sidePanel != null) {
            sidePanel.setVisible(false);
            sidePanel.setManaged(false);
        }
    }

    // plays entrance animations on the login card and hero float cards
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

    // extracts a user-friendly message from an exception
    private String userMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return "Unable to complete the request. Please try again.";
        if (isDatabaseUnavailable(e)) return "Database connection unavailable. Start MySQL and try again.";
        if (msg.startsWith("Failed to ")) return "Unable to complete the request. Please check the database connection.";
        return msg;
    }

    // checks whether the exception signals a database connectivity failure
    private boolean isDatabaseUnavailable(Exception e) {
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && (msg.contains("Failed to connect to the database")
                    || msg.contains("Communications link failure")
                    || msg.contains("Connection refused")
                    || msg.contains("No suitable driver"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
