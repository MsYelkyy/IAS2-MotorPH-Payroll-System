package motorph.ui.employee;

import motorph.application.service.EmployeeService;
import motorph.api.ApiClientFactory;
import motorph.api.EmployeeApiClient;
import motorph.api.dto.employee.EmployeeProfileDTO;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiFormatter;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Displays the logged-in employee's profile and handles view/edit mode toggling. */
public class EmployeeProfileController {

    private static final Logger logger = Logger.getLogger(EmployeeProfileController.class.getName());

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final double PHOTO_W = 174;
    private static final double PHOTO_H = 220;

    // reads go through the backend; updateOwnContactInfo/updateOwnProfilePhoto
    // (writes) stay on the legacy direct-DB path until employee CRUD migrates (Phase 3)
    private EmployeeService employeeService;
    private EmployeeApiClient employeeApiClient;
    private EmployeeProfileDTO currentProfile;
    private String pendingPhotoPath;
    private boolean isFormattingPhone = false;

    @FXML private StackPane toastRoot;

    // Photo
    @FXML private ImageView profilePhotoView;
    @FXML private Label     profilePhotoInitials;
    @FXML private Button    uploadImageButton;
    @FXML private StackPane profilePhotoFrame;

    // Header
    @FXML private Label profileFullNameLabel;
    @FXML private Label profileEmployeeIdLabel;

    // Mode buttons
    @FXML private Button editProfileButton;
    @FXML private Button saveChangesButton;
    @FXML private Button cancelButton;

    // Personal information
    @FXML private Label profileFirstNameLabel;
    @FXML private Label profileLastNameLabel;
    @FXML private Label profileBirthDateLabel;
    @FXML private Label profileCivilStatusLabel;

    // Employment details
    @FXML private Label profileEmploymentStatusLabel;
    @FXML private Label profilePositionLabel;
    @FXML private Label profileDepartmentLabel;
    @FXML private Label profileSupervisorLabel;

    // Contact information
    @FXML private Label     profileCompanyEmailLabel;
    @FXML private Label     profilePersonalEmailLabel;
    @FXML private Label     profilePhoneDisplayLabel;
    @FXML private Label     profileAddressDisplayLabel;
    @FXML private TextField profilePhoneField;
    @FXML private TextField profileAddressField;

    // Message
    @FXML private Label profileMessageLabel;

