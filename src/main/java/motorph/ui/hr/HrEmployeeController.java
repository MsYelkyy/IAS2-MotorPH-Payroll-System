package motorph.ui.hr;

import motorph.application.dto.auth.EmailPreviewDTO;
import motorph.application.dto.auth.EmailPreviewType;
import motorph.application.service.UserAccountService;
import motorph.api.SalaryBenefitApiClient;
import motorph.api.dto.salary.EmployeeBenefitDTO;
import motorph.api.dto.salary.SalaryHistoryDTO;
import motorph.domain.value.EmploymentStatus;
import motorph.domain.value.Gender;
import motorph.api.ApiClientFactory;
import motorph.api.EmployeeApiClient;
import motorph.api.dto.auth.CredentialsPreview;
import motorph.api.dto.employee.DepartmentDTO;
import motorph.api.dto.employee.EmployeeCreateRequest;
import motorph.api.dto.employee.EmployeeCreateResponse;
import motorph.api.dto.employee.EmployeeProfileDTO;
import motorph.api.dto.employee.EmployeeSummaryDTO;
import motorph.api.dto.employee.EmployeeUpdateRequest;
import motorph.api.dto.employee.GovernmentDetailsDTO;
import motorph.api.dto.employee.PositionDTO;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.TextFieldFormatters;
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
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** Manages the Employee Management sub-view: list, unified view/edit/add modal. */
public class HrEmployeeController {

    private static final Logger LOGGER = Logger.getLogger(HrEmployeeController.class.getName());
    private static final DateTimeFormatter BIRTH_DATE_FMT = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final double MONTHLY_HOURS = 313.0;

    // requires name@domain.tld — an @, a domain, and a dot-extension (e.g. .com, .ph)
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    /** Tracks which mode the unified form panel is in. */
    private enum FormMode { ADD, VIEW, EDIT }

    // the company-email lookup stays on the legacy direct-DB path until migrated
    private EmployeeApiClient employeeApiClient;
    private UserAccountService userAccountService;
    private SalaryBenefitApiClient salaryBenefitApiClient;

    private List<EmployeeSummaryDTO> allSummaries = new ArrayList<>();
    private List<DepartmentDTO> allDepartments = new ArrayList<>();
    private List<PositionDTO> allPositions = new ArrayList<>();
    private final ObservableList<EmployeeSummaryDTO> employeeItems = FXCollections.observableArrayList();
    private FilteredList<EmployeeSummaryDTO> filteredEmployees;

    private final Map<Integer, String>       departmentNames     = new HashMap<>();
    private final Map<Integer, List<PositionDTO>> positionsByDept = new HashMap<>();
    private final Map<String, Integer>       deptNameToId        = new LinkedHashMap<>();
    private final Map<String, PositionDTO>   addPositionLabelToObj  = new LinkedHashMap<>();
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
    @FXML private VBox   listView;

    // ── Unified form panel ────────────────────────────────────────────────────
    @FXML private VBox       formPanel;
    @FXML private ScrollPane formScrollPane;
    @FXML private Label      formTitleLabel;
    @FXML private Label      breadcrumbCurrentLabel;
    @FXML private Label      addFormErrorLabel;
    @FXML private TabPane    profileTabPane;

    // summary header (photo + name + status badge + employee number)
    @FXML private StackPane summaryPhotoFrame;
    @FXML private ImageView summaryPhotoView;
    @FXML private Label     summaryPhotoInitials;
    @FXML private Label     summaryNameLabel;
    @FXML private Label     summaryStatusBadge;
    @FXML private Label     summaryEmployeeIdLabel;

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
    @FXML private TextField grossSemiMonthlyField;

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
        employeeApiClient   = ApiClientFactory.getInstance().getEmployeeApiClient();
        userAccountService  = serviceFactory.getUserAccountService();
        salaryBenefitApiClient = ApiClientFactory.getInstance().getSalaryBenefitApiClient();
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
        setBreadcrumb("Add Employee");
        UiMessageUtil.clearMessage(addFormErrorLabel);
        setVisible(editIdentityRow, false);
        resetSummaryHeader();
        setFormFieldsEditable(true);
        showAsterisks(true);
        setGovFieldsReadOnly(false);
        switchToAddEditButtons();
        selectFirstTab();
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
        if (!validateAddForm()) {
            UiMessageUtil.showErrorToast(toastRoot, "Some required fields are missing or invalid. Please check the highlighted fields.");
            return;
        }

