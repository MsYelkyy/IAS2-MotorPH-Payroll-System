package motorph.ui.finance;

import motorph.api.ApiClientFactory;
import motorph.api.EmployeeApiClient;
import motorph.api.PayrunApiClient;
import motorph.api.PayslipApiClient;
import motorph.api.dto.employee.EmployeeProfileDTO;
import motorph.api.dto.employee.GovernmentDetailsDTO;
import motorph.api.dto.payroll.CompletedPayrunSummaryDTO;
import motorph.api.dto.payroll.PayrollPreviewDTO;
import motorph.api.dto.payroll.PayrollSummaryDTO;
import motorph.api.dto.payroll.PayrunCreateRequest;
import motorph.api.dto.payroll.PayrunDTO;
import motorph.api.dto.payroll.PayrunReadinessDTO;
import motorph.api.dto.payroll.PayslipReportDTO;
import motorph.api.dto.payroll.PendingPayrunSummaryDTO;
import motorph.domain.value.PayrollType;
import motorph.domain.value.ReadinessStatus;
import motorph.ui.util.DatePickerInputHelper;
import motorph.ui.util.UiFormatter;
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
import javafx.scene.control.TableView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Manages the shared payroll management module owned by Finance and reused by Admin. */
public class PayrollManagementController {

    private static final Logger LOG = Logger.getLogger(PayrollManagementController.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final String FORM_PANEL_CLASS = "payrun-form-active";
    private static final String FORM_BACKDROP_STYLE = "-fx-background-color: rgba(0,0,0,0.46);";

    private PayrunApiClient payrunApiClient;
    private PayslipApiClient payslipApiClient;
    private EmployeeApiClient employeeApiClient;

    private PayrunDTO selectedPayrun;
    private List<PayrollSummaryDTO> selectedPayrunSummaries;
    private PayslipReportDTO currentPayslip;
    private Panel lastDetailPanel = Panel.LANDING;

    private final ObservableList<PendingPayrunSummaryDTO> pendingItems = FXCollections.observableArrayList();
    private final ObservableList<CompletedPayrunSummaryDTO> completedItems = FXCollections.observableArrayList();
    private final ObservableList<PayrunReadinessDTO> readinessItems = FXCollections.observableArrayList();
    private final ObservableList<PayrollSummaryDTO> resultItems = FXCollections.observableArrayList();
    private FilteredList<PayrollSummaryDTO> filteredResults;

    // toolbar
    @FXML private StackPane toastRoot;
    @FXML private Button backButton;
    @FXML private VBox detailHeaderBox;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailSubtitleLabel;
    @FXML private Label moduleMessageLabel;
    @FXML private Button generateButton;
    @FXML private Button printButton;
    @FXML private Button newPayrunButton;

    // new payrun modal
    @FXML private Region payrunFormBackdrop;
    @FXML private VBox payrunFormPanel;
    @FXML private DatePicker payrunStartDatePicker;
    @FXML private DatePicker payrunEndDatePicker;
    @FXML private DatePicker payrunPayDatePicker;
    @FXML private ComboBox<PayrollType> payrunTypeCombo;
    @FXML private Label payrunFormErrorLabel;

    // landing
    @FXML private VBox landingPanel;
    @FXML private TabPane payrollTabPane;
    @FXML private TableView<PendingPayrunSummaryDTO> pendingTable;
    @FXML private TableColumn<PendingPayrunSummaryDTO, LocalDate> pendingPayDateColumn;
    @FXML private TableColumn<PendingPayrunSummaryDTO, String> pendingPeriodColumn;
    @FXML private TableColumn<PendingPayrunSummaryDTO, Number> pendingEmployeesColumn;
    @FXML private TableColumn<PendingPayrunSummaryDTO, Number> pendingReadyColumn;
    @FXML private TableColumn<PendingPayrunSummaryDTO, Number> pendingIncompleteColumn;
    @FXML private TableColumn<PendingPayrunSummaryDTO, String> pendingStatusColumn;
    @FXML private TableColumn<PendingPayrunSummaryDTO, Void> pendingActionColumn;

    @FXML private TableView<CompletedPayrunSummaryDTO> completedTable;
    @FXML private TableColumn<CompletedPayrunSummaryDTO, LocalDate> completedPayDateColumn;
    @FXML private TableColumn<CompletedPayrunSummaryDTO, String> completedPeriodColumn;
    @FXML private TableColumn<CompletedPayrunSummaryDTO, Number> completedPaidColumn;
    @FXML private TableColumn<CompletedPayrunSummaryDTO, Number> completedGrossColumn;
    @FXML private TableColumn<CompletedPayrunSummaryDTO, Number> completedDeductionsColumn;
    @FXML private TableColumn<CompletedPayrunSummaryDTO, Number> completedNetColumn;
    @FXML private TableColumn<CompletedPayrunSummaryDTO, String> completedStatusColumn;
    @FXML private TableColumn<CompletedPayrunSummaryDTO, Void> completedActionColumn;
    @FXML private Label landingPendingPayrunsCard;
    @FXML private Label landingReadyEmployeesCard;
    @FXML private Label landingIncompleteEmployeesCard;
    @FXML private Label landingCompletedPayrunsCard;
    @FXML private Label landingEmployeesPaidCard;
    @FXML private Label landingTotalNetPayCard;

    // pending detail
    @FXML private VBox pendingDetailPanel;
    @FXML private Label pendingCardTotal;
    @FXML private Label pendingCardReady;
    @FXML private Label pendingCardIncomplete;
    @FXML private Label pendingCardNoAttendance;
    @FXML private TableView<PayrunReadinessDTO> readinessTable;
    @FXML private TableColumn<PayrunReadinessDTO, Number> readyEmpIdColumn;
    @FXML private TableColumn<PayrunReadinessDTO, String> readyNameColumn;
    @FXML private TableColumn<PayrunReadinessDTO, String> readyDeptColumn;
    @FXML private TableColumn<PayrunReadinessDTO, String> readyPositionColumn;
    @FXML private TableColumn<PayrunReadinessDTO, Number> readyDaysColumn;
    @FXML private TableColumn<PayrunReadinessDTO, Number> readyHoursColumn;
    @FXML private TableColumn<PayrunReadinessDTO, String> readyAttendanceColumn;
    @FXML private TableColumn<PayrunReadinessDTO, Void> readyActionColumn;

    // completed detail
    @FXML private VBox completedDetailPanel;
    @FXML private Label completedCardPaid;
    @FXML private Label completedCardGross;
    @FXML private Label completedCardDeductions;
    @FXML private Label completedCardNet;
    @FXML private TextField employeeSearchField;
    @FXML private Button clearResultFiltersButton;
    @FXML private TableView<PayrollSummaryDTO> resultTable;
    @FXML private TableColumn<PayrollSummaryDTO, Number> resultEmployeeIdColumn;
    @FXML private TableColumn<PayrollSummaryDTO, String> resultEmployeeColumn;
    @FXML private TableColumn<PayrollSummaryDTO, Number> resultGrossPayColumn;
    @FXML private TableColumn<PayrollSummaryDTO, Number> resultDeductionsColumn;
    @FXML private TableColumn<PayrollSummaryDTO, Number> resultNetPayColumn;
    @FXML private TableColumn<PayrollSummaryDTO, String> resultStatusColumn;
    @FXML private TableColumn<PayrollSummaryDTO, Void> resultActionColumn;

    // payslip detail
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
        ApiClientFactory factory = ApiClientFactory.getInstance();
        payrunApiClient = factory.getPayrunApiClient();
        payslipApiClient = factory.getPayslipApiClient();
        employeeApiClient = factory.getEmployeeApiClient();

        payrunTypeCombo.getItems().setAll(PayrollType.values());
        payrunTypeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(PayrollType type) {
                return formatPayrollType(type);
            }

            @Override public PayrollType fromString(String value) {
                return null;
            }
        });

        setupPendingTable();
        setupCompletedTable();
        setupReadinessTable();
        setupResultTable();
        setupFilters();
        showLanding();

        DatePickerInputHelper.enableMaskedInput(payrunStartDatePicker);
        DatePickerInputHelper.enableMaskedInput(payrunEndDatePicker);
        DatePickerInputHelper.enableMaskedInput(payrunPayDatePicker);
    }

    /** Refreshes the payroll landing when the nav button is clicked. */
    public void refresh() {
        showLanding();
    }

    @FXML
    private void handleBack() {
        if (payrunFormPanel.isVisible()) {
            hideNewPayrunForm();
            return;
        }
        if (payslipDetailPanel.isVisible()) {
            // return to the detail page we came from
            if (lastDetailPanel == Panel.COMPLETED_DETAIL && selectedPayrun != null) {
                showCompletedDetail(selectedPayrun);
            } else {
                showLanding();
            }
        } else {
            showLanding();
        }
    }

    @FXML
    private void handleNewPayrun() {
        showNewPayrunForm();
    }

    @FXML
    private void handleGenerate() {
        if (selectedPayrun == null) return;

        if (isAlreadyCompleted(selectedPayrun.getPayrunId())) {
            showAlreadyGeneratedAlert();
            return;
        }

        PendingPayrunSummaryDTO summary = findPendingSummary(selectedPayrun.getPayrunId());
        int ready = summary != null ? summary.getReadyEmployees() : 0;
        int incomplete = summary != null ? summary.getIncompleteEmployees() : 0;
        int noAttendance = summary != null ? summary.getNoAttendanceEmployees() : 0;

        if (ready == 0) {
            showCannotGeneratePayrollAlert();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Payroll Date: " + selectedPayrun.getPayDate().format(DATE_FMT) + "\n"
                        + "Ready Employees: " + ready + "\n"
                        + "Incomplete Employees: " + incomplete + "\n"
                        + "No Attendance Employees: " + noAttendance + "\n\n"
                        + "Only Ready employees will be processed.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Generate Payroll");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.YES) {
            try {
                int generated = payrunApiClient.generatePayroll(selectedPayrun.getPayrunId());
                showCompletedDetail(selectedPayrun);
                UiMessageUtil.showToast(toastRoot, "Payroll generated for " + generated + " employee(s).");
            } catch (Exception e) {
                showGenerateErrorAlert(UiMessageUtil.toUserMessage(e));
            }
        }
    }

    private void showGenerateErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Generate Payroll");
        alert.setHeaderText("Could Not Generate Payroll");
        alert.showAndWait();
    }

    private void showCannotGeneratePayrollAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                "No ready employees are available for this payroll period.\n\n"
                        + "Please review attendance records before generating payroll.",
                ButtonType.OK);
        alert.setTitle("Cannot Generate Payroll");
        alert.setHeaderText("Cannot Generate Payroll");
        alert.showAndWait();
    }

    // checks if the payrun already has generated payroll results
    private boolean isAlreadyCompleted(int payrunId) {
        return completedItems.stream().anyMatch(c -> c.getPayrunId() == payrunId);
    }

    private void showAlreadyGeneratedAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                "Payroll has already been generated for this payroll period.\n\n"
                        + "Open it from the Completed tab to view the results.",
                ButtonType.OK);
        alert.setTitle("Payroll Already Generated");
        alert.setHeaderText("Payroll Already Generated");
        alert.showAndWait();
    }

    // shows a non-persisted payroll preview for a ready employee
    private void showPayrollPreview(PayrunReadinessDTO row) {
        if (selectedPayrun == null || row == null || !row.isReady()) return;

        PayrollPreviewDTO preview;
        try {
            preview = payrunApiClient.previewPayrollForEmployee(selectedPayrun.getPayrunId(), row.getEmployeeId());
        } catch (Exception e) {
            setMessage("Failed to compute preview: " + e.getMessage());
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Payroll Preview");
        alert.setHeaderText("Preview Only — Not Generated");
        alert.getDialogPane().setContent(buildPreviewContent(preview));
        alert.getDialogPane().setPrefWidth(420);
        alert.showAndWait();
    }

    // builds the preview modal body
    private VBox buildPreviewContent(PayrollPreviewDTO preview) {
        VBox box = new VBox(6);
        box.getChildren().addAll(
                previewLine("Employee", preview.getFullName() + " (#" + preview.getEmployeeId() + ")"),
                previewLine("Pay Period",
                        preview.getPeriodStart().format(DATE_FMT) + " - " + preview.getPeriodEnd().format(DATE_FMT)),
                previewLine("Total Hours", String.format("%.2f", preview.getTotalHours())),
                previewLine("Gross Pay", UiFormatter.formatPeso(preview.getGrossPay())),
                previewLine("Total Deductions", UiFormatter.formatPeso(preview.getTotalDeductions())),
                previewLine("Net Pay", UiFormatter.formatPeso(preview.getNetPay())));
        return box;
    }

    // builds one label/value row for the preview modal
    private HBox previewLine(String label, String value) {
        Label name = new Label(label);
        name.setMinWidth(140);
        name.setStyle("-fx-font-weight: bold;");
        HBox line = new HBox(8, name, new Label(value));
        line.setAlignment(Pos.CENTER_LEFT);
        return line;
    }

    @FXML
    private void handlePrint() {
        if (currentPayslip == null || selectedPayrun == null) return;
        printPayslip(currentPayslip.getEmployeeId(), selectedPayrun.getPayrunId());
    }

    // downloads the payslip PDF and opens it in the OS's default PDF viewer -
    // there is no in-app Jasper preview window anymore now that generation
    // happens server-side
    private void printPayslip(int employeeId, int payrunId) {
        try {
            byte[] pdf = payslipApiClient.getPayslipPdf(employeeId, payrunId);

            File tempFile = File.createTempFile("payslip-" + employeeId + "-" + payrunId + "-", ".pdf");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), pdf);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(tempFile);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to open payslip PDF", e);
            setMessage("Failed to open payslip: " + e.getMessage());
        }
    }

    // ── New payrun modal ──────────────────────────────────────────────

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
            PayrunCreateRequest request = new PayrunCreateRequest(
                    payrunStartDatePicker.getValue(),
                    payrunEndDatePicker.getValue(),
                    payrunPayDatePicker.getValue(),
                    payrunTypeCombo.getValue());
            PayrunDTO created = payrunApiClient.createPayrun(request);
            hideNewPayrunForm();
            setMessage("Payrun created.");
            showPendingDetail(created);
        } catch (Exception e) {
            payrunFormErrorLabel.setText(e.getMessage());
        }
    }

    // ── Panel navigation ──────────────────────────────────────────────

    private enum Panel { LANDING, PENDING_DETAIL, COMPLETED_DETAIL, PAYSLIP_DETAIL }

    private void showPanel(Panel active) {
        setPane(landingPanel, active == Panel.LANDING);
        setPane(pendingDetailPanel, active == Panel.PENDING_DETAIL);
        setPane(completedDetailPanel, active == Panel.COMPLETED_DETAIL);
        setPane(payslipDetailPanel, active == Panel.PAYSLIP_DETAIL);
        setPane(payrunFormBackdrop, false);
        setPane(payrunFormPanel, false);
        applyPayrunFormVisuals(false);

        setVis(backButton, active != Panel.LANDING);
        setVis(detailHeaderBox, active != Panel.LANDING);
        setVis(moduleMessageLabel, active == Panel.LANDING);
        setVis(newPayrunButton, active == Panel.LANDING);
        setVis(generateButton, active == Panel.PENDING_DETAIL);
        setVis(printButton, active == Panel.PAYSLIP_DETAIL);

        if (active != Panel.LANDING) {
            setMessage("");
        }

        if (active == Panel.PENDING_DETAIL || active == Panel.COMPLETED_DETAIL) {
            lastDetailPanel = active;
        }
    }

    private void showLanding() {
        try {
            pendingItems.setAll(payrunApiClient.getPendingPayruns());
            completedItems.setAll(payrunApiClient.getCompletedPayruns());
            updateLandingCards();
        } catch (Exception e) {
            setMessage("Failed to load payroll dates: " + e.getMessage());
        }
        showPanel(Panel.LANDING);
    }

    private void showPendingDetail(PayrunDTO payrun) {
        if (payrun == null) return;
        selectedPayrun = payrun;

        try {
            PendingPayrunSummaryDTO summary = findPendingSummary(payrun.getPayrunId());
            List<PayrunReadinessDTO> rows = payrunApiClient.getPayrunReadiness(payrun.getPayrunId());
            int total = summary != null ? summary.getTotalEmployees() : rows.size();
            int ready = summary != null ? summary.getReadyEmployees() : countReadiness(rows, ReadinessStatus.READY);
            int incomplete = summary != null ? summary.getIncompleteEmployees() : countReadiness(rows, ReadinessStatus.INCOMPLETE);
            int noAttendance = summary != null ? summary.getNoAttendanceEmployees() : countReadiness(rows, ReadinessStatus.NO_ATTENDANCE);

            pendingCardTotal.setText(String.valueOf(total));
            pendingCardReady.setText(String.valueOf(ready));
            pendingCardIncomplete.setText(String.valueOf(incomplete));
            pendingCardNoAttendance.setText(String.valueOf(noAttendance));

            detailTitleLabel.setText("Payroll Date: " + payrun.getPayDate().format(DATE_FMT));
            detailSubtitleLabel.setText("Pay Period: " + formatPeriod(payrun));

            readinessItems.setAll(rows);
        } catch (Exception e) {
            setMessage("Failed to load pending detail: " + e.getMessage());
        }
        showPanel(Panel.PENDING_DETAIL);
    }

    private void showCompletedDetail(PayrunDTO payrun) {
        if (payrun == null) return;
        selectedPayrun = payrun;
        clearResultFilters();

        try {
            selectedPayrunSummaries = payrunApiClient.getPayrollSummaries(payrun.getPayrunId());

            double gross = selectedPayrunSummaries.stream().mapToDouble(PayrollSummaryDTO::getGrossPay).sum();
            double deductions = selectedPayrunSummaries.stream().mapToDouble(PayrollSummaryDTO::getTotalDeductions).sum();
            double net = selectedPayrunSummaries.stream().mapToDouble(PayrollSummaryDTO::getNetPay).sum();

            completedCardPaid.setText(String.valueOf(selectedPayrunSummaries.size()));
            completedCardGross.setText(UiFormatter.formatPeso(gross));
            completedCardDeductions.setText(UiFormatter.formatPeso(deductions));
            completedCardNet.setText(UiFormatter.formatPeso(net));

            detailTitleLabel.setText("Payroll Date: " + payrun.getPayDate().format(DATE_FMT));
            detailSubtitleLabel.setText("Pay Period: " + formatPeriod(payrun));

            resultItems.setAll(selectedPayrunSummaries);
            applyResultFilters();
        } catch (Exception e) {
            setMessage("Failed to load payroll detail: " + e.getMessage());
        }
        showPanel(Panel.COMPLETED_DETAIL);
    }

    private void showPayslipDetail(PayrollSummaryDTO summary) {
        try {
            currentPayslip = payslipApiClient.getPayslipReport(summary.getEmployeeId(), summary.getPayrunId());
            populatePayslip(currentPayslip);
            detailTitleLabel.setText("Payslip - " + currentPayslip.getEmployeeName());
            detailSubtitleLabel.setText("Pay Period: "
                    + currentPayslip.getStartDate().format(DATE_FMT)
                    + " - " + currentPayslip.getEndDate().format(DATE_FMT));
            showPanel(Panel.PAYSLIP_DETAIL);
        } catch (Exception e) {
            setMessage("Failed to load payslip: " + e.getMessage());
        }
    }

    // ── Pending date helper ───────────────────────────────────────────

    private PendingPayrunSummaryDTO findPendingSummary(int payrunId) {
        return pendingItems.stream()
                .filter(p -> p.getPayrunId() == payrunId)
                .findFirst()
                .orElse(null);
    }

    private void updateLandingCards() {
        int ready = pendingItems.stream().mapToInt(PendingPayrunSummaryDTO::getReadyEmployees).sum();
        int incomplete = pendingItems.stream().mapToInt(PendingPayrunSummaryDTO::getIncompleteEmployees).sum();
        int paid = completedItems.stream().mapToInt(CompletedPayrunSummaryDTO::getEmployeesPaid).sum();
        double totalNet = completedItems.stream().mapToDouble(CompletedPayrunSummaryDTO::getTotalNetPay).sum();

        landingPendingPayrunsCard.setText(String.valueOf(pendingItems.size()));
        landingReadyEmployeesCard.setText(String.valueOf(ready));
        landingIncompleteEmployeesCard.setText(String.valueOf(incomplete));
        landingCompletedPayrunsCard.setText(String.valueOf(completedItems.size()));
        landingEmployeesPaidCard.setText(String.valueOf(paid));
        landingTotalNetPayCard.setText(UiFormatter.formatPeso(totalNet));
    }

    private int countReadiness(List<PayrunReadinessDTO> rows, ReadinessStatus status) {
        return (int) rows.stream()
                .filter(row -> row.getStatus() == status)
                .count();
    }

    private PayrunDTO toPayrun(PendingPayrunSummaryDTO dto) {
        return new PayrunDTO(dto.getPayrunId(), dto.getStartDate(), dto.getEndDate(),
                dto.getPayDate(), dto.getPayrollType());
    }

    private PayrunDTO toPayrun(CompletedPayrunSummaryDTO dto) {
        return new PayrunDTO(dto.getPayrunId(), dto.getStartDate(), dto.getEndDate(),
                dto.getPayDate(), dto.getPayrollType());
    }

    // ── Table setup ───────────────────────────────────────────────────

    private void setupPendingTable() {
        pendingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        pendingTable.setItems(pendingItems);

        pendingPayDateColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPayDate()));
        pendingPayDateColumn.setCellFactory(c -> pendingDateCell());
        pendingPeriodColumn.setCellValueFactory(d ->
                new SimpleStringProperty(formatPeriod(d.getValue().getStartDate(), d.getValue().getEndDate())));
        pendingEmployeesColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTotalEmployees()));
        pendingReadyColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getReadyEmployees()));
        pendingIncompleteColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getIncompleteEmployees()));
        pendingStatusColumn.setCellValueFactory(d -> new SimpleStringProperty("Pending"));
        pendingStatusColumn.setCellFactory(c -> badgeCell("status-pending"));

        pendingActionColumn.setSortable(false);
        pendingActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final Button generateButton = new Button("Generate");
            private final HBox box = new HBox(8, viewButton, generateButton);
            {
                viewButton.getStyleClass().addAll("table-action-button", "neutral-table-button");
                generateButton.getStyleClass().add("primary-table-button");
                box.setAlignment(Pos.CENTER_RIGHT);
                viewButton.setOnAction(e -> {
                    PendingPayrunSummaryDTO dto = getTableRow().getItem();
                    if (dto != null) showPendingDetail(toPayrun(dto));
                });
                generateButton.setOnAction(e -> {
                    PendingPayrunSummaryDTO dto = getTableRow().getItem();
                    if (dto != null) {
                        selectedPayrun = toPayrun(dto);
                        handleGenerate();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void setupCompletedTable() {
        completedTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        completedTable.setItems(completedItems);

        completedPayDateColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getPayDate()));
        completedPayDateColumn.setCellFactory(c -> completedDateCell());
        completedPeriodColumn.setCellValueFactory(d ->
                new SimpleStringProperty(formatPeriod(d.getValue().getStartDate(), d.getValue().getEndDate())));
        completedPaidColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getEmployeesPaid()));
        completedGrossColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTotalGrossPay()));
        completedGrossColumn.setCellFactory(c -> completedPesoCell());
        completedDeductionsColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTotalDeductions()));
        completedDeductionsColumn.setCellFactory(c -> completedPesoCell());
        completedNetColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTotalNetPay()));
        completedNetColumn.setCellFactory(c -> completedPesoCell());
        completedStatusColumn.setCellValueFactory(d -> new SimpleStringProperty("Completed"));
        completedStatusColumn.setCellFactory(c -> badgeCell("status-completed"));

        completedActionColumn.setSortable(false);
        completedActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            {
                viewButton.getStyleClass().addAll("table-action-button", "neutral-table-button");
                viewButton.setOnAction(e -> {
                    CompletedPayrunSummaryDTO dto = getTableRow().getItem();
                    if (dto != null) showCompletedDetail(toPayrun(dto));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setGraphic(empty ? null : viewButton);
            }
        });
    }

    private void setupReadinessTable() {
        readinessTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        readinessTable.setItems(readinessItems);

        readyEmpIdColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getEmployeeId()));
        readyNameColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        readyDeptColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDepartment()));
        readyPositionColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPosition()));
        readyDaysColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getDaysWorked()));
        readyHoursColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTotalHours()));
        readyHoursColumn.setCellFactory(c -> hoursCell());
        readyAttendanceColumn.setCellValueFactory(d ->
                new SimpleStringProperty(readinessLabel(d.getValue().getStatus())));
        readyAttendanceColumn.setCellFactory(c -> attendanceBadgeCell());

        readyActionColumn.setSortable(false);
        readyActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button previewButton = new Button("Preview");
            {
                previewButton.getStyleClass().addAll("table-action-button", "neutral-table-button");
                previewButton.setOnAction(e -> {
                    PayrunReadinessDTO row = getTableRow().getItem();
                    if (row != null) showPayrollPreview(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_RIGHT);
                PayrunReadinessDTO row = getTableRow() == null ? null : getTableRow().getItem();
                setGraphic(empty || row == null || !row.isReady() ? null : previewButton);
            }
        });
    }

    private void setupResultTable() {
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        resultEmployeeIdColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getEmployeeId()));
        resultEmployeeColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        resultGrossPayColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getGrossPay()));
        resultGrossPayColumn.setCellFactory(c -> pesoCell());
        resultDeductionsColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTotalDeductions()));
        resultDeductionsColumn.setCellFactory(c -> pesoCell());
        resultNetPayColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getNetPay()));
        resultNetPayColumn.setCellFactory(c -> pesoCell());
        resultStatusColumn.setCellValueFactory(d -> new SimpleStringProperty("Paid"));
        resultStatusColumn.setCellFactory(c -> badgeCell("status-completed"));

        resultActionColumn.setSortable(false);
        resultActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewButton = new Button("View Payslip");
            private final Button printButton = new Button("Print Payslip");
            private final HBox box = new HBox(8, viewButton, printButton);
            {
                viewButton.getStyleClass().addAll("table-action-button", "neutral-table-button");
                printButton.getStyleClass().add("print-payslip-button");
                printButton.getStyleClass().add("compact-print-button");
                box.setAlignment(Pos.CENTER_RIGHT);
                viewButton.setOnAction(e -> {
                    PayrollSummaryDTO summary = getTableRow().getItem();
                    if (summary != null) showPayslipDetail(summary);
                });
                printButton.setOnAction(e -> {
                    PayrollSummaryDTO summary = getTableRow().getItem();
                    if (summary != null) {
                        printPayslip(summary.getEmployeeId(), summary.getPayrunId());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setGraphic(empty ? null : box);
            }
        });

        filteredResults = new FilteredList<>(resultItems, result -> true);
        SortedList<PayrollSummaryDTO> sortedResults = new SortedList<>(filteredResults);
        sortedResults.comparatorProperty().bind(resultTable.comparatorProperty());
        resultTable.setItems(sortedResults);
    }

    // ── Cell factories ────────────────────────────────────────────────

    private TableCell<PendingPayrunSummaryDTO, LocalDate> pendingDateCell() {
        return new TableCell<>() {
            @Override protected void updateItem(LocalDate value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.format(DATE_FMT));
            }
        };
    }

    private TableCell<CompletedPayrunSummaryDTO, LocalDate> completedDateCell() {
        return new TableCell<>() {
            @Override protected void updateItem(LocalDate value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.format(DATE_FMT));
            }
        };
    }

    private TableCell<CompletedPayrunSummaryDTO, Number> completedPesoCell() {
        return new TableCell<>() {
            @Override protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setText(empty || value == null ? null : UiFormatter.formatPeso(value.doubleValue()));
            }
        };
    }

    private TableCell<PayrollSummaryDTO, Number> pesoCell() {
        return new TableCell<>() {
            @Override protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setText(empty || value == null ? null : UiFormatter.formatPeso(value.doubleValue()));
            }
        };
    }

    private TableCell<PayrunReadinessDTO, Number> hoursCell() {
        return new TableCell<>() {
            @Override protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setText(empty || value == null ? null : String.format("%.2f", value.doubleValue()));
            }
        };
    }

    private <T> TableCell<T, String> badgeCell(String extraClass) {
        return new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().addAll("status-badge", extraClass);
            }

            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                } else {
                    badge.setText(value);
                    setGraphic(badge);
                }
            }
        };
    }

    private TableCell<PayrunReadinessDTO, String> attendanceBadgeCell() {
        return new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("status-badge"); }

            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                badge.getStyleClass().removeAll("status-ready", "status-incomplete", "status-none");
                if (empty || value == null) {
                    setGraphic(null);
                    return;
                }
                PayrunReadinessDTO row = getTableRow() == null ? null : getTableRow().getItem();
                ReadinessStatus status = row == null ? null : row.getStatus();
                if (status == ReadinessStatus.READY) {
                    badge.getStyleClass().add("status-ready");
                } else if (status == ReadinessStatus.INCOMPLETE) {
                    badge.getStyleClass().add("status-incomplete");
                } else {
                    badge.getStyleClass().add("status-none");
                }
                badge.setText(value);
                setGraphic(badge);
            }
        };
    }

    private String readinessLabel(ReadinessStatus status) {
        if (status == null) return "";
        return switch (status) {
            case READY -> "Ready";
            case INCOMPLETE -> "Incomplete";
            case NO_ATTENDANCE -> "No Attendance";
        };
    }

    // ── Filters (completed detail) ────────────────────────────────────

    private void setupFilters() {
        employeeSearchField.textProperty().addListener((obs, old, value) -> applyResultFilters());
    }

    private void applyResultFilters() {
        if (filteredResults == null) return;

        String query = normalize(employeeSearchField.getText());

        filteredResults.setPredicate(result -> {
            if (query.isBlank()) {
                return true;
            }
            return String.valueOf(result.getEmployeeId()).contains(query)
                    || normalize(result.getFullName()).contains(query);
        });
    }

    @FXML
    private void clearResultFilters() {
        employeeSearchField.clear();
        applyResultFilters();
    }

    // ── Formatting helpers ────────────────────────────────────────────

    private String formatPeriod(PayrunDTO payrun) {
        return formatPeriod(payrun.getStartDate(), payrun.getEndDate());
    }

    private String formatPeriod(LocalDate start, LocalDate end) {
        return start.format(DATE_FMT) + " - " + end.format(DATE_FMT);
    }

    private String formatPayrollType(PayrollType type) {
        return type == null ? "" : type.name().replace('_', ' ');
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    // ── Visibility helpers ────────────────────────────────────────────

    private void setPane(javafx.scene.Node node, boolean show) {
        if (node != null) {
            node.setVisible(show);
            node.setManaged(show);
        }
    }

    private void setVis(javafx.scene.Node node, boolean show) {
        setPane(node, show);
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

    private void setMessage(String message) {
        if (moduleMessageLabel != null) {
            moduleMessageLabel.setText(message);
        }
    }

    // ── Payslip population / text ─────────────────────────────────────

    private void populatePayslip(PayslipReportDTO payslip) {
        slipNameLabel.setText(payslip.getEmployeeName());
        slipPositionLabel.setText(payslip.getPositionName());
        slipSuperLabel.setText("Supervisor: " + resolveSupervisorName(payslip.getEmployeeId()));
        slipPeriodLabel.setText(payslip.getStartDate().format(DATE_FMT)
                + " - " + payslip.getEndDate().format(DATE_FMT));
        slipPayDateLabel.setText("Pay Date: " + payslip.getPayDate().format(DATE_FMT));

        GovernmentDetailsDTO govt = resolveGovernmentDetails(payslip.getEmployeeId());
        slipSssLabel.setText("SSS: " + safe(govt == null ? null : govt.getSssNumber()));
        slipPhilhLabel.setText("PhilHealth: " + safe(govt == null ? null : govt.getPhilhealthNumber()));
        slipPagibigLabel.setText("Pag-IBIG: " + safe(govt == null ? null : govt.getPagibigNumber()));
        slipTinLabel.setText("TIN: " + safe(govt == null ? null : govt.getTinNumber()));

        slipBasicPayLabel.setText(UiFormatter.formatPeso(payslip.getBasicPay()));
        slipRiceLabel.setText(UiFormatter.formatPeso(payslip.getRiceSubsidy()));
        slipPhoneLabel.setText(UiFormatter.formatPeso(payslip.getPhoneAllowance()));
        slipClothingLabel.setText(UiFormatter.formatPeso(payslip.getClothingAllowance()));
        slipTotalCompLabel.setText(UiFormatter.formatPeso(
                payslip.getBasicPay() + payslip.getOvertimePay() + payslip.getTotalBenefits()));

        slipSssDedLabel.setText(UiFormatter.formatPeso(payslip.getSssDeduction()));
        slipPhDedLabel.setText(UiFormatter.formatPeso(payslip.getPhilhealthDeduction()));
        slipPagibigDedLabel.setText(UiFormatter.formatPeso(payslip.getPagibigDeduction()));
        slipTaxDedLabel.setText(UiFormatter.formatPeso(payslip.getWithholdingTax()));
        slipLateDedLabel.setText(UiFormatter.formatPeso(payslip.getOtherDeductions()));
        slipUndertimeDedLabel.setText("—");
        slipTotalDedLabel.setText(UiFormatter.formatPeso(payslip.getTotalDeductions()));
        slipNetPayLabel.setText(UiFormatter.formatPeso(payslip.getNetPay()));
    }

    // resolves the employee's supervisor name for display; falls back on error
    private String resolveSupervisorName(int employeeId) {
        try {
            EmployeeProfileDTO profile = employeeApiClient.getEmployee(employeeId);
            return profile != null && profile.getSupervisorName() != null ? profile.getSupervisorName() : "—";
        } catch (Exception e) {
            return "—";
        }
    }

    // resolves government ID numbers for display; falls back to null on error
    // (e.g. the caller's role lacks VIEW_EMPLOYEE) so the payslip still renders
    private GovernmentDetailsDTO resolveGovernmentDetails(int employeeId) {
        try {
            return employeeApiClient.getGovernmentDetails(employeeId);
        } catch (Exception e) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
