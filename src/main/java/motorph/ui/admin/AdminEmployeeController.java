package motorph.ui.admin;

import motorph.application.dto.employee.EmployeeProfileDTO;
import motorph.application.dto.employee.EmployeeSummaryDTO;
import motorph.application.service.EmployeeService;
import motorph.application.service.SalaryBenefitService;
import motorph.application.service.UserAccountService;
import motorph.domain.model.Department;
import motorph.domain.model.Employee;
import motorph.domain.model.EmployeeBenefit;
import motorph.domain.model.GovernmentDetails;
import motorph.domain.model.Position;
import motorph.domain.model.SalaryHistory;
import motorph.domain.value.EmploymentStatus;
import motorph.domain.value.Gender;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiMessageUtil;

import javafx.scene.layout.StackPane;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** Manages the Employee Management sub-view: list, unified view/edit/add modal. */
public class AdminEmployeeController {

    private static final Logger LOGGER = Logger.getLogger(AdminEmployeeController.class.getName());
    private static final DateTimeFormatter BIRTH_DATE_FMT = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final String FORM_PANEL_CLASS    = "admin-add-form-panel";
    private static final String FORM_BACKDROP_STYLE = "-fx-background-color: rgba(0,0,0,0.46);";
    private static final double MONTHLY_HOURS = 313.0;

    /** Tracks which mode the unified form panel is in. */
    private enum FormMode { ADD, VIEW, EDIT }

    private EmployeeService employeeService;
    private UserAccountService userAccountService;
    private SalaryBenefitService salaryBenefitService;

    private List<EmployeeSummaryDTO> allSummaries = new ArrayList<>();
    private List<Department> allDepartments = new ArrayList<>();
    private List<Position> allPositions = new ArrayList<>();
    private final ObservableList<EmployeeSummaryDTO> employeeItems = FXCollections.observableArrayList();
    private FilteredList<EmployeeSummaryDTO> filteredEmployees;

    private final Map<Integer, String>       departmentNames     = new HashMap<>();
    private final Map<Integer, List<Position>> positionsByDept   = new HashMap<>();
    private final Map<String, Integer>       deptNameToId        = new LinkedHashMap<>();
    private final Map<String, Position>      addPositionLabelToObj  = new LinkedHashMap<>();
    private final Map<String, Integer>       addSupervisorLabelToId = new LinkedHashMap<>();

    private EmployeeProfileDTO selectedProfile;
    private FormMode currentMode = FormMode.ADD;

    // ── Toolbar / filter ──────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> departmentFilterCombo;
    @FXML private Label activeEmployeeCountLabel;
    @FXML private Label totalEmployeeCountLabel;
    @FXML private StackPane toastRoot;

    // ── Table ─────────────────────────────────────────────────────────────────
    @FXML private TableView<EmployeeSummaryDTO> employeeTable;
    @FXML private TableColumn<EmployeeSummaryDTO, Number> employeeIdColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, String> employeeNameColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, String> employeeStatusColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, String> employeeTypeColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, String> employeeDepartmentColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, Void>   employeeActionsColumn;

    @FXML private Region formBackdrop;

    // ── Unified form panel ────────────────────────────────────────────────────
    @FXML private VBox       formPanel;
    @FXML private ScrollPane formScrollPane;
    @FXML private Label      formTitleLabel;
    @FXML private Label      addFormErrorLabel;

    // action bars inside formPanel
    @FXML private HBox   viewActions;
    @FXML private Button deleteEmployeeButton;
    @FXML private Button editEmployeeButton;
    @FXML private HBox   addEditActions;

    // identity row (shown only in view/edit mode)
    @FXML private HBox      editIdentityRow;
    @FXML private TextField editEmployeeIdField;
    @FXML private TextField editCompanyEmailField;

    // personal fields
    @FXML private TextField addFirstNameField;
    @FXML private Label     addFirstNameError;
    @FXML private Label     addFirstNameAsterisk;
    @FXML private TextField addLastNameField;
    @FXML private Label     addLastNameError;
    @FXML private Label     addLastNameAsterisk;
    @FXML private ComboBox<String> addGenderCombo;
    @FXML private Label     addGenderError;
    @FXML private Label     addGenderAsterisk;
    @FXML private ComboBox<String> addCivilStatusCombo;
    @FXML private Label     addCivilStatusError;
    @FXML private Label     addCivilStatusAsterisk;
    @FXML private TextField addBirthDateField;
    @FXML private Label     addBirthDateError;
    @FXML private Label     addBirthDateAsterisk;
    @FXML private TextField addPhoneField;
    @FXML private Label     addPhoneError;
    @FXML private Label     addPhoneAsterisk;
    @FXML private TextField addEmailField;
    @FXML private Label     addEmailError;
    @FXML private Label     addEmailAsterisk;
    @FXML private TextField addAddressField;
    @FXML private Label     addAddressError;
    @FXML private Label     addAddressAsterisk;

    // employment fields
    @FXML private ComboBox<String> addEmploymentStatusCombo;
    @FXML private Label     addEmploymentStatusError;
    @FXML private Label     addEmploymentStatusAsterisk;
    @FXML private ComboBox<String> addSupervisorCombo;
    @FXML private Label     addSupervisorError;
    @FXML private Label     addSupervisorAsterisk;
    @FXML private ComboBox<String> addDepartmentCombo;
    @FXML private Label     addDepartmentError;
    @FXML private Label     addDepartmentAsterisk;
    @FXML private ComboBox<String> addPositionCombo;
    @FXML private Label     addPositionError;
    @FXML private Label     addPositionAsterisk;
    @FXML private DatePicker addHireDatePicker;
    @FXML private Label     addHireDateError;
    @FXML private Label     addHireDateAsterisk;
    @FXML private TextField addBasicSalaryField;
    @FXML private Label     addBasicSalaryError;
    @FXML private Label     addBasicSalaryAsterisk;
    @FXML private TextField addHourlyRateField;

    // government fields
    @FXML private TextField sssField;
    @FXML private Label     sssError;
    @FXML private Label     sssAsterisk;
    @FXML private TextField philhealthField;
    @FXML private Label     philhealthError;
    @FXML private Label     philhealthAsterisk;
    @FXML private TextField tinField;
    @FXML private Label     tinError;
    @FXML private Label     tinAsterisk;
    @FXML private TextField pagibigField;
    @FXML private Label     pagibigError;
    @FXML private Label     pagibigAsterisk;

