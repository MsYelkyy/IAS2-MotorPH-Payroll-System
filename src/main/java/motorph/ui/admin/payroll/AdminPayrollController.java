package motorph.ui.admin.payroll;

import motorph.application.dto.employee.EmployeeSummaryDTO;
import motorph.application.dto.payroll.PayrollSummaryDTO;
import motorph.application.dto.payroll.PayrunCreateDTO;
import motorph.application.dto.payroll.PayslipDTO;
import motorph.application.service.EmployeeService;
import motorph.application.service.PayrollService;
import motorph.domain.model.Payrun;
import motorph.domain.value.PayrollType;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Manages the admin payroll module: payruns, payroll generation, and payslip display. */
public class AdminPayrollController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final String FORM_PANEL_CLASS = "admin-add-form-panel";
    private static final String FORM_BACKDROP_STYLE = "-fx-background-color: rgba(0,0,0,0.46);";

    private PayrollService payrollService;
    private EmployeeService employeeService;

    private Payrun selectedPayrun;
    private List<PayrollSummaryDTO> selectedPayrunSummaries;
    private PayslipDTO currentPayslip;

    private final ObservableList<Payrun> payrunItems = FXCollections.observableArrayList();
    private final ObservableList<PayrollSummaryDTO> resultItems = FXCollections.observableArrayList();
    private FilteredList<Payrun> filteredPayruns;
    private FilteredList<PayrollSummaryDTO> filteredResults;

    @FXML private Button backButton;
    @FXML private Label moduleMessageLabel;
    @FXML private Button generateOneButton;
    @FXML private Button generateAllButton;
    @FXML private Button printButton;
    @FXML private Button newPayrunButton;
    @FXML private Button clearPayrunFiltersButton;
    @FXML private Button clearResultFiltersButton;
    @FXML private Region payrunFormBackdrop;
    @FXML private VBox payrunFormPanel;
    @FXML private DatePicker payrunStartDatePicker;
    @FXML private DatePicker payrunEndDatePicker;
    @FXML private DatePicker payrunPayDatePicker;
    @FXML private ComboBox<PayrollType> payrunTypeCombo;
    @FXML private Label payrunFormErrorLabel;

    @FXML private VBox payrunListPanel;
    @FXML private ComboBox<String> periodFilterCombo;
    @FXML private DatePicker payDateFilterPicker;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private TableView<Payrun> payrunTable;
    @FXML private TableColumn<Payrun, Number> payrunIdColumn;
    @FXML private TableColumn<Payrun, LocalDate> payrunPeriodColumn;
    @FXML private TableColumn<Payrun, LocalDate> payrunPayDateColumn;
    @FXML private TableColumn<Payrun, String> payrunTypeColumn;
    @FXML private TableColumn<Payrun, Void> payrunActionColumn;

    @FXML private VBox payrunDetailPanel;
    @FXML private Label payrunInfoLabel;
    @FXML private TextField employeeIdFilterField;
    @FXML private TextField employeeFilterField;
    @FXML private TableView<PayrollSummaryDTO> resultTable;
    @FXML private TableColumn<PayrollSummaryDTO, Number> resultEmployeeIdColumn;
    @FXML private TableColumn<PayrollSummaryDTO, String> resultEmployeeColumn;
    @FXML private TableColumn<PayrollSummaryDTO, Number> resultGrossPayColumn;
    @FXML private TableColumn<PayrollSummaryDTO, Number> resultDeductionsColumn;
    @FXML private TableColumn<PayrollSummaryDTO, Number> resultNetPayColumn;
    @FXML private TableColumn<PayrollSummaryDTO, Void> resultActionColumn;

    @FXML private VBox payslipDetailPanel;
    @FXML private Label slipNameLabel;
    @FXML private Label slipPositionLabel;
    @FXML private Label slipSuperLabel;
    @FXML private Label slipPeriodLabel;
    @FXML private Label slipPayDateLabel;
    @FXML private Label slipSssLabel;
    @FXML private Label slipPhilhLabel;
    @FXML private Label slipPagibigLabel;
    @FXML private Label slipTinLabel;
    @FXML private Label slipBasicPayLabel;
    @FXML private Label slipRiceLabel;
    @FXML private Label slipPhoneLabel;
    @FXML private Label slipClothingLabel;
    @FXML private Label slipTotalCompLabel;
    @FXML private Label slipSssDedLabel;
    @FXML private Label slipPhDedLabel;
    @FXML private Label slipPagibigDedLabel;
    @FXML private Label slipTaxDedLabel;
    @FXML private Label slipLateDedLabel;
    @FXML private Label slipUndertimeDedLabel;
    @FXML private Label slipTotalDedLabel;
    @FXML private Label slipNetPayLabel;

    @FXML
    private void initialize() {
        payrollService = EmployeeUIServiceFactory.getInstance().getPayrollService();
        employeeService = EmployeeUIServiceFactory.getInstance().getEmployeeService();

        payrunTypeCombo.getItems().setAll(PayrollType.values());
        payrunTypeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(PayrollType type) {
                return formatPayrollType(type);
            }

            @Override public PayrollType fromString(String value) {
                return null;
            }
        });

        setupPayrunTable();
        setupResultTable();
        setupFilters();
        showPayrunList();
    }

    /** Refreshes the payrun list when the nav button is clicked. */
    public void refresh() {
        showPayrunList();
    }

    @FXML
    private void handleBack() {
        if (payrunFormPanel.isVisible()) {
            hideNewPayrunForm();
            return;
        }
        if (payslipDetailPanel.isVisible()) {
            showPayrunDetail(selectedPayrun);
        } else if (payrunDetailPanel.isVisible()) {
            showPayrunList();
        } else {
            showPayrunList();
        }
    }

    @FXML
    private void handleNewPayrun() {
        showNewPayrunForm();
    }

    @FXML
    private void handleGenerateAll() {
        if (selectedPayrun == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Generate payroll for all active employees in this payrun?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Generate All");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.YES) {
            try {
                payrollService.runPayrollForPayrun(selectedPayrun.getPayrunId());
                showPayrunDetail(selectedPayrun);
                setMessage("Payroll generated for all employees.");
            } catch (Exception e) {
                setMessage("Error: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleGenerateForEmployee() {
        if (selectedPayrun == null) return;

        List<EmployeeSummaryDTO> employees = employeeService.getAllEmployeeSummaries();
        if (employees.isEmpty()) {
            setMessage("No employees found.");
            return;
        }

        Dialog<EmployeeSummaryDTO> dialog = new Dialog<>();
        dialog.setTitle("Generate for Employee");
        dialog.setHeaderText("Select an employee:");

        ComboBox<EmployeeSummaryDTO> combo = new ComboBox<>();
        combo.getItems().setAll(employees);
        combo.setConverter(new StringConverter<>() {
            @Override public String toString(EmployeeSummaryDTO employee) {
                return employee == null ? "" : employee.getFullName() + " (#" + employee.getEmployeeId() + ")";
            }

            @Override public EmployeeSummaryDTO fromString(String value) {
                return null;
            }
        });
        combo.setPrefWidth(320);
        combo.setValue(employees.get(0));

        dialog.getDialogPane().setContent(combo);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? combo.getValue() : null);

        dialog.showAndWait().ifPresent(employee -> {
            try {
                payrollService.runPayrollForEmployee(employee.getEmployeeId(), selectedPayrun.getPayrunId());
                showPayrunDetail(selectedPayrun);
                setMessage("Payroll generated for " + employee.getFullName() + ".");
            } catch (Exception e) {
                setMessage("Error: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handlePrint() {
        if (currentPayslip == null) return;

        javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(buildPayslipText(currentPayslip));
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefRowCount(28);
        area.setPrefColumnCount(60);
        area.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12;");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payslip - " + currentPayslip.getFullName());
        alert.setHeaderText(null);
        alert.getDialogPane().setContent(area);
        alert.getDialogPane().setPrefWidth(640);
        alert.showAndWait();
    }

    private void showNewPayrunForm() {
        payrunStartDatePicker.setValue(null);
        payrunEndDatePicker.setValue(null);
        payrunPayDatePicker.setValue(null);
        payrunTypeCombo.setValue(PayrollType.SEMI_MONTHLY);
        payrunFormErrorLabel.setText("");
        applyPayrunFormVisuals(true);
        setPane(payrunFormBackdrop, true);
        setPane(payrunFormPanel, true);
    }

    @FXML
    private void hideNewPayrunForm() {
        setPane(payrunFormPanel, false);
        setPane(payrunFormBackdrop, false);
        applyPayrunFormVisuals(false);
        payrunFormErrorLabel.setText("");
    }

    @FXML
    private void saveNewPayrun() {
        payrunFormErrorLabel.setText("");
        if (payrunStartDatePicker.getValue() == null
                || payrunEndDatePicker.getValue() == null
                || payrunPayDatePicker.getValue() == null) {
            payrunFormErrorLabel.setText("All date fields are required.");
            return;
        }
        if (payrunEndDatePicker.getValue().isBefore(payrunStartDatePicker.getValue())) {
            payrunFormErrorLabel.setText("End date cannot be before start date.");
            return;
        }

        try {
            PayrunCreateDTO dto = new PayrunCreateDTO(
                    payrunStartDatePicker.getValue(),
                    payrunEndDatePicker.getValue(),
                    payrunPayDatePicker.getValue(),
                    payrunTypeCombo.getValue());
            Payrun created = payrollService.createPayrun(dto);
            hideNewPayrunForm();
            showPayrunDetail(created);
            setMessage("Payrun created.");
        } catch (Exception e) {
            payrunFormErrorLabel.setText(e.getMessage());
        }
    }

    private enum Panel { PAYRUN_LIST, PAYRUN_DETAIL, PAYSLIP_DETAIL }

    private void showPanel(Panel active) {
        setPane(payrunListPanel, active == Panel.PAYRUN_LIST);
        setPane(payrunDetailPanel, active == Panel.PAYRUN_DETAIL);
        setPane(payslipDetailPanel, active == Panel.PAYSLIP_DETAIL);
        setPane(payrunFormBackdrop, false);
        setPane(payrunFormPanel, false);
        applyPayrunFormVisuals(false);

        setVis(backButton, active != Panel.PAYRUN_LIST);
        setVis(newPayrunButton, active == Panel.PAYRUN_LIST);
        setVis(generateAllButton, active == Panel.PAYRUN_DETAIL);
        setVis(generateOneButton, active == Panel.PAYRUN_DETAIL);
        setVis(printButton, active == Panel.PAYSLIP_DETAIL);
        setPayrunFilterVisible(active == Panel.PAYRUN_LIST);
        setResultFilterVisible(active == Panel.PAYRUN_DETAIL);
    }

    private void setPane(javafx.scene.Node node, boolean show) {
        if (node != null) {
            node.setVisible(show);
            node.setManaged(show);
        }
    }

    private void applyPayrunFormVisuals(boolean visible) {
        if (payrunFormPanel != null) {
            payrunFormPanel.getStyleClass().remove(FORM_PANEL_CLASS);
            if (visible) {
                payrunFormPanel.getStyleClass().add(FORM_PANEL_CLASS);
            }
        }

        if (payrunFormBackdrop != null) {
            payrunFormBackdrop.setStyle(visible ? FORM_BACKDROP_STYLE : "");
        }
    }

    private void setVis(javafx.scene.Node node, boolean show) {
        if (node != null) {
            node.setVisible(show);
            node.setManaged(show);
        }
    }

    private void setPayrunFilterVisible(boolean show) {
        setVis(periodFilterCombo, show);
        setVis(payDateFilterPicker, show);
        setVis(typeFilterCombo, show);
        setVis(clearPayrunFiltersButton, show);
    }

    private void setResultFilterVisible(boolean show) {
        setVis(employeeIdFilterField, show);
        setVis(employeeFilterField, show);
        setVis(clearResultFiltersButton, show);
    }

    private void showPayrunList() {
        try {
            List<Payrun> payruns = payrollService.getPayrunsWithPayrollData();
            payrunItems.setAll(payruns);
            populatePayrunFilters(payruns);
            applyPayrunFilters();
        } catch (Exception e) {
            setMessage("Failed to load payruns: " + e.getMessage());
        }
        showPanel(Panel.PAYRUN_LIST);
    }

    private void showPayrunDetail(Payrun payrun) {
        if (payrun == null) return;

        boolean changedPayrun = selectedPayrun == null
                || !selectedPayrun.getPayrunId().equals(payrun.getPayrunId());
        selectedPayrun = payrun;
        if (changedPayrun) {
            clearResultFilters();
        }

        try {
            selectedPayrunSummaries = payrollService.getPayrollSummaries(payrun.getPayrunId());
            payrunInfoLabel.setText(
                    "Period: " + payrun.getStartDate().format(DATE_FMT)
                    + " - " + payrun.getEndDate().format(DATE_FMT)
                    + "   Pay Date: " + payrun.getPayDate().format(DATE_FMT)
                    + "   Type: " + formatPayrollType(payrun.getPayrollType()));

            resultTable.setPlaceholder(emptyLabel(selectedPayrunSummaries.isEmpty()
                    ? "No results yet. Click 'Generate All' or 'Generate for Employee'."
                    : "No payroll results match the current filters."));
            resultItems.setAll(selectedPayrunSummaries);
            applyResultFilters();
        } catch (Exception e) {
            setMessage("Failed to load payrun detail: " + e.getMessage());
        }
        showPanel(Panel.PAYRUN_DETAIL);
    }

    private void showPayslipDetail(PayrollSummaryDTO summary) {
        try {
            currentPayslip = payrollService.buildPayslip(summary.getEmployeeId(), summary.getPayrunId());
            populatePayslip(currentPayslip);
            showPanel(Panel.PAYSLIP_DETAIL);
        } catch (Exception e) {
            setMessage("Failed to load payslip: " + e.getMessage());
        }
    }

    private void setupPayrunTable() {
        payrunTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        payrunIdColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getPayrunId()));
        payrunPeriodColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getStartDate()));
        payrunPeriodColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate value, boolean empty) {
                super.updateItem(value, empty);
                Payrun payrun = empty ? null : getTableRow().getItem();
                setText(payrun == null ? null : formatPeriod(payrun));
            }
        });
        payrunPayDateColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getPayDate()));
        payrunPayDateColumn.setCellFactory(col -> dateCell());
        payrunTypeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatPayrollType(data.getValue().getPayrollType())));
        payrunActionColumn.setSortable(false);
        payrunActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            {
                viewButton.getStyleClass().add("table-action-button");
                viewButton.setOnAction(event -> {
                    Payrun payrun = getTableRow().getItem();
                    if (payrun != null) {
                        showPayrunDetail(payrun);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setGraphic(empty ? null : viewButton);
            }
        });

        filteredPayruns = new FilteredList<>(payrunItems, payrun -> true);
        SortedList<Payrun> sortedPayruns = new SortedList<>(filteredPayruns);
        sortedPayruns.comparatorProperty().bind(payrunTable.comparatorProperty());
        payrunTable.setItems(sortedPayruns);
    }

    private void setupResultTable() {
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        resultEmployeeIdColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getEmployeeId()));
        resultEmployeeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFullName()));
        resultGrossPayColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getGrossPay()));
        resultGrossPayColumn.setCellFactory(col -> pesoCell());
        resultDeductionsColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getTotalDeductions()));
        resultDeductionsColumn.setCellFactory(col -> pesoCell());
        resultNetPayColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getNetPay()));
        resultNetPayColumn.setCellFactory(col -> pesoCell());
        resultActionColumn.setSortable(false);
        resultActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button payslipButton = new Button("Payslip");
            {
                payslipButton.getStyleClass().add("table-action-button");
                payslipButton.setOnAction(event -> {
                    PayrollSummaryDTO summary = getTableRow().getItem();
                    if (summary != null) {
                        showPayslipDetail(summary);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setGraphic(empty ? null : payslipButton);
            }
        });

        filteredResults = new FilteredList<>(resultItems, result -> true);
        SortedList<PayrollSummaryDTO> sortedResults = new SortedList<>(filteredResults);
        sortedResults.comparatorProperty().bind(resultTable.comparatorProperty());
        resultTable.setItems(sortedResults);
    }

    private TableCell<Payrun, LocalDate> dateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.format(DATE_FMT));
            }
        };
    }

    private TableCell<PayrollSummaryDTO, Number> pesoCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setText(empty || value == null ? null : UiFormatter.formatPeso(value.doubleValue()));
            }
        };
    }

    private void setupFilters() {
        periodFilterCombo.setOnAction(event -> applyPayrunFilters());
        payDateFilterPicker.valueProperty().addListener((obs, old, value) -> applyPayrunFilters());
        typeFilterCombo.setOnAction(event -> applyPayrunFilters());
        employeeIdFilterField.textProperty().addListener((obs, old, value) -> applyResultFilters());
        employeeFilterField.textProperty().addListener((obs, old, value) -> applyResultFilters());
    }

    private void populatePayrunFilters(List<Payrun> payruns) {
        String selectedPeriod = periodFilterCombo.getValue();
        String selectedType = typeFilterCombo.getValue();

        Set<String> periods = new LinkedHashSet<>();
        Set<String> types = new LinkedHashSet<>();
        periods.add("All Periods");
        types.add("All Types");

        for (Payrun payrun : payruns) {
            periods.add(formatPeriod(payrun));
            types.add(formatPayrollType(payrun.getPayrollType()));
        }

        periodFilterCombo.getItems().setAll(periods);
        typeFilterCombo.getItems().setAll(types);
        periodFilterCombo.setValue(periodFilterCombo.getItems().contains(selectedPeriod)
                ? selectedPeriod
                : "All Periods");
        typeFilterCombo.setValue(typeFilterCombo.getItems().contains(selectedType)
                ? selectedType
                : "All Types");
    }

    private void applyPayrunFilters() {
        if (filteredPayruns == null) return;

        String period = periodFilterCombo.getValue();
        LocalDate payDate = payDateFilterPicker.getValue();
        String type = typeFilterCombo.getValue();

        filteredPayruns.setPredicate(payrun -> {
            boolean periodMatches = period == null
                    || "All Periods".equals(period)
                    || formatPeriod(payrun).equals(period);
            boolean payDateMatches = payDate == null || payrun.getPayDate().equals(payDate);
            boolean typeMatches = type == null
                    || "All Types".equals(type)
                    || formatPayrollType(payrun.getPayrollType()).equals(type);

            return periodMatches && payDateMatches && typeMatches;
        });
    }

    @FXML
    private void clearPayrunFilters() {
        periodFilterCombo.setValue("All Periods");
        payDateFilterPicker.setValue(null);
        typeFilterCombo.setValue("All Types");
        applyPayrunFilters();
    }

    private void applyResultFilters() {
        if (filteredResults == null) return;

        String id = normalize(employeeIdFilterField.getText());
        String employee = normalize(employeeFilterField.getText());

        filteredResults.setPredicate(result -> {
            boolean idMatches = id.isBlank()
                    || String.valueOf(result.getEmployeeId()).contains(id);
            boolean employeeMatches = employee.isBlank()
                    || normalize(result.getFullName()).contains(employee);

            return idMatches && employeeMatches;
        });
    }

    @FXML
    private void clearResultFilters() {
        employeeIdFilterField.clear();
        employeeFilterField.clear();
        applyResultFilters();
    }

    private String formatPeriod(Payrun payrun) {
        return payrun.getStartDate().format(DATE_FMT) + " - " + payrun.getEndDate().format(DATE_FMT);
    }

    private String formatPayrollType(PayrollType type) {
        return type == null ? "" : type.name().replace('_', ' ');
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private Label emptyLabel(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("muted-text");
        label.setStyle("-fx-padding: 16;");
        return label;
    }

    private void populatePayslip(PayslipDTO payslip) {
        slipNameLabel.setText(payslip.getFullName());
        slipPositionLabel.setText(payslip.getPosition());
        slipSuperLabel.setText("Supervisor: " + payslip.getSupervisor());
        slipPeriodLabel.setText(payslip.getPeriodStart().format(DATE_FMT)
                + " - " + payslip.getPeriodEnd().format(DATE_FMT));
        slipPayDateLabel.setText("Pay Date: " + payslip.getPayrollDate().format(DATE_FMT));
        slipSssLabel.setText("SSS: " + payslip.getSssNumber());
        slipPhilhLabel.setText("PhilHealth: " + payslip.getPhilhealthNumber());
        slipPagibigLabel.setText("Pag-IBIG: " + payslip.getPagibigNumber());
        slipTinLabel.setText("TIN: " + payslip.getTinNumber());

        slipBasicPayLabel.setText(UiFormatter.formatPeso(payslip.getBasicPay()));
        slipRiceLabel.setText(UiFormatter.formatPeso(payslip.getRiceSubsidy()));
        slipPhoneLabel.setText(UiFormatter.formatPeso(payslip.getPhoneAllowance()));
        slipClothingLabel.setText(UiFormatter.formatPeso(payslip.getClothingAllowance()));
        slipTotalCompLabel.setText(UiFormatter.formatPeso(payslip.getTotalCompensation()));

        slipSssDedLabel.setText(UiFormatter.formatPeso(payslip.getSssDeduction()));
        slipPhDedLabel.setText(UiFormatter.formatPeso(payslip.getPhilhealthDeduction()));
        slipPagibigDedLabel.setText(UiFormatter.formatPeso(payslip.getPagibigDeduction()));
        slipTaxDedLabel.setText(UiFormatter.formatPeso(payslip.getTaxDeduction()));
        slipLateDedLabel.setText(UiFormatter.formatPeso(payslip.getLateDeduction()));
        slipUndertimeDedLabel.setText(UiFormatter.formatPeso(payslip.getUndertimeDeduction()));
        slipTotalDedLabel.setText(UiFormatter.formatPeso(payslip.getTotalDeductions()));
        slipNetPayLabel.setText(UiFormatter.formatPeso(payslip.getNetPay()));
    }

    private String buildPayslipText(PayslipDTO payslip) {
        return ("MOTORPH PAYROLL MANAGEMENT SYSTEM\n"
                + "Employee Payslip\n"
                + "==========================================\n"
                + "Employee : " + payslip.getFullName() + "\n"
                + "Position : " + payslip.getPosition() + "\n"
                + "Supervisor: " + payslip.getSupervisor() + "\n"
                + "Period   : " + payslip.getPeriodStart().format(DATE_FMT)
                + " to " + payslip.getPeriodEnd().format(DATE_FMT) + "\n"
                + "Pay Date : " + payslip.getPayrollDate().format(DATE_FMT) + "\n"
                + "------------------------------------------\n"
                + "SSS No.       : " + payslip.getSssNumber() + "\n"
                + "PhilHealth No.: " + payslip.getPhilhealthNumber() + "\n"
                + "Pag-IBIG No.  : " + payslip.getPagibigNumber() + "\n"
                + "TIN No.       : " + payslip.getTinNumber() + "\n"
                + "------------------------------------------\n"
                + "EARNINGS\n"
                + "  Basic Pay          : " + UiFormatter.formatPeso(payslip.getBasicPay()) + "\n"
                + "  Rice Subsidy       : " + UiFormatter.formatPeso(payslip.getRiceSubsidy()) + "\n"
                + "  Phone Allowance    : " + UiFormatter.formatPeso(payslip.getPhoneAllowance()) + "\n"
                + "  Clothing Allowance : " + UiFormatter.formatPeso(payslip.getClothingAllowance()) + "\n"
                + "  Total Compensation : " + UiFormatter.formatPeso(payslip.getTotalCompensation()) + "\n"
                + "DEDUCTIONS\n"
                + "  SSS               : " + UiFormatter.formatPeso(payslip.getSssDeduction()) + "\n"
                + "  PhilHealth        : " + UiFormatter.formatPeso(payslip.getPhilhealthDeduction()) + "\n"
                + "  Pag-IBIG          : " + UiFormatter.formatPeso(payslip.getPagibigDeduction()) + "\n"
                + "  Withholding Tax   : " + UiFormatter.formatPeso(payslip.getTaxDeduction()) + "\n"
                + "  Late              : " + UiFormatter.formatPeso(payslip.getLateDeduction()) + "\n"
                + "  Undertime         : " + UiFormatter.formatPeso(payslip.getUndertimeDeduction()) + "\n"
                + "  Total Deductions  : " + UiFormatter.formatPeso(payslip.getTotalDeductions()) + "\n"
                + "==========================================\n"
                + "NET PAY: " + UiFormatter.formatPeso(payslip.getNetPay()) + "\n");
    }

    private void setMessage(String message) {
        if (moduleMessageLabel != null) {
            moduleMessageLabel.setText(message);
        }
    }
}
