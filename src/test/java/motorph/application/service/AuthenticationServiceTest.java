package motorph.application.service;

import motorph.application.dto.login.LoginRequestDTO;
import motorph.application.dto.login.LoginResultDTO;
import motorph.application.exception.AuthenticationException;
import motorph.application.exception.ValidationException;
import motorph.application.security.PasswordHasher;
import motorph.domain.model.UserAccount;
import motorph.domain.value.OtpPurpose;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests authentication service login and OTP behavior.
 */
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    private AuthenticationService authenticationService;
    private UserAccountRepository userAccountRepository;
    private EmployeeRepository employeeRepository;
    private TwoFactorService twoFactorService;
    private EmailService emailService;
    private UserAccessLogService userAccessLogService;

    private static final String VALID_EMAIL = "user@company.com";
    private static final String VALID_PASSWORD = "password123";
    private static final String PERSONAL_EMAIL = "personal@email.com";
    private static final int VALID_USER_ID = 1;
    private static final int VALID_EMPLOYEE_ID = 101;

    @BeforeEach
    void setUp() {
        userAccountRepository = mock(UserAccountRepository.class);
        employeeRepository = mock(EmployeeRepository.class);
        twoFactorService = mock(TwoFactorService.class);
        emailService = mock(EmailService.class);
        userAccessLogService = mock(UserAccessLogService.class);

        authenticationService = new AuthenticationService(
                userAccountRepository,
                employeeRepository,
                twoFactorService,
                emailService,
                userAccessLogService
        );
    }

    // rejects null login request
    @Test
    @DisplayName("authenticate should reject null login request")
    void authenticateShouldRejectNullRequest() {
        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(null));
    }

    // rejects blank company email
    @Test
    @DisplayName("authenticate should reject blank company email")
    void authenticateShouldRejectBlankEmail() {
        LoginRequestDTO request = new LoginRequestDTO("", VALID_PASSWORD);

        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(request));
    }

    // rejects blank password
    @Test
    @DisplayName("authenticate should reject blank password")
    void authenticateShouldRejectBlankPassword() {
        LoginRequestDTO request = new LoginRequestDTO(VALID_EMAIL, "");

        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(request));
    }

    // rejects zero user account id
    @Test
    @DisplayName("verifyLoginOtp should reject zero user account ID")
    void verifyLoginOtpShouldRejectZeroUserAccountId() {
        assertThrows(ValidationException.class,
                () -> authenticationService.verifyLoginOtp(0, "123456"));
    }

    // rejects negative user account id
    @Test
    @DisplayName("verifyLoginOtp should reject negative user account ID")
    void verifyLoginOtpShouldRejectNegativeUserAccountId() {
        assertThrows(ValidationException.class,
                () -> authenticationService.verifyLoginOtp(-1, "123456"));
    }

    // rejects login when email does not exist
    @Test
    @DisplayName("authenticate should reject unknown company email")
    void authenticateShouldRejectUnknownEmail() {
        LoginRequestDTO request = new LoginRequestDTO("notfound@company.com", VALID_PASSWORD);

        when(userAccountRepository.findByCompanyEmail("notfound@company.com"))
                .thenReturn(null);

        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(request));

        verify(userAccessLogService).logLoginFailed(
                "notfound@company.com",
                "Login failed because email was not found."
        );
    }

    // rejects inactive account
    @Test
    @DisplayName("authenticate should reject inactive account")
    void authenticateShouldRejectInactiveAccount() {
        LoginRequestDTO request = new LoginRequestDTO(VALID_EMAIL, VALID_PASSWORD);
        UserAccount inactiveAccount = createUserAccount(false, false, VALID_PASSWORD);

        when(userAccountRepository.findByCompanyEmail(VALID_EMAIL))
                .thenReturn(inactiveAccount);

        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(request));

        verify(userAccessLogService).logLoginFailed(
                VALID_EMAIL,
                "Login failed because account is inactive."
        );
    }

    // rejects locked account
    @Test
    @DisplayName("authenticate should reject locked account")
    void authenticateShouldRejectLockedAccount() {
        LoginRequestDTO request = new LoginRequestDTO(VALID_EMAIL, VALID_PASSWORD);
        UserAccount lockedAccount = createUserAccount(true, true, VALID_PASSWORD);

        when(userAccountRepository.findByCompanyEmail(VALID_EMAIL))
                .thenReturn(lockedAccount);

        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(request));

        verify(userAccessLogService).logLoginFailed(
                VALID_EMAIL,
                "Login failed because account is locked."
        );
    }

    // rejects wrong password
    @Test
    @DisplayName("authenticate should reject wrong password")
    void authenticateShouldRejectWrongPassword() {
        LoginRequestDTO request = new LoginRequestDTO(VALID_EMAIL, "wrongPassword");
        UserAccount activeAccount = createUserAccount(true, false, VALID_PASSWORD);

        when(userAccountRepository.findByCompanyEmail(VALID_EMAIL))
                .thenReturn(activeAccount);

        assertThrows(AuthenticationException.class,
                () -> authenticationService.authenticate(request));

        verify(userAccessLogService).logLoginFailed(
                VALID_EMAIL,
                "Login failed because password did not match."
        );
    }

    // sends OTP when credentials are valid
    @Test
    @DisplayName("authenticate should send OTP when credentials are valid")
    void authenticateShouldSendOtpWhenCredentialsAreValid() {
        LoginRequestDTO request = new LoginRequestDTO(VALID_EMAIL, VALID_PASSWORD);
        UserAccount activeAccount = createUserAccount(true, false, VALID_PASSWORD);

        when(userAccountRepository.findByCompanyEmail(VALID_EMAIL))
                .thenReturn(activeAccount);

        when(employeeRepository.findPersonalEmailByEmployeeId(VALID_EMPLOYEE_ID))
                .thenReturn(PERSONAL_EMAIL);

        when(twoFactorService.createOtp(VALID_USER_ID, null, OtpPurpose.LOGIN_2FA))
                .thenReturn("123456");

        LoginResultDTO result = authenticationService.authenticate(request);

        assertNotNull(result);
        assertEquals(VALID_USER_ID, result.getUserAccountId());
        assertFalse(result.isAuthenticated());
        assertTrue(result.isOtpRequired());

        verify(emailService).sendOtp(
                PERSONAL_EMAIL,
                "123456",
                "MotorPH Login Verification"
        );
    }

    // rejects OTP verification when user account does not exist
    @Test
    @DisplayName("verifyLoginOtp should reject missing user account")
    void verifyLoginOtpShouldRejectMissingUserAccount() {
        when(userAccountRepository.findById(VALID_USER_ID))
                .thenReturn(null);

        assertThrows(AuthenticationException.class,
                () -> authenticationService.verifyLoginOtp(VALID_USER_ID, "123456"));
    }

    // completes login when OTP is valid
    @Test
    @DisplayName("verifyLoginOtp should complete login when OTP is valid")
    void verifyLoginOtpShouldCompleteLoginWhenOtpIsValid() {
        UserAccount activeAccount = createUserAccount(true, false, VALID_PASSWORD);

        when(userAccountRepository.findById(VALID_USER_ID))
                .thenReturn(activeAccount);

        LoginResultDTO result = authenticationService.verifyLoginOtp(VALID_USER_ID, "123456");

        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertFalse(result.isOtpRequired());

        verify(twoFactorService).verifyLoginOtp(VALID_USER_ID, "123456");
        verify(userAccountRepository).updateLastLogin(VALID_USER_ID);
        verify(userAccessLogService).logLoginSuccess(
                VALID_USER_ID,
                "Login successful."
        );
    }

    // constructor requires user account repository
    @Test
    @DisplayName("constructor should require UserAccountRepository")
    void constructorShouldRequireUserAccountRepository() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticationService(
                        null,
                        employeeRepository,
                        twoFactorService,
                        emailService,
                        userAccessLogService
                ));
    }

    // constructor requires employee repository
    @Test
    @DisplayName("constructor should require EmployeeRepository")
    void constructorShouldRequireEmployeeRepository() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticationService(
                        userAccountRepository,
                        null,
                        twoFactorService,
                        emailService,
                        userAccessLogService
                ));
    }

    // constructor requires two factor service
    @Test
    @DisplayName("constructor should require TwoFactorService")
    void constructorShouldRequireTwoFactorService() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticationService(
                        userAccountRepository,
                        employeeRepository,
                        null,
                        emailService,
                        userAccessLogService
                ));
    }

    // constructor requires email service
    @Test
    @DisplayName("constructor should require EmailService")
    void constructorShouldRequireEmailService() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticationService(
                        userAccountRepository,
                        employeeRepository,
                        twoFactorService,
                        null,
                        userAccessLogService
                ));
    }

    // creates a test user account with a valid hashed password
    private UserAccount createUserAccount(boolean active, boolean locked, String rawPassword) {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(rawPassword, salt);

        return new UserAccount(
                VALID_USER_ID,
                VALID_EMPLOYEE_ID,
                VALID_EMAIL,
                "username",
                hash,
                salt,
                Role.EMPLOYEE,
                locked,
                active,
                null
        );
    }
}