package motorph.ui.factory;

import motorph.application.service.EmployeeService;
import motorph.application.service.ITPasswordResetService;
import motorph.application.service.ITUserAccountService;
import motorph.application.service.PasswordResetService;
import motorph.infrastructure.repository.PasswordResetRequestRepository;
import motorph.infrastructure.repository.UserAccountRepository;

/** Owns IT account management services. */
class ITServiceGroup {

    private final ITUserAccountService itUserAccountService;
    private final ITPasswordResetService itPasswordResetService;

    // constructors
    ITServiceGroup(UserAccountRepository userAccountRepository,
                   EmployeeService employeeService,
                   PasswordResetService passwordResetService) {

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
    ITUserAccountService getITUserAccountService() {
        return itUserAccountService;
    }

    ITPasswordResetService getITPasswordResetService() {
        return itPasswordResetService;
    }
}
