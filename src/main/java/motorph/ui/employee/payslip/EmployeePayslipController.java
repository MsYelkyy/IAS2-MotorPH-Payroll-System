package motorph.ui.employee.payslip;

import motorph.application.service.EmployeeService;
import motorph.application.service.PayrollService;
import motorph.domain.model.Employee;
import motorph.domain.model.PayrollResult;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiFormatter;
import motorph.ui.util.UiMessageUtil;
import motorph.ui.util.UiValidationUtil;

import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Handles payslip search, preview, export, and printing for the current employee. */
public class EmployeePayslipController {

    private PayrollService payrollService;
    private EmployeeService employeeService;
    private PayrollResult currentPayslipResult;

    @FXML
    private TextField payslipSearchField;

    @FXML
    private Label payslipGrossPayLabel;

    @FXML
    private Label payslipDeductionsLabel;

    @FXML
    private Label payslipNetPayLabel;

    @FXML
    private Label payslipMessageLabel;

    @FXML
    private void initialize() {
        payrollService  = EmployeeUIServiceFactory.getInstance().getPayrollService();
        employeeService = EmployeeUIServiceFactory.getInstance().getEmployeeService();
    }

    // loads the most recent payslip on view activation
    public void refresh() {
        handleSearchPayslip();
    }

    @FXML
    private void handleSearchPayslip() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) return;

        UiValidationUtil.clearInvalid(payslipSearchField);

        try {
            PayrollResult result;
            String search = UiValidationUtil.getText(payslipSearchField);

            if (search.isBlank()) {
                result = payrollService.getPayrollResultsByEmployee(employeeId)
                        .stream()
                        .max(Comparator.comparing(PayrollResult::getGeneratedAt))
                        .orElse(null);
            } else {
                result = payrollService.getPayrollResult(employeeId, Integer.parseInt(search));
            }

            if (result == null) {
                currentPayslipResult = null;
                clearPayslipDisplay();
                UiMessageUtil.setError(payslipMessageLabel, "No persisted payslip found for this employee.");
                return;
            }
            displayPayslipResult(result);
        } catch (NumberFormatException e) {
            UiValidationUtil.markInvalid(payslipSearchField);
            UiMessageUtil.setError(payslipMessageLabel, "Enter a numeric payrun ID.");
        } catch (Exception e) {
            UiMessageUtil.setError(payslipMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handlePreviewPayslip() {
        if (!hasCurrentResult()) return;

        TextArea preview = new TextArea(buildPayslipText());
        preview.setEditable(false);
        preview.setWrapText(false);
        preview.setPrefColumnCount(56);
        preview.setPrefRowCount(22);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MotorPH Payslip Preview");
        alert.setHeaderText("Payslip Preview");
        alert.getDialogPane().setContent(preview);
        alert.showAndWait();
    }

    @FXML
    private void handleExportPayslip() {
        if (!hasCurrentResult()) return;
        try {
            Path outputDir = Path.of("target", "payslips");
            Files.createDirectories(outputDir);
            String fileName = "motorph-payslip-employee-"
                    + currentPayslipResult.getEmployeeId()
                    + "-payrun-"
                    + currentPayslipResult.getPayrunId()
                    + ".txt";
            Path outputFile = outputDir.resolve(fileName);
            Files.writeString(outputFile, buildPayslipText());
            UiMessageUtil.setSuccess(payslipMessageLabel, "Payslip exported to " + outputFile.toAbsolutePath() + ".");
        } catch (IOException e) {
            UiMessageUtil.setError(payslipMessageLabel, "Unable to export payslip file.");
        }
    }

    @FXML
    private void handlePrintPayslip() {
        if (!hasCurrentResult()) return;

        TextArea printView = new TextArea(buildPayslipText());
        printView.setEditable(false);
        printView.setWrapText(false);
        printView.setPrefColumnCount(68);
        printView.setPrefRowCount(28);

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            UiMessageUtil.setError(payslipMessageLabel, "No printer is available.");
            return;
        }
        boolean accepted = job.showPrintDialog(payslipSearchField.getScene().getWindow());
        if (!accepted) {
            UiMessageUtil.setError(payslipMessageLabel, "Print cancelled.");
            return;
        }
        if (job.printPage(printView)) {
            job.endJob();
            UiMessageUtil.setSuccess(payslipMessageLabel, "Payslip sent to printer.");
        } else {
            UiMessageUtil.setError(payslipMessageLabel, "Unable to print payslip.");
        }
    }

    // shows payslip data in the preview labels and keeps a reference to the result
    private void displayPayslipResult(PayrollResult result) {
        currentPayslipResult = result;
        if (payslipGrossPayLabel   != null) payslipGrossPayLabel.setText("Gross Pay: " + UiFormatter.formatPeso(result.getGrossPay()));
        if (payslipDeductionsLabel != null) payslipDeductionsLabel.setText("Deductions: " + UiFormatter.formatPeso(result.getTotalDeductions()));
        if (payslipNetPayLabel     != null) payslipNetPayLabel.setText("Net Pay: " + UiFormatter.formatPeso(result.getNetPay()));
        UiMessageUtil.setSuccess(payslipMessageLabel,
                "Loaded payrun #" + result.getPayrunId() + " generated " + result.getGeneratedAt().toLocalDate() + ".");
    }

    // clears the payslip display labels to zero
    private void clearPayslipDisplay() {
        if (payslipGrossPayLabel   != null) payslipGrossPayLabel.setText("Gross Pay: PHP 0.00");
        if (payslipDeductionsLabel != null) payslipDeductionsLabel.setText("Deductions: PHP 0.00");
        if (payslipNetPayLabel     != null) payslipNetPayLabel.setText("Net Pay: PHP 0.00");
    }

    // returns true if a payslip result is loaded, or shows an error otherwise
    private boolean hasCurrentResult() {
        if (currentPayslipResult != null) return true;
        UiMessageUtil.setError(payslipMessageLabel, "Search and load a payslip before previewing, exporting, or printing.");
        return false;
    }

    // builds a plain-text payslip using name and email from the session/service
    private String buildPayslipText() {
        int employeeId = currentPayslipResult.getEmployeeId();
        String name  = resolveEmployeeName(employeeId);
        String email = UiFormatter.safe(CurrentUserSession.getCompanyEmail(), "employee@motorph.com");
        return UiFormatter.buildPayslipText(currentPayslipResult, name, email);
    }

    // resolves the employee's full name from the service, with a safe fallback
    private String resolveEmployeeName(int employeeId) {
        try {
            Employee employee = employeeService.getEmployeeById(employeeId);
            return employee.getFullName();
        } catch (Exception e) {
            return "Employee";
        }
    }

    // extracts a user-friendly message from an exception
    private String userMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return "Unable to complete the request. Please try again.";
        if (isDatabaseUnavailable(e)) return "Database connection unavailable. Start MySQL and try again.";
        return msg;
    }

    // checks whether the exception signals a database connectivity failure
    private boolean isDatabaseUnavailable(Exception e) {
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && (msg.contains("Failed to connect to the database")
                    || msg.contains("Communications link failure")
                    || msg.contains("Connection refused"))) return true;
            current = current.getCause();
        }
        return false;
    }
}
