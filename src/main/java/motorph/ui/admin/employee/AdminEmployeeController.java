package motorph.ui.admin.employee;

import motorph.application.dto.employee.EmployeeProfileDTO;
import motorph.application.dto.employee.EmployeeSummaryDTO;
import motorph.application.service.EmployeeService;
import motorph.domain.model.Department;
import motorph.domain.model.Employee;
import motorph.domain.model.Position;
import motorph.domain.value.EmploymentStatus;
import motorph.domain.value.Gender;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiMessageUtil;

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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Manages the Employee Management sub-view: list, search, detail panel, add, edit, deactivate. */
public class AdminEmployeeController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final String FORM_PANEL_CLASS = "admin-add-form-panel";
    private static final String FORM_BACKDROP_STYLE = "-fx-background-color: rgba(0,0,0,0.46);";

    private EmployeeService employeeService;

    private List<EmployeeSummaryDTO> allSummaries = new ArrayList<>();
    private List<Department> allDepartments = new ArrayList<>();
    private List<Position> allPositions = new ArrayList<>();
    private final ObservableList<EmployeeSummaryDTO> employeeItems = FXCollections.observableArrayList();
    private FilteredList<EmployeeSummaryDTO> filteredEmployees;

    private final Map<Integer, String> departmentNames = new HashMap<>();
    private final Map<Integer, List<Position>> positionsByDept = new HashMap<>();
    private final Map<String, Integer> deptNameToId = new LinkedHashMap<>();
    private final Map<String, Position> positionLabelToObj = new LinkedHashMap<>();
    private final Map<String, Integer> supervisorLabelToId = new LinkedHashMap<>();

    private EmployeeProfileDTO selectedProfile;
    private boolean editMode;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> departmentFilterCombo;
    @FXML private Label activeEmployeeCountLabel;
    @FXML private Label totalEmployeeCountLabel;
    @FXML private Label moduleMessageLabel;

    @FXML private HBox listDetailPanel;
    @FXML private TableView<EmployeeSummaryDTO> employeeTable;
    @FXML private TableColumn<EmployeeSummaryDTO, Number> employeeIdColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, String> employeeNameColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, String> employeeStatusColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, String> employeeTypeColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, String> employeeDepartmentColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, Void> employeeActionsColumn;

    @FXML private Region formBackdrop;

    @FXML private VBox detailPanel;
    @FXML private Label detailNameLabel;
    @FXML private Label detailActiveBadge;
    @FXML private Label detailEmploymentBadge;
    @FXML private Label detailIdLabel;
    @FXML private Label detailDeptLabel;
    @FXML private Label detailPositionLabel;
    @FXML private Label detailHireDateLabel;
    @FXML private Label detailSupervisorLabel;
    @FXML private Label detailGenderLabel;
    @FXML private Label detailBirthDateLabel;
    @FXML private Label detailCivilStatusLabel;
    @FXML private Label detailPhoneLabel;
    @FXML private Label detailEmailLabel;
    @FXML private Label detailAddressLabel;
    @FXML private Button deactivateButton;

    @FXML private VBox formPanel;
    @FXML private Label formTitleLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private ComboBox<String> civilStatusCombo;
    @FXML private DatePicker birthDatePicker;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private DatePicker hireDatePicker;
    @FXML private ComboBox<String> employmentStatusCombo;
    @FXML private ComboBox<String> supervisorCombo;
    @FXML private ComboBox<String> departmentCombo;
    @FXML private ComboBox<String> positionCombo;
    @FXML private TextField addressField;
    @FXML private Label formErrorLabel;

    @FXML
    private void initialize() {
        employeeService = EmployeeUIServiceFactory.getInstance().getEmployeeService();
        loadReferenceData();
        populateStaticCombos();
        setupListFilters();
        setupEmployeeTable();
        setupSearchListener();
        loadEmployees();
    }

    /** Called by AdminMainController each time the employee nav item is activated. */
    public void refresh() {
        loadEmployees();
        closeDetail();
        UiMessageUtil.clearMessage(moduleMessageLabel);
    }

    @FXML
    private void showAddForm() {
        editMode = false;
        selectedProfile = null;
        clearForm();
        reloadSupervisorCombo();
        formTitleLabel.setText("Add Employee");
        UiMessageUtil.clearMessage(formErrorLabel);
        showFormPanel();
    }

    @FXML
    private void showEditForm() {
        if (selectedProfile == null) return;

        editMode = true;
        reloadSupervisorCombo();
        populateFormFromProfile(selectedProfile);
        formTitleLabel.setText("Edit Employee - #" + selectedProfile.getEmployeeId());
        UiMessageUtil.clearMessage(formErrorLabel);
        showFormPanel();
    }

    private void openEditFormForEmployee(EmployeeSummaryDTO dto) {
        try {
            selectedProfile = employeeService.getEmployeeProfileAdmin(dto.getEmployeeId());
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
            return;
        }

        employeeTable.refresh();
        editMode = true;
        reloadSupervisorCombo();
        populateFormFromProfile(selectedProfile);
        formTitleLabel.setText("Edit Employee - #" + selectedProfile.getEmployeeId());
        UiMessageUtil.clearMessage(formErrorLabel);
        showFormPanel();
    }

    @FXML
    private void handleSave() {
        UiMessageUtil.clearMessage(formErrorLabel);
        try {
            Employee built = buildEmployeeFromForm();

            if (editMode) {
                built.setEmployeeId(selectedProfile.getEmployeeId());
                built.setActive(selectedProfile.isActive());
                employeeService.updateEmployee(built);
                UiMessageUtil.setSuccess(moduleMessageLabel, "Employee updated successfully.");
            } else {
                employeeService.createEmployeeWithTemporaryAccount(built);
                UiMessageUtil.setSuccess(moduleMessageLabel,
                        "Employee added and temporary account created.");
            }

            loadEmployees();
            hideFormPanel();

        } catch (Exception e) {
            UiMessageUtil.setError(formErrorLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    @FXML
    private void handleDeactivate() {
        if (selectedProfile == null) return;

        if (selectedProfile.getEmployeeId() != null
                && selectedProfile.getEmployeeId() == CurrentUserSession.getEmployeeId()) {
            UiMessageUtil.setError(moduleMessageLabel, "You cannot deactivate your own account.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate Employee");
        confirm.setHeaderText("Deactivate " + selectedProfile.getFullName() + "?");
        confirm.setContentText(
                "This deactivates the employee record and disables their user account. "
                        + "All payroll history is preserved. "
                        + "An IT administrator can reverse this action."
        );

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    employeeService.deactivateEmployee(selectedProfile.getEmployeeId());
                    UiMessageUtil.setSuccess(moduleMessageLabel,
                            selectedProfile.getFullName() + " has been deactivated.");
                    loadEmployees();
                    closeDetail();
                } catch (Exception e) {
                    UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
                }
            }
        });
    }

    @FXML
    private void handleCancelForm() {
        hideFormPanel();
    }

    private void loadReferenceData() {
        try {
            allDepartments = employeeService.getAllDepartments();
            allPositions = employeeService.getAllPositions();

            departmentNames.clear();
            deptNameToId.clear();
            positionsByDept.clear();

            for (Department department : allDepartments) {
                departmentNames.put(department.getDepartmentId(), department.getDepartmentName());
                deptNameToId.put(department.getDepartmentName(), department.getDepartmentId());
            }

            for (Position position : allPositions) {
                positionsByDept.computeIfAbsent(position.getDepartmentId(), key -> new ArrayList<>())
                        .add(position);
            }
        } catch (Exception ignored) {
            // Combos will be empty; user sees the validation error on save.
        }
    }

    private void loadEmployees() {
        try {
            allSummaries = employeeService.getAllEmployeeSummaries();
            employeeItems.setAll(allSummaries);
            updateEmployeeCounts();
            applyEmployeeFilter();
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    private void updateEmployeeCounts() {
        long active = allSummaries.stream().filter(EmployeeSummaryDTO::isActive).count();
        activeEmployeeCountLabel.setText(String.valueOf(active));
        totalEmployeeCountLabel.setText(String.valueOf(allSummaries.size()));
    }

    private void populateStaticCombos() {
        genderCombo.getItems().setAll("MALE", "FEMALE");
        civilStatusCombo.getItems().setAll("Single", "Married", "Widowed", "Separated");
        employmentStatusCombo.getItems().setAll("REGULAR", "PROBATIONARY", "CONTRACTUAL");

        departmentCombo.getItems().setAll(
                allDepartments.stream()
                        .map(Department::getDepartmentName)
                        .collect(Collectors.toList())
        );

        departmentCombo.setOnAction(e -> onDepartmentSelected());
    }

    private void setupListFilters() {
        statusFilterCombo.getItems().setAll("All Statuses", "ACTIVE", "INACTIVE");
        statusFilterCombo.setValue("All Statuses");
        statusFilterCombo.setOnAction(event -> applyEmployeeFilter());

        typeFilterCombo.getItems().setAll("All Types", "REGULAR", "PROBATIONARY", "CONTRACTUAL");
        typeFilterCombo.setValue("All Types");
        typeFilterCombo.setOnAction(event -> applyEmployeeFilter());

        departmentFilterCombo.getItems().setAll("All Departments");
        departmentFilterCombo.getItems().addAll(
                allDepartments.stream()
                        .map(Department::getDepartmentName)
                        .collect(Collectors.toList())
        );
        departmentFilterCombo.setValue("All Departments");
        departmentFilterCombo.setOnAction(event -> applyEmployeeFilter());
    }

    private void onDepartmentSelected() {
        String deptName = departmentCombo.getValue();
        positionCombo.getItems().clear();
        positionCombo.setValue(null);
        positionLabelToObj.clear();

        if (deptName == null) return;
        Integer deptId = deptNameToId.get(deptName);
        if (deptId == null) return;

        List<Position> filtered = positionsByDept.getOrDefault(deptId, Collections.emptyList());
        filtered.forEach(position -> {
            positionCombo.getItems().add(position.getPositionName());
            positionLabelToObj.put(position.getPositionName(), position);
        });
    }

    private void reloadSupervisorCombo() {
        supervisorLabelToId.clear();
        supervisorCombo.getItems().clear();
        supervisorCombo.getItems().add("-- None --");

        allSummaries.stream()
                .filter(EmployeeSummaryDTO::isActive)
                .forEach(dto -> {
                    String label = "#" + dto.getEmployeeId() + "  " + dto.getFullName();
                    supervisorLabelToId.put(label, dto.getEmployeeId());
                    supervisorCombo.getItems().add(label);
                });
    }

    private void populateFormFromProfile(EmployeeProfileDTO profile) {
        firstNameField.setText(safe(profile.getFirstName()));
        lastNameField.setText(safe(profile.getLastName()));
        genderCombo.setValue(profile.getGender() != null ? profile.getGender().name() : null);
        civilStatusCombo.setValue(profile.getCivilStatus());
        birthDatePicker.setValue(profile.getBirthDate());
        phoneField.setText(safe(profile.getPhoneNumber()));
        emailField.setText(safe(profile.getPersonalEmail()));
        hireDatePicker.setValue(profile.getHireDate());
        employmentStatusCombo.setValue(
                profile.getEmploymentStatus() != null ? profile.getEmploymentStatus().name() : null);
        addressField.setText(safe(profile.getAddress()));

        departmentCombo.setValue(profile.getDepartmentName());
        positionCombo.setValue(profile.getPositionName());

        if (profile.getSupervisorId() != null) {
            String label = supervisorCombo.getItems().stream()
                    .filter(item -> item.startsWith("#" + profile.getSupervisorId() + "  "))
                    .findFirst()
                    .orElse(null);
            supervisorCombo.setValue(label);
        } else {
            supervisorCombo.setValue("-- None --");
        }
    }

    /** Builds an Employee domain object from the current form values for passing to the service. */
    private Employee buildEmployeeFromForm() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String genderStr = genderCombo.getValue();
        LocalDate hireDate = hireDatePicker.getValue();
        String statusStr = employmentStatusCombo.getValue();
        String deptName = departmentCombo.getValue();
        String positionName = positionCombo.getValue();

        if (firstName.isEmpty()) throw new IllegalArgumentException("First name is required.");
        if (lastName.isEmpty()) throw new IllegalArgumentException("Last name is required.");
        if (genderStr == null) throw new IllegalArgumentException("Gender is required.");
        if (hireDate == null) throw new IllegalArgumentException("Hire date is required.");
        if (statusStr == null) throw new IllegalArgumentException("Employment status is required.");
        if (deptName == null) throw new IllegalArgumentException("Department is required.");
        if (positionName == null) throw new IllegalArgumentException("Position is required.");

        Integer deptId = deptNameToId.get(deptName);
        if (deptId == null) throw new IllegalArgumentException("Invalid department selected.");

        Position position = positionLabelToObj.get(positionName);
        if (position == null) throw new IllegalArgumentException("Invalid position selected.");

        Integer supervisorId = null;
        String supVal = supervisorCombo.getValue();
        if (supVal != null && !supVal.equals("-- None --")) {
            supervisorId = supervisorLabelToId.get(supVal);
        }

        return new Employee(
                null,
                firstName,
                lastName,
                birthDatePicker.getValue(),
                Gender.fromValue(genderStr),
                addressField.getText() != null ? addressField.getText().trim() : null,
                civilStatusCombo.getValue(),
                phoneField.getText() != null ? phoneField.getText().trim() : null,
                emailField.getText() != null ? emailField.getText().trim() : null,
                EmploymentStatus.fromValue(statusStr),
                position.getPositionId(),
                deptId,
                hireDate,
                supervisorId,
                true,
                null
        );
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        genderCombo.setValue(null);
        civilStatusCombo.setValue(null);
        birthDatePicker.setValue(null);
        phoneField.clear();
        emailField.clear();
        hireDatePicker.setValue(null);
        employmentStatusCombo.setValue(null);
        departmentCombo.setValue(null);
        positionCombo.getItems().clear();
        positionCombo.setValue(null);
        positionLabelToObj.clear();
        supervisorCombo.setValue(null);
        addressField.clear();
        UiMessageUtil.clearMessage(formErrorLabel);
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, old, text) -> applyEmployeeFilter());
    }

    private void setupEmployeeTable() {
        employeeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        employeeIdColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getEmployeeId()));
        employeeNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFullName()));
        employeeStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isActive() ? "ACTIVE" : "INACTIVE"));
        employeeStatusColumn.setCellFactory(column -> statusCell());
        employeeTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEmploymentStatus() != null
                        ? data.getValue().getEmploymentStatus().name()
                        : dash(null)));
        employeeDepartmentColumn.setCellValueFactory(data -> new SimpleStringProperty(
                departmentNames.getOrDefault(
                        data.getValue().getDepartmentId(),
                        "#" + data.getValue().getDepartmentId())));

        employeeActionsColumn.setSortable(false);
        employeeActionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");
            private final HBox actions = new HBox(6, viewButton, editButton);

            {
                viewButton.getStyleClass().add("table-action-button");
                editButton.getStyleClass().add("table-action-button");
                actions.setAlignment(Pos.CENTER_LEFT);

                viewButton.setOnAction(event -> {
                    EmployeeSummaryDTO dto = getTableRow().getItem();
                    if (dto != null) {
                        selectEmployee(dto);
                    }
                });
                editButton.setOnAction(event -> {
                    EmployeeSummaryDTO dto = getTableRow().getItem();
                    if (dto != null) {
                        openEditFormForEmployee(dto);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(empty ? null : actions);
            }
        });

        employeeTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(EmployeeSummaryDTO dto, boolean empty) {
                super.updateItem(dto, empty);
                boolean selected = !empty
                        && dto != null
                        && selectedProfile != null
                        && Objects.equals(dto.getEmployeeId(), selectedProfile.getEmployeeId());
                setStyle(selected ? "-fx-background-color: rgba(59,130,246,0.10);" : "");
            }
        });

        filteredEmployees = new FilteredList<>(employeeItems, employee -> true);
        SortedList<EmployeeSummaryDTO> sortedEmployees = new SortedList<>(filteredEmployees);
        sortedEmployees.comparatorProperty().bind(employeeTable.comparatorProperty());
        employeeTable.setItems(sortedEmployees);
    }

    private TableCell<EmployeeSummaryDTO, String> statusCell() {
        return new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    return;
                }

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

        String query = searchField.getText();
        String lower = query == null ? "" : query.trim().toLowerCase();
        String status = statusFilterCombo.getValue();
        String type = typeFilterCombo.getValue();
        String department = departmentFilterCombo.getValue();

        filteredEmployees.setPredicate(dto -> {
            boolean searchMatches = lower.isBlank()
                    || dto.getFullName().toLowerCase().contains(lower)
                    || String.valueOf(dto.getEmployeeId()).contains(lower);
            boolean statusMatches = status == null
                    || "All Statuses".equals(status)
                    || ("ACTIVE".equals(status) && dto.isActive())
                    || ("INACTIVE".equals(status) && !dto.isActive());
            boolean typeMatches = type == null
                    || "All Types".equals(type)
                    || (dto.getEmploymentStatus() != null && dto.getEmploymentStatus().name().equals(type));
            String departmentName = departmentNames.getOrDefault(
                    dto.getDepartmentId(),
                    "#" + dto.getDepartmentId());
            boolean departmentMatches = department == null
                    || "All Departments".equals(department)
                    || departmentName.equals(department);

            return searchMatches && statusMatches && typeMatches && departmentMatches;
        });
    }

    private void selectEmployee(EmployeeSummaryDTO dto) {
        try {
            selectedProfile = employeeService.getEmployeeProfileAdmin(dto.getEmployeeId());
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
            return;
        }

        employeeTable.refresh();
        loadDetailPanel(selectedProfile);
        showDetailPanel();
    }

    private void loadDetailPanel(EmployeeProfileDTO profile) {
        detailNameLabel.setText(profile.getFullName());

        detailActiveBadge.setText(profile.isActive() ? "ACTIVE" : "INACTIVE");
        detailActiveBadge.getStyleClass().removeAll("status-badge-inactive");
        if (!profile.isActive()) detailActiveBadge.getStyleClass().add("status-badge-inactive");

        detailEmploymentBadge.setText(
                profile.getEmploymentStatus() != null ? profile.getEmploymentStatus().name() : dash(null));
        detailIdLabel.setText("#" + profile.getEmployeeId());
        detailDeptLabel.setText(dash(profile.getDepartmentName()));
        detailPositionLabel.setText(dash(profile.getPositionName()));
        detailHireDateLabel.setText(
                profile.getHireDate() != null ? DATE_FMT.format(profile.getHireDate()) : dash(null));
        detailSupervisorLabel.setText(dash(profile.getSupervisorName()));
        detailGenderLabel.setText(profile.getGender() != null ? profile.getGender().name() : dash(null));
        detailBirthDateLabel.setText(
                profile.getBirthDate() != null ? DATE_FMT.format(profile.getBirthDate()) : dash(null));
        detailCivilStatusLabel.setText(dash(profile.getCivilStatus()));
        detailPhoneLabel.setText(dash(profile.getPhoneNumber()));
        detailEmailLabel.setText(dash(profile.getPersonalEmail()));
        detailAddressLabel.setText(dash(profile.getAddress()));

        deactivateButton.setDisable(!profile.isActive());
        deactivateButton.setText(profile.isActive() ? "Deactivate Employee" : "Already Deactivated");
    }

    private void showDetailPanel() {
        setVisible(detailPanel, true);
    }

    private void closeDetail() {
        selectedProfile = null;
        if (employeeTable != null) {
            employeeTable.refresh();
        }
        setVisible(detailPanel, false);
    }

    private void showFormPanel() {
        applyFormVisuals(true);
        setVisible(formBackdrop, true);
        setVisible(formPanel, true);
    }

    private void hideFormPanel() {
        setVisible(formPanel, false);
        setVisible(formBackdrop, false);
        applyFormVisuals(false);
    }

    private void applyFormVisuals(boolean visible) {
        if (formPanel != null) {
            formPanel.getStyleClass().remove(FORM_PANEL_CLASS);
            if (visible) {
                formPanel.getStyleClass().add(FORM_PANEL_CLASS);
            }
        }

        if (formBackdrop != null) {
            formBackdrop.setStyle(visible ? FORM_BACKDROP_STYLE : "");
        }
    }

    private void setVisible(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private String dash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
