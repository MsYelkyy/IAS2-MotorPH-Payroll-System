package motorph.ui.auth;

import motorph.api.ApiClientFactory;
import motorph.api.ApiException;
import motorph.api.AuthApiClient;
import motorph.api.dto.auth.PasswordRecoveryVerifyResponse;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Asks for the six-digit code from the user's authenticator app. The same screen serves the login
 * step and the Forgot Password step; only the wording and what happens after the backend accepts the
 * code differ. The code is never shown or logged, and nothing but the code itself is kept.
 */
public class TotpVerifyController {

    private enum Mode { LOGIN, RECOVERY }

    private AuthApiClient authApiClient;
    private LoginController loginController;
    private Timeline countdown;
    private Mode mode = Mode.LOGIN;
    private String pendingChallengeId;

    @FXML private VBox verifyRoot;
    @FXML private Label eyebrowLabel;
    @FXML private Label headingLabel;
    @FXML private Label subheadingLabel;
    @FXML private TextField codeField;
    @FXML private ProgressBar countdownBar;
    @FXML private Label countdownLabel;
    @FXML private Label messageLabel;
    @FXML private Button verifyButton;

    @FXML
    private void initialize() {
        authApiClient = ApiClientFactory.getInstance().getAuthApiClient();

        TotpCodeInput.restrictToCode(codeField);
        messageLabel.managedProperty().bind(messageLabel.textProperty().isNotEmpty());
        codeField.textProperty().addListener((obs, oldValue, newValue) -> clearMessage());
        codeField.setOnAction(event -> handleVerify());

        countdown = TotpCodeInput.countdown(countdownBar, countdownLabel);
        verifyRoot.visibleProperty().addListener((obs, wasVisible, isVisible) -> {
            if (isVisible) {
                countdown.play();
            } else {
                countdown.stop();
            }
        });
    }

    public void setLoginController(LoginController loginController) {
        this.loginController = loginController;
    }

    // shows the login step for the challenge the backend issued after the password
    public void showForLogin(String challengeId) {
        mode = Mode.LOGIN;
        eyebrowLabel.setText("TWO-FACTOR AUTHENTICATION");
        headingLabel.setText("Enter your code");
        subheadingLabel.setText("Enter the 6-digit code from your authenticator app.");
        begin(challengeId);
    }

    // shows the Forgot Password step; a correct code leads to the new-password screen
    public void showForRecovery(String challengeId) {
        mode = Mode.RECOVERY;
        eyebrowLabel.setText("ACCOUNT RECOVERY");
        headingLabel.setText("Verify it's you");
        subheadingLabel.setText("Enter the 6-digit code from your authenticator app to reset your password.");
        begin(challengeId);
    }

    @FXML
    private void handleVerify() {
        clearMessage();
        UiValidationUtil.clearInvalid(codeField);

        if (pendingChallengeId == null) {
            showError("Please start again.");
            return;
        }

        String code = UiValidationUtil.getText(codeField);

        if (!code.matches("\\d{" + TotpCodeInput.DIGITS + "}")) {
            UiValidationUtil.markInvalid(codeField);
            showError("Enter the 6-digit code from your authenticator app.");
            return;
        }

        try {
            if (mode == Mode.LOGIN) {
                authApiClient.verifyTotp(pendingChallengeId, code);
                pendingChallengeId = null;
                codeField.clear();
                loginController.finishLogin();
            } else {
                PasswordRecoveryVerifyResponse verified = authApiClient.verifyRecoveryCode(pendingChallengeId, code);
                pendingChallengeId = null;
                codeField.clear();
                loginController.onRecoveryVerified(verified.resetToken());
            }
        } catch (Exception e) {
            UiValidationUtil.markInvalid(codeField);
            showError(messageFor(e));

            if (isAttemptLimitReached(e)) {
                pendingChallengeId = null;
                verifyButton.setDisable(true);
                codeField.setDisable(true);
            }
        }
    }

    @FXML
    private void handleBack() {
        reset();

        if (loginController != null) {
            loginController.showLoginCard();
        }
    }

    private void begin(String challengeId) {
        pendingChallengeId = challengeId;
        codeField.clear();
        codeField.setDisable(false);
        verifyButton.setDisable(false);
        UiValidationUtil.clearInvalid(codeField);
        clearMessage();
        codeField.requestFocus();
    }

    private void reset() {
        pendingChallengeId = null;
        codeField.clear();
        clearMessage();
    }

    private String messageFor(Exception e) {
        if (e instanceof ApiException api && api.getHttpStatus() == 401
                && "AUTHENTICATION_FAILED".equals(api.getCode())) {
            return "Your verification session has expired. Please sign in again.";
        }

        return UiMessageUtil.toUserMessage(e);
    }

    private boolean isAttemptLimitReached(Exception e) {
        return e instanceof ApiException api
                && api.getHttpStatus() == 409
                && api.getMessage() != null
                && api.getMessage().startsWith("Too many incorrect codes");
    }

    private void showError(String message) {
        UiMessageUtil.setInlineError(messageLabel, message);
    }

    private void clearMessage() {
        UiMessageUtil.clearMessage(messageLabel);
    }
}
