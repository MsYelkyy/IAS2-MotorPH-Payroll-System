package motorph.ui.factory;

import motorph.application.service.AttendanceService;
import motorph.application.service.AuthenticationService;
import motorph.application.service.EmailService;
import motorph.application.service.EmployeeService;
import motorph.application.service.LeaveService;
import motorph.application.service.OvertimeService;
import motorph.application.service.PasswordResetService;
import motorph.application.service.PayrollService;
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
import motorph.infrastructure.repository.PayrollBenefitRepository;
import motorph.infrastructure.repository.PayrollDeductionRepository;
import motorph.infrastructure.repository.PayrollEarningRepository;
import motorph.infrastructure.repository.PayrollResultRepository;
import motorph.infrastructure.repository.PayrunRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;
import motorph.infrastructure.repository.TwoFactorCodeRepository;
import motorph.infrastructure.repository.UserAccessLogRepository;
import motorph.infrastructure.repository.UserAccountRepository;

/**
 * Provides application services needed by employee UI controllers.
 */
public final class EmployeeUIServiceFactory {

    private static EmployeeUIServiceFactory instance;

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final PayrollService payrollService;
    private final UserAccountService userAccountService;

    // creates all service instances by wiring repositories together
    private EmployeeUIServiceFactory() {
        UserAccountRepository userAccountRepository = new UserAccountRepository();
        EmployeeRepository employeeRepository = new EmployeeRepository();
        AttendanceRepository attendanceRepository = new AttendanceRepository();
        EmailService emailService = new EmailService(EmailConfig.mailtrapSandbox());
        UserAccountService sharedUserAccountService = new UserAccountService(userAccountRepository, emailService);
        EmployeeService sharedEmployeeService = new EmployeeService(employeeRepository, sharedUserAccountService);
        AttendanceService sharedAttendanceService = new AttendanceService(attendanceRepository);
        TwoFactorService twoFactorService = new TwoFactorService(new TwoFactorCodeRepository());
        UserAccessLogService accessLogService = new UserAccessLogService(new UserAccessLogRepository());
        OvertimeService overtimeService = new OvertimeService(
                new OvertimeRepository(),
                sharedAttendanceService,
                sharedEmployeeService
        );

        this.userAccountService = sharedUserAccountService;
        this.employeeService = sharedEmployeeService;
        this.attendanceService = sharedAttendanceService;
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
                accessLogService
        );
        this.passwordResetService = new PasswordResetService(
                new PasswordResetRequestRepository(),
                userAccountRepository,
                employeeRepository,
                twoFactorService,
                emailService,
                accessLogService
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
}
