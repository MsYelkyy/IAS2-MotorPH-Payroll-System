package motorph.ui.auth;

import motorph.api.ApiClientFactory;
import motorph.api.ApiException;
import motorph.api.AuthApiClient;
import motorph.api.dto.auth.TotpSetupResponse;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * First-login enrolment: shows the QR code the backend generated, then asks for one current code to
 * prove the app was set up correctly. Nothing is enabled until the backend accepts that code. The
 * QR code and the manual key are shown only on this screen and are cleared as soon as the screen
 * is left, so no secret stays in the window after enrolment ends.
 */
public class TotpSetupController {

    private AuthApiClient authApiClient;
    private LoginController loginController;
    private Timeline countdown;
    private String pendingChallengeId;

    @FXML private VBox setupRoot;
    @FXML private ImageView qrImage;
    @FXML private Label qrPlaceholder;
    @FXML private VBox manualKeyBox;
    @FXML private Label manualKeyLabel;
    @FXML private TextField codeField;
    @FXML private ProgressBar countdownBar;
    @FXML private Label countdownLabel;
    @FXML private Label messageLabel;
    @FXML private Button confirmButton;

    @FXML
    private void initialize() {
        authApiClient = ApiClientFactory.getInstance().getAuthApiClient();

        TotpCodeInput.restrictToCode(codeField);
        messageLabel.managedProperty().bind(messageLabel.textProperty().isNotEmpty());
        codeField.textProperty().addListener((obs, oldValue, newValue) -> UiMessageUtil.clearMessage(messageLabel));
        codeField.setOnAction(event -> handleConfirm());

        countdown = TotpCodeInput.countdown(countdownBar, countdownLabel);
        setupRoot.visibleProperty().addListener((obs, wasVisible, isVisible) -> {
            if (isVisible) {
                countdown.play();
            } else {
                countdown.stop();
                clearSecret();
            }
        });
    }

    public void setLoginController(LoginController loginController) {
        this.loginController = loginController;
    }

    // asks the backend for a new secret for this challenge and shows its QR code
    public void begin(String challengeId) {
        pendingChallengeId = challengeId;
        clearSecret();
        codeField.clear();
        confirmButton.setDisable(true);
        UiValidationUtil.clearInvalid(codeField);
        UiMessageUtil.clearMessage(messageLabel);

        try {
            TotpSetupResponse setup = authApiClient.startTotpSetup(challengeId);
            qrImage.setImage(new Image(new ByteArrayInputStream(Base64.getDecoder().decode(setup.qrCodePngBase64()))));
            qrPlaceholder.setVisible(false);
            manualKeyLabel.setText(setup.manualEntryKey());
            manualKeyBox.setVisible(true);
            manualKeyBox.setManaged(true);
            confirmButton.setDisable(false);
            codeField.requestFocus();
        } catch (Exception e) {
            qrPlaceholder.setText("The QR code could not be created.");
            UiMessageUtil.setInlineError(messageLabel, messageFor(e));
        }
    }

    @FXML
    private void handleConfirm() {
        UiMessageUtil.clearMessage(messageLabel);
        UiValidationUtil.clearInvalid(codeField);

        if (pendingChallengeId == null) {
            UiMessageUtil.setInlineError(messageLabel, "Please sign in again to set up your authenticator.");
            return;
        }

        String code = UiValidationUtil.getText(codeField);

        if (!code.matches("\\d{" + TotpCodeInput.DIGITS + "}")) {
            UiValidationUtil.markInvalid(codeField);
            UiMessageUtil.setInlineError(messageLabel, "Enter the 6-digit code shown in your authenticator app.");
            return;
        }

        try {
            authApiClient.confirmTotpSetup(pendingChallengeId, code);
            pendingChallengeId = null;
            clearSecret();
            codeField.clear();
            loginController.finishLogin();
        } catch (Exception e) {
            UiValidationUtil.markInvalid(codeField);
            UiMessageUtil.setInlineError(messageLabel, messageFor(e));

            if (e instanceof ApiException api && api.getHttpStatus() == 409
                    && api.getMessage() != null && api.getMessage().startsWith("Too many incorrect codes")) {
                pendingChallengeId = null;
                confirmButton.setDisable(true);
                clearSecret();
            }
        }
    }

    @FXML
    private void handleCancel() {
        pendingChallengeId = null;
        clearSecret();
        codeField.clear();
        UiMessageUtil.clearMessage(messageLabel);

        if (loginController != null) {
            loginController.showLoginCard();
        }
    }

    // drops the QR code and the manual key from the window
    private void clearSecret() {
        qrImage.setImage(null);
        qrPlaceholder.setText("Preparing your QR code...");
        qrPlaceholder.setVisible(true);
        manualKeyLabel.setText("");
        manualKeyBox.setVisible(false);
        manualKeyBox.setManaged(false);
    }

    private String messageFor(Exception e) {
        if (e instanceof ApiException api && api.getHttpStatus() == 401
                && "AUTHENTICATION_FAILED".equals(api.getCode())) {
            return "Your setup session has expired. Please sign in again.";
        }

        return UiMessageUtil.toUserMessage(e);
    }
}
