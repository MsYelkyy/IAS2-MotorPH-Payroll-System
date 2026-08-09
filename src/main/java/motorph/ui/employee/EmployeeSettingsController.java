package motorph.ui.employee;

import motorph.application.service.UserAccountService;
import motorph.api.ApiClientFactory;
import motorph.api.EmployeeApiClient;
import motorph.api.dto.employee.EmployeeProfileDTO;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiFormatter;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** Displays account info and handles self-service password changes for the employee. */
public class EmployeeSettingsController {

    private UserAccountService userAccountService;
    private EmployeeApiClient employeeApiClient;

    @FXML
    private TextField settingsEmailField;

    @FXML
    private TextField settingsRecoveryEmailField;

    @FXML
    private PasswordField settingsOldPasswordField;

    @FXML
    private PasswordField settingsNewPasswordField;

    @FXML
    private PasswordField settingsConfirmPasswordField;

    @FXML
    private Label settingsMessageLabel;

    @FXML
    private void initialize() {
        userAccountService = EmployeeUIServiceFactory.getInstance().getUserAccountService();
        employeeApiClient  = ApiClientFactory.getInstance().getEmployeeApiClient();
    }

    // loads account and recovery email for the current employee session
    public void refresh() {
        String email = CurrentUserSession.getCompanyEmail();
        if (settingsEmailField != null) settingsEmailField.setText(UiFormatter.safe(email, ""));

        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) return;

        try {
            EmployeeProfileDTO employee = employeeApiClient.getEmployee(employeeId);
            if (settingsRecoveryEmailField != null) {
                settingsRecoveryEmailField.setText(UiFormatter.safe(employee.getPersonalEmail(), ""));
            }
        } catch (Exception e) {
            UiMessageUtil.setError(settingsMessageLabel, userMessage(e));
        }
    }

    // clears password fields and messages
    public void resetForm() {
        settingsOldPasswordField.clear();
        settingsNewPasswordField.clear();
        settingsConfirmPasswordField.clear();

        if (settingsMessageLabel != null) {
            settingsMessageLabel.setText("");
        }
    }

    @FXML
    private void handleChangePassword() {
        int accountId = CurrentUserSession.getUserAccountId();
        if (accountId <= 0) {
            UiMessageUtil.setError(settingsMessageLabel, "Please log in again before changing your password.");
            return;
        }

        UiValidationUtil.clearInvalid(settingsOldPasswordField);
        UiValidationUtil.clearInvalid(settingsNewPasswordField);
        UiValidationUtil.clearInvalid(settingsConfirmPasswordField);

        String oldPassword     = UiValidationUtil.getText(settingsOldPasswordField);
        String newPassword     = UiValidationUtil.getText(settingsNewPasswordField);
        String confirmPassword = UiValidationUtil.getText(settingsConfirmPasswordField);
        boolean valid = true;

        if (oldPassword.isBlank()) {
            UiValidationUtil.markInvalid(settingsOldPasswordField);
            valid = false;
        }

        if (newPassword.length() < 8) {
            UiValidationUtil.markInvalid(settingsNewPasswordField);
            valid = false;
        }

        if (!newPassword.equals(confirmPassword)) {
            UiValidationUtil.markInvalid(settingsConfirmPasswordField);
            valid = false;
        }

        if (!valid) {
            UiMessageUtil.setError(settingsMessageLabel, "New password must be 8+ characters and match confirmation.");
            return;
        }

        try {
            userAccountService.changeOwnPassword(accountId, oldPassword, newPassword);
            settingsOldPasswordField.clear();
            settingsNewPasswordField.clear();
            settingsConfirmPasswordField.clear();
            UiMessageUtil.setSuccess(settingsMessageLabel, "Password updated successfully.");
        } catch (Exception e) {
            UiMessageUtil.setError(settingsMessageLabel, userMessage(e));
        }
    }

    // extracts a user-friendly message from an exception
    private String userMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return "Unable to complete the request. Please try again.";
        if (isDatabaseUnavailable(e)) return "Database connection unavailable. Start MySQL and try again.";
        return msg;
    }

    // checks whether the exception signals a database connectivity failure
    private boolean isDatabaseUnavailable(Exception e) {
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && (msg.contains("Failed to connect to the database")
                    || msg.contains("Communications link failure")
                    || msg.contains("Connection refused"))) return true;
            current = current.getCause();
        }
        return false;
    }

}
