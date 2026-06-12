package motorph.ui.employee.profile;

import motorph.application.dto.employee.EmployeeProfileDTO;
import motorph.application.service.EmployeeService;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiFormatter;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.time.format.DateTimeFormatter;

/** Displays the logged-in employee's profile and allows updating phone number and address. */
public class EmployeeProfileController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private EmployeeService employeeService;

    // ── Photo ────────────────────────────────────────────────────────────────

    @FXML
    private ImageView profilePhotoView;

    @FXML
    private Label profilePhotoInitials;

    // ── Header ───────────────────────────────────────────────────────────────

    @FXML
    private Label profileFullNameLabel;

    @FXML
    private Label profileStatusLabel;

    @FXML
    private Label profileEmployeeIdLabel;

    // ── Personal information ─────────────────────────────────────────────────

    @FXML
    private Label profileFirstNameLabel;

    @FXML
    private Label profileLastNameLabel;

    @FXML
    private Label profileBirthDateLabel;

    @FXML
    private Label profileCivilStatusLabel;

    // ── Employment details ───────────────────────────────────────────────────

    @FXML
    private Label profileEmploymentStatusLabel;

    @FXML
    private Label profilePositionLabel;

    @FXML
    private Label profileDepartmentLabel;

    @FXML
    private Label profileHireDateLabel;

    @FXML
    private Label profileSupervisorLabel;

    // ── Contact information ──────────────────────────────────────────────────

    @FXML
    private Label profileCompanyEmailLabel;

    @FXML
    private Label profilePersonalEmailLabel;

    @FXML
    private TextField profilePhoneField;

    @FXML
    private TextField profileAddressField;

    // ── Message ──────────────────────────────────────────────────────────────

    @FXML
    private Label profileMessageLabel;

    @FXML
    private void initialize() {
        employeeService = EmployeeUIServiceFactory.getInstance().getEmployeeService();
    }

    // loads and displays the current employee's profile from the service
    public void refresh() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            UiMessageUtil.setError(profileMessageLabel, "No active session found.");
            return;
        }

        try {
            EmployeeProfileDTO profile = employeeService.getEmployeeProfile(employeeId);
            populateFields(profile);
            UiMessageUtil.clearMessage(profileMessageLabel);
        } catch (Exception e) {
            UiMessageUtil.setError(profileMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    // validates input and saves phone number and address via the service
    @FXML
    private void handleSaveContactInfo() {
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
            UiMessageUtil.setSuccess(profileMessageLabel, "Contact information updated successfully.");
        } catch (Exception e) {
            UiMessageUtil.setError(profileMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    // fills every display label and the editable fields from the profile DTO
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

        // employment
        set(profileEmploymentStatusLabel, profile.getEmploymentStatus() != null
                ? profile.getEmploymentStatus().name() : "—");
        set(profilePositionLabel,   "Position #"   + profile.getPositionId());
        set(profileDepartmentLabel, "Department #" + profile.getDepartmentId());
        set(profileHireDateLabel,   profile.getHireDate() != null
                ? profile.getHireDate().format(DATE_FORMAT) : "—");
        set(profileSupervisorLabel, profile.getSupervisorId() != null
                ? "Employee #" + profile.getSupervisorId() : "—");

        // contact
        set(profileCompanyEmailLabel,  UiFormatter.safe(CurrentUserSession.getCompanyEmail(), "—"));
        set(profilePersonalEmailLabel, UiFormatter.safe(profile.getPersonalEmail(), "—"));

        // editable fields
        if (profilePhoneField   != null) profilePhoneField.setText(UiFormatter.safe(profile.getPhoneNumber(), ""));
        if (profileAddressField != null) profileAddressField.setText(UiFormatter.safe(profile.getAddress(), ""));

        // initials used when no photo is available
        if (profilePhotoInitials != null) {
            profilePhotoInitials.setText(buildInitials(profile.getFirstName(), profile.getLastName()));
        }

        loadPhoto(profile.getPhotoPath());
    }

    // tries to load a photo from the given path; falls back to the initials placeholder on failure
    private void loadPhoto(String photoPath) {
        boolean showPhoto = false;

        if (photoPath != null && !photoPath.isBlank()) {
            try {
                File file = new File(photoPath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString(), 96, 96, true, true);
                    if (!image.isError() && profilePhotoView != null) {
                        profilePhotoView.setImage(image);
                        showPhoto = true;
                    }
                }
            } catch (Exception ignored) {
                // fall through to initials placeholder
            }
        }

        if (profilePhotoView != null) {
            profilePhotoView.setVisible(showPhoto);
            profilePhotoView.setManaged(showPhoto);
        }
        if (profilePhotoInitials != null) {
            profilePhotoInitials.setVisible(!showPhoto);
            profilePhotoInitials.setManaged(!showPhoto);
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
}
