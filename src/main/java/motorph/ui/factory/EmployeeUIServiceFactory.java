package motorph.ui.factory;

import motorph.application.service.AttendanceService;
import motorph.application.service.AuthenticationService;
import motorph.application.service.DatabaseBackupService;
import motorph.application.service.EmailPreviewService;
import motorph.application.service.EmployeeService;
import motorph.application.service.ITPasswordResetService;
import motorph.application.service.ITUserAccountService;
import motorph.application.service.LeaveService;
import motorph.application.service.OvertimeService;
import motorph.application.service.PasswordResetService;
import motorph.application.service.PayrollService;
import motorph.application.service.PermissionService;
import motorph.application.service.SalaryBenefitService;
import motorph.application.service.SupportTicketService;
import motorph.application.service.SystemConfigService;
import motorph.application.service.UserAccessLogService;
import motorph.application.service.UserAccountService;
import motorph.application.service.report.AttendanceReportService;
import motorph.application.service.report.AuditReportService;
import motorph.application.service.report.PayrollDashboardService;
import motorph.application.service.report.PayrollReportService;
import motorph.application.service.report.TicketReportService;

/** Provides application services needed by UI controllers. */
public final class EmployeeUIServiceFactory {

    private static EmployeeUIServiceFactory instance;

    private final CoreServiceGroup core;
    private final AuthServiceGroup auth;
    private final PayrollServiceGroup payroll;
    private final OperationsServiceGroup operations;
    private final ITServiceGroup it;
    private final ReportServiceGroup reports;
    private final SystemServiceGroup system;

    // constructors
    private EmployeeUIServiceFactory() {
        this.core       = new CoreServiceGroup();
        this.auth       = new AuthServiceGroup(
                core.getUserAccountRepository(),
                core.getEmployeeService(),
                core.getTwoFactorService(),
                core.getEmailService(),
                core.getUserAccessLogService()
        );
        this.payroll    = new PayrollServiceGroup(core.getAttendanceService(), core.getEmployeeService());
        this.operations = new OperationsServiceGroup(core.getAttendanceService(), core.getEmployeeService());
        this.it         = new ITServiceGroup(
                core.getUserAccountRepository(),
                core.getEmployeeService(),
                auth.getPasswordResetService()
        );
        this.reports    = new ReportServiceGroup();
        this.system     = new SystemServiceGroup(core.getUserAccessLogService());
    }

    // initializes the singleton; must be called before getInstance()
    public static void initialize() {
        if (instance == null) {
            instance = new EmployeeUIServiceFactory();
        }
    }

    // returns the singleton instance; throws if initialize() was not called
    public static EmployeeUIServiceFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("EmployeeUIServiceFactory not initialized. Call EmployeeUIServiceFactory.initialize() first.");
        }
        return instance;
    }

    // getters
    public AuthenticationService getAuthenticationService() {
        return auth.getAuthenticationService();
    }

    public PasswordResetService getPasswordResetService() {
        return auth.getPasswordResetService();
    }

    public EmployeeService getEmployeeService() {
        return core.getEmployeeService();
    }

    public AttendanceService getAttendanceService() {
        return core.getAttendanceService();
    }

    public LeaveService getLeaveService() {
        return operations.getLeaveService();
    }

    public PayrollService getPayrollService() {
        return payroll.getPayrollService();
    }

    public OvertimeService getOvertimeService() {
        return payroll.getOvertimeService();
    }

    public UserAccountService getUserAccountService() {
        return core.getUserAccountService();
    }

    public UserAccessLogService getUserAccessLogService() {
        return core.getUserAccessLogService();
    }

    public SupportTicketService getSupportTicketService() {
        return it.getSupportTicketService();
    }

    public ITUserAccountService getITUserAccountService() {
        return it.getITUserAccountService();
    }

    public ITPasswordResetService getITPasswordResetService() {
        return it.getITPasswordResetService();
    }

    public EmailPreviewService getEmailPreviewService() {
        return core.getEmailPreviewService();
    }

    public SalaryBenefitService getSalaryBenefitService() {
        return payroll.getSalaryBenefitService();
    }

    public PermissionService getPermissionService() {
        return system.getPermissionService();
    }

    public SystemConfigService getSystemConfigService() {
        return system.getSystemConfigService();
    }

    public DatabaseBackupService getDatabaseBackupService() {
        return system.getDatabaseBackupService();
    }

    public AttendanceReportService getAttendanceReportService() {
        return reports.getAttendanceReportService();
    }

    public AuditReportService getAuditReportService() {
        return reports.getAuditReportService();
    }

    public PayrollReportService getPayrollReportService() {
        return reports.getPayrollReportService();
    }

    public PayrollDashboardService getPayrollDashboardService() {
        return reports.getPayrollDashboardService();
    }

    public TicketReportService getTicketReportService() {
        return reports.getTicketReportService();
    }
}
