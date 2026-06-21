package motorph.ui.finance;

import motorph.application.dto.report.payroll.PayslipReportDTO;
import motorph.application.service.EmployeeService;
import motorph.application.service.PayrollService;
import motorph.application.service.report.PayrollReportService;
import motorph.domain.model.Employee;
import motorph.domain.model.Payrun;
import motorph.ui.factory.FinanceUIServiceFactory;
import motorph.ui.util.UiFormatter;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Finance-owned screen to search, preview, export, and print existing persisted payslips. */
public class FinanceViewPayslipController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private EmployeeService employeeService;
    private PayrollService payrollService;
    private PayrollReportService payrollReportService;

    private PayslipReportDTO currentPayslip;

    // root / search
    @FXML private StackPane toastRoot;
    @FXML private ComboBox<Employee> employeeCombo;
    @FXML private ComboBox<Payrun> payrunCombo;
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
        FinanceUIServiceFactory factory = FinanceUIServiceFactory.getInstance();
        employeeService      = factory.getEmployeeService();
        payrollService       = factory.getPayrollService();
        payrollReportService = factory.getPayrollReportService();

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
        Employee employee = employeeCombo.getValue();
        Payrun payrun = payrunCombo.getValue();

        if (employee == null || payrun == null) {
            showError("Select Employee and Payrun",
                    "Please select both an employee and a payrun before searching.");
            return;
        }

        try {
            currentPayslip = payrollReportService.getPayslipReport(
                    employee.getEmployeeId(), payrun.getPayrunId());
            populateDetails(currentPayslip);
            previewArea.setText("Click Preview to view the payslip summary.");
            setActionsDisabled(false);
        } catch (Exception e) {
            clearResult();
            showError("Payslip Not Found",
                    "No persisted payslip exists for this employee and payrun.\n\n"
                            + UiMessageUtil.toUserMessage(e));
        }
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
            payrollReportService.exportPayslipReportToPdf(
                    currentPayslip.getEmployeeId(), currentPayslip.getPayrunId(), target.getAbsolutePath());
            UiMessageUtil.showToast(toastRoot, "Payslip exported to " + target.getName() + ".");
        } catch (Exception e) {
            showError("Could Not Export Payslip", UiMessageUtil.toUserMessage(e));
        }
    }

    // opens the Jasper preview where the payslip can be printed
    @FXML
    private void handlePrint() {
        if (currentPayslip == null) return;
        try {
            payrollReportService.previewPayslipReport(
                    currentPayslip.getEmployeeId(), currentPayslip.getPayrunId());
        } catch (Exception e) {
            showError("Could Not Open Payslip", UiMessageUtil.toUserMessage(e));
        }
    }

    // Dropdown setup
    private void setupEmployeeCombo() {
        employeeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Employee e) {
                return e == null ? "Select Employee" : e.getEmployeeId() + " - " + e.getFullName();
            }

            @Override public Employee fromString(String value) {
                return null;
            }
        });
        loadEmployees();
    }

    private void setupPayrunCombo() {
        payrunCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Payrun p) {
                return p == null
                        ? "Select Payrun"
                        : p.getPayrunId() + " | " + DATE_FMT.format(p.getStartDate())
                                + " - " + DATE_FMT.format(p.getEndDate());
            }

            @Override public Payrun fromString(String value) {
                return null;
            }
        });
        loadPayruns();
    }

    // loads active employees into the employee dropdown
    private void loadEmployees() {
        try {
            List<Employee> employees = employeeService.getAllActiveEmployees();
            employeeCombo.getItems().setAll(employees);
        } catch (Exception e) {
            employeeCombo.getItems().clear();
        }
        employeeCombo.setValue(null);
    }

    // loads payruns that have payroll results into the payrun dropdown
    private void loadPayruns() {
        try {
            List<Payrun> payruns = payrollService.getPayrunsWithPayrollData();
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

    // builds the read-only text summary shown before opening the Jasper preview
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
                + "Total Hours Worked: " + String.format("%.2f", payslip.getTotalHoursWorked()) + "\n"
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

    // shows a blocking validation/error dialog
    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("View Payslip");
        alert.setHeaderText(header);
        if (toastRoot.getScene() != null) {
            alert.initOwner(toastRoot.getScene().getWindow());
        }
        alert.showAndWait();
    }
}
