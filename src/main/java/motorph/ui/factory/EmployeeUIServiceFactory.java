package motorph.ui.factory;

import motorph.application.service.AttendanceService;
import motorph.application.service.DatabaseBackupService;
import motorph.application.service.PermissionService;
import motorph.application.service.SalaryBenefitService;
import motorph.application.service.AuthenticationService;
import motorph.application.service.EmailService;
import motorph.application.service.EmployeeService;
import motorph.application.service.LeaveService;
import motorph.application.service.OvertimeService;
import motorph.application.service.PasswordResetService;
import motorph.application.service.PayrollService;
import motorph.application.service.SupportTicketService;
import motorph.application.service.TwoFactorService;
import motorph.application.service.UserAccessLogService;
import motorph.application.service.UserAccountService;
import motorph.domain.calculator.PayrollCalculator;
import motorph.infrastructure.config.EmailConfig;
import motorph.infrastructure.repository.AttendanceRepository;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.GovernmentDetailsRepository;
import motorph.infrastructure.repository.LeaveCreditRepository;
import motorph.infrastructure.repository.LeaveRepository;
import motorph.infrastructure.repository.OvertimeRepository;
import motorph.infrastructure.repository.PasswordResetRequestRepository;
import motorph.infrastructure.repository.PermissionRepository;
import motorph.infrastructure.repository.PayrollBenefitRepository;
import motorph.infrastructure.repository.PayrollDeductionRepository;
import motorph.infrastructure.repository.PayrollEarningRepository;
import motorph.infrastructure.repository.PayrollResultRepository;
import motorph.infrastructure.repository.PayrunRepository;
import motorph.infrastructure.repository.DepartmentRepository;
import motorph.infrastructure.repository.PositionRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;
import motorph.infrastructure.repository.SupportTicketRepository;
import motorph.infrastructure.repository.TwoFactorCodeRepository;
import motorph.infrastructure.repository.UserAccessLogRepository;
import motorph.infrastructure.repository.UserAccountRepository;
import motorph.application.service.SystemConfigService;
import motorph.infrastructure.repository.PhilHealthRateRepository;
import motorph.infrastructure.repository.SssRateRepository;
import motorph.infrastructure.repository.PagIbigRateRepository;
import motorph.infrastructure.repository.WithholdingTaxBracketRepository;

/** Provides application services needed by UI controllers. */
public final class EmployeeUIServiceFactory {

    private static EmployeeUIServiceFactory instance;

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final PayrollService payrollService;
    private final UserAccountService userAccountService;
    private final UserAccessLogService userAccessLogService;
    private final SupportTicketService supportTicketService;
    private final SalaryBenefitService salaryBenefitService;
    private final PermissionService permissionService;
    private final SystemConfigService systemConfigService;
    private final DatabaseBackupService databaseBackupService;

    // creates all service instances by wiring repositories together
    private EmployeeUIServiceFactory() {
        UserAccountRepository userAccountRepository = new UserAccountRepository();
        EmployeeRepository employeeRepository = new EmployeeRepository();
        AttendanceRepository attendanceRepository = new AttendanceRepository();
        UserAccessLogRepository userAccessLogRepository = new UserAccessLogRepository();
        TwoFactorCodeRepository twoFactorCodeRepository = new TwoFactorCodeRepository();

        EmailService emailService = new EmailService(EmailConfig.mailtrapSandbox());
        UserAccountService sharedUserAccountService = new UserAccountService(userAccountRepository, emailService);
        EmployeeService sharedEmployeeService = new EmployeeService(
                employeeRepository,
                sharedUserAccountService,
                new DepartmentRepository(),
                new PositionRepository()
        );
        AttendanceService sharedAttendanceService = new AttendanceService(attendanceRepository);
        TwoFactorService twoFactorService = new TwoFactorService(twoFactorCodeRepository);
        UserAccessLogService sharedAccessLogService = new UserAccessLogService(userAccessLogRepository);

        OvertimeService overtimeService = new OvertimeService(
                new OvertimeRepository(),
                sharedAttendanceService,
                sharedEmployeeService
        );

        this.userAccountService = sharedUserAccountService;
        this.employeeService = sharedEmployeeService;
        this.attendanceService = sharedAttendanceService;
        this.userAccessLogService = sharedAccessLogService;

        this.leaveService = new LeaveService(
                new LeaveRepository(),
                new LeaveCreditRepository(),
                sharedEmployeeService
        );

        this.payrollService = new PayrollService(
                new PayrunRepository(),
                new PayrollResultRepository(),
                new PayrollEarningRepository(),
                new PayrollBenefitRepository(),
                new PayrollDeductionRepository(),
                new SalaryHistoryRepository(),
                new EmployeeBenefitRepository(),
                new GovernmentDetailsRepository(),
                sharedAttendanceService,
                overtimeService,
                sharedEmployeeService,
                new PayrollCalculator()
        );

        this.authenticationService = new AuthenticationService(
                userAccountRepository,
                employeeRepository,
                twoFactorService,
                emailService,
                sharedAccessLogService
        );

        this.passwordResetService = new PasswordResetService(
                new PasswordResetRequestRepository(),
                userAccountRepository,
                employeeRepository,
                twoFactorService,
                emailService,
                sharedAccessLogService
        );

        this.supportTicketService = new SupportTicketService(
                new SupportTicketRepository()
        );

        this.salaryBenefitService = new SalaryBenefitService(
                new SalaryHistoryRepository(),
                new EmployeeBenefitRepository()
        );

        this.permissionService = new PermissionService(new PermissionRepository());
        this.databaseBackupService = new DatabaseBackupService(sharedAccessLogService);
        this.systemConfigService = new SystemConfigService(
                new PhilHealthRateRepository(),
                new SssRateRepository(),
                new PagIbigRateRepository(),
                new WithholdingTaxBracketRepository(),
                sharedAccessLogService
        );
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
        return authenticationService;
    }

    public PasswordResetService getPasswordResetService() {
        return passwordResetService;
    }

    public EmployeeService getEmployeeService() {
        return employeeService;
    }

    public AttendanceService getAttendanceService() {
        return attendanceService;
    }

    public LeaveService getLeaveService() {
        return leaveService;
    }

    public PayrollService getPayrollService() {
        return payrollService;
    }

    public UserAccountService getUserAccountService() {
        return userAccountService;
    }

    public UserAccessLogService getUserAccessLogService() {
        return userAccessLogService;
    }

    public SupportTicketService getSupportTicketService() {
        return supportTicketService;
    }

    public SalaryBenefitService getSalaryBenefitService() {
        return salaryBenefitService;
    }

    public PermissionService getPermissionService() {
        return permissionService;
    }

    public SystemConfigService getSystemConfigService() {
        return systemConfigService;
    }

    public DatabaseBackupService getDatabaseBackupService() {
        return databaseBackupService;
    }
}