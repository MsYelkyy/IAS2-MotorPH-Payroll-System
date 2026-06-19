package motorph.ui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.print.PrinterJob;
import motorph.application.dto.employee.EmployeeProfileDTO;
import motorph.application.dto.login.LoginRequestDTO;
import motorph.application.dto.login.LoginResultDTO;
import motorph.application.exception.AuthenticationException;
import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.application.service.AttendanceService;
import motorph.application.service.AuthenticationService;
import motorph.application.service.EmailService;
import motorph.application.service.EmployeeService;
import motorph.application.service.LeaveService;
import motorph.application.service.PasswordResetService;
import motorph.application.service.PayrollService;
import motorph.application.service.TwoFactorService;
import motorph.application.service.UserAccessLogService;
import motorph.domain.calculator.PayrollCalculator;
import motorph.domain.model.Employee;
import motorph.domain.model.PayrollResult;
import motorph.domain.model.Payrun;
import motorph.domain.value.PayrollType;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.AttendanceRepository;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.GovernmentDetailsRepository;
import motorph.infrastructure.repository.LeaveApprovalRepository;
import motorph.infrastructure.repository.LeaveCreditRepository;
import motorph.infrastructure.repository.LeaveRepository;
import motorph.infrastructure.repository.PasswordResetRequestRepository;
import motorph.infrastructure.repository.PayrollResultRepository;
import motorph.infrastructure.repository.PayrunRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;
import motorph.infrastructure.repository.TwoFactorCodeRepository;
import motorph.infrastructure.repository.UserAccessLogRepository;
import motorph.infrastructure.repository.UserAccountRepository;
import motorph.infrastructure.config.DatabaseConnection;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class LoginController {

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;
    private final EmployeeService employeeService;
    private final PayrollService payrollService;
    private final PayrunRepository payrunRepository;

    private LoginResultDTO pendingLogin;
    private int pendingResetRequestId;
    private int currentEmployeeId;
    private String currentEmployeeName = "Finance Officer";
    private String currentCompanyEmail = "finance@motorph.com";
    private Role currentRole = Role.FINANCE;

    @FXML private StackPane rootCanvas;
    @FXML private HBox loginView;
    @FXML private HBox resetView;
    @FXML private HBox appView;
    @FXML private VBox loginFormBox;
    @FXML private VBox otpFormBox;
    @FXML private VBox resetEmailBox;
    @FXML private VBox resetOtpBox;
    @FXML private VBox resetPasswordBox;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField otpField;
    @FXML private TextField resetEmailField;
    @FXML private TextField resetOtpField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label loginMessageLabel;
    @FXML private Label resetMessageLabel;

    @FXML private Label sidebarNameLabel;
    @FXML private Label sidebarRoleLabel;
    @FXML private Label sidebarEmailLabel;
    @FXML private Label footerStatusLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private ComboBox<String> languageCombo;
    @FXML private ComboBox<String> themeCombo;

    @FXML private Button dashboardNavButton;
    @FXML private Button payrollNavButton;
    @FXML private Button payslipNavButton;
    @FXML private Button reportsNavButton;
    @FXML private Button settingsNavButton;

    @FXML private VBox dashboardView;
    @FXML private VBox payrollView;
    @FXML private VBox payslipView;
    @FXML private VBox reportsView;
    @FXML private VBox settingsView;

    @FXML private Label latestPayrunValueLabel;
    @FXML private Label upcomingPayDateValueLabel;
    @FXML private Label pendingPeriodsValueLabel;
    @FXML private Label generatedPayrollValueLabel;
    @FXML private Label totalNetPayValueLabel;
    @FXML private Label recentActivityLabel;
    @FXML private BarChart<String, Number> payrollBarChart;
    @FXML private CategoryAxis payrollBarXAxis;
    @FXML private NumberAxis payrollBarYAxis;
    @FXML private GridPane dashboardActivityGrid;

    @FXML private DatePicker payrunStartDatePicker;
    @FXML private DatePicker payrunEndDatePicker;
    @FXML private DatePicker payrunPayDatePicker;
    @FXML private ComboBox<String> payrollTypeCombo;
    @FXML private TextField generateEmployeeIdField;
    @FXML private GridPane payrunGrid;
    @FXML private GridPane generatedPayrollGrid;
    @FXML private Label payrollSummaryLabel;

    @FXML private TextField payslipSearchField;
    @FXML private ComboBox<String> payslipPayrunCombo;
    @FXML private Label payslipEmployeeNameLabel;
    @FXML private Label payslipEmployeeIdLabel;
    @FXML private Label payslipDepartmentLabel;
    @FXML private Label payslipPositionLabel;
    @FXML private Label payslipPeriodLabel;
    @FXML private Label payslipGrossLabel;
    @FXML private Label payslipDeductionsLabel;
    @FXML private Label payslipNetLabel;
    @FXML private TextArea payslipPreviewArea;

    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private ComboBox<String> reportDepartmentCombo;
    @FXML private ComboBox<String> reportPayrunCombo;
    @FXML private TextField reportEmployeeField;
    @FXML private DatePicker reportStartDatePicker;
    @FXML private DatePicker reportEndDatePicker;
    @FXML private TextArea reportPreviewArea;

    @FXML private Label settingsNameLabel;
    @FXML private Label settingsRoleLabel;
    @FXML private Label settingsEmailLabel;
    @FXML private Label settingsDataSourceLabel;

    private JasperPrint currentReportPrint;

    public LoginController() {
        EmployeeRepository employeeRepository = new EmployeeRepository();
        UserAccountRepository userAccountRepository = new UserAccountRepository();
        TwoFactorCodeRepository twoFactorCodeRepository = new TwoFactorCodeRepository();
        EmailService emailService = new EmailService("motorph@example.com", "", true);
        UserAccessLogService accessLogService = new UserAccessLogService(new UserAccessLogRepository());
        TwoFactorService twoFactorService = new TwoFactorService(twoFactorCodeRepository);

        authenticationService = new AuthenticationService(
                userAccountRepository,
                employeeRepository,
                twoFactorService,
                emailService,
                accessLogService
        );
        passwordResetService = new PasswordResetService(
                new PasswordResetRequestRepository(),
                userAccountRepository,
                employeeRepository,
                twoFactorService,
                emailService
        );
        employeeService = new EmployeeService(employeeRepository, userAccountRepository);
        AttendanceService attendanceService = new AttendanceService(new AttendanceRepository());
        LeaveService leaveService = new LeaveService(
                new LeaveRepository(),
                new LeaveCreditRepository(),
                new LeaveApprovalRepository(),
                employeeService
        );
        leaveService.toString();

        payrunRepository = new PayrunRepository();
        payrollService = new PayrollService(
                payrunRepository,
                new PayrollResultRepository(),
                new SalaryHistoryRepository(),
                new EmployeeBenefitRepository(),
                new GovernmentDetailsRepository(),
                attendanceService,
                employeeService,
                new PayrollCalculator()
        );
    }

    @FXML
    private void initialize() {
        setupGlobalControls();
        setupFinanceControls();
        showLogin();
    }

    @FXML
    private void handleLogin() {
        clearLoginMessage();
        String email = text(emailField);
        String password = text(passwordField);

        if (email.isBlank() || password.isBlank()) {
            showLoginError("Company email and password are required.");
            return;
        }

        if (!email.endsWith("@motorph.com")) {
            showLoginError("Use your MotorPH company email.");
            return;
        }

        try {
            pendingLogin = authenticationService.authenticate(new LoginRequestDTO(email, password));
            showOtpForm(pendingLogin.getMessage());
        } catch (RuntimeException ex) {
            showLoginError(readable(ex));
        }
    }

    @FXML
    private void handleVerifyOtp() {
        clearLoginMessage();

        if (pendingLogin == null) {
            showLoginError("Sign in first before entering an OTP.");
            return;
        }

        String otp = text(otpField);
        if (!otp.matches("\\d{6}")) {
            showLoginError("Enter a valid six-digit OTP.");
            return;
        }

        try {
            LoginResultDTO result = authenticationService.verifyLoginOtp(pendingLogin.getUserAccountId(), otp);

            if (result.getRole() != Role.FINANCE && result.getRole() != Role.ADMIN) {
                showLoginError("This workspace is available to Finance users only.");
                return;
            }

            startFinanceSession(result);
        } catch (RuntimeException ex) {
            showLoginError(readable(ex));
        }
    }

    @FXML
    private void showForgotPassword() {
        loginView.setVisible(false);
        loginView.setManaged(false);
        appView.setVisible(false);
        appView.setManaged(false);
        resetView.setVisible(true);
        resetView.setManaged(true);
        resetEmailBox.setVisible(true);
        resetEmailBox.setManaged(true);
        resetOtpBox.setVisible(false);
        resetOtpBox.setManaged(false);
        resetPasswordBox.setVisible(false);
        resetPasswordBox.setManaged(false);
        resetMessageLabel.setText("");
    }

    @FXML
    private void handleSendResetOtp() {
        clearResetMessage();
        String email = text(resetEmailField);

        if (email.isBlank() || !email.endsWith("@motorph.com")) {
            showResetError("Enter a valid MotorPH company email.");
            return;
        }

        try {
            pendingResetRequestId = passwordResetService.requestPasswordReset(email);
            showResetOtpStep("Reset OTP sent to the registered personal email.");
        } catch (RuntimeException ex) {
            showResetError(readable(ex));
        }
    }

    @FXML
    private void handleVerifyResetOtp() {
        clearResetMessage();
        String otp = text(resetOtpField);

        if (pendingResetRequestId <= 0) {
            showResetError("Request a reset OTP first.");
            return;
        }

        if (!otp.matches("\\d{6}")) {
            showResetError("Enter a valid six-digit OTP.");
            return;
        }

        try {
            passwordResetService.verifyResetOtp(pendingResetRequestId, otp);
            resetOtpBox.setVisible(false);
            resetOtpBox.setManaged(false);
            resetPasswordBox.setVisible(true);
            resetPasswordBox.setManaged(true);
            showResetSuccess("OTP verified. Enter your new password.");
        } catch (RuntimeException ex) {
            showResetError(readable(ex));
        }
    }

    @FXML
    private void handleSaveNewPassword() {
        clearResetMessage();
        String newPassword = text(newPasswordField);
        String confirmPassword = text(confirmPasswordField);

        if (pendingResetRequestId <= 0) {
            showResetError("Password reset request is missing.");
            return;
        }

        if (newPassword.length() < 8) {
            showResetError("New password must be at least eight characters.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showResetError("New password and confirm password do not match.");
            return;
        }

        try {
            passwordResetService.resetPassword(pendingResetRequestId, newPassword, confirmPassword);
            showLogin();
            showLoginSuccess("Password reset completed. Sign in with your new password.");
        } catch (RuntimeException ex) {
            showResetError(readable(ex));
        }
    }

    @FXML
    private void showLogin() {
        resetView.setVisible(false);
        resetView.setManaged(false);
        appView.setVisible(false);
        appView.setManaged(false);
        loginView.setVisible(true);
        loginView.setManaged(true);
        loginFormBox.setVisible(true);
        loginFormBox.setManaged(true);
        otpFormBox.setVisible(false);
        otpFormBox.setManaged(false);
    }

    @FXML
    private void showDashboard() {
        showView(dashboardView, dashboardNavButton, "Finance Dashboard", "PAYROLL MONITORING");
        refreshDashboard();
    }

    @FXML
    private void showPayrollManagement() {
        showView(payrollView, payrollNavButton, "Payroll Management", "CREATE, GENERATE, AND REVIEW PAYRUNS");
        refreshPayrunTables();
    }

    @FXML
    private void showGeneratePayslip() {
        showView(payslipView, payslipNavButton, "Generate Payslip", "SEARCH, PREVIEW, EXPORT, AND PRINT");
        refreshPayslipPayrunCombo();
    }

    @FXML
    private void showPayrollReports() {
        showView(reportsView, reportsNavButton, "Payroll Reports", "SUMMARY AND DEPARTMENT MONITORING");
        refreshReportPayrunCombo();
        currentReportPrint = null;
        reportPreviewArea.setText("Select a report template and filters, then generate a preview.");
    }

    @FXML
    private void showSettings() {
        showView(settingsView, settingsNavButton, "Account Settings", "FINANCE USER AND SESSION DETAILS");
        settingsNameLabel.setText(currentEmployeeName);
        settingsRoleLabel.setText(currentRole.name());
        settingsEmailLabel.setText(currentCompanyEmail);
        settingsDataSourceLabel.setText("Connected to persisted payroll data");
    }

    @FXML
    private void handleCreatePayrun() {
        LocalDate start = payrunStartDatePicker.getValue();
        LocalDate end = payrunEndDatePicker.getValue();
        LocalDate payDate = payrunPayDatePicker.getValue();
        String typeText = payrollTypeCombo.getValue();

        if (start == null || end == null || payDate == null || typeText == null) {
            showFooterError("Complete the period, pay date, and payroll type.");
            return;
        }

        if (end.isBefore(start)) {
            showFooterError("End date cannot be before start date.");
            return;
        }

        try {
            PayrollType type = PayrollType.fromValue(typeText);
            if (payrunRepository.existsByPeriod(start, end)) {
                showFooterError("A payrun already exists for this payroll period.");
                return;
            }
            payrunRepository.save(new Payrun(null, start, end, payDate, type));
            showFooterSuccess("Payrun saved for " + formatDate(start) + " to " + formatDate(end) + ".");
            refreshDashboard();
            refreshPayrunTables();
            refreshPayslipPayrunCombo();
        } catch (RuntimeException ex) {
            showFooterError(readable(ex));
        }
    }

    @FXML
    private void handleGeneratePayroll() {
        Optional<Payrun> payrun = latestPayrun();

        if (payrun.isEmpty()) {
            showFooterError("Create or load a payrun before generating payroll.");
            return;
        }

        generatePayrollForPayrun(payrun.get());
    }

    @FXML
    private void handleSearchPayslip() {
        String query = text(payslipSearchField);
        String selectedPayrun = payslipPayrunCombo.getValue();

        if (query.isBlank()) {
            showFooterError("Enter an employee ID or employee name.");
            return;
        }

        try {
            Employee employee = findEmployee(query);
            Payrun payrun = selectedPayrun == null || selectedPayrun.isBlank()
                    ? latestPayrun().orElseThrow(() -> new EntityNotFoundException("Payrun not found."))
                    : payrunByComboText(selectedPayrun);
            PayrollResult result = payrollService.getPayrollResult(employee.getEmployeeId(), payrun.getPayrunId());

            fillPayslip(employee, payrun, result);
            showFooterSuccess("Payslip loaded from persisted payroll results.");
        } catch (RuntimeException ex) {
            showFooterError(readable(ex));
        }
    }

    @FXML
    private void handlePreviewPayslip() {
        if (payslipPreviewArea.getText().isBlank()) {
            showFooterError("Search for a persisted payroll result first.");
            return;
        }
        showInfo("Payslip Preview", payslipPreviewArea.getText());
    }

    @FXML
    private void handleExportPayslip() {
        exportPdf("payslips", "payslip-preview.pdf", payslipPreviewArea.getText());
    }

    @FXML
    private void handlePrintPayslip() {
        if (payslipPreviewArea.getText().isBlank()) {
            showFooterError("Search for a payslip before printing.");
            return;
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showFooterError("No printer is available.");
            return;
        }

        Text printText = new Text(payslipPreviewArea.getText());
        printText.setWrappingWidth(520);

        boolean confirmed = job.showPrintDialog(rootCanvas.getScene().getWindow());
        if (confirmed && job.printPage(printText)) {
            job.endJob();
            showFooterSuccess("Payslip sent to the print dialog.");
            return;
        }

        showFooterError("Payslip printing was cancelled.");
    }

    @FXML
    private void handleGenerateReport() {
        String reportType = reportTypeCombo.getValue();
        LocalDate start = reportStartDatePicker.getValue();
        LocalDate end = reportEndDatePicker.getValue();

        if (reportType == null || start == null || end == null) {
            showFooterError("Choose a report type and date range.");
            return;
        }

        if (end.isBefore(start)) {
            showFooterError("Report end date cannot be before start date.");
            return;
        }

        try {
            List<ReportRow> rows = reportRows(start, end, selectedReportPayrun());
            currentReportPrint = buildJasperReport(reportType, start, end, rows);
            reportPreviewArea.setText(buildReportPreview(reportType, start, end, rows));
            showFooterSuccess("JasperReports preview generated from persisted payroll data.");
        } catch (RuntimeException ex) {
            currentReportPrint = null;
            showFooterError(readable(ex));
        }
    }

    @FXML
    private void handleExportReport() {
        if (currentReportPrint == null) {
            showFooterError("Generate a JasperReports preview before exporting.");
            return;
        }

        try {
            Path folder = Path.of("target", "payroll-reports");
            Files.createDirectories(folder);
            Path file = folder.resolve("payroll-report-preview.pdf");
            JasperExportManager.exportReportToPdfFile(currentReportPrint, file.toString());
            openFile(file);
            showFooterSuccess("Exported JasperReports PDF to " + file + ".");
        } catch (IOException | JRException ex) {
            showFooterError("Unable to export JasperReports PDF.");
        }
    }

    @FXML
    private void handlePrintReport() {
        if (currentReportPrint == null) {
            showFooterError("Generate a JasperReports preview before printing.");
            return;
        }

        try {
            JasperPrintManager.printReport(currentReportPrint, true);
            showFooterSuccess("Report sent to the print dialog.");
        } catch (JRException ex) {
            showFooterError("Unable to open report print dialog.");
        }
    }

    @FXML
    private void handleSwitchRole() {
        List<Role> roles = availableRolesForCurrentUser();
        if (roles.size() <= 1) {
            showFooterError("No additional assigned roles are available for this account.");
            return;
        }

        ChoiceDialog<Role> dialog = new ChoiceDialog<>(currentRole, roles);
        dialog.setTitle("Switch Role");
        dialog.setHeaderText("Select an assigned role");
        dialog.setContentText("Role");
        dialog.showAndWait().ifPresent(role -> {
            currentRole = role;
            sidebarRoleLabel.setText(role.name());
            settingsRoleLabel.setText(role.name());
            showFooterSuccess("Switched active role to " + role.name() + " without logging out.");
        });
    }

    private void generatePayrollForPayrun(Payrun payrun) {
        if (payrun == null) {
            showFooterError("Select a payrun before generating payroll.");
            return;
        }

        int generated = 0;
        int skipped = 0;
        int failed = 0;

        List<Employee> employees;
        try {
            employees = employeeService.getAllActiveEmployees();
        } catch (RuntimeException ex) {
            showFooterError(readable(ex));
            return;
        }

        for (Employee employee : employees) {
            try {
                payrollService.runPayrollForEmployee(employee.getEmployeeId(), payrun.getPayrunId());
                generated++;
            } catch (BusinessRuleException ex) {
                skipped++;
            } catch (RuntimeException ex) {
                failed++;
            }
        }

        payrollSummaryLabel.setText("Payrun " + payrun.getPayrunId()
                + ": generated " + generated
                + ", skipped existing " + skipped
                + ", failed " + failed + ".");

        if (generated > 0 || skipped > 0) {
            showFooterSuccess("Payroll generation completed for payrun " + payrun.getPayrunId() + ".");
        } else {
            showFooterError("No payroll results were generated for payrun " + payrun.getPayrunId() + ".");
        }

        refreshDashboard();
        refreshPayrunTables();
        refreshPayslipPayrunCombo();
        refreshReportPayrunCombo();
    }

    private void showPayrunDetails(Payrun payrun) {
        List<PayrollResult> results = loadPayrollResults(payrun.getPayrunId());
        StringBuilder details = new StringBuilder();
        details.append("Payrun ").append(payrun.getPayrunId()).append('\n');
        details.append("Date Covered: ").append(formatDate(payrun.getStartDate())).append(" - ").append(formatDate(payrun.getEndDate())).append('\n');
        details.append("Pay Date: ").append(formatDate(payrun.getPayDate())).append('\n');
        details.append("Payroll Type: ").append(payrun.getPayrollType()).append("\n\n");
        details.append("Employee | Gross | Deductions | Net\n");

        for (PayrollResult result : results) {
            details.append(result.getEmployeeId()).append(" | ")
                    .append(money(result.getGrossPay())).append(" | ")
                    .append(money(result.getTotalDeductions())).append(" | ")
                    .append(money(result.getNetPay())).append('\n');
        }

        showInfo("Generated Payrolls", details.toString());
    }

    @FXML
    private void handleLogout() {
        currentEmployeeId = 0;
        currentEmployeeName = "Finance Officer";
        currentCompanyEmail = "finance@motorph.com";
        currentRole = Role.FINANCE;
        pendingLogin = null;
        clearFinanceFields();
        showLogin();
        showLoginSuccess("Signed out successfully.");
    }

    private void setupGlobalControls() {
        languageCombo.setItems(FXCollections.observableArrayList("English"));
        languageCombo.getSelectionModel().select("English");
        themeCombo.setItems(FXCollections.observableArrayList("System", "Dark", "Light"));
        themeCombo.getSelectionModel().select("System");
        themeCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyTheme(newValue));
    }

    private void setupFinanceControls() {
        payrollTypeCombo.setItems(FXCollections.observableArrayList("SEMI_MONTHLY", "MONTHLY", "WEEKLY", "BI_WEEKLY"));
        payrollTypeCombo.getSelectionModel().select("SEMI_MONTHLY");
        reportTypeCombo.setItems(FXCollections.observableArrayList(
                "Payroll Summary Report",
                "Department Payroll Summary",
                "Organization Payroll Register"
        ));
        reportDepartmentCombo.setItems(FXCollections.observableArrayList("All Departments", "Human Resources", "Finance", "Operations", "IT"));
        reportDepartmentCombo.getSelectionModel().select("All Departments");
        reportPayrunCombo.setItems(FXCollections.observableArrayList("All Payruns"));
        reportPayrunCombo.getSelectionModel().select("All Payruns");

        LocalDate today = LocalDate.now();
        payrunStartDatePicker.setValue(today.withDayOfMonth(1));
        payrunEndDatePicker.setValue(today.withDayOfMonth(Math.min(today.lengthOfMonth(), 15)));
        payrunPayDatePicker.setValue(today.withDayOfMonth(Math.min(today.lengthOfMonth(), 30)));
        reportStartDatePicker.setValue(today.withDayOfMonth(1));
        reportEndDatePicker.setValue(today);
        seedPayrollChart(List.of());
    }

    private void startFinanceSession(LoginResultDTO result) {
        currentEmployeeId = result.getEmployeeId();
        currentCompanyEmail = result.getCompanyEmail();
        currentRole = result.getRole();
        currentEmployeeName = resolveEmployeeName(result.getEmployeeId());

        sidebarNameLabel.setText(currentEmployeeName);
        sidebarRoleLabel.setText(currentRole.name());
        sidebarEmailLabel.setText(currentCompanyEmail);

        loginView.setVisible(false);
        loginView.setManaged(false);
        resetView.setVisible(false);
        resetView.setManaged(false);
        appView.setVisible(true);
        appView.setManaged(true);
        showDashboard();
        showFooterSuccess("Welcome to your finance workspace.");
    }

    private void showOtpForm(String message) {
        loginFormBox.setVisible(false);
        loginFormBox.setManaged(false);
        otpFormBox.setVisible(true);
        otpFormBox.setManaged(true);
        otpField.clear();
        showLoginSuccess(message);
    }

    private void showResetOtpStep(String message) {
        resetEmailBox.setVisible(false);
        resetEmailBox.setManaged(false);
        resetOtpBox.setVisible(true);
        resetOtpBox.setManaged(true);
        resetPasswordBox.setVisible(false);
        resetPasswordBox.setManaged(false);
        showResetSuccess(message);
    }

    private void showView(Node selectedView, Button selectedButton, String title, String subtitle) {
        List<Node> views = List.of(dashboardView, payrollView, payslipView, reportsView, settingsView);
        List<Button> buttons = List.of(dashboardNavButton, payrollNavButton, payslipNavButton, reportsNavButton, settingsNavButton);

        for (Node view : views) {
            boolean selected = view == selectedView;
            view.setVisible(selected);
            view.setManaged(selected);
        }

        for (Button button : buttons) {
            button.getStyleClass().remove("selected-nav");
        }
        selectedButton.getStyleClass().add("selected-nav");
        pageTitleLabel.setText(title);
        pageSubtitleLabel.setText(subtitle);
    }

    private void refreshDashboard() {
        List<Payrun> payruns = loadPayruns();
        List<PayrollResult> latestResults = payruns.isEmpty()
                ? List.of()
                : loadPayrollResults(payruns.get(0).getPayrunId());

        latestPayrunValueLabel.setText(payruns.isEmpty() ? "No payrun" : payruns.get(0).getPayrunId().toString());
        upcomingPayDateValueLabel.setText(nextPayDate(payruns));
        pendingPeriodsValueLabel.setText(String.valueOf(pendingPayrunCount(payruns)));
        generatedPayrollValueLabel.setText(String.valueOf(latestResults.size()));
        totalNetPayValueLabel.setText(money(latestResults.stream().mapToDouble(PayrollResult::getNetPay).sum()));
        recentActivityLabel.setText(payruns.isEmpty()
                ? "No payrun records are available."
                : "Latest payrun " + payruns.get(0).getPayrunId() + " has " + latestResults.size() + " generated payroll result(s).");

        seedPayrollChart(latestResults);
        fillActivityGrid(payruns, latestResults);
    }

    private void refreshPayrunTables() {
        List<Payrun> payruns = loadPayruns();
        fillPayrunGrid(payruns);

        if (payruns.isEmpty()) {
            generatedPayrollGrid.getChildren().clear();
            addRow(generatedPayrollGrid, 0, "Pay Date", "Date Covered", "Employees", "Gross", "Deductions", "Net", "Status", "View");
            payrollSummaryLabel.setText("No generated payroll results are available.");
            return;
        }

        List<PayrollResult> results = loadPayrollResults(payruns.get(0).getPayrunId());
        fillGeneratedPayrollGrid(payruns);
        payrollSummaryLabel.setText(results.isEmpty()
                ? "No generated payrolls found for the latest payrun."
                : results.size() + " payroll result(s) found for latest payrun " + payruns.get(0).getPayrunId() + ".");
    }

    private void refreshPayslipPayrunCombo() {
        List<String> values = loadPayruns().stream()
                .map(this::payrunComboText)
                .toList();

        payslipPayrunCombo.setItems(FXCollections.observableArrayList(values));
        if (!values.isEmpty()) {
            payslipPayrunCombo.getSelectionModel().select(0);
        }
    }

    private void refreshReportPayrunCombo() {
        List<String> values = new ArrayList<>();
        values.add("All Payruns");
        values.addAll(loadPayruns().stream().map(this::payrunComboText).toList());
        reportPayrunCombo.setItems(FXCollections.observableArrayList(values));
        reportPayrunCombo.getSelectionModel().selectFirst();
    }

    private void fillPayrunGrid(List<Payrun> payruns) {
        payrunGrid.getChildren().clear();
        addRow(payrunGrid, 0, "Payrun", "Period", "Pay Date", "Type", "Status", "Generate");

        if (payruns.isEmpty()) {
            addRow(payrunGrid, 1, "-", "No payruns found", "-", "-", "-", "-");
            return;
        }

        int row = 1;
        for (Payrun payrun : payruns.stream().limit(8).toList()) {
            addRow(
                    payrunGrid,
                    row++,
                    String.valueOf(payrun.getPayrunId()),
                    formatDate(payrun.getStartDate()) + " - " + formatDate(payrun.getEndDate()),
                    formatDate(payrun.getPayDate()),
                    payrun.getPayrollType().name(),
                    payrun.getPayDate().isAfter(LocalDate.now()) ? "Pending" : "Released"
            );
            Button generateButton = new Button("Generate");
            generateButton.getStyleClass().add("small-pill-button");
            generateButton.setOnAction(event -> generatePayrollForPayrun(payrun));
            payrunGrid.add(generateButton, 5, row - 1);
        }
    }

    private void fillGeneratedPayrollGrid(List<Payrun> payruns) {
        generatedPayrollGrid.getChildren().clear();
        addRow(generatedPayrollGrid, 0, "Pay Date", "Date Covered", "Employees", "Gross", "Deductions", "Net", "Status", "View");

        if (payruns.isEmpty()) {
            addRow(generatedPayrollGrid, 1, "-", "No payroll results found", "-", "-", "-", "-", "-");
            return;
        }

        int row = 1;
        for (Payrun payrun : payruns.stream().limit(8).toList()) {
            List<PayrollResult> results = loadPayrollResults(payrun.getPayrunId());
            if (results.isEmpty()) {
                continue;
            }

            double gross = results.stream().mapToDouble(PayrollResult::getGrossPay).sum();
            double deductions = results.stream().mapToDouble(PayrollResult::getTotalDeductions).sum();
            double net = results.stream().mapToDouble(PayrollResult::getNetPay).sum();
            addRow(
                    generatedPayrollGrid,
                    row++,
                    formatDate(payrun.getPayDate()),
                    formatDate(payrun.getStartDate()) + " - " + formatDate(payrun.getEndDate()),
                    String.valueOf(results.size()),
                    money(gross),
                    money(deductions),
                    money(net),
                    "Generated"
            );
            Button viewButton = new Button("View");
            viewButton.getStyleClass().add("small-pill-button");
            viewButton.setOnAction(event -> showPayrunDetails(payrun));
            generatedPayrollGrid.add(viewButton, 7, row - 1);
        }

        if (row == 1) {
            addRow(generatedPayrollGrid, 1, "-", "No payroll results found", "-", "-", "-", "-", "-");
        }
    }

    private void fillActivityGrid(List<Payrun> payruns, List<PayrollResult> latestResults) {
        dashboardActivityGrid.getChildren().clear();
        addRow(dashboardActivityGrid, 0, "Activity", "Reference", "Status");

        if (payruns.isEmpty()) {
            addRow(dashboardActivityGrid, 1, "Payrun", "No records found", "Ready");
            return;
        }

        addRow(dashboardActivityGrid, 1, "Latest payrun", String.valueOf(payruns.get(0).getPayrunId()), "Loaded");
        addRow(dashboardActivityGrid, 2, "Generated payrolls", String.valueOf(latestResults.size()), "Monitored");
        addRow(dashboardActivityGrid, 3, "Upcoming pay date", nextPayDate(payruns), "Scheduled");
    }

    private void addRow(GridPane grid, int row, String... values) {
        for (int column = 0; column < values.length; column++) {
            Label label = new Label(values[column]);
            label.getStyleClass().add(row == 0 ? "table-head" : "data-line");
            grid.add(label, column, row);
        }
    }

    private void seedPayrollChart(List<PayrollResult> results) {
        payrollBarChart.getData().clear();
        payrollBarXAxis.setLabel("");
        payrollBarYAxis.setLabel("");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Net Pay");

        if (results.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No Data", 0));
        } else {
            for (PayrollResult result : results.stream().limit(8).toList()) {
                series.getData().add(new XYChart.Data<>("E" + result.getEmployeeId(), result.getNetPay()));
            }
        }

        payrollBarChart.getData().add(series);
    }

    private List<Payrun> loadPayruns() {
        try {
            return payrunRepository.findAll();
        } catch (RuntimeException ex) {
            showFooterError("Unable to load payruns. Check the database connection.");
            return List.of();
        }
    }

    private List<PayrollResult> loadPayrollResults(int payrunId) {
        try {
            return payrollService.getPayrollResultsByPayrun(payrunId);
        } catch (RuntimeException ex) {
            return List.of();
        }
    }

    private Optional<Payrun> latestPayrun() {
        return loadPayruns().stream().findFirst();
    }

    private Employee findEmployee(String query) {
        if (query.matches("\\d+")) {
            return employeeService.getEmployeeById(Integer.parseInt(query));
        }

        String cleaned = query.toLowerCase(Locale.ROOT);
        return employeeService.getAllActiveEmployees()
                .stream()
                .filter(employee -> employee.getFullName().toLowerCase(Locale.ROOT).contains(cleaned))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Employee not found."));
    }

    private void fillPayslip(Employee employee, Payrun payrun, PayrollResult result) {
        String departmentName = resolveDepartmentName(employee.getDepartmentId());
        String positionName = resolvePositionName(employee.getPositionId());
        payslipEmployeeNameLabel.setText(employee.getFullName());
        payslipEmployeeIdLabel.setText(String.valueOf(employee.getEmployeeId()));
        payslipDepartmentLabel.setText(departmentName);
        payslipPositionLabel.setText(positionName);
        payslipPeriodLabel.setText(formatDate(payrun.getStartDate()) + " - " + formatDate(payrun.getEndDate()));
        payslipGrossLabel.setText(money(result.getGrossPay()));
        payslipDeductionsLabel.setText(money(result.getTotalDeductions()));
        payslipNetLabel.setText(money(result.getNetPay()));
        payslipPreviewArea.setText("""
                MotorPH Payroll System
                Payslip Preview

                Employee: %s
                Employee ID: %s
                Department: %s
                Position: %s
                Payrun: %s
                Payroll Period: %s - %s
                Pay Date: %s

                Total Hours Worked: %.2f
                Gross Pay: %s
                Total Deductions: %s
                Net Pay: %s
                Generated: %s
                """.formatted(
                employee.getFullName(),
                employee.getEmployeeId(),
                departmentName,
                positionName,
                payrun.getPayrunId(),
                formatDate(payrun.getStartDate()),
                formatDate(payrun.getEndDate()),
                formatDate(payrun.getPayDate()),
                result.getTotalHoursWorked(),
                money(result.getGrossPay()),
                money(result.getTotalDeductions()),
                money(result.getNetPay()),
                result.getGeneratedAt() == null ? "Not recorded" : result.getGeneratedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
        ));
    }

    private String buildReportPreview(String reportType, LocalDate start, LocalDate end, List<ReportRow> rows) {
        double gross = rows.stream().mapToDouble(ReportRow::getGrossPayValue).sum();
        double deductions = rows.stream().mapToDouble(ReportRow::getTotalDeductionsValue).sum();
        double net = rows.stream().mapToDouble(ReportRow::getNetPayValue).sum();
        long payrunCount = rows.stream().map(ReportRow::getPayrunId).distinct().count();

        return """
                MotorPH Payroll System
                %s

                Period: %s - %s
                Department Filter: %s
                Payrun Filter: %s
                Employee Filter: %s

                Payruns Included: %d
                Payroll Results: %d
                Gross Pay: %s
                Total Deductions: %s
                Net Pay: %s

                JasperReports template: %s
                """.formatted(
                reportType,
                formatDate(start),
                formatDate(end),
                safeValue(reportDepartmentCombo.getValue()),
                safeValue(reportPayrunCombo.getValue()),
                text(reportEmployeeField).isBlank() ? "All employees" : text(reportEmployeeField),
                payrunCount,
                rows.size(),
                money(gross),
                money(deductions),
                money(net),
                reportTemplatePath(reportType)
        );
    }

    private List<ReportRow> reportRows(LocalDate start, LocalDate end, Optional<Payrun> selectedPayrun) {
        String departmentFilter = safeValue(reportDepartmentCombo.getValue());
        String employeeFilter = text(reportEmployeeField).toLowerCase(Locale.ROOT);

        List<Payrun> payruns = selectedPayrun
                .map(List::of)
                .orElseGet(this::loadPayruns)
                .stream()
                .filter(payrun -> !payrun.getStartDate().isAfter(end) && !payrun.getEndDate().isBefore(start))
                .toList();

        List<ReportRow> rows = new ArrayList<>();
        for (Payrun payrun : payruns) {
            for (PayrollResult result : loadPayrollResults(payrun.getPayrunId())) {
                Employee employee;
                try {
                    employee = employeeService.getEmployeeById(result.getEmployeeId());
                } catch (RuntimeException ex) {
                    continue;
                }

                String department = resolveDepartmentName(employee.getDepartmentId());
                if (!departmentFilter.equals("All Departments") && !department.equalsIgnoreCase(departmentFilter)) {
                    continue;
                }

                if (!employeeFilter.isBlank()
                        && !employee.getFullName().toLowerCase(Locale.ROOT).contains(employeeFilter)
                        && !String.valueOf(employee.getEmployeeId()).contains(employeeFilter)) {
                    continue;
                }

                rows.add(new ReportRow(
                        String.valueOf(payrun.getPayrunId()),
                        formatDate(payrun.getPayDate()),
                        formatDate(payrun.getStartDate()) + " - " + formatDate(payrun.getEndDate()),
                        String.valueOf(employee.getEmployeeId()),
                        employee.getFullName(),
                        department,
                        resolvePositionName(employee.getPositionId()),
                        money(result.getGrossPay()),
                        money(result.getTotalDeductions()),
                        money(result.getNetPay()),
                        result.getGrossPay(),
                        result.getTotalDeductions(),
                        result.getNetPay()
                ));
            }
        }
        return rows;
    }

    private JasperPrint buildJasperReport(String reportType, LocalDate start, LocalDate end, List<ReportRow> rows) {
        if (rows.isEmpty()) {
            throw new BusinessRuleException("No payroll data matched the selected report filters.");
        }

        try {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_TITLE", reportType);
            parameters.put("PERIOD", formatDate(start) + " - " + formatDate(end));
            parameters.put("DEPARTMENT_FILTER", safeValue(reportDepartmentCombo.getValue()));
            parameters.put("PAYRUN_FILTER", safeValue(reportPayrunCombo.getValue()));
            parameters.put("EMPLOYEE_FILTER", text(reportEmployeeField).isBlank() ? "All employees" : text(reportEmployeeField));
            parameters.put("TOTAL_GROSS", money(rows.stream().mapToDouble(ReportRow::getGrossPayValue).sum()));
            parameters.put("TOTAL_DEDUCTIONS", money(rows.stream().mapToDouble(ReportRow::getTotalDeductionsValue).sum()));
            parameters.put("TOTAL_NET", money(rows.stream().mapToDouble(ReportRow::getNetPayValue).sum()));

            return JasperFillManager.fillReport(
                    JasperCompileManager.compileReport(buildReportDesign(reportType)),
                    parameters,
                    new JRBeanCollectionDataSource(rows)
            );
        } catch (JRException ex) {
            throw new RuntimeException("Failed to generate JasperReports preview.", ex);
        }
    }

    private JasperDesign buildReportDesign(String reportType) throws JRException {
        JasperDesign design = new JasperDesign();
        design.setName("finance_payroll_report");
        design.setPageWidth(842);
        design.setPageHeight(595);
        design.setOrientation(OrientationEnum.LANDSCAPE);
        design.setColumnWidth(802);
        design.setLeftMargin(20);
        design.setRightMargin(20);
        design.setTopMargin(20);
        design.setBottomMargin(20);

        for (String parameter : List.of(
                "REPORT_TITLE", "PERIOD", "DEPARTMENT_FILTER", "PAYRUN_FILTER",
                "EMPLOYEE_FILTER", "TOTAL_GROSS", "TOTAL_DEDUCTIONS", "TOTAL_NET")) {
            JRDesignParameter designParameter = new JRDesignParameter();
            designParameter.setName(parameter);
            designParameter.setValueClass(String.class);
            design.addParameter(designParameter);
        }

        for (String field : List.of(
                "payrunId", "payDate", "dateCovered", "employeeId", "employeeName",
                "department", "position", "grossPay", "totalDeductions", "netPay")) {
            JRDesignField designField = new JRDesignField();
            designField.setName(field);
            designField.setValueClass(String.class);
            design.addField(designField);
        }

        design.setTitle(reportTitleBand(reportType));
        design.setColumnHeader(reportHeaderBand());
        ((JRDesignSection) design.getDetailSection()).addBand(reportDetailBand());
        design.setSummary(reportSummaryBand());
        return design;
    }

    private JRDesignBand reportTitleBand(String reportType) {
        JRDesignBand band = new JRDesignBand();
        band.setHeight(84);
        band.addElement(staticText("MotorPH Payroll System", 0, 0, 280, 24, 18, true));
        band.addElement(textField("$P{REPORT_TITLE}", 0, 28, 330, 20, 12, true));
        band.addElement(textField("\"Period: \" + $P{PERIOD} + \" | Department: \" + $P{DEPARTMENT_FILTER} + \" | Payrun: \" + $P{PAYRUN_FILTER}", 0, 54, 802, 16, 10, false));
        band.addElement(textField("\"Employee Filter: \" + $P{EMPLOYEE_FILTER}", 0, 70, 802, 14, 10, false));
        return band;
    }

    private JRDesignBand reportHeaderBand() {
        JRDesignBand band = new JRDesignBand();
        band.setHeight(24);
        int x = 0;
        String[][] headers = {
                {"Payrun", "54"}, {"Pay Date", "82"}, {"Date Covered", "126"},
                {"Employee", "64"}, {"Name", "124"}, {"Department", "92"},
                {"Position", "92"}, {"Gross", "54"}, {"Deductions", "70"}, {"Net", "44"}
        };
        for (String[] header : headers) {
            int width = Integer.parseInt(header[1]);
            band.addElement(staticText(header[0], x, 0, width, 18, 9, true));
            x += width + 4;
        }
        return band;
    }

    private JRDesignBand reportDetailBand() {
        JRDesignBand band = new JRDesignBand();
        band.setHeight(20);
        int x = 0;
        String[][] fields = {
                {"payrunId", "54"}, {"payDate", "82"}, {"dateCovered", "126"},
                {"employeeId", "64"}, {"employeeName", "124"}, {"department", "92"},
                {"position", "92"}, {"grossPay", "54"}, {"totalDeductions", "70"}, {"netPay", "44"}
        };
        for (String[] field : fields) {
            int width = Integer.parseInt(field[1]);
            band.addElement(textField("$F{" + field[0] + "}", x, 0, width, 16, 8, false));
            x += width + 4;
        }
        return band;
    }

    private JRDesignBand reportSummaryBand() {
        JRDesignBand band = new JRDesignBand();
        band.setHeight(46);
        JRDesignTextField totals = textField("\"Gross: \" + $P{TOTAL_GROSS} + \"   Deductions: \" + $P{TOTAL_DEDUCTIONS}", 390, 8, 412, 16, 10, true);
        totals.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        band.addElement(totals);
        JRDesignTextField net = textField("\"Net Pay: \" + $P{TOTAL_NET}", 390, 26, 412, 16, 10, true);
        net.setHorizontalTextAlign(HorizontalTextAlignEnum.RIGHT);
        band.addElement(net);
        return band;
    }

    private JRDesignStaticText staticText(String value, int x, int y, int width, int height, int size, boolean bold) {
        JRDesignStaticText text = new JRDesignStaticText();
        text.setText(value);
        text.setX(x);
        text.setY(y);
        text.setWidth(width);
        text.setHeight(height);
        text.setFontSize((float) size);
        text.setBold(bold);
        return text;
    }

    private JRDesignTextField textField(String expression, int x, int y, int width, int height, int size, boolean bold) {
        JRDesignTextField field = new JRDesignTextField();
        field.setX(x);
        field.setY(y);
        field.setWidth(width);
        field.setHeight(height);
        field.setFontSize((float) size);
        field.setBold(bold);
        field.setBlankWhenNull(true);
        field.setExpression(new JRDesignExpression(expression));
        return field;
    }

    private Optional<Payrun> selectedReportPayrun() {
        String selected = reportPayrunCombo.getValue();
        if (selected == null || selected.isBlank() || selected.equals("All Payruns")) {
            return Optional.empty();
        }
        return Optional.of(payrunByComboText(selected));
    }

    private String reportTemplatePath(String reportType) {
        if ("Department Payroll Summary".equals(reportType)) {
            return "/motorph/reports/payroll/department_payroll_summary_report.jrxml";
        }
        return "/motorph/reports/payroll/payroll_summary_report.jrxml";
    }

    private Payrun payrunByComboText(String text) {
        int id = Integer.parseInt(text.substring(0, text.indexOf(" ")).trim());
        return loadPayruns().stream()
                .filter(payrun -> payrun.getPayrunId() == id)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Payrun not found."));
    }

    private String payrunComboText(Payrun payrun) {
        return payrun.getPayrunId() + " | " + formatDate(payrun.getStartDate()) + " - " + formatDate(payrun.getEndDate());
    }

    private String resolveEmployeeName(int employeeId) {
        try {
            EmployeeProfileDTO profile = employeeService.getEmployeeProfile(employeeId);
            return profile.getFirstName() + " " + profile.getLastName();
        } catch (RuntimeException ex) {
            return "Finance Officer";
        }
    }

    private String resolveDepartmentName(int departmentId) {
        return lookupName(
                "SELECT department_name FROM department WHERE department_id = ?",
                departmentId,
                "Department #" + departmentId
        );
    }

    private String resolvePositionName(int positionId) {
        return lookupName(
                "SELECT position_name FROM position WHERE position_id = ?",
                positionId,
                "Position #" + positionId
        );
    }

    private String lookupName(String sql, int id, String fallback) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException | RuntimeException ex) {
            return fallback;
        }
        return fallback;
    }

    private List<Role> availableRolesForCurrentUser() {
        List<Role> roles = new ArrayList<>();
        String sql = """
                SELECT DISTINCT r.role_name
                FROM user_account ua
                JOIN role r ON ua.role_id = r.role_id
                WHERE ua.employee_id = ? AND ua.is_active = 1 AND ua.is_locked = 0
                ORDER BY r.role_name
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentEmployeeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(Role.fromValue(rs.getString("role_name")));
                }
            }
        } catch (SQLException | RuntimeException ex) {
            return List.of(currentRole);
        }

        if (!roles.contains(currentRole)) {
            roles.add(0, currentRole);
        }
        return roles;
    }

    private void exportPdf(String folderName, String fileName, String content) {
        if (content == null || content.isBlank()) {
            showFooterError("Generate a preview before exporting.");
            return;
        }

        try {
            Path folder = Path.of("target", folderName);
            Files.createDirectories(folder);
            Path file = folder.resolve(fileName);
            Files.write(file, buildSimplePdf(content));
            showFooterSuccess("Exported preview to " + file + ".");

            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(file.toFile());
                } catch (IOException ignored) {
                    // Export already completed; opening the file is optional.
                }
            }
        } catch (IOException ex) {
            showFooterError("Unable to export preview.");
        }
    }

    private void openFile(Path file) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }

        try {
            Desktop.getDesktop().open(file.toFile());
        } catch (IOException ignored) {
            // Export already completed; opening the file is optional.
        }
    }

    private byte[] buildSimplePdf(String text) {
        String[] rawLines = text.split("\\R");
        List<String> drawingCommands = new ArrayList<>();
        int y = 760;

        for (String rawLine : rawLines) {
            if (y < 60) {
                break;
            }
            drawingCommands.add("BT /F1 10 Tf 48 " + y + " Td (" + escapePdf(rawLine) + ") Tj ET\n");
            y -= 16;
        }

        String content = String.join("", drawingCommands);
        byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + contentBytes.length + " >>\nstream\n" + content + "endstream"
        );

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();

        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }

        int xrefOffset = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");

        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private String escapePdf(String value) {
        String safe = value.replace("₱", "PHP ");
        safe = safe.replaceAll("[^\\x20-\\x7E]", "?");
        return safe.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String nextPayDate(List<Payrun> payruns) {
        return payruns.stream()
                .map(Payrun::getPayDate)
                .filter(date -> !date.isBefore(LocalDate.now()))
                .min(Comparator.naturalOrder())
                .map(this::formatDate)
                .orElse(payruns.isEmpty() ? "No schedule" : formatDate(payruns.get(0).getPayDate()));
    }

    private int pendingPayrunCount(List<Payrun> payruns) {
        return (int) payruns.stream()
                .filter(payrun -> payrun.getPayDate().isAfter(LocalDate.now()))
                .count();
    }

    private void clearFinanceFields() {
        payslipPreviewArea.clear();
        reportPreviewArea.clear();
        currentReportPrint = null;
        payrollSummaryLabel.setText("");
        payslipEmployeeNameLabel.setText("-");
        payslipEmployeeIdLabel.setText("-");
        payslipDepartmentLabel.setText("-");
        payslipPositionLabel.setText("-");
        payslipPeriodLabel.setText("-");
        payslipGrossLabel.setText("-");
        payslipDeductionsLabel.setText("-");
        payslipNetLabel.setText("-");
    }

    private void applyTheme(String theme) {
        rootCanvas.getStyleClass().remove("light-mode");
        if ("Light".equals(theme)) {
            rootCanvas.getStyleClass().add("light-mode");
        }
    }

    private void showLoginError(String message) {
        loginMessageLabel.getStyleClass().remove("success-message");
        if (!loginMessageLabel.getStyleClass().contains("error-message")) {
            loginMessageLabel.getStyleClass().add("error-message");
        }
        loginMessageLabel.setText(message);
    }

    private void showLoginSuccess(String message) {
        loginMessageLabel.getStyleClass().remove("error-message");
        if (!loginMessageLabel.getStyleClass().contains("success-message")) {
            loginMessageLabel.getStyleClass().add("success-message");
        }
        loginMessageLabel.setText(message);
    }

    private void showResetError(String message) {
        resetMessageLabel.getStyleClass().remove("success-message");
        if (!resetMessageLabel.getStyleClass().contains("error-message")) {
            resetMessageLabel.getStyleClass().add("error-message");
        }
        resetMessageLabel.setText(message);
    }

    private void showResetSuccess(String message) {
        resetMessageLabel.getStyleClass().remove("error-message");
        if (!resetMessageLabel.getStyleClass().contains("success-message")) {
            resetMessageLabel.getStyleClass().add("success-message");
        }
        resetMessageLabel.setText(message);
    }

    private void showFooterSuccess(String message) {
        footerStatusLabel.getStyleClass().remove("error-message");
        if (!footerStatusLabel.getStyleClass().contains("success-message")) {
            footerStatusLabel.getStyleClass().add("success-message");
        }
        footerStatusLabel.setText(message);
    }

    private void showFooterError(String message) {
        footerStatusLabel.getStyleClass().remove("success-message");
        if (!footerStatusLabel.getStyleClass().contains("error-message")) {
            footerStatusLabel.getStyleClass().add("error-message");
        }
        footerStatusLabel.setText(message);
    }

    private void clearLoginMessage() {
        loginMessageLabel.setText("");
    }

    private void clearResetMessage() {
        resetMessageLabel.setText("");
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String text(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private String readable(RuntimeException ex) {
        if (ex instanceof AuthenticationException
                || ex instanceof ValidationException
                || ex instanceof BusinessRuleException
                || ex instanceof EntityNotFoundException) {
            return ex.getMessage();
        }
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            return ex.getCause().getMessage();
        }
        return ex.getMessage() == null ? "Unable to complete the action." : ex.getMessage();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    private String money(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
        return format.format(amount);
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public static class ReportRow {
        private final String payrunId;
        private final String payDate;
        private final String dateCovered;
        private final String employeeId;
        private final String employeeName;
        private final String department;
        private final String position;
        private final String grossPay;
        private final String totalDeductions;
        private final String netPay;
        private final double grossPayValue;
        private final double totalDeductionsValue;
        private final double netPayValue;

        public ReportRow(String payrunId,
                         String payDate,
                         String dateCovered,
                         String employeeId,
                         String employeeName,
                         String department,
                         String position,
                         String grossPay,
                         String totalDeductions,
                         String netPay,
                         double grossPayValue,
                         double totalDeductionsValue,
                         double netPayValue) {
            this.payrunId = payrunId;
            this.payDate = payDate;
            this.dateCovered = dateCovered;
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.department = department;
            this.position = position;
            this.grossPay = grossPay;
            this.totalDeductions = totalDeductions;
            this.netPay = netPay;
            this.grossPayValue = grossPayValue;
            this.totalDeductionsValue = totalDeductionsValue;
            this.netPayValue = netPayValue;
        }

        public String getPayrunId() { return payrunId; }
        public String getPayDate() { return payDate; }
        public String getDateCovered() { return dateCovered; }
        public String getEmployeeId() { return employeeId; }
        public String getEmployeeName() { return employeeName; }
        public String getDepartment() { return department; }
        public String getPosition() { return position; }
        public String getGrossPay() { return grossPay; }
        public String getTotalDeductions() { return totalDeductions; }
        public String getNetPay() { return netPay; }
        public double getGrossPayValue() { return grossPayValue; }
        public double getTotalDeductionsValue() { return totalDeductionsValue; }
        public double getNetPayValue() { return netPayValue; }
    }
}
