package motorph.ui.controller;

import motorph.application.dto.login.LoginRequestDTO;
import motorph.application.dto.login.LoginResultDTO;
import motorph.application.security.PasswordHasher;
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
import motorph.domain.model.Attendance;
import motorph.domain.model.Employee;
import motorph.domain.model.LeaveRequest;
import motorph.domain.model.PayrollResult;
import motorph.domain.model.UserAccount;
import motorph.domain.value.AttendanceStatus;
import motorph.domain.value.LeaveStatus;
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

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.print.PrinterJob;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class LoginController {

    private static final String MOTORPH_EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@motorph\\.com$";
    private static final String HR_DEMO_EMAIL = "hr.demo@motorph.com";
    private static final String HR_DEMO_PASSWORD = "HrDemo@2026!";
    private static final int HR_DEMO_USER_ACCOUNT_ID = 90001;
    private static final int HR_DEMO_EMPLOYEE_ID = 90001;

    private static final Map<String, String> FILIPINO_LABELS = Map.ofEntries(
            // ── Login & Auth ───────────────────────────────────────────
            Map.entry("Welcome Back", "Maligayang Pagbabalik"),
            Map.entry("Sign in to continue to your employee workspace.", "Mag-sign in upang magpatuloy sa inyong workspace."),
            Map.entry("Company Email", "Email ng Kumpanya"),
            Map.entry("Password", "Password"),
            Map.entry("Remember me", "Tandaan ako"),
            Map.entry("Forgot Password", "Nakalimutan ang Password"),
            Map.entry("Sign In", "Mag-sign In"),
            Map.entry("Protected by Two-Factor Authentication", "Protektado ng Two-Factor Authentication"),
            Map.entry("Show", "Ipakita"),
            Map.entry("Hide", "Itago"),
            // ── OTP & Password Reset ───────────────────────────────────
            Map.entry("Verify OTP", "I-verify ang OTP"),
            Map.entry("Enter the verification code sent to your registered email address.", "Ilagay ang verification code na ipinadala sa inyong registered email."),
            Map.entry("OTP Code", "OTP Code"),
            Map.entry("Verify and Continue", "I-verify at Magpatuloy"),
            Map.entry("← Back to Login", "← Bumalik sa Login"),
            Map.entry("Reset Password", "I-reset ang Password"),
            Map.entry("Enter your email to receive a reset OTP.", "Ilagay ang inyong email para makatanggap ng reset OTP."),
            Map.entry("Email", "Email"),
            Map.entry("Send Reset OTP", "Ipadala ang Reset OTP"),
            Map.entry("Verify Reset OTP", "I-verify ang Reset OTP"),
            Map.entry("Enter the reset code sent to your registered email address.", "Ilagay ang reset code na ipinadala sa inyong registered email."),
            Map.entry("← Back to Email", "← Bumalik sa Email"),
            Map.entry("Create New Password", "Lumikha ng Bagong Password"),
            Map.entry("Use at least 8 characters.", "Gumamit ng hindi bababa sa 8 na karakter."),
            Map.entry("New Password", "Bagong Password"),
            Map.entry("Confirm Password", "Kumpirmahin ang Password"),
            Map.entry("Update Password", "I-update ang Password"),
            // ── Navigation ─────────────────────────────────────────────
            Map.entry("Dashboard", "Dashboard"),
            Map.entry("Profile", "Profil"),
            Map.entry("Attendance", "Presensya"),
            Map.entry("Payslip / Payroll", "Payslip / Payroll"),
            Map.entry("Account Settings", "Mga Setting ng Account"),
            Map.entry("Logout", "Mag-logout"),
            Map.entry("EMPLOYEE", "EMPLEYADO"),
            Map.entry("Operations", "Operasyon"),
            // ── Dashboard labels ───────────────────────────────────────
            Map.entry("EMPLOYEE WORKSPACE", "WORKSPACE NG EMPLEYADO"),
            Map.entry("TOTAL NET PAY", "KABUUANG NET PAY"),
            Map.entry("ATTENDANCE RATE", "RATE NG PRESENSYA"),
            Map.entry("DAYS WORKED", "MGA ARAW NA NAGTRABAHO"),
            Map.entry("AVAILABLE LEAVE", "NALALABAS NA LEAVE"),
            Map.entry("PENDING REQUESTS", "MGA KAHILINGANG NAGHIHINTAY"),
            Map.entry("NET PAY TREND", "TREND NG NET PAY"),
            Map.entry("EARNINGS BREAKDOWN", "BREAKDOWN NG KITA"),
            Map.entry("ATTENDANCE SUMMARY", "BUOD NG PRESENSYA"),
            Map.entry("LEAVE SUMMARY", "BUOD NG LEAVE"),
            Map.entry("Vacation Leave", "Vacation Leave"),
            Map.entry("Sick Leave", "Sick Leave"),
            Map.entry("Emergency Leave", "Emergency Leave"),
            Map.entry("Special Leave", "Espesyal na Leave"),
            Map.entry("Maternity Leave", "Maternity Leave"),
            Map.entry("UPCOMING PAYDAY", "DARATING NA SAHOD"),
            Map.entry("In 26 days", "Sa loob ng 26 na araw"),
            Map.entry("RECENT PAYSLIP", "PINAKABAGONG PAYSLIP"),
            Map.entry("ANNOUNCEMENTS", "MGA ANUNSYO"),
            Map.entry("Company Meeting", "Pulong ng Kumpanya"),
            Map.entry("All employees are expected to attend.", "Lahat ng empleyado ay inaasahang dadalo."),
            Map.entry("Active Session", "Aktibong Session"),
            Map.entry("View Payslip", "Tingnan ang Payslip"),
            Map.entry("Welcome to your employee workspace.", "Maligayang pagdating sa inyong workspace."),
            Map.entry("Employee workspace is ready.", "Handa na ang workspace ng empleyado."),
            Map.entry("Please log in before using this action.", "Mangyaring mag-login bago gamitin ang aksyong ito."),
            // ── Attendance view ────────────────────────────────────────
            Map.entry("Attendance Logs", "Mga Talaan ng Presensya"),
            Map.entry("Work Date", "Petsa ng Trabaho"),
            Map.entry("Time In", "Pasok"),
            Map.entry("Time Out", "Labas"),
            Map.entry("Total Hours", "Kabuuang Oras"),
            Map.entry("Status", "Katayuan"),
            Map.entry("Pending", "Naghihintay"),
            // ── Leave view ─────────────────────────────────────────────
            Map.entry("Leave Request", "Kahilingang Leave"),
            Map.entry("Leave Type", "Uri ng Leave"),
            Map.entry("Available Credits", "Nalalabas na Credits"),
            Map.entry("Start Date", "Petsa ng Simula"),
            Map.entry("End Date", "Petsa ng Katapusan"),
            Map.entry("Leave Hours", "Oras ng Leave"),
            Map.entry("Reason", "Dahilan"),
            Map.entry("Submit Leave Request", "Isumite ang Kahilingang Leave"),
            Map.entry("Leave History", "Kasaysayan ng Leave"),
            Map.entry("Pending requests can be cancelled. Approved requests consume leave credits.", "Ang mga naghihintay na kahilingan ay maaaring kanselahin. Ang mga naaprubahang kahilingan ay kumukuha ng leave credits."),
            Map.entry("Cancel Latest Pending Request", "Kanselahin ang Pinakabagong Kahilingan"),
            // ── Payslip view ───────────────────────────────────────────
            Map.entry("Payslip Preview", "Preview ng Payslip"),
            Map.entry("Gross Pay:", "Gross Pay:"),
            Map.entry("Deductions:", "Mga Bawas:"),
            Map.entry("Net Pay:", "Net Pay:"),
            Map.entry("Search", "Hanapin"),
            Map.entry("Preview Payslip", "I-preview ang Payslip"),
            Map.entry("Export Text Payslip", "I-export ang Payslip"),
            Map.entry("Print Payslip", "I-print ang Payslip"),
            Map.entry("Employee can only view their own persisted payroll data.", "Ang empleyado ay makakakita lamang ng sariling payroll data."),
            // ── Account Settings ───────────────────────────────────────
            Map.entry("Email is view-only and remains the login identity.", "Ang email ay para sa pagtingin lamang at nananatiling login identity."),
            Map.entry("Change Password", "Palitan ang Password"),
            // ── OTP footer ─────────────────────────────────────────────
            Map.entry("MotorPH Payroll Management System  ·  v1.0", "MotorPH Payroll Management System  ·  v1.0")
    );

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final PayrollService payrollService;
    private final UserAccountRepository userAccountRepository;
    private final Map<String, Integer> leaveTypeIds = new LinkedHashMap<>();
    private final NumberFormat pesoFormat = NumberFormat.getCurrencyInstance(Locale.of("en", "PH"));

    private boolean passwordVisible = false;
    private boolean offlinePreviewMode = false;
    private boolean hrDemoMode = false;
    private boolean darkMode = true;
    private LoginResultDTO pendingLoginResult;
    private int pendingResetRequestId;
    private int currentUserAccountId;
    private int currentEmployeeId;
    private Integer latestPendingLeaveRequestId;
    private PayrollResult currentPayslipResult;

    public LoginController() {
        userAccountRepository = new UserAccountRepository();
        EmployeeRepository employeeRepository = new EmployeeRepository();
        AttendanceRepository attendanceRepository = new AttendanceRepository();
        EmployeeService sharedEmployeeService = new EmployeeService(employeeRepository, userAccountRepository);
        AttendanceService sharedAttendanceService = new AttendanceService(attendanceRepository);
        TwoFactorService twoFactorService = new TwoFactorService(new TwoFactorCodeRepository());
        EmailService emailService = new EmailService("motorph@example.com", "", true);
        UserAccessLogService accessLogService = new UserAccessLogService(new UserAccessLogRepository());

        employeeService = sharedEmployeeService;
        attendanceService = sharedAttendanceService;
        leaveService = new LeaveService(
                new LeaveRepository(),
                new LeaveCreditRepository(),
                new LeaveApprovalRepository(),
                sharedEmployeeService
        );
        payrollService = new PayrollService(
                new PayrunRepository(),
                new PayrollResultRepository(),
                new SalaryHistoryRepository(),
                new EmployeeBenefitRepository(),
                new GovernmentDetailsRepository(),
                sharedAttendanceService,
                sharedEmployeeService,
                new PayrollCalculator()
        );

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

        leaveTypeIds.put("Vacation Leave", 1);
        leaveTypeIds.put("Sick Leave", 2);
        leaveTypeIds.put("Emergency Leave", 3);
    }

    @FXML
    private StackPane rootPane;

    @FXML
    private BorderPane loginPane;

    @FXML
    private BorderPane appPane;

    @FXML
    private VBox loginCard;

    @FXML
    private VBox floatingNetPayCard;

    @FXML
    private VBox floatingAttendanceCard;

    @FXML
    private VBox floatingLeaveCard;

    @FXML
    private VBox floatingPaydayCard;

    @FXML
    private VBox floatingPayslipCard;

    @FXML
    private VBox otpCard;

    @FXML
    private VBox forgotCard;

    @FXML
    private VBox forgotOtpCard;

    @FXML
    private VBox forgotPasswordCard;

    @FXML
    private TextField companyEmailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordTextField;

    @FXML
    private Button passwordToggleBtn;

    // Hosts inside the login form and app topbar where we insert the global controls programmatically
    @FXML
    private javafx.scene.layout.HBox globalControlsHostLogin;

    @FXML
    private javafx.scene.layout.HBox globalControlsHostApp;

    // Programmatic controls (distinct instances for login and app hosts)
    private ComboBox<String> loginLanguageCombo;
    private ComboBox<String> appLanguageCombo;
    private ComboBox<String> loginThemeCombo;
    private ComboBox<String> appThemeCombo;
    private Button loginMoreBtn;
    private Button appMoreBtn;

    @FXML
    private VBox sidePanel;

    @FXML
    private TextField otpField;

    @FXML
    private TextField resetEmailField;

    @FXML
    private TextField resetOtpField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label loginMessageLabel;

    @FXML
    private Label otpMessageLabel;

    @FXML
    private Label forgotMessageLabel;

    @FXML
    private Label forgotOtpMessageLabel;

    @FXML
    private Label forgotPasswordMessageLabel;

    @FXML
    private Label appMessageLabel;

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label pageSubtitleLabel;

    @FXML
    private VBox dashboardView;

    @FXML
    private VBox attendanceView;

    @FXML
    private VBox leaveView;

    @FXML
    private VBox payslipView;

    @FXML
    private VBox settingsView;

    @FXML
    private Button dashboardNavButton;

    @FXML
    private Button attendanceNavButton;

    @FXML
    private Button leaveNavButton;

    @FXML
    private Button payslipNavButton;

    @FXML
    private Button settingsNavButton;

    @FXML
    private Label dashboardEmployeeNameLabel;

    @FXML
    private Label dashboardEmployeeRoleLabel;

    @FXML
    private Label dashboardTaskPercentLabel;

    @FXML
    private Label dashboardLeaveCreditsLabel;

    @FXML
    private Label dashboardNetPayLabel;

    @FXML
    private Label dashboardEmailLabel;

    @FXML
    private LineChart<String, Number> payrollTrendChart;

    @FXML
    private BarChart<String, Number> earningsBreakdownChart;

    @FXML
    private BarChart<String, Number> attendanceSummaryChart;

    @FXML
    private Label attendanceDateLabel;

    @FXML
    private Label attendanceTimeInLabel;

    @FXML
    private Label attendanceTimeOutLabel;

    @FXML
    private Label attendanceHoursLabel;

    @FXML
    private Label attendanceStatusLabel;

    @FXML
    private ComboBox<String> leaveTypeComboBox;

    @FXML
    private TextField leaveCreditsField;

    @FXML
    private TextField leaveStartDateField;

    @FXML
    private TextField leaveEndDateField;

    @FXML
    private TextField leaveHoursField;

    @FXML
    private TextField leaveReasonField;

    @FXML
    private Label leaveHistoryLabel;

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
    private TextField settingsEmailField;

    @FXML
    private TextField settingsRecoveryEmailField;

    @FXML
    private PasswordField settingsOldPasswordField;

    @FXML
    private PasswordField settingsNewPasswordField;

    @FXML
    private PasswordField settingsConfirmPasswordField;

    @FXML
    private void initialize() {
        // create programmatic global controls (distinct instances per host) and insert into hosts
        loginLanguageCombo = new ComboBox<>();
        loginLanguageCombo.getStyleClass().add("global-combo");
        loginLanguageCombo.setPrefWidth(140);
        loginLanguageCombo.getItems().setAll("English", "Filipino");
        loginLanguageCombo.setValue("English");
        loginLanguageCombo.setOnAction(e -> syncLanguage(loginLanguageCombo.getValue()));

        appLanguageCombo = new ComboBox<>();
        appLanguageCombo.getStyleClass().add("global-combo");
        appLanguageCombo.setPrefWidth(140);
        appLanguageCombo.getItems().setAll("English", "Filipino");
        appLanguageCombo.setValue("English");
        appLanguageCombo.setOnAction(e -> syncLanguage(appLanguageCombo.getValue()));

        loginThemeCombo = new ComboBox<>();
        loginThemeCombo.getStyleClass().add("global-combo");
        loginThemeCombo.setPrefWidth(120);
        loginThemeCombo.getItems().setAll("System", "Light", "Dark");
        loginThemeCombo.setValue("Dark");
        loginThemeCombo.setOnAction(e -> syncTheme(loginThemeCombo.getValue()));

        appThemeCombo = new ComboBox<>();
        appThemeCombo.getStyleClass().add("global-combo");
        appThemeCombo.setPrefWidth(120);
        appThemeCombo.getItems().setAll("System", "Light", "Dark");
        appThemeCombo.setValue("Dark");
        appThemeCombo.setOnAction(e -> syncTheme(appThemeCombo.getValue()));

        loginMoreBtn = new Button("⋮");
        loginMoreBtn.getStyleClass().add("more-button");
        loginMoreBtn.setOnAction(e -> toggleSidePanel());

        appMoreBtn = new Button("⋮");
        appMoreBtn.getStyleClass().add("more-button");
        appMoreBtn.setOnAction(e -> toggleSidePanel());

        // Populate global controls on both login header and app topbar
        if (globalControlsHostLogin != null) {
            globalControlsHostLogin.getChildren().clear();
            globalControlsHostLogin.getChildren().addAll(loginLanguageCombo, loginThemeCombo, loginMoreBtn);
            // avoid empty areas of the host intercepting mouse events
            globalControlsHostLogin.setPickOnBounds(false);
            if (globalControlsHostLogin.getParent() instanceof javafx.scene.layout.HBox parentHBox) {
                for (Node n : parentHBox.getChildren()) {
                    if (n != globalControlsHostLogin && n instanceof javafx.scene.layout.Region r) {
                        r.setPickOnBounds(false);
                    }
                }
            }
        }

        if (globalControlsHostApp != null) {
            globalControlsHostApp.getChildren().clear();
            globalControlsHostApp.getChildren().addAll(appLanguageCombo, appThemeCombo, appMoreBtn);
            // avoid empty areas of the host intercepting mouse events
            globalControlsHostApp.setPickOnBounds(false);
            if (globalControlsHostApp.getParent() instanceof javafx.scene.layout.HBox parentHBox) {
                for (Node n : parentHBox.getChildren()) {
                    if (n != globalControlsHostApp && n instanceof javafx.scene.layout.Region r) {
                        r.setPickOnBounds(false);
                    }
                }
            }
        }

        // ensure side panel hidden initially
        if (sidePanel != null) {
            sidePanel.setVisible(false);
            sidePanel.setManaged(false);
        }

        // apply defaults from the app topbar controls
        if (appThemeCombo != null) syncTheme(appThemeCombo.getValue());
        if (appLanguageCombo != null) syncLanguage(appLanguageCombo.getValue());
        leaveTypeComboBox.getItems().setAll(leaveTypeIds.keySet());
        leaveTypeComboBox.setOnAction(event -> refreshLeaveCredits());
        attendanceDateLabel.setText(LocalDate.now().toString());
        seedDashboardCharts();
        showLogin();
        showDashboard();
        startLoginAnimations();
    }

    @FXML
    private void applyTheme() {
        String mode = null;

        if (loginThemeCombo != null) mode = loginThemeCombo.getValue();
        else if (appThemeCombo != null) mode = appThemeCombo.getValue();

        syncTheme(mode);
    }

    @FXML
    private void toggleSidePanel() {
        if (sidePanel == null) return;

        if (sidePanel.isVisible()) {
            TranslateTransition tt = new TranslateTransition(Duration.millis(260), sidePanel);
            tt.setFromX(0);
            tt.setToX(280);
            tt.setInterpolator(Interpolator.EASE_IN);
            tt.setOnFinished(e -> {
                sidePanel.setVisible(false);
                sidePanel.setManaged(false);
            });
            tt.play();
        } else {
            sidePanel.setTranslateX(280);
            sidePanel.setVisible(true);
            sidePanel.setManaged(true);
            TranslateTransition tt = new TranslateTransition(Duration.millis(260), sidePanel);
            tt.setFromX(280);
            tt.setToX(0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            tt.play();
        }
    }

    @FXML
    private void handleLogin() {
        clearLoginState();

        String companyEmail = text(companyEmailField);
        String password = passwordVisible ? text(passwordTextField) : text(passwordField);
        boolean valid = true;

        if (!isMotorphEmail(companyEmail)) {
            markInvalid(companyEmailField);
            valid = false;
        }

        if (password.isEmpty()) {
            markInvalid(passwordField);
            valid = false;
        }

        if (!valid) {
            setError(loginMessageLabel, "Enter your MotorPH email and password.");
            return;
        }

        if (isHrDemoCredentials(companyEmail, password)) {
            startHrDemoSession();
            return;
        }

        try {
            pendingLoginResult = authenticationService.authenticate(new LoginRequestDTO(companyEmail, password));
        } catch (Exception e) {
            if (isDatabaseUnavailable(e)) {
                offlinePreviewMode = true;
                pendingLoginResult = null;
                clearInvalid(companyEmailField);
                clearInvalid(passwordField);
                showCard(otpCard);
                setSuccess(otpMessageLabel, "Database is offline. Preview mode enabled. Enter any 6-digit OTP.");
                return;
            }

            markInvalid(companyEmailField);
            markInvalid(passwordField);
            setError(loginMessageLabel, userMessage(e));
            return;
        }

        showCard(otpCard);
            setSuccess(otpMessageLabel, "OTP sent to your registered email address.");
    }

    public void startHrDemoSession() {
        hrDemoMode = true;
        offlinePreviewMode = false;
        pendingLoginResult = new LoginResultDTO(
                HR_DEMO_USER_ACCOUNT_ID,
                HR_DEMO_EMPLOYEE_ID,
                HR_DEMO_EMAIL,
                Role.HR,
                true,
                false,
                "HR demo login successful."
        );
        currentUserAccountId = HR_DEMO_USER_ACCOUNT_ID;
        currentEmployeeId = HR_DEMO_EMPLOYEE_ID;
        currentPayslipResult = null;
        latestPendingLeaveRequestId = null;

        clearLoginState();
        clearForgotState();
        companyEmailField.setText(HR_DEMO_EMAIL);
        passwordField.clear();
        passwordTextField.clear();
        otpField.clear();

        loginPane.setVisible(false);
        loginPane.setManaged(false);
        appPane.setVisible(true);
        appPane.setManaged(true);

        showDashboard();
        setSuccess(appMessageLabel, "HR demo account loaded.");
    }

    @FXML
    private void togglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordToggleBtn.setText("Hide");
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            passwordToggleBtn.setText("Show");
        }
    }

    @FXML
    private void handleOtpVerification() {
        clearInvalid(otpField);
        String otp = text(otpField);

        if (offlinePreviewMode) {
            if (!otp.matches("\\d{6}")) {
                markInvalid(otpField);
                setError(otpMessageLabel, "Enter a 6-digit OTP code.");
                return;
            }

            offlinePreviewMode = false;
            pendingLoginResult = null;
            currentUserAccountId = 0;
            currentEmployeeId = 0;
            loginPane.setVisible(false);
            loginPane.setManaged(false);
            appPane.setVisible(true);
            appPane.setManaged(true);
            showDashboard();
            setSuccess(appMessageLabel, "Employee workspace is ready.");
            return;
        }

        if (pendingLoginResult == null || !pendingLoginResult.isOtpRequired()) {
            setError(otpMessageLabel, "Please sign in again before verifying OTP.");
            showCard(loginCard);
            return;
        }

        try {
            LoginResultDTO result = authenticationService.verifyLoginOtp(
                    pendingLoginResult.getUserAccountId(),
                    otp
            );
            currentUserAccountId = result.getUserAccountId();
            currentEmployeeId = result.getEmployeeId();
            pendingLoginResult = null;
        } catch (Exception e) {
            markInvalid(otpField);
            setError(otpMessageLabel, userMessage(e));
            return;
        }

        loginPane.setVisible(false);
        loginPane.setManaged(false);
        appPane.setVisible(true);
        appPane.setManaged(true);
        showDashboard();
        setSuccess(appMessageLabel, "Login successful. Employee workspace is ready.");
    }

    @FXML
    private void showForgotPassword() {
        showCard(forgotCard);
        clearForgotState();
    }

    @FXML
    private void handlePasswordReset() {
        clearForgotState();

        String resetEmail = text(resetEmailField);

        if (!isMotorphEmail(resetEmail)) {
            markInvalid(resetEmailField);
            setError(forgotMessageLabel, "Enter a valid MotorPH email address.");
            return;
        }

        try {
            pendingResetRequestId = passwordResetService.requestPasswordReset(resetEmail);
        } catch (Exception e) {
            if (isDatabaseUnavailable(e)) {
                clearInvalid(resetEmailField);
                setError(forgotMessageLabel, "Password reset needs MySQL. Start the database and try again.");
                return;
            }

            markInvalid(resetEmailField);
            setError(forgotMessageLabel, userMessage(e));
            return;
        }

        showCard(forgotOtpCard);
        setSuccess(forgotOtpMessageLabel, "Reset OTP sent to your registered email address.");
    }

    @FXML
    private void handleResetOtpVerification() {
        clearInvalid(resetOtpField);

        if (pendingResetRequestId <= 0) {
            setError(forgotOtpMessageLabel, "Please request a reset OTP first.");
            showCard(forgotCard);
            return;
        }

        try {
            passwordResetService.verifyResetOtp(pendingResetRequestId, text(resetOtpField));
        } catch (Exception e) {
            markInvalid(resetOtpField);
            setError(forgotOtpMessageLabel, userMessage(e));
            return;
        }

        showCard(forgotPasswordCard);
        setSuccess(forgotPasswordMessageLabel, "OTP verified. Enter your new password.");
    }

    @FXML
    private void handleCompletePasswordReset() {
        clearInvalid(newPasswordField);
        clearInvalid(confirmPasswordField);

        String newPassword = text(newPasswordField);
        String confirmPassword = text(confirmPasswordField);
        boolean valid = true;

        if (newPassword.length() < 8) {
            markInvalid(newPasswordField);
            valid = false;
        }

        if (!newPassword.equals(confirmPassword)) {
            markInvalid(confirmPasswordField);
            valid = false;
        }

        if (!valid) {
            setError(forgotPasswordMessageLabel, "Password must be 8+ characters and match confirmation.");
            return;
        }

        try {
            passwordResetService.resetPassword(pendingResetRequestId, newPassword, confirmPassword);
        } catch (Exception e) {
            setError(forgotPasswordMessageLabel, userMessage(e));
            return;
        }

        pendingResetRequestId = 0;
        newPasswordField.clear();
        confirmPasswordField.clear();
        showCard(loginCard);
        setSuccess(loginMessageLabel, "Password updated successfully. You can now sign in.");
    }

    @FXML
    private void showLogin() {
        showCard(loginCard);
        clearLoginState();
        offlinePreviewMode = false;
        pendingLoginResult = null;
    }

    @FXML
    private void showDashboard() {
        if (hrDemoMode) {
            showView(dashboardView, dashboardNavButton, "Hello, HR.", "HR WORKSPACE");
        } else {
            showView(dashboardView, dashboardNavButton, "Hello, Harty.", "EMPLOYEE WORKSPACE");
        }
        loadDashboardData();
    }

    @FXML
    private void showAttendance() {
        showView(attendanceView, attendanceNavButton, "Attendance", "Daily time-in, time-out, and attendance logs");
        refreshAttendanceStatus();
    }

    @FXML
    private void showLeave() {
        showView(leaveView, leaveNavButton, "Leave Request", "Submit leave requests and monitor credits");
        refreshLeaveCredits();
        refreshLeaveHistory();
    }

    @FXML
    private void showPayslip() {
        showView(payslipView, payslipNavButton, "Payslip / Payroll", "View persisted payroll results and payslips");
        handleSearchPayslip();
    }

    @FXML
    private void showSettings() {
        showView(settingsView, settingsNavButton, "Account Settings", "Manage password and recovery-account details");
        loadSettingsData();
    }

    @FXML
    private void showSuccessMessage() {
        setSuccess(appMessageLabel, "Action accepted. Connect this control to the service layer next.");
    }

    @FXML
    private void handleTimeIn() {
        if (!hasLoggedInEmployee()) {
            return;
        }

        if (hrDemoMode) {
            setSuccess(appMessageLabel, "HR demo account is read-only.");
            return;
        }

        try {
            attendanceService.timeIn(currentEmployeeId);
            refreshAttendanceStatus();
            setSuccess(appMessageLabel, "Time in recorded.");
        } catch (Exception e) {
            setError(appMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleTimeOut() {
        if (!hasLoggedInEmployee()) {
            return;
        }

        if (hrDemoMode) {
            setSuccess(appMessageLabel, "HR demo account is read-only.");
            return;
        }

        try {
            attendanceService.timeOut(currentEmployeeId);
            refreshAttendanceStatus();
            setSuccess(appMessageLabel, "Time out recorded.");
        } catch (Exception e) {
            setError(appMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleSubmitLeaveRequest() {
        if (!hasLoggedInEmployee()) {
            return;
        }

        if (hrDemoMode) {
            setSuccess(appMessageLabel, "HR demo account is read-only.");
            return;
        }

        clearLeaveInvalid();

        try {
            Integer leaveTypeId = leaveTypeIds.get(leaveTypeComboBox.getValue());

            if (leaveTypeId == null) {
                throw new IllegalArgumentException("Select a leave type.");
            }

            LocalDate startDate = parseDate(leaveStartDateField);
            LocalDate endDate = parseDate(leaveEndDateField);
            double leaveHours = parsePositiveDouble(leaveHoursField, "Leave hours must be a positive number.");

            leaveService.submitLeaveRequest(
                    currentEmployeeId,
                    leaveTypeId,
                    startDate,
                    endDate,
                    leaveHours,
                    text(leaveReasonField)
            );

            setSuccess(appMessageLabel, "Leave request submitted.");
            refreshLeaveCredits();
            refreshLeaveHistory();
        } catch (DateTimeParseException e) {
            markInvalid(leaveStartDateField);
            markInvalid(leaveEndDateField);
            setError(appMessageLabel, "Use YYYY-MM-DD for leave dates.");
        } catch (Exception e) {
            setError(appMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleCancelLatestLeaveRequest() {
        if (!hasLoggedInEmployee()) {
            return;
        }

        if (hrDemoMode) {
            setSuccess(appMessageLabel, "HR demo account is read-only.");
            return;
        }

        if (latestPendingLeaveRequestId == null) {
            setError(appMessageLabel, "No pending leave request to cancel.");
            return;
        }

        try {
            leaveService.cancelLeaveRequest(currentEmployeeId, latestPendingLeaveRequestId);
            latestPendingLeaveRequestId = null;
            setSuccess(appMessageLabel, "Pending leave request cancelled.");
            refreshLeaveHistory();
            refreshLeaveCredits();
        } catch (Exception e) {
            setError(appMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleSearchPayslip() {
        if (!hasLoggedInEmployee()) {
            return;
        }

        clearInvalid(payslipSearchField);

        if (hrDemoMode) {
            currentPayslipResult = demoPayrollResult();
            showPayrollResult(currentPayslipResult);
            return;
        }

        try {
            PayrollResult result;
            String search = text(payslipSearchField);

            if (search.isBlank()) {
                result = payrollService.getPayrollResultsByEmployee(currentEmployeeId)
                        .stream()
                        .max(Comparator.comparing(PayrollResult::getGeneratedAt))
                        .orElse(null);
            } else {
                result = payrollService.getPayrollResult(currentEmployeeId, Integer.parseInt(search));
            }

            if (result == null) {
                currentPayslipResult = null;
                payslipGrossPayLabel.setText("Gross Pay: PHP 0.00");
                payslipDeductionsLabel.setText("Deductions: PHP 0.00");
                payslipNetPayLabel.setText("Net Pay: PHP 0.00");
                setError(payslipMessageLabel, "No persisted payslip found for this employee.");
                return;
            }

            showPayrollResult(result);
        } catch (NumberFormatException e) {
            markInvalid(payslipSearchField);
            setError(payslipMessageLabel, "Enter a numeric payrun ID.");
        } catch (Exception e) {
            setError(payslipMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handlePreviewPayslip() {
        if (!hasPayslipResult()) {
            return;
        }

        TextArea preview = new TextArea(buildPayslipText(currentPayslipResult));
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
        if (!hasPayslipResult()) {
            return;
        }

        try {
            Path outputDir = Path.of("target", "payslips");
            Files.createDirectories(outputDir);

            String fileName = "motorph-payslip-employee-"
                    + currentPayslipResult.getEmployeeId()
                    + "-payrun-"
                    + currentPayslipResult.getPayrunId()
                    + ".txt";
            Path outputFile = outputDir.resolve(fileName);
            Files.writeString(outputFile, buildPayslipText(currentPayslipResult));
            setSuccess(payslipMessageLabel, "Payslip exported to " + outputFile.toAbsolutePath() + ".");
        } catch (IOException e) {
            setError(payslipMessageLabel, "Unable to export payslip file.");
        }
    }

    @FXML
    private void handlePrintPayslip() {
        if (!hasPayslipResult()) {
            return;
        }

        TextArea printView = new TextArea(buildPayslipText(currentPayslipResult));
        printView.setEditable(false);
        printView.setWrapText(false);
        printView.setPrefColumnCount(68);
        printView.setPrefRowCount(28);

        PrinterJob job = PrinterJob.createPrinterJob();

        if (job == null) {
            setError(payslipMessageLabel, "No printer is available.");
            return;
        }

        boolean accepted = job.showPrintDialog(payslipView.getScene().getWindow());

        if (!accepted) {
            setError(payslipMessageLabel, "Print cancelled.");
            return;
        }

        if (job.printPage(printView)) {
            job.endJob();
            setSuccess(payslipMessageLabel, "Payslip sent to printer.");
            return;
        }

        setError(payslipMessageLabel, "Unable to print payslip.");
    }

    @FXML
    private void handleChangePassword() {
        if (currentUserAccountId <= 0) {
            setError(appMessageLabel, "Please log in again before changing your password.");
            return;
        }

        if (hrDemoMode) {
            setSuccess(appMessageLabel, "HR demo account is read-only.");
            return;
        }

        clearInvalid(settingsOldPasswordField);
        clearInvalid(settingsNewPasswordField);
        clearInvalid(settingsConfirmPasswordField);

        String oldPassword = text(settingsOldPasswordField);
        String newPassword = text(settingsNewPasswordField);
        String confirmPassword = text(settingsConfirmPasswordField);

        if (oldPassword.isBlank()) {
            markInvalid(settingsOldPasswordField);
            setError(appMessageLabel, "Old password is required.");
            return;
        }

        if (newPassword.length() < 8 || !newPassword.equals(confirmPassword)) {
            markInvalid(settingsNewPasswordField);
            markInvalid(settingsConfirmPasswordField);
            setError(appMessageLabel, "New password must be 8+ characters and match confirmation.");
            return;
        }

        try {
            UserAccount account = userAccountRepository.findById(currentUserAccountId);

            if (account == null) {
                throw new IllegalStateException("User account not found.");
            }

            if (!PasswordHasher.verify(oldPassword, account.getPasswordSalt(), account.getPasswordHash())) {
                markInvalid(settingsOldPasswordField);
                setError(appMessageLabel, "Old password is incorrect.");
                return;
            }

            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hash(newPassword, salt);
            userAccountRepository.updatePassword(currentUserAccountId, hash, salt);
            settingsOldPasswordField.clear();
            settingsNewPasswordField.clear();
            settingsConfirmPasswordField.clear();
            setSuccess(appMessageLabel, "Password updated successfully.");
        } catch (Exception e) {
            setError(appMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleLogout() {
        appPane.setVisible(false);
        appPane.setManaged(false);
        loginPane.setVisible(true);
        loginPane.setManaged(true);
        passwordField.clear();
        passwordTextField.clear();
        otpField.clear();
        pendingLoginResult = null;
        offlinePreviewMode = false;
        hrDemoMode = false;
        currentUserAccountId = 0;
        currentEmployeeId = 0;
        showLogin();
        setSuccess(loginMessageLabel, "Logged out successfully.");
    }

    private void loadDashboardData() {
        if (hrDemoMode) {
            dashboardEmployeeNameLabel.setText("HR Demo User");
            dashboardEmployeeRoleLabel.setText("Human Resources");
            dashboardEmailLabel.setText(HR_DEMO_EMAIL);
            pageTitleLabel.setText("Hello, HR.");
            pageSubtitleLabel.setText("HR WORKSPACE");
            dashboardTaskPercentLabel.setText("4");
            settingsEmailField.setText(HR_DEMO_EMAIL);
            settingsRecoveryEmailField.setText("hr.demo.recovery@motorph.com");
            refreshDashboardPayslip();
            refreshAttendanceStatus();
            refreshLeaveCredits();
            refreshLeaveHistory();
            return;
        }

        if (currentEmployeeId <= 0) {
            return;
        }

        try {
            Employee employee = employeeService.getEmployeeById(currentEmployeeId);
            String fullName = employee.getFullName();
            dashboardEmployeeNameLabel.setText(fullName);
            dashboardEmployeeRoleLabel.setText(employee.getEmploymentStatus().name() + " / Department " + employee.getDepartmentId());
            dashboardEmailLabel.setText(safe(employee.getPersonalEmail(), "No recovery email on file"));
            pageTitleLabel.setText("Hello, " + employee.getFirstName() + ".");
            settingsRecoveryEmailField.setText(employee.getPersonalEmail());

            refreshLeaveCredits();
            refreshDashboardPayslip();
        } catch (Exception e) {
            setError(appMessageLabel, userMessage(e));
        }
    }

    private void refreshDashboardPayslip() {
        if (hrDemoMode) {
            showPayrollResult(demoPayrollResult());
            return;
        }

        if (currentEmployeeId <= 0) {
            return;
        }

        try {
            Optional<PayrollResult> latest = payrollService.getPayrollResultsByEmployee(currentEmployeeId)
                    .stream()
                    .max(Comparator.comparing(PayrollResult::getGeneratedAt));

            dashboardNetPayLabel.setText(latest.map(result -> pesoFormat.format(result.getNetPay())).orElse("PHP 0.00"));
        } catch (Exception e) {
            dashboardNetPayLabel.setText("PHP 0.00");
        }
    }

    private void refreshAttendanceStatus() {
        attendanceDateLabel.setText(LocalDate.now().toString());

        if (hrDemoMode) {
            attendanceTimeInLabel.setText("08:00");
            attendanceTimeOutLabel.setText("17:00");
            attendanceHoursLabel.setText("9.00");
            attendanceStatusLabel.setText(AttendanceStatus.PRESENT.name());
            return;
        }

        if (currentEmployeeId <= 0) {
            return;
        }

        try {
            Attendance attendance = attendanceService.getAttendanceForDate(currentEmployeeId, LocalDate.now());
            attendanceTimeInLabel.setText(attendance.getTimeIn() == null ? "—" : attendance.getTimeIn().toString());
            attendanceTimeOutLabel.setText(attendance.getTimeOut() == null ? "—" : attendance.getTimeOut().toString());
            attendanceHoursLabel.setText(String.format("%.2f", attendance.getHoursWorked()));
            attendanceStatusLabel.setText(attendance.getAttendanceStatus().name());
        } catch (Exception e) {
            attendanceTimeInLabel.setText("—");
            attendanceTimeOutLabel.setText("—");
            attendanceHoursLabel.setText("—");
            attendanceStatusLabel.setText("Pending");

            String message = userMessage(e);
            if (!message.contains("Attendance record not found")) {
                setError(appMessageLabel, message);
            }
        }
    }

    private void refreshLeaveCredits() {
        String selectedType = leaveTypeComboBox.getValue();
        Integer leaveTypeId = leaveTypeIds.get(selectedType);

        if (hrDemoMode) {
            if (selectedType == null) {
                leaveTypeComboBox.setValue("Vacation Leave");
            }

            leaveCreditsField.setText("120.0 hours");
            dashboardLeaveCreditsLabel.setText("15");
            return;
        }

        if (leaveTypeId == null) {
            leaveCreditsField.setText("Select a type");
            dashboardLeaveCreditsLabel.setText("—");
            return;
        }

        if (currentEmployeeId <= 0) {
            leaveCreditsField.setText("Login required");
            return;
        }

        try {
            double credits = leaveService.getRemainingLeaveCredits(currentEmployeeId, leaveTypeId, LocalDate.now().getYear());
            leaveCreditsField.setText(String.format("%.1f hours", credits));
            dashboardLeaveCreditsLabel.setText(String.format("%.0f", credits / 8.0));
        } catch (Exception e) {
            leaveCreditsField.setText("Unavailable");
            dashboardLeaveCreditsLabel.setText("—");
        }
    }

    private void refreshLeaveHistory() {
        if (hrDemoMode) {
            latestPendingLeaveRequestId = 9001;
            leaveHistoryLabel.setText("Latest: PENDING | 2026-06-12 to 2026-06-14 | 16.0 hours");
            return;
        }

        if (currentEmployeeId <= 0) {
            return;
        }

        try {
            List<LeaveRequest> requests = leaveService.getLeaveRequestsByEmployeeId(currentEmployeeId);
            latestPendingLeaveRequestId = requests.stream()
                    .filter(request -> request.getStatus() == LeaveStatus.PENDING)
                    .max(Comparator.comparing(LeaveRequest::getRequestDate))
                    .map(LeaveRequest::getRequestId)
                    .orElse(null);

            if (requests.isEmpty()) {
                leaveHistoryLabel.setText("No leave requests found.");
                return;
            }

            LeaveRequest latest = requests.stream()
                    .max(Comparator.comparing(LeaveRequest::getRequestDate))
                    .orElseThrow();

            leaveHistoryLabel.setText(
                    "Latest: " + latest.getStatus().name()
                            + " | " + latest.getStartDate()
                            + " to " + latest.getEndDate()
                            + " | " + latest.getLeaveHours() + " hours"
            );
        } catch (Exception e) {
            latestPendingLeaveRequestId = null;
            leaveHistoryLabel.setText(userMessage(e));
        }
    }

    private void loadSettingsData() {
        if (hrDemoMode) {
            settingsEmailField.setText(HR_DEMO_EMAIL);
            settingsRecoveryEmailField.setText("hr.demo.recovery@motorph.com");
            return;
        }

        if (currentEmployeeId <= 0) {
            return;
        }

        try {
            Employee employee = employeeService.getEmployeeById(currentEmployeeId);
            settingsEmailField.setText(companyEmailField.getText());
            settingsRecoveryEmailField.setText(employee.getPersonalEmail());
        } catch (Exception e) {
            setError(appMessageLabel, userMessage(e));
        }
    }

    private void showPayrollResult(PayrollResult result) {
        currentPayslipResult = result;
        payslipGrossPayLabel.setText("Gross Pay: " + pesoFormat.format(result.getGrossPay()));
        payslipDeductionsLabel.setText("Deductions: " + pesoFormat.format(result.getTotalDeductions()));
        payslipNetPayLabel.setText("Net Pay: " + pesoFormat.format(result.getNetPay()));
        setSuccess(payslipMessageLabel, "Loaded payrun #" + result.getPayrunId() + " generated " + result.getGeneratedAt().toLocalDate() + ".");
        dashboardNetPayLabel.setText(pesoFormat.format(result.getNetPay()));
    }

    private boolean hasPayslipResult() {
        if (currentPayslipResult != null) {
            return true;
        }

        setError(payslipMessageLabel, "Search and load a payslip before previewing, exporting, or printing.");
        return false;
    }

    private String buildPayslipText(PayrollResult result) {
        String employeeName = dashboardEmployeeNameLabel.getText();
        String email = companyEmailField.getText();
        String generated = result.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return """
                MOTORPH PAYROLL MANAGEMENT SYSTEM
                Employee Payslip
                ==========================================

                Employee: %s
                Email: %s
                Employee ID: %d
                Payrun ID: %d
                Generated: %s

                ------------------------------------------
                Total Hours Worked: %.2f
                Gross Pay: %s
                Total Deductions: %s
                Net Pay: %s
                ------------------------------------------

                This payslip is generated from persisted MotorPH payroll results.
                For official release, connect the JasperReports template when the
                JasperReports dependency and JRXML design are added to the project.
                """.formatted(
                safe(employeeName, "Employee"),
                safe(email, "employee@motorph.com"),
                result.getEmployeeId(),
                result.getPayrunId(),
                generated,
                result.getTotalHoursWorked(),
                pesoFormat.format(result.getGrossPay()),
                pesoFormat.format(result.getTotalDeductions()),
                pesoFormat.format(result.getNetPay())
        );
    }

    private void seedDashboardCharts() {
        payrollTrendChart.getData().setAll(series(
                "Net Pay (PHP)",
                new String[]{"Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar", "Apr", "May", "Jun"},
                new double[]{27000, 34000, 29500, 27500, 12500, 29000, 31500, 28500, 23500, 26000, 24500, 23000}
        ));

        earningsBreakdownChart.getData().setAll(series(
                "Earnings",
                new String[]{"Basic", "Allowances", "Overtime", "Night Diff", "Holiday", "Bonus"},
                new double[]{17500, 6000, 3250, 1750, 1250, 750}
        ));

        attendanceSummaryChart.getData().setAll(
                series("Present", new String[]{"Jun 1", "Jun 3", "Jun 5", "Jun 7", "Jun 9", "Jun 11", "Jun 13", "Jun 15"},
                        new double[]{7, 9, 9, 10, 9, 6, 6, 6}),
                series("Late", new String[]{"Jun 1", "Jun 3", "Jun 5", "Jun 7", "Jun 9", "Jun 11", "Jun 13", "Jun 15"},
                        new double[]{2, 3, 6, 5, 0, 1, 0, 4}),
                series("Absent", new String[]{"Jun 1", "Jun 3", "Jun 5", "Jun 7", "Jun 9", "Jun 11", "Jun 13", "Jun 15"},
                        new double[]{0, 0, 0, 0, 0, 1, 0, 0})
        );
    }

    private XYChart.Series<String, Number> series(String name, String[] categories, double[] values) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);

        for (int i = 0; i < categories.length; i++) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(categories[i], values[i]);
            series.getData().add(data);
            data.nodeProperty().addListener((observable, oldNode, node) -> {
                if (node != null) {
                    Tooltip.install(node, new Tooltip(data.getXValue() + ": " + data.getYValue()));
                }
            });
        }

        return series;
    }

    private void startLoginAnimations() {
        fadeIn(loginCard, 220, 0);
        scaleIn(loginCard, 240, 0);

        floatNode(floatingNetPayCard, -8, 4.8, 0.1);
        floatNode(floatingAttendanceCard, 9, 5.6, 0.6);
        floatNode(floatingLeaveCard, -7, 5.2, 1.0);
        floatNode(floatingPaydayCard, 6, 6.0, 0.4);
        floatNode(floatingPayslipCard, -6, 5.8, 0.8);

        fadeIn(floatingNetPayCard, 420, 100);
        fadeIn(floatingAttendanceCard, 420, 100);
        fadeIn(floatingLeaveCard, 420, 100);
        fadeIn(floatingPaydayCard, 420, 100);
        fadeIn(floatingPayslipCard, 420, 100);
    }

    private void applyLanguage() {
        String lang = null;

        if (loginLanguageCombo != null) lang = loginLanguageCombo.getValue();
        else if (appLanguageCombo != null) lang = appLanguageCombo.getValue();

        syncLanguage(lang);
    }

    private void syncLanguage(String lang) {
        if (lang == null) return;

        if (loginLanguageCombo != null && !lang.equals(loginLanguageCombo.getValue())) loginLanguageCombo.setValue(lang);
        if (appLanguageCombo != null && !lang.equals(appLanguageCombo.getValue())) appLanguageCombo.setValue(lang);

        boolean filipino = "Filipino".equals(lang);
        applyLanguage(rootPane, filipino);
    }

    private void syncTheme(String mode) {
        if (mode == null) mode = "System";

        if (loginThemeCombo != null && !mode.equals(loginThemeCombo.getValue())) loginThemeCombo.setValue(mode);
        if (appThemeCombo != null && !mode.equals(appThemeCombo.getValue())) appThemeCombo.setValue(mode);

        if (mode.isBlank() || "System".equals(mode)) {
            rootPane.getStyleClass().remove("light-mode");
            darkMode = true;
            return;
        }

        if ("Light".equals(mode)) {
            if (!rootPane.getStyleClass().contains("light-mode")) {
                rootPane.getStyleClass().add("light-mode");
            }
            darkMode = false;
            return;
        }

        rootPane.getStyleClass().remove("light-mode");
        darkMode = true;
    }

    // Robust translation lookup (case-insensitive / trimmed fallback)
    private String translateText(String english) {
        if (english == null) return "";
        String direct = FILIPINO_LABELS.get(english);
        if (direct != null) return direct;

        String key = english.trim();
        for (Map.Entry<String, String> e : FILIPINO_LABELS.entrySet()) {
            if (e.getKey().trim().equalsIgnoreCase(key)) return e.getValue();
        }

        return english;
    }

    private void applyLanguage(Node node, boolean filipino) {
        if (node == null) {
            return;
        }

        if (node instanceof Labeled labeled) {
            Object storedText = labeled.getProperties().get("englishText");

            if (storedText == null) {
                storedText = labeled.getText() == null ? "" : labeled.getText();
                labeled.getProperties().put("englishText", storedText);
            }

            String englishText = storedText.toString();
            String newText = filipino ? translateText(englishText) : englishText;
            labeled.setText(newText);

            // enable wrapping for labels when Filipino to avoid truncation
            if (labeled instanceof javafx.scene.control.Label lbl) {
                Object storedWrap = lbl.getProperties().get("englishWrap");

                if (storedWrap == null) {
                    lbl.getProperties().put("englishWrap", lbl.isWrapText());
                }

                boolean originalWrap = (boolean) lbl.getProperties().get("englishWrap");
                lbl.setWrapText(filipino || originalWrap);
            }
        }

        if (node instanceof TextInputControl input) {
            Object storedPrompt = input.getProperties().get("englishPrompt");

            if (storedPrompt == null) {
                storedPrompt = input.getPromptText();
                input.getProperties().put("englishPrompt", storedPrompt == null ? "" : storedPrompt);
            }

            String englishPrompt = storedPrompt.toString();
            input.setPromptText(filipino ? translateText(englishPrompt) : englishPrompt);
        }

        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> applyLanguage(child, filipino));
        }
    }

    private void fadeIn(Node node, double durationMillis, double delayMillis) {
        if (node == null) {
            return;
        }

        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(durationMillis), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(delayMillis));
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();
    }

    private void scaleIn(Node node, double durationMillis, double delayMillis) {
        if (node == null) {
            return;
        }

        node.setScaleX(0.98);
        node.setScaleY(0.98);
        ScaleTransition scale = new ScaleTransition(Duration.millis(durationMillis), node);
        scale.setFromX(0.98);
        scale.setFromY(0.98);
        scale.setToX(1);
        scale.setToY(1);
        scale.setDelay(Duration.millis(delayMillis));
        scale.setInterpolator(Interpolator.EASE_OUT);
        scale.play();
    }

    private void floatNode(Node node, double offsetY, double seconds, double delaySeconds) {
        if (node == null) {
            return;
        }

        TranslateTransition floatAnimation = new TranslateTransition(Duration.seconds(seconds), node);
        floatAnimation.setFromY(0);
        floatAnimation.setByY(offsetY);
        floatAnimation.setDelay(Duration.seconds(delaySeconds));
        floatAnimation.setAutoReverse(true);
        floatAnimation.setCycleCount(TranslateTransition.INDEFINITE);
        floatAnimation.setInterpolator(Interpolator.EASE_BOTH);
        floatAnimation.play();
    }

    private boolean hasLoggedInEmployee() {
        if (hrDemoMode || currentEmployeeId > 0) {
            return true;
        }

        setError(appMessageLabel, "Please log in before using this action.");
        return false;
    }

    private LocalDate parseDate(TextField field) {
        clearInvalid(field);
        String value = text(field);

        if (value.isBlank()) {
            markInvalid(field);
            throw new DateTimeParseException("Date is required.", value, 0);
        }

        return LocalDate.parse(value);
    }

    private double parsePositiveDouble(TextField field, String message) {
        clearInvalid(field);

        try {
            double value = Double.parseDouble(text(field));

            if (value <= 0) {
                throw new NumberFormatException(message);
            }

            return value;
        } catch (NumberFormatException e) {
            markInvalid(field);
            throw new IllegalArgumentException(message);
        }
    }

    private void clearLeaveInvalid() {
        clearInvalid(leaveStartDateField);
        clearInvalid(leaveEndDateField);
        clearInvalid(leaveHoursField);
        clearInvalid(leaveReasonField);
    }

    private void showCard(VBox cardToShow) {
        for (VBox card : List.of(loginCard, otpCard, forgotCard, forgotOtpCard, forgotPasswordCard)) {
            boolean selected = card == cardToShow;
            card.setVisible(selected);
            card.setManaged(selected);
        }
    }

    private void showView(VBox viewToShow, Button selectedButton, String title, String subtitle) {
        for (VBox view : List.of(dashboardView, attendanceView, leaveView, payslipView, settingsView)) {
            boolean selected = view == viewToShow;
            view.setVisible(selected);
            view.setManaged(selected);
        }

        for (Button button : List.of(dashboardNavButton, attendanceNavButton, leaveNavButton, payslipNavButton, settingsNavButton)) {
            button.getStyleClass().remove("selected-nav");
        }

        if (!selectedButton.getStyleClass().contains("selected-nav")) {
            selectedButton.getStyleClass().add("selected-nav");
        }

        pageTitleLabel.setText(title);
        pageSubtitleLabel.setText(subtitle);
    }

    private void clearLoginState() {
        clearInvalid(companyEmailField);
        clearInvalid(passwordField);
        clearInvalid(passwordTextField);
        loginMessageLabel.setText("");
        loginMessageLabel.getStyleClass().removeAll("success-message", "error-message");
        passwordField.setVisible(true);
        passwordField.setManaged(true);
        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);
        passwordTextField.clear();
        passwordVisible = false;
        passwordToggleBtn.setText("Show");
    }

    private void clearForgotState() {
        clearInvalid(resetEmailField);
        clearInvalid(resetOtpField);
        clearInvalid(newPasswordField);
        clearInvalid(confirmPasswordField);
        forgotMessageLabel.setText("");
        forgotOtpMessageLabel.setText("");
        forgotPasswordMessageLabel.setText("");
        forgotMessageLabel.getStyleClass().removeAll("success-message", "error-message");
        forgotOtpMessageLabel.getStyleClass().removeAll("success-message", "error-message");
        forgotPasswordMessageLabel.getStyleClass().removeAll("success-message", "error-message");
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private boolean isMotorphEmail(String email) {
        return email != null && email.trim().toLowerCase(Locale.ROOT).matches(MOTORPH_EMAIL_PATTERN);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isHrDemoCredentials(String companyEmail, String password) {
        if (companyEmail == null || password == null) {
            return false;
        }

        return HR_DEMO_EMAIL.equalsIgnoreCase(companyEmail.trim())
                && HR_DEMO_PASSWORD.equals(password.trim());
    }

    private PayrollResult demoPayrollResult() {
        return new PayrollResult(
                9001,
                HR_DEMO_EMPLOYEE_ID,
                2506,
                160.0,
                82500.0,
                12450.0,
                70050.0,
                LocalDateTime.of(2026, 6, 10, 15, 30)
        );
    }

    private void markInvalid(TextField field) {
        if (!field.getStyleClass().contains("invalid-field")) {
            field.getStyleClass().add("invalid-field");
        }
    }

    private void clearInvalid(TextField field) {
        field.getStyleClass().remove("invalid-field");
    }

    private void setError(Label label, String message) {
        label.getStyleClass().remove("success-message");
        if (!label.getStyleClass().contains("error-message")) {
            label.getStyleClass().add("error-message");
        }
        label.setText(message);
    }

    private void setSuccess(Label label, String message) {
        label.getStyleClass().remove("error-message");
        if (!label.getStyleClass().contains("success-message")) {
            label.getStyleClass().add("success-message");
        }
        label.setText(message);
    }

    private String userMessage(Exception e) {
        String message = e.getMessage();

        if (message == null || message.isBlank()) {
            return "Unable to complete the request. Please try again.";
        }

        if (isDatabaseUnavailable(e)) {
            return "Database connection unavailable. Start MySQL and try again.";
        }

        if (message.startsWith("Failed to ")) {
            return "Unable to complete the request. Please check the database connection.";
        }

        return message;
    }

    private boolean isDatabaseUnavailable(Exception e) {
        Throwable current = e;

        while (current != null) {
            String message = current.getMessage();

            if (message != null
                    && (message.contains("Failed to connect to the database")
                    || message.contains("Communications link failure")
                    || message.contains("Connection refused")
                    || message.contains("No suitable driver"))) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