    // benefit fields
    @FXML private TextField riceSubsidyField;
    @FXML private Label     riceSubsidyError;
    @FXML private TextField phoneAllowanceField;
    @FXML private Label     phoneAllowanceError;
    @FXML private TextField clothingAllowanceField;
    @FXML private Label     clothingAllowanceError;

    @FXML
    private void initialize() {
        EmployeeUIServiceFactory serviceFactory = EmployeeUIServiceFactory.getInstance();
        employeeService     = serviceFactory.getEmployeeService();
        userAccountService  = serviceFactory.getUserAccountService();
        salaryBenefitService = serviceFactory.getSalaryBenefitService();
        loadReferenceData();
        populateStaticCombos();
        setupListFilters();
        setupEmployeeTable();
        setupSearchListener();
        setupFormListeners();
        loadEmployees();
    }

    /** Called by AdminMainController each time the employee nav item is activated. */
    public void refresh() {
        loadEmployees();
    }

    // ── Add Employee ──────────────────────────────────────────────────────────

    @FXML
    private void showAddForm() {
        currentMode = FormMode.ADD;
        selectedProfile = null;
        clearForm();
        reloadAddSupervisorCombo();
        formTitleLabel.setText("Add Employee");
        UiMessageUtil.clearMessage(addFormErrorLabel);
        setVisible(editIdentityRow, false);
        setFormFieldsEditable(true);
        showAsterisks(true);
        setGovFieldsReadOnly(false);
        switchToAddEditButtons();
        openFormPanel();
    }

    @FXML
    private void handleCancelForm() {
        if (currentMode == FormMode.EDIT) {
            // return to view mode for the same employee
            openViewMode(selectedProfile);
        } else {
            hideFormPanel();
            currentMode = FormMode.ADD;
            selectedProfile = null;
        }
    }

