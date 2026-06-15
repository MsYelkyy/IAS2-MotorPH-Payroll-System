package motorph.application.service;

import motorph.application.dto.auth.EmailPreviewDTO;
import motorph.application.dto.login.LoginRequestDTO;
import motorph.application.dto.login.LoginResultDTO;
import motorph.infrastructure.config.EmailConfig;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.TwoFactorCodeRepository;
import motorph.infrastructure.repository.UserAccessLogRepository;
import motorph.infrastructure.repository.UserAccountRepository;

/**
 * Handles login operations and hides infrastructure dependencies from the UI.
 */
public class LoginServiceImpl implements LoginService {

    private final AuthenticationService authenticationService;
    private final EmailService emailService;

    // constructors
    public LoginServiceImpl() {
        this.emailService = new EmailService(EmailConfig.mailtrapSandbox());
        this.authenticationService = buildAuthenticationService(emailService);
    }

    // authenticates user credentials and sends OTP when required
    @Override
    public LoginResultDTO authenticate(LoginRequestDTO request) {
        return authenticationService.authenticate(request);
    }

    // verifies login OTP and completes login
    @Override
    public LoginResultDTO verifyLoginOtp(int userAccountId, String rawOtp) {
        return authenticationService.verifyLoginOtp(userAccountId, rawOtp);
    }

    // checks if a QA email preview is available
    @Override
    public boolean hasEmailPreview() {
        return emailService.hasEmailPreview();
    }

    // returns the latest QA email preview
    @Override
    public EmailPreviewDTO getLatestEmailPreview() {
        return emailService.getLatestEmailPreview();
    }

    // creates authentication service dependencies
    private AuthenticationService buildAuthenticationService(EmailService emailService) {
        UserAccountRepository userAccountRepository = new UserAccountRepository();
        EmployeeRepository employeeRepository = new EmployeeRepository();
        TwoFactorCodeRepository twoFactorCodeRepository = new TwoFactorCodeRepository();
        UserAccessLogRepository userAccessLogRepository = new UserAccessLogRepository();

        TwoFactorService twoFactorService = new TwoFactorService(twoFactorCodeRepository);
        UserAccessLogService userAccessLogService = new UserAccessLogService(userAccessLogRepository);

        return new AuthenticationService(
                userAccountRepository,
                employeeRepository,
                twoFactorService,
                emailService,
                userAccessLogService
        );
    }
}