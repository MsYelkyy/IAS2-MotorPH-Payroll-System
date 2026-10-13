package motorph.ui.finance;

import motorph.api.ApiClientFactory;
import motorph.api.ApiException;
import motorph.api.EmployeeApiClient;
import motorph.api.PayrunApiClient;
import motorph.api.PayslipApiClient;
import motorph.api.dto.employee.EmployeeSummaryDTO;
import motorph.api.dto.payroll.PayrunDTO;
import motorph.api.dto.payroll.PayslipReportDTO;
import motorph.ui.util.UiFormatter;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Finance-owned screen to search, preview, export, and print existing persisted payslips. */
public class FinanceViewPayslipController {

    private static final Logger LOG = Logger.getLogger(FinanceViewPayslipController.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private EmployeeApiClient employeeApiClient;
    private PayrunApiClient payrunApiClient;
    private PayslipApiClient payslipApiClient;

    private PayslipReportDTO currentPayslip;

    // root / search
    @FXML private StackPane toastRoot;
    @FXML private ComboBox<EmployeeSummaryDTO> employeeCombo;
    @FXML private ComboBox<PayrunDTO> payrunCombo;
    @FXML private Button searchButton;

    // employee details card
    @FXML private Label detailNameLabel;
    @FXML private Label detailEmployeeIdLabel;
    @FXML private Label detailDepartmentLabel;
    @FXML private Label detailPositionLabel;
    @FXML private Label detailPeriodLabel;
    @FXML private Label detailGrossLabel;
    @FXML private Label detailDeductionsLabel;
    @FXML private Label detailNetPayLabel;
    @FXML private Button previewButton;
    @FXML private Button exportButton;
    @FXML private Button printButton;

    // payslip preview card
    @FXML private TextArea previewArea;

    @FXML
    private void initialize() {
        ApiClientFactory factory = ApiClientFactory.getInstance();
        employeeApiClient = factory.getEmployeeApiClient();
        payrunApiClient   = factory.getPayrunApiClient();
        payslipApiClient  = factory.getPayslipApiClient();

        setupEmployeeCombo();
        setupPayrunCombo();
        clearResult();
    }

    // reloads dropdown options when the view becomes active
    public void refresh() {
        loadEmployees();
        loadPayruns();
        clearResult();
    }

    // searches for the persisted payslip of the selected employee and payrun
    @FXML
    private void handleSearch() {
        EmployeeSummaryDTO employee = employeeCombo.getValue();
        PayrunDTO payrun = payrunCombo.getValue();

        if (employee == null || payrun == null) {
            showError("Select Employee and Payrun",
                    "Please select both an employee and a payrun before searching.");
            return;
        }

        try {
            currentPayslip = payslipApiClient.getPayslipReport(
                    employee.getEmployeeId(), payrun.getPayrunId());
            populateDetails(currentPayslip);
            previewArea.setText("Click Preview to view the payslip summary.");
            setActionsDisabled(false);
        } catch (Exception e) {
            clearResult();
            showError("Could Not Load Payslip", searchFailureMessage(e));
        }
    }

    // only a 404 means the payslip is missing; a permission or server error is reported as what it is
    static String searchFailureMessage(Exception e) {
        if (e instanceof ApiException api && api.getHttpStatus() == 404) {
            return "No persisted payslip exists for this employee and payrun.";
        }
        return UiMessageUtil.toUserMessage(e);
    }

    // shows the payslip summary in the preview container
    @FXML
    private void handlePreview() {
        if (currentPayslip == null) return;
        populatePreview(currentPayslip);
        UiMessageUtil.showToast(toastRoot, "Payslip loaded from persisted payroll results.");
    }

    // exports the payslip to a PDF file chosen by the user
    @FXML
    private void handleExportPdf() {
        if (currentPayslip == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Payslip PDF");
        chooser.setInitialFileName("Payslip_" + currentPayslip.getEmployeeId()
                + "_Payrun" + currentPayslip.getPayrunId() + ".pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File target = chooser.showSaveDialog(toastRoot.getScene().getWindow());
        if (target == null) return;

        try {
            byte[] pdf = payslipApiClient.getPayslipPdf(currentPayslip.getEmployeeId(), currentPayslip.getPayrunId());
            Files.write(target.toPath(), pdf);
            UiMessageUtil.showToast(toastRoot, "Payslip exported to " + target.getName() + ".");
        } catch (Exception e) {
            showError("Could Not Export Payslip", UiMessageUtil.toUserMessage(e));
        }
    }

    // downloads the payslip PDF and opens it in the OS's default PDF viewer
    @FXML
    private void handlePrint() {
        if (currentPayslip == null) return;
        try {
            byte[] pdf = payslipApiClient.getPayslipPdf(currentPayslip.getEmployeeId(), currentPayslip.getPayrunId());

            File tempFile = File.createTempFile(
                    "payslip-" + currentPayslip.getEmployeeId() + "-" + currentPayslip.getPayrunId() + "-", ".pdf");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), pdf);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(tempFile);
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save or open payslip PDF", e);
            showError("Could Not Open Payslip", e.getMessage());
        } catch (Exception e) {
            showError("Could Not Open Payslip", UiMessageUtil.toUserMessage(e));
        }
    }

    // Dropdown setup
    private void setupEmployeeCombo() {
        employeeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(EmployeeSummaryDTO e) {
                return e == null ? "Select Employee" : e.getEmployeeId() + " - " + e.getFullName();
            }

            @Override public EmployeeSummaryDTO fromString(String value) {
                return null;
            }
        });
        loadEmployees();
    }

    private void setupPayrunCombo() {
        payrunCombo.setConverter(new StringConverter<>() {
            @Override public String toString(PayrunDTO p) {
                return p == null
                        ? "Select Payrun"
                        : p.getPayrunId() + " | " + DATE_FMT.format(p.getStartDate())
                                + " - " + DATE_FMT.format(p.getEndDate());
            }

            @Override public PayrunDTO fromString(String value) {
                return null;
            }
        });
        loadPayruns();
    }

    // loads active employees into the employee dropdown
    private void loadEmployees() {
        try {
            List<EmployeeSummaryDTO> employees = employeeApiClient.getAllEmployeeSummaries().stream()
                    .filter(EmployeeSummaryDTO::isActive)
                    .toList();
            employeeCombo.getItems().setAll(employees);
        } catch (Exception e) {
            employeeCombo.getItems().clear();
        }
        employeeCombo.setValue(null);
    }

    // loads payruns that have payroll results into the payrun dropdown
    private void loadPayruns() {
        try {
            List<PayrunDTO> payruns = payrunApiClient.getPayrunsWithPayrollData();
            payrunCombo.getItems().setAll(payruns);
        } catch (Exception e) {
            payrunCombo.getItems().clear();
        }
        payrunCombo.setValue(null);
    }

    // Detail card / preview population
    private void populateDetails(PayslipReportDTO payslip) {
        detailNameLabel.setText(payslip.getEmployeeName());
        detailEmployeeIdLabel.setText(String.valueOf(payslip.getEmployeeId()));
        detailDepartmentLabel.setText(UiFormatter.safe(payslip.getDepartmentName(), "—"));
        detailPositionLabel.setText(UiFormatter.safe(payslip.getPositionName(), "—"));
        detailPeriodLabel.setText(DATE_FMT.format(payslip.getStartDate())
                + " - " + DATE_FMT.format(payslip.getEndDate()));
        detailGrossLabel.setText(UiFormatter.formatPeso(payslip.getGrossPay()));
        detailDeductionsLabel.setText(UiFormatter.formatPeso(payslip.getTotalDeductions()));
        detailNetPayLabel.setText(UiFormatter.formatPeso(payslip.getNetPay()));
    }

    // builds the read-only text summary shown before opening the PDF
    private void populatePreview(PayslipReportDTO payslip) {
        String text = "MotorPH Payroll System\n"
                + "Payslip Preview\n\n"
                + "Employee: " + payslip.getEmployeeName() + "\n"
                + "Employee ID: " + payslip.getEmployeeId() + "\n"
                + "Department: " + UiFormatter.safe(payslip.getDepartmentName(), "—") + "\n"
                + "Position: " + UiFormatter.safe(payslip.getPositionName(), "—") + "\n"
                + "Payrun: " + payslip.getPayrunId() + "\n"
                + "Payroll Period: " + DATE_FMT.format(payslip.getStartDate())
                        + " - " + DATE_FMT.format(payslip.getEndDate()) + "\n"
                + "Pay Date: " + DATE_FMT.format(payslip.getPayDate()) + "\n\n"
                + "Gross Pay: " + UiFormatter.formatPeso(payslip.getGrossPay()) + "\n"
                + "Total Deductions: " + UiFormatter.formatPeso(payslip.getTotalDeductions()) + "\n"
                + "Net Pay: " + UiFormatter.formatPeso(payslip.getNetPay()) + "\n"
                + "Generated: " + STAMP_FMT.format(java.time.LocalDateTime.now());
        previewArea.setText(text);
    }

    // clears the result cards and disables actions until a payslip is loaded
    private void clearResult() {
        currentPayslip = null;
        detailNameLabel.setText("—");
        detailEmployeeIdLabel.setText("—");
        detailDepartmentLabel.setText("—");
        detailPositionLabel.setText("—");
        detailPeriodLabel.setText("—");
        detailGrossLabel.setText("—");
        detailDeductionsLabel.setText("—");
        detailNetPayLabel.setText("—");
        previewArea.setText("Select an employee and payrun, then click Search to load a payslip.");
        setActionsDisabled(true);
    }

    // toggles the preview/export/print buttons
    private void setActionsDisabled(boolean disabled) {
        previewButton.setDisable(disabled);
        exportButton.setDisable(disabled);
        printButton.setDisable(disabled);
    }

    // surfaces a validation/error notification in the window's toast corner
    private void showError(String header, String message) {
        UiMessageUtil.showErrorToastTop(toastRoot, message);
    }
}
