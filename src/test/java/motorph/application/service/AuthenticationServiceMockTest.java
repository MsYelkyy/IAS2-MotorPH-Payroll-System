package motorph.application.service;


import motorph.application.dto.login.LoginRequestDTO;
import motorph.application.dto.login.LoginResultDTO;
import motorph.application.exception.AuthenticationException;
import motorph.application.security.PasswordHasher;
import motorph.domain.model.UserAccount;
import motorph.domain.value.OtpPurpose;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Tests authentication business rules using mocked dependencies.
 */
@DisplayName("AuthenticationService Mock Tests")
class AuthenticationServiceMockTest {


    private static final Logger logger = Logger.getLogger(AuthenticationServiceMockTest.class.getName());

    private static final String EMAIL = "juan.delacruz@motorph.com";
    private static final String PASSWORD = "Secret123!";


    // valid login returns an OTP-required result and sends the OTP
    @Test
    @DisplayName("Positive - authenticate should require OTP for valid credentials")
    void authenticateShouldRequireOtpForValidCredentials() {
        logger.info("Testing valid credentials require OTP.");

        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        TwoFactorService twoFactorService = mock(TwoFactorService.class);
        EmailService emailService = mock(EmailService.class);

        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(activeAccount());
        when(employeeService.getPersonalEmail(anyInt())).thenReturn("juan.personal@email.com");
        when(twoFactorService.createOtp(anyInt(), any(), eq(OtpPurpose.LOGIN_2FA))).thenReturn("123456");

        AuthenticationService service = new AuthenticationService(
                userAccountRepository, employeeService, twoFactorService, emailService, null);

        LoginResultDTO result = service.authenticate(new LoginRequestDTO(EMAIL, PASSWORD));

        assertTrue(result.isOtpRequired());
        assertFalse(result.isAuthenticated());
        verify(emailService).sendOtp(eq(EMAIL), eq("123456"), anyString());
    }


    // wrong password is rejected even when the email is valid
    @Test
    @DisplayName("Negative - authenticate should reject incorrect password")
    void authenticateShouldRejectIncorrectPassword() {
        logger.info("Testing incorrect password rejection.");

        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(activeAccount());

        AuthenticationService service = new AuthenticationService(
                userAccountRepository, mock(EmployeeService.class),
                mock(TwoFactorService.class), mock(EmailService.class), null);

        assertThrows(
                AuthenticationException.class,
                () -> service.authenticate(new LoginRequestDTO(EMAIL, "WrongPassword!"))
        );
    }


    // a locked account cannot log in despite a correct password
    @Test
    @DisplayName("Negative - authenticate should reject a locked account")
    void authenticateShouldRejectLockedAccount() {
        logger.info("Testing locked account rejection.");

        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(lockedAccount());

        AuthenticationService service = new AuthenticationService(
                userAccountRepository, mock(EmployeeService.class),
                mock(TwoFactorService.class), mock(EmailService.class), null);

        assertThrows(
                AuthenticationException.class,
                () -> service.authenticate(new LoginRequestDTO(EMAIL, PASSWORD))
        );
    }


    // an inactive account cannot log in despite a correct password
    @Test
    @DisplayName("Negative - authenticate should reject an inactive account")
    void authenticateShouldRejectInactiveAccount() {
        logger.info("Testing inactive account rejection.");

        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(inactiveAccount());

        AuthenticationService service = new AuthenticationService(
                userAccountRepository, mock(EmployeeService.class),
                mock(TwoFactorService.class), mock(EmailService.class), null);

        assertThrows(
                AuthenticationException.class,
                () -> service.authenticate(new LoginRequestDTO(EMAIL, PASSWORD))
        );
    }


