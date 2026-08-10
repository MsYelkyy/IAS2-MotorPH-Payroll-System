package motorph.ui.factory;

import motorph.application.service.EmailPreviewService;
import motorph.application.service.EmailService;
import motorph.application.service.EmployeeService;
import motorph.application.service.TwoFactorService;
import motorph.application.service.UserAccessLogService;
import motorph.application.service.UserAccountService;
import motorph.infrastructure.config.EmailConfig;
import motorph.infrastructure.repository.DepartmentRepository;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.GovernmentDetailsRepository;
import motorph.infrastructure.repository.PositionRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;
import motorph.infrastructure.repository.TwoFactorCodeRepository;
import motorph.infrastructure.repository.UserAccessLogRepository;
import motorph.infrastructure.repository.UserAccountRepository;

/** Owns the foundational shared services used across all other groups. */
class CoreServiceGroup {

    private final EmailPreviewService emailPreviewService;
    private final EmailService emailService;
    private final UserAccountService userAccountService;
    private final EmployeeService employeeService;
    private final TwoFactorService twoFactorService;
    private final UserAccessLogService userAccessLogService;
    private final UserAccountRepository userAccountRepository;

    // constructors
    CoreServiceGroup() {
        UserAccountRepository userAccountRepository = new UserAccountRepository();
        this.userAccountRepository = userAccountRepository;

        this.emailPreviewService = new EmailPreviewService();
        this.emailService = new EmailService(EmailConfig.previewOnly(), emailPreviewService);

        this.userAccountService = new UserAccountService(userAccountRepository, emailService);

        this.employeeService = new EmployeeService(
                new EmployeeRepository(),
                userAccountService,
                new DepartmentRepository(),
                new PositionRepository(),
                new SalaryHistoryRepository(),
                new EmployeeBenefitRepository(),
                new GovernmentDetailsRepository()
        );

        this.twoFactorService = new TwoFactorService(new TwoFactorCodeRepository());
        this.userAccessLogService = new UserAccessLogService(new UserAccessLogRepository());
    }

    // getters
    EmailPreviewService getEmailPreviewService() {
        return emailPreviewService;
    }

    EmailService getEmailService() {
        return emailService;
    }

    UserAccountService getUserAccountService() {
        return userAccountService;
    }

    EmployeeService getEmployeeService() {
        return employeeService;
    }

    TwoFactorService getTwoFactorService() {
        return twoFactorService;
    }

    UserAccessLogService getUserAccessLogService() {
        return userAccessLogService;
    }

    UserAccountRepository getUserAccountRepository() {
        return userAccountRepository;
    }
}