    @FXML
    private void handleAddSave() {
        if (currentMode == FormMode.EDIT) {
            handleEditSave();
            return;
        }

        UiMessageUtil.clearMessage(addFormErrorLabel);
        if (!validateAddForm()) return;

        try {
            Employee built = buildEmployeeFromForm();

            double basicSalary = parsePositiveDouble(addBasicSalaryField.getText());
            double hourlyRate  = computeHourlyRate(basicSalary);
            double rice        = parseNonNegativeDouble(riceSubsidyField.getText());
            double phone       = parseNonNegativeDouble(phoneAllowanceField.getText());
            double clothing    = parseNonNegativeDouble(clothingAllowanceField.getText());

            String sss        = sssField.getText().trim();
            String philhealth = philhealthField.getText().trim();
            String tin        = tinField.getText().trim();
            String pagibig    = pagibigField.getText().trim();

            employeeService.createEmployeeWithSalaryAndBenefits(
                    built, basicSalary, hourlyRate, sss, philhealth, tin, pagibig,
                    rice, phone, clothing);

            loadEmployees();
            hideFormPanel();
            UiMessageUtil.showToast(toastRoot, "Employee added and temporary account created.");

        } catch (Exception e) {
            UiMessageUtil.setError(addFormErrorLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    private void handleEditSave() {
        UiMessageUtil.clearMessage(addFormErrorLabel);
        if (selectedProfile == null) {
            UiMessageUtil.setError(addFormErrorLabel, "No employee is selected.");
            return;
        }
        if (!validateEditForm()) return;

        try {
            Employee built = buildEmployeeFromForm();
            built.setEmployeeId(selectedProfile.getEmployeeId());
            built.setActive(selectedProfile.isActive());
            employeeService.updateEmployee(built);

            selectedProfile = employeeService.getEmployeeProfileAdmin(selectedProfile.getEmployeeId());
            loadEmployees();
            openViewMode(selectedProfile);
            UiMessageUtil.showToast(toastRoot, "Employee updated successfully.");

        } catch (Exception e) {
            UiMessageUtil.setError(addFormErrorLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    // ── View Employee ─────────────────────────────────────────────────────────

    private void openViewModal(EmployeeSummaryDTO dto) {
        try {
            selectedProfile = employeeService.getEmployeeProfileAdmin(dto.getEmployeeId());
        } catch (Exception e) {
            showErrorAlert("Could Not Load Employee", UiMessageUtil.toUserMessage(e));
            return;
        }
        openViewMode(selectedProfile);
    }

    private void openViewMode(EmployeeProfileDTO profile) {
        currentMode = FormMode.VIEW;
        selectedProfile = profile;

        populateFormFields(profile);
        setFormFieldsEditable(false);
        showAsterisks(false);
        setGovFieldsReadOnly(true);
        setVisible(editIdentityRow, true);
        formTitleLabel.setText("Employee Details");
        UiMessageUtil.clearMessage(addFormErrorLabel);
        clearAllErrors();

        // inactive employees are strictly read-only: hide Edit and Delete
        setVisible(editEmployeeButton, profile.isActive());
        setVisible(deleteEmployeeButton, profile.isActive());
        switchToViewButtons();
        openFormPanel();
    }

    @FXML
    private void handleCloseView() {
        hideFormPanel();
        currentMode = FormMode.ADD;
        selectedProfile = null;
    }

    @FXML
    private void handleStartEdit() {
        if (selectedProfile == null) return;
        currentMode = FormMode.EDIT;

        // populateFormFields calls reloadViewOrEditSupervisorCombo internally
        populateFormFields(selectedProfile);
        setFormFieldsEditable(true);
        showAsterisks(true);
        // personal email and supervisor are optional when editing an existing employee
        setVisible(addEmailAsterisk, false);
        setVisible(addSupervisorAsterisk, false);
        setGovFieldsReadOnly(true);
        setVisible(editIdentityRow, true);
        formTitleLabel.setText("Edit Employee");
        UiMessageUtil.clearMessage(addFormErrorLabel);
        clearAllErrors();

        switchToAddEditButtons();
        if (formScrollPane != null) formScrollPane.setVvalue(0);
    }

    // ── Delete Employee ───────────────────────────────────────────────────────

    @FXML
    private void handleDeleteEmployee() {
        if (selectedProfile == null) return;

        if (Objects.equals(selectedProfile.getEmployeeId(), CurrentUserSession.getEmployeeId())) {
            showErrorAlert("Action Not Allowed", "You cannot delete your own account.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Employee");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete employee " + selectedProfile.getEmployeeId() + "?");
        confirm.getButtonTypes().setAll(
                new ButtonType("Cancel"),
                new ButtonType("Delete Employee")
        );

        confirm.showAndWait().ifPresent(response -> {
            if ("Delete Employee".equals(response.getText())) {
                try {
                    employeeService.deactivateEmployee(selectedProfile.getEmployeeId());
                    hideFormPanel();
                    currentMode = FormMode.ADD;
                    selectedProfile = null;
                    loadEmployees();
                    UiMessageUtil.showToast(toastRoot, "Employee deleted successfully.");
                } catch (Exception e) {
                    showErrorAlert("Delete Failed", UiMessageUtil.toUserMessage(e));
                }
            }
        });
    }

    // ── Populate / clear form ─────────────────────────────────────────────────

    private void populateFormFields(EmployeeProfileDTO profile) {
        editEmployeeIdField.setText("#" + profile.getEmployeeId());
        editCompanyEmailField.setText(companyEmailFor(profile.getEmployeeId()));

        addFirstNameField.setText(safe(profile.getFirstName()));
        addLastNameField.setText(safe(profile.getLastName()));
        addGenderCombo.setValue(profile.getGender() != null ? profile.getGender().name() : null);
        addCivilStatusCombo.setValue(toTitleCase(profile.getCivilStatus()));

        if (profile.getBirthDate() != null) {
            addBirthDateField.setText(BIRTH_DATE_FMT.format(profile.getBirthDate()));
        } else {
            addBirthDateField.clear();
        }

        addPhoneField.setText(safe(profile.getPhoneNumber()));
        addEmailField.setText(safe(profile.getPersonalEmail()));
        addAddressField.setText(safe(profile.getAddress()));

        addEmploymentStatusCombo.setValue(
                profile.getEmploymentStatus() != null ? profile.getEmploymentStatus().name() : null);
        addHireDatePicker.setValue(profile.getHireDate());

        addDepartmentCombo.setValue(profile.getDepartmentName());
        onAddDepartmentSelected();
        selectAddPositionIfValid(profile.getPositionName());

        reloadViewOrEditSupervisorCombo();
        if (profile.getSupervisorId() != null) {
            String label = addSupervisorLabelToId.entrySet().stream()
                    .filter(e -> Objects.equals(e.getValue(), profile.getSupervisorId()))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            addSupervisorCombo.setValue(label);
        } else {
            addSupervisorCombo.setValue(null);
        }

        populateSalaryFields(profile.getEmployeeId());
        populateGovernmentFields(profile.getEmployeeId());
        populateBenefitFields(profile.getEmployeeId());
    }

    /** Make all editable form fields read-only or editable. */
    private void setFormFieldsEditable(boolean editable) {
        boolean ro = !editable;

        addFirstNameField.setEditable(editable);
        addFirstNameField.setFocusTraversable(editable);
        addLastNameField.setEditable(editable);
        addLastNameField.setFocusTraversable(editable);
        addBirthDateField.setEditable(editable);
        addBirthDateField.setFocusTraversable(editable);
        addPhoneField.setEditable(editable);
        addPhoneField.setFocusTraversable(editable);
        addEmailField.setEditable(editable);
        addEmailField.setFocusTraversable(editable);
        addAddressField.setEditable(editable);
        addAddressField.setFocusTraversable(editable);

        addGenderCombo.setDisable(ro);
        addCivilStatusCombo.setDisable(ro);
        addEmploymentStatusCombo.setDisable(ro);
        addSupervisorCombo.setDisable(ro);
        addDepartmentCombo.setDisable(ro);
        addPositionCombo.setDisable(ro);
        addHireDatePicker.setDisable(ro);

        addBasicSalaryField.setEditable(editable);
        addBasicSalaryField.setFocusTraversable(editable);

        applyReadOnlyStyle(addFirstNameField, ro);
        applyReadOnlyStyle(addLastNameField, ro);
        applyReadOnlyStyle(addBirthDateField, ro);
        applyReadOnlyStyle(addPhoneField, ro);
        applyReadOnlyStyle(addEmailField, ro);
        applyReadOnlyStyle(addAddressField, ro);
        applyReadOnlyStyle(addBasicSalaryField, ro);
    }

    private void applyReadOnlyStyle(TextField field, boolean readOnly) {
        field.getStyleClass().removeAll("input-field", "readonly-field", "personal-info-control");
        if (readOnly) {
            field.getStyleClass().addAll("input-field", "readonly-field");
        } else {
            field.getStyleClass().addAll("input-field", "personal-info-control");
        }
    }

    /** Show or hide all required-asterisk labels in the form. */
    private void showAsterisks(boolean show) {
        for (Label lbl : new Label[]{
                addFirstNameAsterisk, addLastNameAsterisk, addGenderAsterisk, addCivilStatusAsterisk,
                addBirthDateAsterisk, addPhoneAsterisk, addEmailAsterisk, addAddressAsterisk,
                addEmploymentStatusAsterisk, addSupervisorAsterisk, addDepartmentAsterisk,
                addPositionAsterisk, addHireDateAsterisk, addBasicSalaryAsterisk,
                sssAsterisk, philhealthAsterisk, tinAsterisk, pagibigAsterisk
        }) setVisible(lbl, show);
    }

    /** Make government ID fields and benefit fields read-only (used in edit mode — can't change IDs). */
    private void setGovFieldsReadOnly(boolean readOnly) {
        for (TextField field : new TextField[]{
                sssField, philhealthField, tinField, pagibigField,
                riceSubsidyField, phoneAllowanceField, clothingAllowanceField
        }) {
            field.setEditable(!readOnly);
            field.setFocusTraversable(!readOnly);
            field.getStyleClass().removeAll("input-field", "readonly-field");
            field.getStyleClass().add("input-field");
            if (readOnly) field.getStyleClass().add("readonly-field");
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validateAddForm() {
        boolean ok = true;
        ok &= requireText(addFirstNameField, addFirstNameError, "Required");
        ok &= requireText(addLastNameField, addLastNameError, "Required");
        ok &= requireCombo(addGenderCombo, addGenderError, "Required");
        ok &= requireCombo(addCivilStatusCombo, addCivilStatusError, "Required");
        ok &= validateBirthDate();
        ok &= requireText(addPhoneField, addPhoneError, "Required");
        ok &= requireText(addEmailField, addEmailError, "Required");
        ok &= requireText(addAddressField, addAddressError, "Required");
        ok &= requireCombo(addEmploymentStatusCombo, addEmploymentStatusError, "Required");
        ok &= requireCombo(addSupervisorCombo, addSupervisorError, "Required");
        ok &= requireCombo(addDepartmentCombo, addDepartmentError, "Required");
        ok &= requireCombo(addPositionCombo, addPositionError, "Required");
        ok &= requireDatePicker(addHireDatePicker, addHireDateError, "Required");
        ok &= validateBasicSalary();
        ok &= validateGovId(sssField, sssError, "00-0000000-0", 12);
        ok &= validateGovId(philhealthField, philhealthError, "00-000000000-0", 14);
        ok &= validateGovId(tinField, tinError, "000-000-000-000", 15);
        ok &= validateGovId(pagibigField, pagibigError, "0000-0000-0000", 14);
        ok &= validateNonNegative(riceSubsidyField, riceSubsidyError);
        ok &= validateNonNegative(phoneAllowanceField, phoneAllowanceError);
        ok &= validateNonNegative(clothingAllowanceField, clothingAllowanceError);
        return ok;
    }

    private boolean validateEditForm() {
        boolean ok = true;
        ok &= requireText(addFirstNameField, addFirstNameError, "Required");
        ok &= requireText(addLastNameField, addLastNameError, "Required");
        ok &= requireCombo(addGenderCombo, addGenderError, "Required");
        ok &= requireCombo(addCivilStatusCombo, addCivilStatusError, "Required");
        ok &= validateBirthDate();
        ok &= requireText(addPhoneField, addPhoneError, "Required");
        // personal email is optional for existing employees
        ok &= requireText(addAddressField, addAddressError, "Required");
        ok &= requireCombo(addEmploymentStatusCombo, addEmploymentStatusError, "Required");
        ok &= requireCombo(addDepartmentCombo, addDepartmentError, "Required");
        ok &= requireCombo(addPositionCombo, addPositionError, "Required");
        ok &= requireDatePicker(addHireDatePicker, addHireDateError, "Required");
        return ok;
    }

    private boolean validateBirthDate() {
        String raw = addBirthDateField.getText() == null ? "" : addBirthDateField.getText().trim();
        if (raw.isEmpty()) { showError(addBirthDateError, "Required"); return false; }
        LocalDate date;
        try { date = LocalDate.parse(raw, BIRTH_DATE_FMT); }
        catch (DateTimeParseException e) { showError(addBirthDateError, "Invalid date"); return false; }
        if (date.isAfter(LocalDate.now())) { showError(addBirthDateError, "Cannot be future"); return false; }
        if (LocalDate.now().minusYears(18).isBefore(date)) { showError(addBirthDateError, "Must be 18+"); return false; }
        hideError(addBirthDateError);
        return true;
    }

    private boolean validateBasicSalary() {
        String raw = addBasicSalaryField.getText() == null ? "" : addBasicSalaryField.getText().trim();
        if (raw.isEmpty()) { showError(addBasicSalaryError, "Required"); return false; }
        try {
            double v = Double.parseDouble(raw);
            if (v <= 0) { showError(addBasicSalaryError, "Must be > 0"); return false; }
        } catch (NumberFormatException e) {
            showError(addBasicSalaryError, "Invalid number"); return false;
        }
        hideError(addBasicSalaryError);
        return true;
    }

    private boolean validateNonNegative(TextField field, Label errorLabel) {
        String raw = field.getText() == null ? "" : field.getText().trim();
        if (raw.isEmpty()) { hideError(errorLabel); return true; }
        try {
            if (Double.parseDouble(raw) < 0) { showError(errorLabel, "Cannot be negative"); return false; }
        } catch (NumberFormatException e) {
            showError(errorLabel, "Invalid number"); return false;
        }
        hideError(errorLabel);
        return true;
    }

    private boolean requireText(TextField field, Label errorLabel, String msg) {
        String val = field.getText() == null ? "" : field.getText().trim();
        if (val.isEmpty()) { showError(errorLabel, msg); return false; }
        hideError(errorLabel);
        return true;
    }

    private boolean requireCombo(ComboBox<String> combo, Label errorLabel, String msg) {
        if (combo.getValue() == null || combo.getValue().isBlank()) {
            showError(errorLabel, msg); return false;
        }
        hideError(errorLabel);
        return true;
    }

    private boolean requireDatePicker(DatePicker dp, Label errorLabel, String msg) {
        if (dp.getValue() == null) { showError(errorLabel, msg); return false; }
        hideError(errorLabel);
        return true;
    }

    private boolean validateGovId(TextField field, Label errorLabel, String hint, int expectedLen) {
        String val = field.getText() == null ? "" : field.getText().trim();
        if (val.isEmpty()) { showError(errorLabel, "Required"); return false; }
        if (val.length() != expectedLen) { showError(errorLabel, "Format: " + hint); return false; }
        hideError(errorLabel);
        return true;
    }

    private void showError(Label label, String msg) {
        if (label == null) return;
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideError(Label label) {
        if (label == null) return;
        label.setVisible(false);
        label.setManaged(false);
    }

    private void clearAllErrors() {
        for (Label lbl : new Label[]{
                addFirstNameError, addLastNameError, addGenderError, addCivilStatusError,
                addBirthDateError, addPhoneError, addEmailError, addAddressError,
                addEmploymentStatusError, addSupervisorError, addDepartmentError,
                addPositionError, addHireDateError, addBasicSalaryError,
                sssError, philhealthError, tinError, pagibigError,
                riceSubsidyError, phoneAllowanceError, clothingAllowanceError
        }) hideError(lbl);
        UiMessageUtil.clearMessage(addFormErrorLabel);
    }

    // ── Form listeners ────────────────────────────────────────────────────────

    private void setupFormListeners() {
        installMaxLengthFormatter(addFirstNameField, 50);
        installMaxLengthFormatter(addLastNameField, 50);
        installMaxLengthFormatter(addAddressField, 255);
        installMaxLengthFormatter(addEmailField, 100);

        installDigitMaskFormatter(addBirthDateField, 8);
        addBirthDateField.textProperty().addListener((obs, o, n) -> {
            applyDateMask(addBirthDateField, n);
            hideError(addBirthDateError);
        });

        addHireDatePicker.setConverter(datePickerConverter());
        TextField addHireEditor = addHireDatePicker.getEditor();
        installDigitMaskFormatter(addHireEditor, 8);
        addHireEditor.textProperty().addListener((obs, o, n) -> {
            String masked = applyDateMask(addHireEditor, n);
            hideError(addHireDateError);
            if (masked.length() == 10) {
                try { addHireDatePicker.setValue(LocalDate.parse(masked, BIRTH_DATE_FMT)); }
                catch (DateTimeParseException ignored) { addHireDatePicker.setValue(null); }
            } else {
                addHireDatePicker.setValue(null);
            }
        });

        installPhoneFormatter(addPhoneField);
        addPhoneField.textProperty().addListener((obs, o, n) -> hideError(addPhoneError));

        installDigitMaskFormatter(sssField, 10);
        sssField.textProperty().addListener((obs, o, n) -> { autoFormatGovId(sssField, n, new int[]{2, 9}, 12); hideError(sssError); });

        installDigitMaskFormatter(philhealthField, 12);
        philhealthField.textProperty().addListener((obs, o, n) -> { autoFormatGovId(philhealthField, n, new int[]{2, 12}, 14); hideError(philhealthError); });

        installDigitMaskFormatter(tinField, 12);
        tinField.textProperty().addListener((obs, o, n) -> { autoFormatGovId(tinField, n, new int[]{3, 7, 11}, 15); hideError(tinError); });

        installDigitMaskFormatter(pagibigField, 12);
        pagibigField.textProperty().addListener((obs, o, n) -> { autoFormatGovId(pagibigField, n, new int[]{4, 9}, 14); hideError(pagibigError); });

        installAmountFormatter(addBasicSalaryField, 9, 2);
        addBasicSalaryField.textProperty().addListener((obs, o, n) -> {
            hideError(addBasicSalaryError);
            updateHourlyRateFromBasicSalary(n, addHourlyRateField);
        });

        installAmountFormatter(riceSubsidyField, 8, 2);
        installAmountFormatter(phoneAllowanceField, 8, 2);
        installAmountFormatter(clothingAllowanceField, 8, 2);
        riceSubsidyField.textProperty().addListener((obs, o, n) -> hideError(riceSubsidyError));
        phoneAllowanceField.textProperty().addListener((obs, o, n) -> hideError(phoneAllowanceError));
        clothingAllowanceField.textProperty().addListener((obs, o, n) -> hideError(clothingAllowanceError));

        addFirstNameField.textProperty().addListener((o, ov, nv) -> hideError(addFirstNameError));
        addLastNameField.textProperty().addListener((o, ov, nv) -> hideError(addLastNameError));
        addEmailField.textProperty().addListener((o, ov, nv) -> hideError(addEmailError));
        addAddressField.textProperty().addListener((o, ov, nv) -> hideError(addAddressError));
        addGenderCombo.valueProperty().addListener((o, ov, nv) -> { if (nv != null) hideError(addGenderError); });
        addCivilStatusCombo.valueProperty().addListener((o, ov, nv) -> { if (nv != null) hideError(addCivilStatusError); });
        addEmploymentStatusCombo.valueProperty().addListener((o, ov, nv) -> { if (nv != null) hideError(addEmploymentStatusError); });
        addSupervisorCombo.valueProperty().addListener((o, ov, nv) -> { if (nv != null) hideError(addSupervisorError); });
        addDepartmentCombo.valueProperty().addListener((o, ov, nv) -> {
            onAddDepartmentSelected();
            if (nv != null) hideError(addDepartmentError);
        });
        addPositionCombo.valueProperty().addListener((o, ov, nv) -> { if (nv != null) hideError(addPositionError); });
        addHireDatePicker.valueProperty().addListener((o, ov, nv) -> { if (nv != null) hideError(addHireDateError); });
    }

    // ── Reference data ────────────────────────────────────────────────────────

    private void loadReferenceData() {
        try {
            allDepartments = employeeService.getAllDepartments();
            allPositions   = employeeService.getAllPositions();

            departmentNames.clear();
            deptNameToId.clear();
            positionsByDept.clear();

            for (Department d : allDepartments) {
                departmentNames.put(d.getDepartmentId(), d.getDepartmentName());
                deptNameToId.put(d.getDepartmentName(), d.getDepartmentId());
            }
            for (Position p : allPositions) {
                positionsByDept.computeIfAbsent(p.getDepartmentId(), k -> new ArrayList<>()).add(p);
            }
        } catch (Exception ignored) { }
    }

    private void loadEmployees() {
        try {
            allSummaries = employeeService.getAllEmployeeSummaries();
            employeeItems.setAll(allSummaries);
            updateEmployeeCounts();
            applyEmployeeFilter();
        } catch (Exception e) {
            showErrorAlert("Could Not Load Employees", UiMessageUtil.toUserMessage(e));
        }
    }

    private void updateEmployeeCounts() {
        long active = allSummaries.stream().filter(EmployeeSummaryDTO::isActive).count();
        activeEmployeeCountLabel.setText(String.valueOf(active));
        totalEmployeeCountLabel.setText(String.valueOf(allSummaries.size()));
    }

    private void populateStaticCombos() {
        addGenderCombo.getItems().setAll("MALE", "FEMALE");
        addCivilStatusCombo.getItems().setAll("Single", "Married", "Widowed", "Separated");
        addEmploymentStatusCombo.getItems().setAll("REGULAR", "PROBATIONARY", "CONTRACTUAL");
        addDepartmentCombo.getItems().setAll(
                allDepartments.stream().map(Department::getDepartmentName).collect(Collectors.toList()));
        addPositionCombo.setDisable(true);
    }

    private void setupListFilters() {
        statusFilterCombo.getItems().setAll("All Statuses", "ACTIVE", "INACTIVE");
        statusFilterCombo.setValue("ACTIVE");
        statusFilterCombo.setOnAction(e -> applyEmployeeFilter());

        typeFilterCombo.getItems().setAll("All Types", "REGULAR", "PROBATIONARY", "CONTRACTUAL");
        typeFilterCombo.setValue("All Types");
        typeFilterCombo.setOnAction(e -> applyEmployeeFilter());

        departmentFilterCombo.getItems().setAll("All Departments");
        departmentFilterCombo.getItems().addAll(
                allDepartments.stream().map(Department::getDepartmentName).collect(Collectors.toList()));
        departmentFilterCombo.setValue("All Departments");
        departmentFilterCombo.setOnAction(e -> applyEmployeeFilter());
    }

    // ── Department / position cascades ────────────────────────────────────────

    private void onAddDepartmentSelected() {
        String deptName = addDepartmentCombo.getValue();
        addPositionCombo.getItems().clear();
        addPositionCombo.setValue(null);
        addPositionLabelToObj.clear();

        if (deptName == null || deptName.isBlank()) { addPositionCombo.setDisable(true); return; }
        Integer deptId = deptNameToId.get(deptName);
        if (deptId == null) { addPositionCombo.setDisable(true); return; }

        List<Position> filtered = positionsByDept.getOrDefault(deptId, Collections.emptyList());
        filtered.forEach(p -> {
            addPositionCombo.getItems().add(p.getPositionName());
            addPositionLabelToObj.put(p.getPositionName(), p);
        });
        addPositionCombo.setDisable(filtered.isEmpty());
    }

    private void selectAddPositionIfValid(String positionName) {
        if (positionName != null && addPositionLabelToObj.containsKey(positionName)) {
            addPositionCombo.setValue(positionName);
        } else {
            addPositionCombo.setValue(null);
        }
    }

    private void reloadAddSupervisorCombo() {
        addSupervisorLabelToId.clear();
        addSupervisorCombo.getItems().clear();
        allSummaries.stream()
                .filter(EmployeeSummaryDTO::isActive)
                .forEach(s -> {
                    addSupervisorCombo.getItems().add(s.getFullName());
                    addSupervisorLabelToId.put(s.getFullName(), s.getEmployeeId());
                });
    }

    private void reloadEditSupervisorCombo() {
        addSupervisorLabelToId.clear();
        addSupervisorCombo.getItems().clear();

        Integer currentEmployeeId = selectedProfile != null ? selectedProfile.getEmployeeId() : -1;
        Integer currentSupervisorId = selectedProfile != null ? selectedProfile.getSupervisorId() : null;

        // Include all active employees except the employee being edited
        allSummaries.stream()
                .filter(s -> s.isActive() && !Objects.equals(s.getEmployeeId(), currentEmployeeId))
                .forEach(s -> {
                    addSupervisorCombo.getItems().add(s.getFullName());
                    addSupervisorLabelToId.put(s.getFullName(), s.getEmployeeId());
                });

        // If the existing supervisor is inactive (not in allSummaries active set), add them anyway
        // so the combo can pre-select them and validation doesn't silently fail
        if (currentSupervisorId != null && !addSupervisorLabelToId.containsValue(currentSupervisorId)) {
            allSummaries.stream()
                    .filter(s -> Objects.equals(s.getEmployeeId(), currentSupervisorId))
                    .findFirst()
                    .ifPresent(s -> {
                        String label = s.getFullName() + " (inactive)";
                        addSupervisorCombo.getItems().add(0, label);
                        addSupervisorLabelToId.put(label, s.getEmployeeId());
                    });
        }
    }

    /** Reload supervisor combo for view/edit based on whether a profile is loaded. */
    private void reloadViewOrEditSupervisorCombo() {
        if (selectedProfile == null) {
            reloadAddSupervisorCombo();
        } else {
            reloadEditSupervisorCombo();
        }
    }

    // ── Build domain object ───────────────────────────────────────────────────

    private Employee buildEmployeeFromForm() {
        String firstName   = addFirstNameField.getText().trim();
        String lastName    = addLastNameField.getText().trim();
        String genderStr   = addGenderCombo.getValue();
        LocalDate hireDate = addHireDatePicker.getValue();
        String statusStr   = addEmploymentStatusCombo.getValue();
        String deptName    = addDepartmentCombo.getValue();
        String posName     = addPositionCombo.getValue();

        if (firstName.isEmpty())  throw new IllegalArgumentException("First name is required.");
        if (lastName.isEmpty())   throw new IllegalArgumentException("Last name is required.");
        if (genderStr == null)    throw new IllegalArgumentException("Gender is required.");
        if (hireDate == null)     throw new IllegalArgumentException("Hire date is required.");
        if (statusStr == null)    throw new IllegalArgumentException("Employment status is required.");
        if (deptName == null)     throw new IllegalArgumentException("Department is required.");
        if (posName == null)      throw new IllegalArgumentException("Position is required.");

        Integer deptId = deptNameToId.get(deptName);
        if (deptId == null) throw new IllegalArgumentException("Invalid department selected.");
        Position position = addPositionLabelToObj.get(posName);
        if (position == null) throw new IllegalArgumentException("Invalid position selected.");

        String supVal = addSupervisorCombo.getValue();
        Integer supervisorId = null;
        if (currentMode == FormMode.ADD) {
            if (supVal == null || supVal.isBlank()) throw new IllegalArgumentException("Supervisor is required.");
            supervisorId = addSupervisorLabelToId.get(supVal);
            if (supervisorId == null) throw new IllegalArgumentException("Invalid supervisor selected.");
        } else {
            if (supVal != null && !supVal.isBlank()) {
                supervisorId = addSupervisorLabelToId.get(supVal);
            }
        }

        LocalDate birthDate = null;
        String rawBirth = addBirthDateField.getText() == null ? "" : addBirthDateField.getText().trim();
        if (!rawBirth.isEmpty()) {
            try { birthDate = LocalDate.parse(rawBirth, BIRTH_DATE_FMT); }
            catch (DateTimeParseException e) { throw new IllegalArgumentException("Invalid birth date format."); }
        }

        return new Employee(
                null, firstName, lastName, birthDate,
                Gender.fromValue(genderStr),
                addAddressField.getText() != null ? addAddressField.getText().trim() : null,
                addCivilStatusCombo.getValue(),
                addPhoneField.getText() != null ? addPhoneField.getText().trim() : null,
                addEmailField.getText() != null ? addEmailField.getText().trim() : null,
                EmploymentStatus.fromValue(statusStr),
                position.getPositionId(), deptId, hireDate, supervisorId, true, null
        );
    }

    // ── Clear form ────────────────────────────────────────────────────────────

    private void clearForm() {
        addFirstNameField.clear();
        addLastNameField.clear();
        addGenderCombo.setValue(null);
        addCivilStatusCombo.setValue(null);
        addBirthDateField.clear();
        addPhoneField.clear();
        addEmailField.clear();
        addAddressField.clear();
        addHireDatePicker.setValue(null);
        addEmploymentStatusCombo.setValue(null);
        addDepartmentCombo.setValue(null);
        addPositionCombo.getItems().clear();
        addPositionCombo.setValue(null);
        addPositionCombo.setDisable(true);
        addPositionLabelToObj.clear();
        addSupervisorCombo.setValue(null);
        addBasicSalaryField.clear();
        addHourlyRateField.clear();
        editEmployeeIdField.clear();
        editCompanyEmailField.clear();
        sssField.clear();
        philhealthField.clear();
        tinField.clear();
        pagibigField.clear();
        riceSubsidyField.clear();
        phoneAllowanceField.clear();
        clothingAllowanceField.clear();
        clearAllErrors();
    }

    private void populateSalaryFields(int employeeId) {
        try {
            SalaryHistory salary = salaryBenefitService.getCurrentSalary(employeeId);
            if (salary == null) {
                salary = salaryBenefitService.getSalaryHistory(employeeId).stream()
                        .findFirst()
                        .orElse(null);
            }

            if (salary == null) {
                addBasicSalaryField.clear();
                addHourlyRateField.clear();
                return;
            }

            addBasicSalaryField.setText(formatAmount(salary.getBasicSalary()));
            addHourlyRateField.setText(formatAmount(salary.getHourlyRate()));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load salary details for employee " + employeeId, e);
        }
    }

    private void populateGovernmentFields(int employeeId) {
        try {
            GovernmentDetails details = employeeService.getGovernmentDetails(employeeId);
            if (details == null) {
                sssField.clear();
                philhealthField.clear();
                tinField.clear();
                pagibigField.clear();
                return;
            }

            sssField.setText(safe(details.getSssNumber()));
            philhealthField.setText(safe(details.getPhilhealthNumber()));
            tinField.setText(safe(details.getTinNumber()));
            pagibigField.setText(safe(details.getPagibigNumber()));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load government details for employee " + employeeId, e);
        }
    }

    private void populateBenefitFields(int employeeId) {
        try {
            Map<Integer, EmployeeBenefit> currentByType = new HashMap<>();
            Map<Integer, EmployeeBenefit> latestByType = new HashMap<>();

            for (EmployeeBenefit benefit : salaryBenefitService.getAllBenefits(employeeId)) {
                int typeId = benefit.getBenefitTypeId();
                if (typeId < 1 || typeId > 3) continue;

                latestByType.putIfAbsent(typeId, benefit);
                if (benefit.isCurrent()) {
                    currentByType.putIfAbsent(typeId, benefit);
                }
            }

            setBenefitField(riceSubsidyField, benefitToDisplay(1, currentByType, latestByType));
            setBenefitField(phoneAllowanceField, benefitToDisplay(2, currentByType, latestByType));
            setBenefitField(clothingAllowanceField, benefitToDisplay(3, currentByType, latestByType));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load benefit details for employee " + employeeId, e);
        }
    }

    private EmployeeBenefit benefitToDisplay(
            int benefitTypeId,
            Map<Integer, EmployeeBenefit> currentByType,
            Map<Integer, EmployeeBenefit> latestByType) {
        EmployeeBenefit current = currentByType.get(benefitTypeId);
        return current != null ? current : latestByType.get(benefitTypeId);
    }

    private void setBenefitField(TextField field, EmployeeBenefit benefit) {
        if (benefit == null) {
            field.clear();
            return;
        }
        field.setText(formatAmount(benefit.getAmount()));
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, o, t) -> applyEmployeeFilter());
    }

    private void setupEmployeeTable() {
        employeeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        employeeIdColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getEmployeeId()));
        employeeNameColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        employeeStatusColumn.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().isActive() ? "ACTIVE" : "INACTIVE"));
        employeeStatusColumn.setCellFactory(col -> statusCell());
        employeeTypeColumn.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEmploymentStatus() != null ? d.getValue().getEmploymentStatus().name() : "—"));
        employeeDepartmentColumn.setCellValueFactory(d -> new SimpleStringProperty(
                departmentNames.getOrDefault(d.getValue().getDepartmentId(),
                        "#" + d.getValue().getDepartmentId())));

        employeeActionsColumn.setSortable(false);
        employeeActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            {
                viewButton.getStyleClass().add("table-action-button");
                viewButton.setOnAction(e -> {
                    EmployeeSummaryDTO dto = getTableRow().getItem();
                    if (dto != null) openViewModal(dto);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(empty ? null : viewButton);
            }
        });

        filteredEmployees = new FilteredList<>(employeeItems, e -> true);
        SortedList<EmployeeSummaryDTO> sorted = new SortedList<>(filteredEmployees);
        sorted.comparatorProperty().bind(employeeTable.comparatorProperty());
        employeeTable.setItems(sorted);
    }

    private TableCell<EmployeeSummaryDTO, String> statusCell() {
        return new TableCell<>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) { setGraphic(null); return; }
                badge.setText(value);
                badge.getStyleClass().removeAll("status-badge", "status-badge-inactive");
                badge.getStyleClass().add("ACTIVE".equals(value) ? "status-badge" : "status-badge-inactive");
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(badge);
            }
        };
    }

    private void applyEmployeeFilter() {
        if (filteredEmployees == null) return;

        String lower  = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String status = statusFilterCombo.getValue();
        String type   = typeFilterCombo.getValue();
        String dept   = departmentFilterCombo.getValue();

        filteredEmployees.setPredicate(dto -> {
            boolean statusMatches;
            if (status == null || "All Statuses".equals(status)) {
                statusMatches = true;
            } else if ("ACTIVE".equals(status)) {
                statusMatches = dto.isActive();
            } else {
                statusMatches = !dto.isActive();
            }

            if ("All Statuses".equals(status) && !dto.isActive()) {
                statusMatches = false;
            }

            boolean searchMatches = lower.isBlank()
                    || dto.getFullName().toLowerCase().contains(lower)
                    || String.valueOf(dto.getEmployeeId()).contains(lower);
            boolean typeMatches = type == null || "All Types".equals(type)
                    || (dto.getEmploymentStatus() != null && dto.getEmploymentStatus().name().equals(type));
            String deptName = departmentNames.getOrDefault(dto.getDepartmentId(), "#" + dto.getDepartmentId());
            boolean deptMatches = dept == null || "All Departments".equals(dept) || deptName.equals(dept);

            return searchMatches && statusMatches && typeMatches && deptMatches;
        });
    }

    // ── Panel show/hide ───────────────────────────────────────────────────────

    private void openFormPanel() {
        applyFormVisuals(true);
        setVisible(formBackdrop, true);
        setVisible(formPanel, true);
        lockBackgroundInteraction(true);
        if (formScrollPane != null) formScrollPane.setVvalue(0);
    }

    private void hideFormPanel() {
        setVisible(formPanel, false);
        setVisible(formBackdrop, false);
        applyFormVisuals(false);
        lockBackgroundInteraction(false);
    }

    private void applyFormVisuals(boolean visible) {
        if (formPanel != null) {
            formPanel.getStyleClass().remove(FORM_PANEL_CLASS);
            if (visible) formPanel.getStyleClass().add(FORM_PANEL_CLASS);
        }
        if (formBackdrop != null) {
            formBackdrop.setStyle(visible ? FORM_BACKDROP_STYLE : "");
        }
    }

    /**
     * Block or unblock interaction with background elements while the form is open.
     * Disables the toolbar, filters, and table so the form acts as a true modal.
     */
    private void lockBackgroundInteraction(boolean lock) {
        searchField.setDisable(lock);
        statusFilterCombo.setDisable(lock);
        typeFilterCombo.setDisable(lock);
        departmentFilterCombo.setDisable(lock);
        employeeTable.setDisable(lock);
        // Disable "Add Employee" button to prevent opening a second modal
        if (formPanel != null && formPanel.getScene() != null) {
            javafx.scene.Scene scene = formPanel.getScene();
            scene.lookup("#addEmployeeBtn");  // no-op if absent
        }
    }

    private void switchToViewButtons() {
        setVisible(viewActions, true);
        setVisible(addEditActions, false);
    }

    private void switchToAddEditButtons() {
        setVisible(viewActions, false);
        setVisible(addEditActions, true);
    }

    // ── Input formatters / masks ──────────────────────────────────────────────

    private void installMaxLengthFormatter(TextField field, int maxLength) {
        field.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= maxLength ? change : null));
    }

    private void installAmountFormatter(TextField field, int maxInt, int maxDec) {
        field.setTextFormatter(new TextFormatter<>(amountFilter(maxInt, maxDec)));
    }

    private void installDigitMaskFormatter(TextField field, int maxDigits) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            if (!text.matches("[\\d-]*")) return null;
            int digits = 0;
            for (char c : text.toCharArray()) if (Character.isDigit(c)) digits++;
            return digits <= maxDigits ? change : null;
        }));
    }

    private void installPhoneFormatter(TextField field) {
        field.textProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            String digits = n.replaceAll("[^\\d]", "");
            if (digits.length() > 10) digits = digits.substring(0, 10);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i == 3 || i == 6) sb.append('-');
                sb.append(digits.charAt(i));
            }
            String result = sb.toString();
            if (!result.equals(n)) { field.setText(result); field.positionCaret(result.length()); }
        });
    }

    private UnaryOperator<TextFormatter.Change> amountFilter(int maxInt, int maxDec) {
        return change -> {
            String text = change.getControlNewText();
            if (text.isEmpty()) return change;
            if (!text.matches("\\d*(\\.\\d*)?")) return null;
            int dot = text.indexOf('.');
            String intPart = dot >= 0 ? text.substring(0, dot) : text;
            String decPart = dot >= 0 ? text.substring(dot + 1) : "";
            if (intPart.length() > maxInt || decPart.length() > maxDec) return null;
            return change;
        };
    }

    private String applyDateMask(TextField field, String newVal) {
        if (newVal == null) return "";
        String digits = newVal.replaceAll("[^\\d]", "");
        if (digits.length() > 8) digits = digits.substring(0, 8);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i == 2 || i == 4) sb.append('-');
            sb.append(digits.charAt(i));
        }
        String result = sb.toString();
        if (!result.equals(newVal)) { field.setText(result); field.positionCaret(result.length()); }
        return result;
    }

    private void autoFormatGovId(TextField field, String newVal, int[] dashPositions, int maxLen) {
        if (newVal == null) return;
        String digits = newVal.replaceAll("[^\\d]", "");
        int maxDigits = maxLen - dashPositions.length;
        if (digits.length() > maxDigits) digits = digits.substring(0, maxDigits);
        StringBuilder sb = new StringBuilder();
        int d = 0;
        for (int pos = 0; pos < maxLen && d < digits.length(); pos++) {
            boolean isDash = false;
            for (int dp : dashPositions) if (pos == dp) { isDash = true; break; }
            if (isDash) sb.append('-');
            else sb.append(digits.charAt(d++));
        }
        String result = sb.toString();
        if (!result.equals(newVal)) { field.setText(result); field.positionCaret(result.length()); }
    }

    private javafx.util.StringConverter<LocalDate> datePickerConverter() {
        return new javafx.util.StringConverter<>() {
            @Override public String toString(LocalDate date) {
                return date == null ? "" : BIRTH_DATE_FMT.format(date);
            }
            @Override public LocalDate fromString(String text) {
                if (text == null || text.trim().length() != 10) return null;
                try { return LocalDate.parse(text.trim(), BIRTH_DATE_FMT); }
                catch (DateTimeParseException e) { return null; }
            }
        };
    }

    // ── Salary helpers ────────────────────────────────────────────────────────

    private double computeHourlyRate(double basicSalary) {
        return basicSalary / MONTHLY_HOURS;
    }

    private void updateHourlyRateFromBasicSalary(String raw, TextField target) {
        if (raw == null || raw.isBlank()) { target.clear(); return; }
        try {
            double v = Double.parseDouble(raw.trim());
            if (v <= 0) { target.clear(); return; }
            target.setText(String.format("%.2f", computeHourlyRate(v)));
        } catch (NumberFormatException ignored) { target.clear(); }
    }

    private double parsePositiveDouble(String text) {
        if (text == null || text.isBlank()) return 0.0;
        try { return Double.parseDouble(text.trim()); } catch (NumberFormatException e) { return 0.0; }
    }

    private double parseNonNegativeDouble(String text) {
        double v = parsePositiveDouble(text);
        return v < 0 ? 0.0 : v;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void setVisible(javafx.scene.Node node, boolean visible) {
        if (node != null) { node.setVisible(visible); node.setManaged(visible); }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static String toTitleCase(String value) {
        if (value == null || value.isBlank()) return null;
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    private String companyEmailFor(int employeeId) {
        try {
            return safe(userAccountService.getCompanyEmailByEmployeeId(employeeId));
        } catch (Exception ignored) {
            return "";
        }
    }

    private String formatAmount(Double value) {
        return value == null ? "" : String.format("%.2f", value);
    }

    private void showErrorAlert(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Employee Management");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