    // the assigned role is carried into the login result
    @Test
    @DisplayName("Positive - authenticate should carry the assigned role into the result")
    void authenticateShouldCarryAssignedRole() {
        logger.info("Testing assigned role is carried into the login result.");

        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        TwoFactorService twoFactorService = mock(TwoFactorService.class);

        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(activeAccount());
        when(employeeService.getPersonalEmail(anyInt())).thenReturn("juan.personal@email.com");
        when(twoFactorService.createOtp(anyInt(), any(), any())).thenReturn("123456");

        AuthenticationService service = new AuthenticationService(
                userAccountRepository, employeeService, twoFactorService, mock(EmailService.class), null);

        LoginResultDTO result = service.authenticate(new LoginRequestDTO(EMAIL, PASSWORD));

        assertEquals(Role.EMPLOYEE, result.getActiveRole());
    }


    // verifyLoginOtp completes login and updates the last login time
    @Test
    @DisplayName("Positive - verifyLoginOtp should complete login and update last login")
    void verifyLoginOtpShouldCompleteLogin() {
        logger.info("Testing login OTP verification completes login.");

        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        TwoFactorService twoFactorService = mock(TwoFactorService.class);

        when(userAccountRepository.findById(1)).thenReturn(activeAccount());

        AuthenticationService service = new AuthenticationService(
                userAccountRepository, mock(EmployeeService.class),
                twoFactorService, mock(EmailService.class), null);

        LoginResultDTO result = service.verifyLoginOtp(1, "123456");

        assertTrue(result.isAuthenticated());
        verify(twoFactorService).verifyLoginOtp(1, "123456");
        verify(userAccountRepository).updateLastLogin(1);
    }


    // a temporary-password account returns a password-change result before OTP
    @Test
    @DisplayName("Positive - authenticate should return temporary-password result when change is required")
    void authenticateShouldReturnTemporaryPasswordResult() {
        logger.info("Testing temporary-password login result.");

        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        when(userAccountRepository.findByCompanyEmail(EMAIL))
                .thenReturn(temporaryPasswordAccount(LocalDateTime.now().plusDays(1)));

        AuthenticationService service = new AuthenticationService(
                userAccountRepository, mock(EmployeeService.class),
                mock(TwoFactorService.class), mock(EmailService.class), null);

        LoginResultDTO result = service.authenticate(new LoginRequestDTO(EMAIL, PASSWORD));

        // temporary password is verified but login is not yet complete and no OTP is sent
        assertFalse(result.isAuthenticated());
        assertFalse(result.isOtpRequired());
    }


    // an expired temporary password is rejected
    @Test
    @DisplayName("Negative - authenticate should reject an expired temporary password")
    void authenticateShouldRejectExpiredTemporaryPassword() {
        logger.info("Testing expired temporary password rejection.");

        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        when(userAccountRepository.findByCompanyEmail(EMAIL))
                .thenReturn(temporaryPasswordAccount(LocalDateTime.now().minusDays(1)));

        AuthenticationService service = new AuthenticationService(
                userAccountRepository, mock(EmployeeService.class),
                mock(TwoFactorService.class), mock(EmailService.class), null);

        assertThrows(
                AuthenticationException.class,
                () -> service.authenticate(new LoginRequestDTO(EMAIL, PASSWORD))
        );
    }


    // builds an active employee account with a real password hash
    private UserAccount activeAccount() {
        return account(false, true);
    }

    // builds a locked account
    private UserAccount lockedAccount() {
        return account(true, true);
    }

    // builds an inactive account
    private UserAccount inactiveAccount() {
        return account(false, false);
    }

    // builds an account with a salt/hash that matches PASSWORD
    private UserAccount account(boolean locked, boolean active) {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(PASSWORD, salt);

        return new UserAccount(
                1, 10001, EMAIL, EMAIL,
                hash, salt,
                List.of(Role.EMPLOYEE),
                locked, active, null, false, null
        );
    }

    // builds an active account that must change a temporary password by the given expiry
    private UserAccount temporaryPasswordAccount(LocalDateTime expiresAt) {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(PASSWORD, salt);

        return new UserAccount(
                1, 10001, EMAIL, EMAIL,
                hash, salt,
                List.of(Role.EMPLOYEE),
                false, true, null, true, expiresAt
        );
    }
}