        try {
            EmployeeFormFields f = buildEmployeeFormFields();

            double basicSalary = parsePositiveDouble(addBasicSalaryField.getText());
            double hourlyRate  = computeHourlyRate(basicSalary);
            double rice        = parseNonNegativeDouble(riceSubsidyField.getText());
            double phone       = parseNonNegativeDouble(phoneAllowanceField.getText());
            double clothing    = parseNonNegativeDouble(clothingAllowanceField.getText());

            String sss        = sssField.getText().trim();
            String philhealth = philhealthField.getText().trim();
            String tin        = tinField.getText().trim();
            String pagibig    = pagibigField.getText().trim();

            EmployeeCreateRequest request = new EmployeeCreateRequest(
                    f.firstName(), f.lastName(), f.birthDate(), f.gender(), f.address(), f.civilStatus(),
                    f.phoneNumber(), f.personalEmail(), f.employmentStatus(), f.positionId(), f.departmentId(),
                    f.hireDate(), f.supervisorId(), basicSalary, hourlyRate, sss, philhealth, tin, pagibig,
                    rice, phone, clothing);

            EmployeeCreateResponse response = employeeApiClient.createEmployee(request);
            if (response.credentialsPreview() != null) {
                showCredentialsPreview(response.credentialsPreview());
            }

            loadEmployees();
            hideFormPanel();
            UiMessageUtil.showToast(toastRoot, "Employee added and temporary account created.");

        } catch (Exception e) {
            String msg = UiMessageUtil.toUserMessage(e);
            UiMessageUtil.setError(addFormErrorLabel, msg);
            UiMessageUtil.showErrorToast(toastRoot, msg);
        }
    }

    private void handleEditSave() {
        UiMessageUtil.clearMessage(addFormErrorLabel);
        if (selectedProfile == null) {
            UiMessageUtil.setError(addFormErrorLabel, "No employee is selected.");
            UiMessageUtil.showErrorToast(toastRoot, "No employee is selected.");
            return;
        }
        if (!validateEditForm()) {
            UiMessageUtil.showErrorToast(toastRoot, "Some required fields are missing or invalid. Please check the highlighted fields.");
            return;
        }

        try {
            EmployeeFormFields f = buildEmployeeFormFields();
            int employeeId = selectedProfile.getEmployeeId();

            EmployeeUpdateRequest request = new EmployeeUpdateRequest(
                    f.firstName(), f.lastName(), f.birthDate(), f.gender(), f.address(), f.civilStatus(),
                    f.phoneNumber(), f.personalEmail(), f.employmentStatus(), f.positionId(), f.departmentId(),
                    f.hireDate(), f.supervisorId(),
                    sssField.getText().trim(),
                    philhealthField.getText().trim(),
                    tinField.getText().trim(),
                    pagibigField.getText().trim(),
                    parseNonNegativeDouble(riceSubsidyField.getText()),
                    parseNonNegativeDouble(phoneAllowanceField.getText()),
                    parseNonNegativeDouble(clothingAllowanceField.getText()));

            selectedProfile = employeeApiClient.updateEmployee(employeeId, request);
            loadEmployees();
            openViewMode(selectedProfile);
            UiMessageUtil.showToast(toastRoot, "Employee updated successfully.");

        } catch (Exception e) {
            String msg = UiMessageUtil.toUserMessage(e);
            UiMessageUtil.setError(addFormErrorLabel, msg);
            UiMessageUtil.showErrorToast(toastRoot, msg);
        }
    }

    // ── View Employee ─────────────────────────────────────────────────────────

    private void openViewModal(EmployeeSummaryDTO dto) {
        try {
            selectedProfile = employeeApiClient.getEmployee(dto.getEmployeeId());
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
        updateSummaryHeader(profile);
        setFormFieldsEditable(false);
        showAsterisks(false);
        setGovFieldsReadOnly(true);
        setVisible(editIdentityRow, true);
        formTitleLabel.setText("Employee Profile");
        setBreadcrumb("Employee Profile");
        UiMessageUtil.clearMessage(addFormErrorLabel);
        clearAllErrors();

        // inactive employees are strictly read-only: hide Edit and Delete
        setVisible(editEmployeeButton, profile.isActive());
        setVisible(deleteEmployeeButton, profile.isActive());
        switchToViewButtons();
        selectFirstTab();
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
        updateSummaryHeader(selectedProfile);
        setFormFieldsEditable(true);
        showAsterisks(true);
        // personal email and supervisor are optional when editing an existing employee
        setVisible(addEmailAsterisk, false);
        setVisible(addSupervisorAsterisk, false);
        // government IDs and allowances are now editable in edit mode
        setGovFieldsReadOnly(false);
        setVisible(editIdentityRow, true);
        formTitleLabel.setText("Edit Employee");
        setBreadcrumb("Edit Employee");
        UiMessageUtil.clearMessage(addFormErrorLabel);
        clearAllErrors();

        switchToAddEditButtons();
        selectFirstTab();
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
                    employeeApiClient.deactivateEmployee(selectedProfile.getEmployeeId());
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

        // Combos/date-pickers: instead of disabling (which JavaFX dims to a
        // different navy shade), keep them enabled — so they render the same
        // color as the text fields — but block interaction when read-only.
        setComboReadOnly(addGenderCombo, ro);
        setComboReadOnly(addCivilStatusCombo, ro);
        setComboReadOnly(addEmploymentStatusCombo, ro);
        setComboReadOnly(addSupervisorCombo, ro);
        setComboReadOnly(addDepartmentCombo, ro);
        setComboReadOnly(addPositionCombo, ro);
        setControlReadOnly(addHireDatePicker, ro);

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
        // emp-grid-control carries the locked size; keep it on at all times so the
        // control never shifts when toggling between editable and read-only.
        field.getStyleClass().removeAll("input-field", "readonly-field");
        if (!field.getStyleClass().contains("emp-grid-control")) {
            field.getStyleClass().add("emp-grid-control");
        }
        field.getStyleClass().add("input-field");
        if (readOnly) field.getStyleClass().add("readonly-field");
    }

    /**
     * Make a ComboBox read-only WITHOUT disabling it. Disabling dims the control
     * to a different (navy) shade; instead we keep it enabled — so it renders the
     * exact same color as the text fields — and just block opening/interaction.
     */
    private void setComboReadOnly(ComboBox<String> combo, boolean readOnly) {
        setControlReadOnly(combo, readOnly);
    }

    private void setControlReadOnly(javafx.scene.control.Control control, boolean readOnly) {
        control.setMouseTransparent(readOnly);
        control.setFocusTraversable(!readOnly);
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
            applyReadOnlyStyle(field, readOnly);
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
        ok &= validateEmail(addEmailField, addEmailError, true);
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
        // personal email is optional for existing employees, but must be valid if provided
        ok &= validateEmail(addEmailField, addEmailError, false);
        ok &= requireText(addAddressField, addAddressError, "Required");
        ok &= requireCombo(addEmploymentStatusCombo, addEmploymentStatusError, "Required");
        ok &= requireCombo(addDepartmentCombo, addDepartmentError, "Required");
        ok &= requireCombo(addPositionCombo, addPositionError, "Required");
        ok &= requireDatePicker(addHireDatePicker, addHireDateError, "Required");
        // government IDs and allowances are editable in edit mode → validate them too
        ok &= validateGovId(sssField, sssError, "00-0000000-0", 12);
        ok &= validateGovId(philhealthField, philhealthError, "00-000000000-0", 14);
        ok &= validateGovId(tinField, tinError, "000-000-000-000", 15);
        ok &= validateGovId(pagibigField, pagibigError, "0000-0000-0000", 14);
        ok &= validateNonNegative(riceSubsidyField, riceSubsidyError);
        ok &= validateNonNegative(phoneAllowanceField, phoneAllowanceError);
        ok &= validateNonNegative(clothingAllowanceField, clothingAllowanceError);
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

    // validates email format; when required is false, a blank value passes (edit mode)
    private boolean validateEmail(TextField field, Label errorLabel, boolean required) {
        String val = field.getText() == null ? "" : field.getText().trim();
        if (val.isEmpty()) {
            if (required) { showError(errorLabel, "Required"); return false; }
            hideError(errorLabel);
            return true;
        }
        if (!val.matches(EMAIL_REGEX)) { showError(errorLabel, "Invalid email"); return false; }
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
            updateGrossSemiMonthlyFromBasicSalary(n);
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

        // Live-update the summary header name while adding/editing.
        addFirstNameField.textProperty().addListener((o, ov, nv) -> refreshSummaryName());
        addLastNameField.textProperty().addListener((o, ov, nv) -> refreshSummaryName());
    }

    private void refreshSummaryName() {
        if (currentMode == FormMode.VIEW) return;
        String first = addFirstNameField.getText() == null ? "" : addFirstNameField.getText().trim();
        String last  = addLastNameField.getText() == null ? "" : addLastNameField.getText().trim();
        String full  = (first + " " + last).trim();
        summaryNameLabel.setText(full.isEmpty() ? "New Employee" : full);
        summaryPhotoInitials.setText(buildInitials(first, last));
    }

    // ── Reference data ────────────────────────────────────────────────────────

    private void loadReferenceData() {
        try {
            allDepartments = employeeApiClient.getAllDepartments();
            allPositions   = employeeApiClient.getAllPositions();

            departmentNames.clear();
            deptNameToId.clear();
            positionsByDept.clear();

            for (DepartmentDTO d : allDepartments) {
                departmentNames.put(d.getDepartmentId(), d.getDepartmentName());
                deptNameToId.put(d.getDepartmentName(), d.getDepartmentId());
            }
            for (PositionDTO p : allPositions) {
                positionsByDept.computeIfAbsent(p.getDepartmentId(), k -> new ArrayList<>()).add(p);
            }
        } catch (Exception e) {
            // leaves the dropdown caches empty on load failure
            LOGGER.log(Level.WARNING, "Failed to load departments and positions", e);
        }
    }

    private void loadEmployees() {
        try {
            allSummaries = employeeApiClient.getAllEmployeeSummaries();
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
                allDepartments.stream().map(DepartmentDTO::getDepartmentName).collect(Collectors.toList()));
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
                allDepartments.stream().map(DepartmentDTO::getDepartmentName).collect(Collectors.toList()));
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

        List<PositionDTO> filtered = positionsByDept.getOrDefault(deptId, Collections.emptyList());
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

    // ── Build request fields ──────────────────────────────────────────────────

    /** Holds the profile fields shared by EmployeeCreateRequest and EmployeeUpdateRequest. */
    private record EmployeeFormFields(
            String firstName,
            String lastName,
            LocalDate birthDate,
            Gender gender,
            String address,
            String civilStatus,
            String phoneNumber,
            String personalEmail,
            EmploymentStatus employmentStatus,
            int positionId,
            int departmentId,
            LocalDate hireDate,
            Integer supervisorId
    ) {
    }

    private EmployeeFormFields buildEmployeeFormFields() {
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
        PositionDTO position = addPositionLabelToObj.get(posName);
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

        return new EmployeeFormFields(
                firstName, lastName, birthDate,
                Gender.fromValue(genderStr),
                addAddressField.getText() != null ? addAddressField.getText().trim() : null,
                addCivilStatusCombo.getValue(),
                addPhoneField.getText() != null ? addPhoneField.getText().trim() : null,
                addEmailField.getText() != null ? addEmailField.getText().trim() : null,
                EmploymentStatus.fromValue(statusStr),
                position.getPositionId(), deptId, hireDate, supervisorId
        );
    }

    // feeds the backend's dev-only credentials preview into the app's existing
    // EmailPreviewService, same pattern as LoginController's OTP preview
    private void showCredentialsPreview(CredentialsPreview credentialsPreview) {
        EmailPreviewDTO preview = new EmailPreviewDTO(
                EmailPreviewType.EMPLOYEE_ACCOUNT_CREDENTIALS,
                credentialsPreview.senderEmail(),
                credentialsPreview.recipientEmail(),
                credentialsPreview.subject(),
                credentialsPreview.body(),
                "Temporary Password",
                credentialsPreview.temporaryPassword(),
                LocalDateTime.now()
        );

        EmployeeUIServiceFactory.getInstance().getEmailPreviewService().savePreview(preview);
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
        grossSemiMonthlyField.clear();
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
            SalaryHistoryDTO salary = salaryBenefitApiClient.getCurrentSalary(employeeId);
            if (salary == null) {
                salary = salaryBenefitApiClient.getSalaryHistory(employeeId).stream()
                        .findFirst()
                        .orElse(null);
            }

            if (salary == null) {
                addBasicSalaryField.clear();
                addHourlyRateField.clear();
                grossSemiMonthlyField.clear();
                return;
            }

            addBasicSalaryField.setText(formatAmount(salary.getBasicSalary()));
            addHourlyRateField.setText(formatAmount(salary.getHourlyRate()));
            grossSemiMonthlyField.setText(formatAmount(salary.getGrossSemiMonthly()));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load salary details for employee " + employeeId, e);
        }
    }

    private void populateGovernmentFields(int employeeId) {
        try {
            GovernmentDetailsDTO details = employeeApiClient.getGovernmentDetails(employeeId);
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
            Map<Integer, EmployeeBenefitDTO> currentByType = new HashMap<>();
            Map<Integer, EmployeeBenefitDTO> latestByType = new HashMap<>();

            for (EmployeeBenefitDTO benefit : salaryBenefitApiClient.getAllBenefits(employeeId)) {
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

    private EmployeeBenefitDTO benefitToDisplay(
            int benefitTypeId,
            Map<Integer, EmployeeBenefitDTO> currentByType,
            Map<Integer, EmployeeBenefitDTO> latestByType) {
        EmployeeBenefitDTO current = currentByType.get(benefitTypeId);
        return current != null ? current : latestByType.get(benefitTypeId);
    }

    private void setBenefitField(TextField field, EmployeeBenefitDTO benefit) {
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

    /** Swap from the list page to the full-page profile (no popup / backdrop). */
    private void openFormPanel() {
        setVisible(listView, false);
        setVisible(formPanel, true);
        if (formScrollPane != null) formScrollPane.setVvalue(0);
    }

    /** Swap back from the profile page to the employee list. */
    private void hideFormPanel() {
        setVisible(formPanel, false);
        setVisible(listView, true);
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
        TextFieldFormatters.installMaxLength(field, maxLength);
    }

    private void installAmountFormatter(TextField field, int maxInt, int maxDec) {
        TextFieldFormatters.installAmount(field, maxInt, maxDec);
    }

    private void installDigitMaskFormatter(TextField field, int maxDigits) {
        TextFieldFormatters.installDigitMask(field, maxDigits);
    }

    private void installPhoneFormatter(TextField field) {
        TextFieldFormatters.installPhone(field);
    }

    private String applyDateMask(TextField field, String newVal) {
        return TextFieldFormatters.applyDateMask(field, newVal);
    }

    private void autoFormatGovId(TextField field, String newVal, int[] dashPositions, int maxLen) {
        TextFieldFormatters.autoFormatGovId(field, newVal, dashPositions, maxLen);
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

    /** Gross semi-monthly rate is simply the monthly basic salary split in two. */
    private void updateGrossSemiMonthlyFromBasicSalary(String raw) {
        if (raw == null || raw.isBlank()) { grossSemiMonthlyField.clear(); return; }
        try {
            double v = Double.parseDouble(raw.trim());
            if (v <= 0) { grossSemiMonthlyField.clear(); return; }
            grossSemiMonthlyField.setText(String.format("%.2f", v / 2.0));
        } catch (NumberFormatException ignored) { grossSemiMonthlyField.clear(); }
    }

    // ── Summary header ────────────────────────────────────────────────────────

    /** Reset the summary header to its "new employee" placeholder state (Add mode). */
    private void resetSummaryHeader() {
        summaryNameLabel.setText("New Employee");
        summaryPhotoInitials.setText("?");
        setVisible(summaryStatusBadge, false);
        summaryEmployeeIdLabel.setText("—");
        loadSummaryPhoto(null, "?");
    }

    /** Populate the summary header from a loaded profile (View / Edit mode). */
    private void updateSummaryHeader(EmployeeProfileDTO profile) {
        if (profile == null) { resetSummaryHeader(); return; }

        summaryNameLabel.setText(blankToDash(profile.getFullName()));
        summaryEmployeeIdLabel.setText("EMP-" + profile.getEmployeeId());

        String status = profile.isActive() ? "ACTIVE" : "INACTIVE";
        summaryStatusBadge.setText(status);
        summaryStatusBadge.getStyleClass().removeAll("status-badge", "status-badge-inactive");
        summaryStatusBadge.getStyleClass().add(profile.isActive() ? "status-badge" : "status-badge-inactive");
        setVisible(summaryStatusBadge, true);

        String initials = buildInitials(profile.getFirstName(), profile.getLastName());
        loadSummaryPhoto(profile.getPhotoPath(), initials);
    }

    /** Size of the circular avatar icon in the summary header. */
    private static final double SUMMARY_AVATAR_SIZE = 64.0;

    /** Load a profile photo into the circular summary avatar, falling back to initials. */
    private void loadSummaryPhoto(String photoPath, String initials) {
        summaryPhotoInitials.setText(initials == null || initials.isBlank() ? "?" : initials);
        applySummaryPhotoClip();
        boolean showPhoto = false;

        if (photoPath != null && !photoPath.isBlank()) {
            try {
                File file = new File(photoPath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    if (!image.isError() && summaryPhotoView != null) {
                        double imgW = image.getWidth();
                        double imgH = image.getHeight();
                        if (imgW > 0 && imgH > 0) {
                            // cover-scale so the image fills the avatar, then the
                            // circular clip crops it into a round icon
                            double scale = Math.max(SUMMARY_AVATAR_SIZE / imgW, SUMMARY_AVATAR_SIZE / imgH);
                            summaryPhotoView.setImage(image);
                            summaryPhotoView.setFitWidth(imgW * scale);
                            summaryPhotoView.setFitHeight(imgH * scale);
                            showPhoto = true;
                        }
                    }
                }
            } catch (Exception e) {
                // falls through to initials
                LOGGER.log(Level.FINE, "Could not load employee summary photo", e);
            }
        }

        setVisible(summaryPhotoView, showPhoto);
        setVisible(summaryPhotoInitials, !showPhoto);
    }

    /** Clip the avatar frame to a circle so the photo renders as a round icon. */
    private void applySummaryPhotoClip() {
        if (summaryPhotoFrame == null) return;
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(
                SUMMARY_AVATAR_SIZE / 2, SUMMARY_AVATAR_SIZE / 2, SUMMARY_AVATAR_SIZE / 2);
        summaryPhotoFrame.setClip(clip);
    }

    private String buildInitials(String firstName, String lastName) {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName.charAt(0));
        if (lastName  != null && !lastName.isBlank())  sb.append(lastName.charAt(0));
        return sb.length() == 0 ? "?" : sb.toString().toUpperCase();
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    /** Always show the first tab (Personal Information) when the panel opens. */
    private void selectFirstTab() {
        if (profileTabPane != null) profileTabPane.getSelectionModel().selectFirst();
    }

    /** Set the trailing breadcrumb label (e.g. "Employees › Employee Profile"). */
    private void setBreadcrumb(String current) {
        if (breadcrumbCurrentLabel != null) breadcrumbCurrentLabel.setText(current);
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
        } catch (Exception e) {
            // returns blank when the account email cannot be resolved
            LOGGER.log(Level.FINE, "Could not resolve company email for employee " + employeeId, e);
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