    @FXML
    private void initialize() {
        employeeService = EmployeeUIServiceFactory.getInstance().getEmployeeService();
        employeeApiClient = ApiClientFactory.getInstance().getEmployeeApiClient();
        applyPhotoClip();
        setupPhoneFormatting();
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
            currentProfile = employeeApiClient.getEmployee(employeeId);
            populateFields(currentProfile);
            enterViewMode();
            UiMessageUtil.clearMessage(profileMessageLabel);
        } catch (Exception e) {
            UiMessageUtil.setError(profileMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    // Discards edits before leaving the screen
    public void cancelEditIfActive() {
        if (saveChangesButton != null && saveChangesButton.isVisible()) {
            handleCancel();
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

        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            UiValidationUtil.markInvalid(profilePhoneField);
            valid = false;
        } else if (digits.length() != 9) {
            UiValidationUtil.markInvalid(profilePhoneField);
            UiMessageUtil.setError(profileMessageLabel, "Phone number must be exactly 9 digits (e.g. 000-000-000).");
            return;
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

            currentProfile = employeeApiClient.getEmployee(employeeId);
            populateFields(currentProfile);
            pendingPhotoPath = null;
            enterViewMode();
            UiMessageUtil.clearMessage(profileMessageLabel);
            UiMessageUtil.showToast(toastRoot, "Profile updated successfully.");
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

    // switches to view mode: shows display labels, hides input fields
    private void enterViewMode() {
        show(profilePhoneDisplayLabel,   true);
        show(profileAddressDisplayLabel, true);
        show(profilePhoneField,          false);
        show(profileAddressField,        false);

        show(editProfileButton,  true);
        show(saveChangesButton,  false);
        show(cancelButton,       false);

        // keep managed=true so space is reserved; just hide it
        if (uploadImageButton != null) uploadImageButton.setVisible(false);
    }

    // switches to edit mode: hides display labels, shows input fields
    private void enterEditMode() {
        // sync input fields from current display labels before showing them
        if (profilePhoneField != null && profilePhoneDisplayLabel != null) {
            String raw = profilePhoneDisplayLabel.getText();
            profilePhoneField.setText("—".equals(raw) ? "" : raw);
        }
        if (profileAddressField != null && profileAddressDisplayLabel != null) {
            String raw = profileAddressDisplayLabel.getText();
            profileAddressField.setText("—".equals(raw) ? "" : raw);
        }

        show(profilePhoneDisplayLabel,   false);
        show(profileAddressDisplayLabel, false);
        show(profilePhoneField,          true);
        show(profileAddressField,        true);

        show(editProfileButton,  false);
        show(saveChangesButton,  true);
        show(cancelButton,       true);

        if (uploadImageButton != null) uploadImageButton.setVisible(true);
    }

    // fills every display label and editable fields from the profile DTO
    private void populateFields(EmployeeProfileDTO profile) {
        // header
        set(profileFullNameLabel, profile.getFullName());
        set(profileEmployeeIdLabel, "Employee ID: " + profile.getEmployeeId());

        // personal
        set(profileFirstNameLabel,   profile.getFirstName());
        set(profileLastNameLabel,    profile.getLastName());
        set(profileBirthDateLabel,   profile.getBirthDate() != null
                ? profile.getBirthDate().format(DATE_FORMAT) : "—");
        set(profileCivilStatusLabel, UiFormatter.safe(profile.getCivilStatus(), "—"));

        // employment
        set(profileEmploymentStatusLabel, profile.getEmploymentStatus() != null
                ? profile.getEmploymentStatus().name() : "—");
        set(profilePositionLabel,   UiFormatter.safe(profile.getPositionName(),   "—"));
        set(profileDepartmentLabel, UiFormatter.safe(profile.getDepartmentName(), "—"));
        set(profileSupervisorLabel, UiFormatter.safe(profile.getSupervisorName(), "—"));

        // Contact labels and input fields
        set(profileCompanyEmailLabel,  UiFormatter.safe(CurrentUserSession.getCompanyEmail(), "—"));
        set(profilePersonalEmailLabel, UiFormatter.safe(profile.getPersonalEmail(), "—"));

        String phone   = UiFormatter.safe(profile.getPhoneNumber(), "");
        String address = UiFormatter.safe(profile.getAddress(), "");

        set(profilePhoneDisplayLabel,   phone.isBlank()   ? "—" : phone);
        set(profileAddressDisplayLabel, address.isBlank() ? "—" : address);

        // normalize phone into XXX-XXX-XXX for the editable field
        String phoneDigits = phone.replaceAll("[^0-9]", "");
        if (phoneDigits.length() > 9) phoneDigits = phoneDigits.substring(0, 9);
        if (profilePhoneField != null) profilePhoneField.setText(formatPhone(phoneDigits));
        if (profileAddressField != null) profileAddressField.setText(address);

        // initials
        if (profilePhotoInitials != null) {
            profilePhotoInitials.setText(buildInitials(profile.getFirstName(), profile.getLastName()));
        }

        loadPhoto(profile.getPhotoPath());
    }

    // clips the photo frame so oversized scaled images never overflow the frame
    private void applyPhotoClip() {
        if (profilePhotoFrame == null) return;
        Rectangle clip = new Rectangle(PHOTO_W, PHOTO_H);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        profilePhotoFrame.setClip(clip);
    }

    // attaches a text-change listener that keeps the phone field in XXX-XXX-XXX format
    private void setupPhoneFormatting() {
        if (profilePhoneField == null) {
            return;
        }

        profilePhoneField.setTextFormatter(new TextFormatter<>(change -> {
            String digits = change.getControlNewText().replaceAll("[^0-9]", "");

            if (!change.getText().matches("[0-9-]*")) {
                return null;
            }

            if (digits.length() > 9) {
                return null;
            }

            String formatted = formatPhone(digits);
            change.setText(formatted);
            change.setRange(0, change.getControlText().length());
            change.setCaretPosition(formatted.length());
            change.setAnchor(formatted.length());

            return change;
        }));
    }

    // converts a raw digit string (max 9) to XXX-XXX-XXX display format
    private String formatPhone(String digits) {
        if (digits.length() <= 3) return digits;
        if (digits.length() <= 6) return digits.substring(0, 3) + "-" + digits.substring(3);
        return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
    }

    // loads a profile photo using cover-style scaling (fills frame, crops overflow)
    private void loadPhoto(String photoPath) {
        boolean showPhoto = false;

        if (photoPath != null && !photoPath.isBlank()) {
            try {
                File file = new File(photoPath);
                if (file.exists()) {
                    // load at natural size (synchronous)
                    Image image = new Image(file.toURI().toString());

                    if (!image.isError() && profilePhotoView != null) {
                        double imgW = image.getWidth();
                        double imgH = image.getHeight();

                        if (imgW > 0 && imgH > 0) {
                            // cover: scale so the image fills the entire frame in both axes
                            double scale = Math.max(PHOTO_W / imgW, PHOTO_H / imgH);
                            profilePhotoView.setImage(image);
                            profilePhotoView.setFitWidth(imgW * scale);
                            profilePhotoView.setFitHeight(imgH * scale);
                            profilePhotoView.setPreserveRatio(false);
                            profilePhotoView.setSmooth(true);
                            showPhoto = true;
                        }
                    }
                }
            } catch (Exception e) {
                // falls through to initials
                logger.log(Level.FINE, "Could not load profile photo", e);
            }
        }

        show(profilePhotoView,     showPhoto);
        show(profilePhotoInitials, !showPhoto);
    }

    // builds a 1–2 letter initials string from first and last name
    private String buildInitials(String firstName, String lastName) {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName.charAt(0));
        if (lastName  != null && !lastName.isBlank())  sb.append(lastName.charAt(0));
        return sb.isEmpty() ? "?" : sb.toString().toUpperCase();
    }

    // sets a label's text safely, treating null as em dash
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
}
