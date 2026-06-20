package motorph.application.service;


import motorph.domain.model.UserAccount;
import motorph.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Tests user account service using mocked dependencies.
 */
@DisplayName("UserAccountService Mock Tests")
class UserAccountServiceMockTest {


    // positive: creates temporary employee account without using real database
    @Test
    @DisplayName("Positive - createTemporaryEmployeeAccount should create active temporary account")
    void createTemporaryEmployeeAccountShouldCreateActiveTemporaryAccount() {
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        EmailService emailService = mock(EmailService.class);


        when(userAccountRepository.existsByCompanyEmail("juan.delacruz@motorph.com"))
                .thenReturn(false);


        when(userAccountRepository.findByCompanyEmail("juan.delacruz@motorph.com"))
                .thenReturn(new UserAccount(
                        1,
                        10006,
                        "juan.delacruz@motorph.com",
                        "juan.delacruz@motorph.com",
                        "sampleHash",
                        "sampleSalt",
                        motorph.domain.value.Role.EMPLOYEE,
                        false,
                        true,
                        null
                ));


        UserAccountService userAccountService = new UserAccountService(
                userAccountRepository,
                emailService
        );


        UserAccount account = userAccountService.createTemporaryEmployeeAccount(
                10006,
                "Juan",
                "Dela Cruz",
                "juan.personal@email.com"
        );


        assertNotNull(account);
        assertEquals(10006, account.getEmployeeId());
        assertTrue(account.isActive());
        assertFalse(account.isLocked());


        verify(userAccountRepository).save(any(UserAccount.class));
        verify(emailService).sendEmployeeAccountCredentials(
                "juan.personal@email.com",
                "juan.delacruz@motorph.com",
                "MotorPH@10006"
        );
    }
}

