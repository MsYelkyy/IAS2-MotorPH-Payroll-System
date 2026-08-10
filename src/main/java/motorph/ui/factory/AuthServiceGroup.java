package motorph.ui.factory;

import motorph.application.service.EmailService;
import motorph.application.service.EmployeeService;
import motorph.application.service.PasswordResetService;
import motorph.application.service.TwoFactorService;
import motorph.application.service.UserAccessLogService;
import motorph.infrastructure.repository.PasswordResetRequestRepository;
import motorph.infrastructure.repository.UserAccountRepository;

/** Owns password reset services. */
class AuthServiceGroup {

    private final PasswordResetService passwordResetService;

    // constructors
    AuthServiceGroup(UserAccountRepository userAccountRepository,
                     EmployeeService employeeService,
                     TwoFactorService twoFactorService,
                     EmailService emailService,
                     UserAccessLogService userAccessLogService) {

        this.passwordResetService = new PasswordResetService(
                new PasswordResetRequestRepository(),
                userAccountRepository,
                employeeService,
                twoFactorService,
                emailService,
                userAccessLogService
        );
    }

    // getters
    PasswordResetService getPasswordResetService() {
        return passwordResetService;
    }
}
