package motorph.ui.factory;

import motorph.application.service.EmailPreviewService;
import motorph.application.service.EmailService;
import motorph.application.service.EmployeeService;
import motorph.application.service.UserAccountService;
import motorph.infrastructure.config.EmailConfig;
import motorph.infrastructure.repository.DepartmentRepository;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.GovernmentDetailsRepository;
import motorph.infrastructure.repository.PositionRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;
import motorph.infrastructure.repository.UserAccountRepository;

/**
 * The last desktop-side services still constructed locally. Everything the
 * IT/Admin screens used to get from here now comes from the backend over
 * REST; what remains is EmployeeService (still backing the sidebar identity
 * lookup, the Admin dashboard headcounts, and employee self-service profile
 * edits) plus the account/email collaborators it needs, and the in-app email
 * preview, which never touched the database.
 */
class CoreServiceGroup {

    private final EmailPreviewService emailPreviewService;
    private final EmployeeService employeeService;

    CoreServiceGroup() {
        this.emailPreviewService = new EmailPreviewService();

        EmailService emailService = new EmailService(EmailConfig.previewOnly(), emailPreviewService);
        UserAccountService userAccountService = new UserAccountService(new UserAccountRepository(), emailService);

        this.employeeService = new EmployeeService(
                new EmployeeRepository(),
                userAccountService,
                new DepartmentRepository(),
                new PositionRepository(),
                new SalaryHistoryRepository(),
                new EmployeeBenefitRepository(),
                new GovernmentDetailsRepository()
        );
    }

    EmailPreviewService getEmailPreviewService() {
        return emailPreviewService;
    }

    EmployeeService getEmployeeService() {
        return employeeService;
    }
}
