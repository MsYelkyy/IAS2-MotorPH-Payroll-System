package motorph.ui.factory;

import motorph.application.service.EmployeeService;
import motorph.application.service.ITPasswordResetService;
import motorph.application.service.ITUserAccountService;
import motorph.application.service.PasswordResetService;
import motorph.application.service.SupportTicketService;
import motorph.infrastructure.repository.PasswordResetRequestRepository;
import motorph.infrastructure.repository.SupportTicketRepository;
import motorph.infrastructure.repository.UserAccountRepository;

/** Owns IT support and account management services. */
class ITServiceGroup {

    private final SupportTicketService supportTicketService;
    private final ITUserAccountService itUserAccountService;
    private final ITPasswordResetService itPasswordResetService;

    // constructors
    ITServiceGroup(UserAccountRepository userAccountRepository,
                   EmployeeService employeeService,
                   PasswordResetService passwordResetService) {

        this.supportTicketService = new SupportTicketService(new SupportTicketRepository());

        this.itUserAccountService = new ITUserAccountService(
                userAccountRepository,
                employeeService
        );

        this.itPasswordResetService = new ITPasswordResetService(
                passwordResetService,
                new PasswordResetRequestRepository(),
                userAccountRepository,
                employeeService
        );
    }

    // getters
    SupportTicketService getSupportTicketService() {
        return supportTicketService;
    }

    ITUserAccountService getITUserAccountService() {
        return itUserAccountService;
    }

    ITPasswordResetService getITPasswordResetService() {
        return itPasswordResetService;
    }
}
