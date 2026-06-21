package motorph.ui.factory;

import motorph.application.service.AuthenticationService;
import motorph.application.service.EmailService;
import motorph.application.service.EmployeeService;
import motorph.application.service.PasswordResetService;
import motorph.application.service.TwoFactorService;
import motorph.application.service.UserAccessLogService;
import motorph.infrastructure.repository.PasswordResetRequestRepository;
import motorph.infrastructure.repository.UserAccountRepository;

/** Owns authentication and password reset services. */
class AuthServiceGroup {

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

    // constructors
    AuthServiceGroup(UserAccountRepository userAccountRepository,
                     EmployeeService employeeService,
                     TwoFactorService twoFactorService,
                     EmailService emailService,
                     UserAccessLogService userAccessLogService) {

        this.authenticationService = new AuthenticationService(
                userAccountRepository,
                employeeService,
                twoFactorService,
                emailService,
                userAccessLogService
        );

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
    AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    PasswordResetService getPasswordResetService() {
        return passwordResetService;
    }
}
