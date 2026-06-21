package motorph.ui.admin;

import motorph.application.dto.employee.EmployeeSummaryDTO;
import motorph.application.dto.salary.SalaryHistoryDTO;
import motorph.application.service.EmployeeService;
import motorph.application.service.SalaryBenefitService;
import motorph.domain.model.Department;
import motorph.domain.model.EmployeeBenefit;
import motorph.domain.model.SalaryHistory;
import motorph.domain.value.SalaryType;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.util.UiFormatter;

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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles the Admin Salary and Benefits screen.
 * Includes salary history, employee benefits, and edit forms.
 */
public class AdminSalaryController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final String DEFAULT_FORM_BACKDROP_STYLE = "-fx-background-color: rgba(0,0,0,0.72);";
    private static final String[] BENEFIT_NAMES = {"-", "Rice Subsidy", "Phone Allowance", "Clothing Allowance"};

    private EmployeeService employeeService;
    private SalaryBenefitService salaryBenefitService;

    private List<EmployeeSummaryDTO> allSummaries = List.of();
    private final Map<Integer, String> departmentNames = new HashMap<>();
    private EmployeeSummaryDTO selectedEmployee;
    private SalaryHistory selectedSalary;
    private EmployeeBenefit selectedBenefit;
    private boolean editSalaryMode;

    private final ObservableList<EmployeeSummaryDTO> employeeItems = FXCollections.observableArrayList();
    private final ObservableList<SalaryHistory> salaryItems = FXCollections.observableArrayList();
    private final ObservableList<EmployeeBenefit> benefitItems = FXCollections.observableArrayList();
    private FilteredList<EmployeeSummaryDTO> filteredEmployees;

    @FXML private Button backButton;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> departmentFilterCombo;
    @FXML private Label moduleMessageLabel;
    @FXML private Button editSalaryButton;
    @FXML private Button endBenefitButton;

    @FXML private VBox employeeListPanel;
    @FXML private TableView<EmployeeSummaryDTO> employeeTable;
    @FXML private TableColumn<EmployeeSummaryDTO, Number> employeeIdColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, String> employeeNameColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, String> employeeStatusColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, String> employeeDepartmentColumn;
    @FXML private TableColumn<EmployeeSummaryDTO, Void> employeeActionColumn;
    @FXML private Region formBackdrop;

    @FXML private VBox employeeDetailPanel;
    @FXML private TableView<SalaryHistory> salaryTable;
    @FXML private TableColumn<SalaryHistory, Number> salaryIdColumn;
    @FXML private TableColumn<SalaryHistory, String> salaryTypeColumn;
    @FXML private TableColumn<SalaryHistory, Number> salaryBasicColumn;
    @FXML private TableColumn<SalaryHistory, Number> salaryHourlyColumn;
    @FXML private TableColumn<SalaryHistory, LocalDate> salaryStartColumn;
    @FXML private TableColumn<SalaryHistory, LocalDate> salaryEndColumn;
    @FXML private TableColumn<SalaryHistory, String> salaryStatusColumn;

    @FXML private TableView<EmployeeBenefit> benefitTable;
    @FXML private TableColumn<EmployeeBenefit, Number> benefitIdColumn;
    @FXML private TableColumn<EmployeeBenefit, String> benefitTypeColumn;
    @FXML private TableColumn<EmployeeBenefit, Number> benefitAmountColumn;
    @FXML private TableColumn<EmployeeBenefit, LocalDate> benefitStartColumn;
    @FXML private TableColumn<EmployeeBenefit, LocalDate> benefitEndColumn;
    @FXML private TableColumn<EmployeeBenefit, String> benefitStatusColumn;

    @FXML private VBox salaryFormPanel;
    @FXML private Label salaryFormTitle;
    @FXML private ComboBox<SalaryType> salaryTypeCombo;
    @FXML private TextField basicSalaryField;
    @FXML private TextField hourlyRateField;
    @FXML private DatePicker salaryStartDatePicker;
    @FXML private DatePicker salaryEndDatePicker;
    @FXML private Label salaryFormErrorLabel;

    @FXML
    private void initialize() {
        employeeService = EmployeeUIServiceFactory.getInstance().getEmployeeService();
        salaryBenefitService = EmployeeUIServiceFactory.getInstance().getSalaryBenefitService();

        salaryTypeCombo.getItems().setAll(SalaryType.values());
        salaryTypeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(SalaryType type) {
                return type == null ? "" : type.name();
            }

            @Override public SalaryType fromString(String value) {
                return null;
            }
        });
        searchField.textProperty().addListener((obs, old, val) -> applyEmployeeFilter());

        loadDepartmentNames();
        setupEmployeeFilters();
        setupEmployeeTable();
        setupSalaryTable();
        setupBenefitTable();
    }

    public void refresh() {
        showEmployeeList();
    }

    @FXML
    private void handleBack() {
        if (salaryFormPanel.isVisible()) {
            if (selectedEmployee != null) {
                showEmployeeDetail(selectedEmployee);
            } else {
                showEmployeeList();
            }
        } else {
            showEmployeeList();
        }
    }

    @FXML
    private void handleEditSalary() {
        if (selectedSalary == null) return;

        clearSalaryForm();
        salaryFormTitle.setText("Edit Salary Dates");
        editSalaryMode = true;
        salaryTypeCombo.setValue(selectedSalary.getSalaryType());
        salaryTypeCombo.setDisable(true);
        if (selectedSalary.getBasicSalary() != null) {
            basicSalaryField.setText(String.valueOf(selectedSalary.getBasicSalary()));
        }
        basicSalaryField.setDisable(true);
        if (selectedSalary.getHourlyRate() != null) {
            hourlyRateField.setText(String.valueOf(selectedSalary.getHourlyRate()));
        }
        hourlyRateField.setDisable(true);
        salaryStartDatePicker.setValue(selectedSalary.getStartDate());
        salaryEndDatePicker.setValue(selectedSalary.getEndDate());
        showPanel(Panel.SALARY_FORM);
    }

    @FXML
    private void handleEndBenefit() {
        if (selectedBenefit == null) return;

        String name = benefitName(selectedBenefit);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "End \"" + name + "\" effective today (" + LocalDate.now().format(DATE_FMT) + ")?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("End Benefit");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try {
                salaryBenefitService.endBenefit(selectedBenefit.getEmployeeBenefitId(), LocalDate.now());
                showEmployeeDetail(selectedEmployee);
                setMessage("Benefit ended.");
            } catch (Exception e) {
                setMessage("Error: " + e.getMessage());
            }
        }
    }

    @FXML
    private void saveSalaryRecord() {
        salaryFormErrorLabel.setText("");
        if (salaryTypeCombo.getValue() == null) {
            salaryFormErrorLabel.setText("Salary type is required.");
            return;
        }
        if (salaryStartDatePicker.getValue() == null) {
            salaryFormErrorLabel.setText("Start date is required.");
            return;
        }

        if (editSalaryMode) {
            try {
                salaryBenefitService.updateSalaryDates(
                        selectedSalary.getSalaryHistoryId(),
                        salaryStartDatePicker.getValue(),
                        salaryEndDatePicker.getValue());
                showEmployeeDetail(selectedEmployee);
                setMessage("Salary dates updated.");
            } catch (Exception e) {
                salaryFormErrorLabel.setText(e.getMessage());
            }
        } else {
            SalaryHistoryDTO dto = new SalaryHistoryDTO(
                    selectedEmployee.getEmployeeId(),
                    salaryTypeCombo.getValue().name(),
                    parseDouble(basicSalaryField.getText()),
                    parseDouble(hourlyRateField.getText()),
                    salaryStartDatePicker.getValue(),
                    salaryEndDatePicker.getValue());
            try {
                salaryBenefitService.addSalaryRecord(dto);
                showEmployeeDetail(selectedEmployee);
                setMessage("Salary record added.");
            } catch (Exception e) {
                salaryFormErrorLabel.setText(e.getMessage());
            }
        }
    }

    private enum Panel { EMPLOYEE_LIST, EMPLOYEE_DETAIL, SALARY_FORM }

    private void showPanel(Panel active) {
        boolean formOpen = active == Panel.SALARY_FORM;

        setPane(employeeListPanel, active == Panel.EMPLOYEE_LIST);
        setPane(employeeDetailPanel, active == Panel.EMPLOYEE_DETAIL || formOpen);
        setPane(formBackdrop, formOpen);
        setPane(salaryFormPanel, active == Panel.SALARY_FORM);
        if (formBackdrop != null) {
            formBackdrop.setStyle(DEFAULT_FORM_BACKDROP_STYLE);
        }

        setVis(backButton, active != Panel.EMPLOYEE_LIST);
        setVis(searchField, active == Panel.EMPLOYEE_LIST);
        setVis(statusFilterCombo, active == Panel.EMPLOYEE_LIST);
        setVis(departmentFilterCombo, active == Panel.EMPLOYEE_LIST);
        setVis(editSalaryButton, active == Panel.EMPLOYEE_DETAIL && selectedSalary != null);
        setVis(endBenefitButton, active == Panel.EMPLOYEE_DETAIL && selectedBenefit != null && isActive(selectedBenefit));
    }

    private void setPane(javafx.scene.Node node, boolean show) {
        if (node != null) {
            node.setVisible(show);
            node.setManaged(show);
        }
    }

    private void setVis(javafx.scene.Node node, boolean show) {
        if (node != null) {
            node.setVisible(show);
            node.setManaged(show);
        }
    }

    private void loadDepartmentNames() {
        departmentNames.clear();
        try {
            for (Department department : employeeService.getAllDepartments()) {
                departmentNames.put(department.getDepartmentId(), department.getDepartmentName());
            }
        } catch (Exception e) {
            setMessage("Failed to load departments: " + e.getMessage());
        }
    }

    private void setupEmployeeFilters() {
        statusFilterCombo.getItems().setAll("All Statuses", "ACTIVE", "INACTIVE");
        statusFilterCombo.setValue("All Statuses");
        statusFilterCombo.setOnAction(event -> applyEmployeeFilter());

        departmentFilterCombo.getItems().setAll("All Departments");
        departmentNames.values().stream()
                .sorted()
                .forEach(departmentFilterCombo.getItems()::add);
        departmentFilterCombo.setValue("All Departments");
        departmentFilterCombo.setOnAction(event -> applyEmployeeFilter());
    }

    private void showEmployeeList() {
        selectedEmployee = null;
        selectedSalary = null;
        selectedBenefit = null;
        salaryItems.clear();
        benefitItems.clear();
        try {
            allSummaries = employeeService.getAllEmployeeSummaries();
        } catch (Exception e) {
            allSummaries = List.of();
            setMessage("Failed to load employees: " + e.getMessage());
        }
        employeeItems.setAll(allSummaries);
        applyEmployeeFilter();
        showPanel(Panel.EMPLOYEE_LIST);
    }

    private void applyEmployeeFilter() {
        if (filteredEmployees == null) return;

        String query = searchField.getText();
        String q = query == null ? "" : query.trim().toLowerCase();
        String status = statusFilterCombo.getValue();
        String department = departmentFilterCombo.getValue();
        filteredEmployees.setPredicate(employee -> {
            boolean matchesSearch = q.isBlank()
                    || employee.getFullName().toLowerCase().contains(q)
                    || String.valueOf(employee.getEmployeeId()).contains(q);
            boolean matchesStatus = status == null
                    || "All Statuses".equals(status)
                    || ("ACTIVE".equals(status) && employee.isActive())
                    || ("INACTIVE".equals(status) && !employee.isActive());
            String departmentName = departmentNames.getOrDefault(
                    employee.getDepartmentId(),
                    "#" + employee.getDepartmentId());
            boolean matchesDepartment = department == null
                    || "All Departments".equals(department)
                    || departmentName.equals(department);

            return matchesSearch && matchesStatus && matchesDepartment;
        });
    }

    private void showEmployeeDetail(EmployeeSummaryDTO employee) {
        selectedEmployee = employee;
        selectedSalary = null;
        selectedBenefit = null;
        try {
            salaryItems.setAll(salaryBenefitService.getSalaryHistory(employee.getEmployeeId()));
            benefitItems.setAll(salaryBenefitService.getAllBenefits(employee.getEmployeeId()));
        } catch (Exception e) {
            salaryItems.clear();
            benefitItems.clear();
            setMessage("Failed to load data: " + e.getMessage());
        }

        showPanel(Panel.EMPLOYEE_DETAIL);
        setMessage("Viewing: " + employee.getFullName());
    }

    private void setupEmployeeTable() {
        employeeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        employeeIdColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getEmployeeId()));
        employeeNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFullName()));
        employeeStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getEmploymentStatus() != null
                        ? data.getValue().getEmploymentStatus().name()
                        : "-"));
        employeeStatusColumn.setCellFactory(column -> statusCell());
        employeeDepartmentColumn.setCellValueFactory(data -> new SimpleStringProperty(
                departmentNames.getOrDefault(
                        data.getValue().getDepartmentId(),
                        "#" + data.getValue().getDepartmentId())));
        employeeActionColumn.setSortable(false);
        employeeActionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button viewButton = new Button("View");

            {
                viewButton.getStyleClass().add("table-action-button");
                viewButton.setOnAction(event -> {
                    EmployeeSummaryDTO employee = getTableRow().getItem();
                    if (employee != null) {
                        showEmployeeDetail(employee);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(empty ? null : viewButton);
            }
        });

        filteredEmployees = new FilteredList<>(employeeItems, employee -> true);
        SortedList<EmployeeSummaryDTO> sortedEmployees = new SortedList<>(filteredEmployees);
        sortedEmployees.comparatorProperty().bind(employeeTable.comparatorProperty());
        employeeTable.setItems(sortedEmployees);
    }

    private void setupSalaryTable() {
        salaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        salaryIdColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getSalaryHistoryId()));
        salaryTypeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSalaryType().name()));
        salaryBasicColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getBasicSalary()));
        salaryBasicColumn.setCellFactory(column -> pesoCell());
        salaryHourlyColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getHourlyRate()));
        salaryHourlyColumn.setCellFactory(column -> pesoCell());
        salaryStartColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getStartDate()));
        salaryStartColumn.setCellFactory(column -> dateCell());
        salaryEndColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getEndDate()));
        salaryEndColumn.setCellFactory(column -> dateCell("Present"));
        salaryStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(isActive(data.getValue()) ? "ACTIVE" : "CLOSED"));
        salaryStatusColumn.setCellFactory(column -> statusCell());

        salaryTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(SalaryHistory salary, boolean empty) {
                super.updateItem(salary, empty);
                boolean selected = !empty && salary != null && salary == selectedSalary;
                setStyle(selected ? "-fx-background-color: rgba(59,130,246,0.10);" : "");
            }
        });
        salaryTable.setOnMouseClicked(event -> {
            SalaryHistory salary = salaryTable.getSelectionModel().getSelectedItem();
            if (salary != null) {
                selectedSalary = salary;
                selectedBenefit = null;
                salaryTable.refresh();
                benefitTable.refresh();
                setVis(editSalaryButton, true);
                setVis(endBenefitButton, false);
            }
        });
        salaryTable.setItems(salaryItems);
    }

    private void setupBenefitTable() {
        benefitTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        benefitIdColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getEmployeeBenefitId()));
        benefitTypeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(benefitName(data.getValue())));
        benefitAmountColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getAmount()));
        benefitAmountColumn.setCellFactory(column -> pesoCell());
        benefitStartColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getEffectiveStartDate()));
        benefitStartColumn.setCellFactory(column -> dateCell());
        benefitEndColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getEffectiveEndDate()));
        benefitEndColumn.setCellFactory(column -> dateCell("Present"));
        benefitStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(isActive(data.getValue()) ? "ACTIVE" : "ENDED"));
        benefitStatusColumn.setCellFactory(column -> statusCell());

        benefitTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(EmployeeBenefit benefit, boolean empty) {
                super.updateItem(benefit, empty);
                boolean selected = !empty && benefit != null && benefit == selectedBenefit;
                setStyle(selected ? "-fx-background-color: rgba(59,130,246,0.10);" : "");
            }
        });
        benefitTable.setOnMouseClicked(event -> {
            EmployeeBenefit benefit = benefitTable.getSelectionModel().getSelectedItem();
            if (benefit != null) {
                selectedBenefit = benefit;
                selectedSalary = null;
                salaryTable.refresh();
                benefitTable.refresh();
                setVis(endBenefitButton, isActive(benefit));
                setVis(editSalaryButton, false);
            }
        });
        benefitTable.setItems(benefitItems);
    }

    private <T> TableCell<T, String> statusCell() {
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
                badge.getStyleClass().add(("ACTIVE".equals(value) || !"INACTIVE".equals(value) && !"ENDED".equals(value) && !"CLOSED".equals(value))
                        ? "status-badge"
                        : "status-badge-inactive");
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(badge);
            }
        };
    }

    private <T> TableCell<T, Number> pesoCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setText(empty || value == null ? null : UiFormatter.formatPeso(value.doubleValue()));
            }
        };
    }

    private <T> TableCell<T, LocalDate> dateCell() {
        return dateCell(null);
    }

    private <T> TableCell<T, LocalDate> dateCell(String nullText) {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value == null ? nullText : value.format(DATE_FMT));
            }
        };
    }

    private void clearSalaryForm() {
        salaryTypeCombo.setValue(SalaryType.MONTHLY);
        salaryTypeCombo.setDisable(false);
        basicSalaryField.clear();
        basicSalaryField.setDisable(false);
        hourlyRateField.clear();
        hourlyRateField.setDisable(false);
        salaryStartDatePicker.setValue(LocalDate.now());
        salaryEndDatePicker.setValue(null);
        salaryFormErrorLabel.setText("");
        editSalaryMode = false;
    }

    private boolean isActive(EmployeeBenefit benefit) {
        return benefit.getEffectiveEndDate() == null || !benefit.getEffectiveEndDate().isBefore(LocalDate.now());
    }

    private boolean isActive(SalaryHistory salary) {
        return salary.getEndDate() == null || !salary.getEndDate().isBefore(LocalDate.now());
    }

    private String benefitName(EmployeeBenefit benefit) {
        int typeId = benefit.getBenefitTypeId();
        return typeId >= 1 && typeId <= 3 ? BENEFIT_NAMES[typeId] : "Other";
    }

    private Double parseDouble(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setMessage(String msg) {
        if (moduleMessageLabel != null) moduleMessageLabel.setText(msg);
    }
}
