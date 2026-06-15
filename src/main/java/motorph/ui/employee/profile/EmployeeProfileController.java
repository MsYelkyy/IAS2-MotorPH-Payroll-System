package motorph.ui.employee.profile;

import motorph.application.dto.employee.EmployeeProfileDTO;
import motorph.application.service.EmployeeService;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiFormatter;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.format.DateTimeFormatter;

/** Displays the logged-in employee's profile and handles view/edit mode toggling. */
public class EmployeeProfileController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private EmployeeService employeeService;
    private EmployeeProfileDTO currentProfile;
    private String pendingPhotoPath;

    // ── Photo ────────────────────────────────────────────────────────────────

    @FXML private ImageView profilePhotoView;
    @FXML private Label     profilePhotoInitials;
    @FXML private Button    uploadImageButton;
    @FXML private StackPane profilePhotoFrame;

    // ── Header ───────────────────────────────────────────────────────────────

    @FXML private Label profileFullNameLabel;
    @FXML private Label profileStatusLabel;
    @FXML private Label profileEmployeeIdLabel;

    // ── Mode buttons ─────────────────────────────────────────────────────────

    @FXML private Button editProfileButton;
    @FXML private Button saveChangesButton;
    @FXML private Button cancelButton;

    // ── Personal information ─────────────────────────────────────────────────

    @FXML private Label profileFirstNameLabel;
    @FXML private Label profileLastNameLabel;
    @FXML private Label profileBirthDateLabel;
    @FXML private Label profileCivilStatusLabel;

    // ── Employment details ───────────────────────────────────────────────────

    @FXML private Label profileEmploymentStatusLabel;
    @FXML private Label profilePositionLabel;
    @FXML private Label profileDepartmentLabel;
    @FXML private Label profileHireDateLabel;
    @FXML private Label profileSupervisorLabel;

    // ── Contact information ──────────────────────────────────────────────────

    @FXML private Label     profileCompanyEmailLabel;
    @FXML private Label     profilePersonalEmailLabel;
    @FXML private TextField profilePhoneField;
    @FXML private TextField profileAddressField;

    // ── Message ──────────────────────────────────────────────────────────────

    @FXML private Label profileMessageLabel;

    @FXML
    private void initialize() {
        employeeService = EmployeeUIServiceFactory.getInstance().getEmployeeService();
        enterViewMode();
    }

    // loads and displays the current employee's profile from the service
    public void refresh() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            UiMessageUtil.setError(profileMessageLabel, "No active session found.");
            return;
        }

        try {
            currentProfile = employeeService.getEmployeeProfile(employeeId);
            populateFields(currentProfile);
            enterViewMode();
            UiMessageUtil.clearMessage(profileMessageLabel);
        } catch (Exception e) {
            UiMessageUtil.setError(profileMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    @FXML
    private void handleEditProfile() {
        enterEditMode();
    }

    @FXML
    private void handleCancel() {
        if (currentProfile != null) {
            populateFields(currentProfile);
        }
        pendingPhotoPath = null;
        enterViewMode();
        UiMessageUtil.clearMessage(profileMessageLabel);
    }

    @FXML
    private void handleSaveChanges() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            UiMessageUtil.setError(profileMessageLabel, "No active session found.");
            return;
        }

        UiValidationUtil.clearInvalid(profilePhoneField);
        UiValidationUtil.clearInvalid(profileAddressField);

        String phone   = UiValidationUtil.getText(profilePhoneField);
        String address = UiValidationUtil.getText(profileAddressField);
        boolean valid  = true;

        if (phone.isBlank()) {
            UiValidationUtil.markInvalid(profilePhoneField);
            valid = false;
        }
        if (address.isBlank()) {
            UiValidationUtil.markInvalid(profileAddressField);
            valid = false;
        }
        if (!valid) {
            UiMessageUtil.setError(profileMessageLabel, "Phone number and address are required.");
            return;
        }

        try {
            employeeService.updateOwnContactInfo(employeeId, phone, address);

            if (pendingPhotoPath != null) {
                employeeService.updateOwnProfilePhoto(employeeId, pendingPhotoPath);
            }

            currentProfile = employeeService.getEmployeeProfile(employeeId);
            populateFields(currentProfile);
            pendingPhotoPath = null;
            enterViewMode();
            UiMessageUtil.setSuccess(profileMessageLabel, "Profile updated successfully.");
        } catch (Exception e) {
            UiMessageUtil.setError(profileMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    @FXML
    private void handleUploadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Profile Photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selected = chooser.showOpenDialog(
                profilePhotoView != null ? profilePhotoView.getScene().getWindow() : null
        );

        if (selected != null) {
            pendingPhotoPath = selected.getAbsolutePath();
            loadPhoto(pendingPhotoPath);
        }
    }

    // switches to view mode: disables editable fields, shows only the edit button
    private void enterViewMode() {
        setDisabled(profilePhoneField,   true);
        setDisabled(profileAddressField, true);
        show(editProfileButton,  true);
        show(saveChangesButton,  false);
        show(cancelButton,       false);
        show(uploadImageButton,  false);
    }

    // switches to edit mode: enables editable fields, shows save/cancel/upload buttons
    private void enterEditMode() {
        setDisabled(profilePhoneField,   false);
        setDisabled(profileAddressField, false);
        show(editProfileButton,  false);
        show(saveChangesButton,  true);
        show(cancelButton,       true);
        show(uploadImageButton,  true);
    }

    // fills every display label and editable fields from the profile DTO
    private void populateFields(EmployeeProfileDTO profile) {
        // header
        set(profileFullNameLabel,   profile.getFullName());
        set(profileStatusLabel,     profile.getEmploymentStatus() != null
                ? profile.getEmploymentStatus().name() : "—");
        set(profileEmployeeIdLabel, "Employee ID: " + profile.getEmployeeId());

        // personal
        set(profileFirstNameLabel,   profile.getFirstName());
        set(profileLastNameLabel,    profile.getLastName());
        set(profileBirthDateLabel,   profile.getBirthDate() != null
                ? profile.getBirthDate().format(DATE_FORMAT) : "—");
        set(profileCivilStatusLabel, UiFormatter.safe(profile.getCivilStatus(), "—"));

        // employment — display names, not raw IDs
        set(profileEmploymentStatusLabel, profile.getEmploymentStatus() != null
                ? profile.getEmploymentStatus().name() : "—");
        set(profilePositionLabel,   UiFormatter.safe(profile.getPositionName(),   "—"));
        set(profileDepartmentLabel, UiFormatter.safe(profile.getDepartmentName(), "—"));
        set(profileHireDateLabel,   profile.getHireDate() != null
                ? profile.getHireDate().format(DATE_FORMAT) : "—");
        set(profileSupervisorLabel, UiFormatter.safe(profile.getSupervisorName(), "—"));

        // contact
        set(profileCompanyEmailLabel,  UiFormatter.safe(CurrentUserSession.getCompanyEmail(), "—"));
        set(profilePersonalEmailLabel, UiFormatter.safe(profile.getPersonalEmail(), "—"));

        if (profilePhoneField   != null) profilePhoneField.setText(UiFormatter.safe(profile.getPhoneNumber(), ""));
        if (profileAddressField != null) profileAddressField.setText(UiFormatter.safe(profile.getAddress(), ""));

        // photo
        if (profilePhotoInitials != null) {
            profilePhotoInitials.setText(buildInitials(profile.getFirstName(), profile.getLastName()));
        }
        loadPhoto(profile.getPhotoPath());
    }

    // loads a 2x2-style profile photo and falls back to initials when unavailable
    private void loadPhoto(String photoPath) {
        boolean showPhoto = false;

        if (photoPath != null && !photoPath.isBlank()) {
            try {
                File file = new File(photoPath);

                if (file.exists()) {
                    Image image = new Image(file.toURI().toString(), 330, 405, false, true);

                    if (!image.isError() && profilePhotoView != null) {
                        profilePhotoView.setImage(image);
                        profilePhotoView.setFitWidth(110);
                        profilePhotoView.setFitHeight(135);
                        profilePhotoView.setPreserveRatio(false);
                        profilePhotoView.setSmooth(true);
                        profilePhotoView.setCache(true);
                        showPhoto = true;
                    }
                }
            } catch (Exception ignored) {
                // fall back to initials placeholder
            }
        }

        if (profilePhotoView != null) {
            profilePhotoView.setVisible(showPhoto);
            profilePhotoView.setManaged(showPhoto);
        }

        if (profilePhotoInitials != null) {
            profilePhotoInitials.setVisible(false);
            profilePhotoInitials.setManaged(false);
        }
    }

    // builds a 1–2 letter initials string from first and last name
    private String buildInitials(String firstName, String lastName) {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName.charAt(0));
        if (lastName  != null && !lastName.isBlank())  sb.append(lastName.charAt(0));
        return sb.isEmpty() ? "?" : sb.toString().toUpperCase();
    }

    // sets a label's text safely, treating null as an em dash
    private void set(Label label, String text) {
        if (label != null) label.setText(text != null ? text : "—");
    }

    // sets both visible and managed together
    private void show(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    // sets a text field's disabled state safely
    private void setDisabled(TextField field, boolean disabled) {
        if (field != null) field.setDisable(disabled);
    }
}
