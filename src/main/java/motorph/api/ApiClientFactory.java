package motorph.api;

/**
 * Composition root for backend REST clients - the ApiClient-based
 * counterpart to EmployeeUIServiceFactory. Grows one *ApiClient per feature
 * area as each is migrated (see Phases 2-4); today it only wires auth.
 */
public final class ApiClientFactory {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private static ApiClientFactory instance;

    private final AuthTokenHolder authTokenHolder;
    private final HttpApiClient httpApiClient;
    private final AuthApiClient authApiClient;
    private final EmployeeApiClient employeeApiClient;
    private final PayslipApiClient payslipApiClient;
    private final ReportApiClient reportApiClient;
    private final AttendanceApiClient attendanceApiClient;
    private final LeaveApiClient leaveApiClient;
    private final OvertimeApiClient overtimeApiClient;
    private final SalaryBenefitApiClient salaryBenefitApiClient;
    private final SupportTicketApiClient supportTicketApiClient;
    private final SystemConfigApiClient systemConfigApiClient;
    private final PayrunApiClient payrunApiClient;
    private final UserAccountApiClient userAccountApiClient;
    private final AccessLogApiClient accessLogApiClient;
    private final RolePermissionApiClient rolePermissionApiClient;
    private final PasswordResetApiClient passwordResetApiClient;

    private ApiClientFactory(String baseUrl) {
        this.authTokenHolder = AuthTokenHolder.getInstance();
        this.httpApiClient = new HttpApiClient(baseUrl, authTokenHolder);
        this.authApiClient = new AuthApiClient(httpApiClient, authTokenHolder);
        this.employeeApiClient = new EmployeeApiClient(httpApiClient);
        this.payslipApiClient = new PayslipApiClient(httpApiClient);
        this.reportApiClient = new ReportApiClient(httpApiClient);
        this.attendanceApiClient = new AttendanceApiClient(httpApiClient);
        this.leaveApiClient = new LeaveApiClient(httpApiClient);
        this.overtimeApiClient = new OvertimeApiClient(httpApiClient);
        this.salaryBenefitApiClient = new SalaryBenefitApiClient(httpApiClient);
        this.supportTicketApiClient = new SupportTicketApiClient(httpApiClient);
        this.systemConfigApiClient = new SystemConfigApiClient(httpApiClient);
        this.payrunApiClient = new PayrunApiClient(httpApiClient);
        this.userAccountApiClient = new UserAccountApiClient(httpApiClient);
        this.accessLogApiClient = new AccessLogApiClient(httpApiClient);
        this.rolePermissionApiClient = new RolePermissionApiClient(httpApiClient);
        this.passwordResetApiClient = new PasswordResetApiClient(httpApiClient);
    }

    public static synchronized ApiClientFactory getInstance() {
        if (instance == null) {
            String baseUrl = System.getProperty("motorph.api.baseUrl", DEFAULT_BASE_URL);
            instance = new ApiClientFactory(baseUrl);
        }

        return instance;
    }

    public AuthApiClient getAuthApiClient() {
        return authApiClient;
    }

    public AuthTokenHolder getAuthTokenHolder() {
        return authTokenHolder;
    }

    public EmployeeApiClient getEmployeeApiClient() {
        return employeeApiClient;
    }

    public PayslipApiClient getPayslipApiClient() {
        return payslipApiClient;
    }

    public ReportApiClient getReportApiClient() {
        return reportApiClient;
    }

    public AttendanceApiClient getAttendanceApiClient() {
        return attendanceApiClient;
    }

    public LeaveApiClient getLeaveApiClient() {
        return leaveApiClient;
    }

    public OvertimeApiClient getOvertimeApiClient() {
        return overtimeApiClient;
    }

    public SalaryBenefitApiClient getSalaryBenefitApiClient() {
        return salaryBenefitApiClient;
    }

    public SupportTicketApiClient getSupportTicketApiClient() {
        return supportTicketApiClient;
    }

    public SystemConfigApiClient getSystemConfigApiClient() {
        return systemConfigApiClient;
    }

    public PayrunApiClient getPayrunApiClient() {
        return payrunApiClient;
    }

    public UserAccountApiClient getUserAccountApiClient() {
        return userAccountApiClient;
    }

    public AccessLogApiClient getAccessLogApiClient() {
        return accessLogApiClient;
    }

    public RolePermissionApiClient getRolePermissionApiClient() {
        return rolePermissionApiClient;
    }

    public PasswordResetApiClient getPasswordResetApiClient() {
        return passwordResetApiClient;
    }
}
