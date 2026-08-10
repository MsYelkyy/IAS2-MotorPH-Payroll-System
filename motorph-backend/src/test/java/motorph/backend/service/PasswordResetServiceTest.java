package motorph.backend.service;

import motorph.backend.domain.PasswordResetRequest;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.PasswordResetRequestRepository;
import motorph.backend.repository.UserAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The forgot-password path is reachable unauthenticated, so its whole
 * contract is that a caller cannot tell the difference between an address
 * that gets a reset and one that does not. Every rejection case below has to
 * behave identically from the outside: no exception, no reset row.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetServiceTest {

    private static final String EMAIL = "juan.cruz@motorph.com";

    @Mock private PasswordResetRequestRepository passwordResetRequestRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PasswordResetDeliveryService deliveryService;
    @Mock private UserAccessLogService userAccessLogService;

    @InjectMocks private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        when(employeeRepository.findPersonalEmailByEmployeeId(anyInt())).thenReturn("juan@personal.com");
        when(passwordResetRequestRepository.findLatestPendingByUserAccountId(anyInt())).thenReturn(null);
    }

    @Test
    @DisplayName("An eligible account gets a pending reset request")
    void requestPasswordResetCreatesRequestForEligibleAccount() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(true, false));

        passwordResetService.requestPasswordReset(EMAIL);

        verify(passwordResetRequestRepository).saveAndReturnId(any(PasswordResetRequest.class));
    }

    @Test
    @DisplayName("An unknown company email is accepted silently and creates nothing")
    void requestPasswordResetStaysSilentForUnknownEmail() {
        when(userAccountRepository.findByCompanyEmail(anyString())).thenReturn(null);

        assertThatCode(() -> passwordResetService.requestPasswordReset("nobody@motorph.com"))
                .doesNotThrowAnyException();

        verify(passwordResetRequestRepository, never()).saveAndReturnId(any());
    }

    @Test
    @DisplayName("A disabled account is accepted silently and creates nothing")
    void requestPasswordResetStaysSilentForInactiveAccount() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(false, false));

        assertThatCode(() -> passwordResetService.requestPasswordReset(EMAIL))
                .doesNotThrowAnyException();

        verify(passwordResetRequestRepository, never()).saveAndReturnId(any());
    }

    @Test
    @DisplayName("A locked account is accepted silently and creates nothing")
    void requestPasswordResetStaysSilentForLockedAccount() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(true, true));

        assertThatCode(() -> passwordResetService.requestPasswordReset(EMAIL))
                .doesNotThrowAnyException();

        verify(passwordResetRequestRepository, never()).saveAndReturnId(any());
    }

    @Test
    @DisplayName("An account with no personal email on file is accepted silently and creates nothing")
    void requestPasswordResetStaysSilentWithoutPersonalEmail() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(true, false));
        when(employeeRepository.findPersonalEmailByEmployeeId(anyInt())).thenReturn("  ");

        assertThatCode(() -> passwordResetService.requestPasswordReset(EMAIL))
                .doesNotThrowAnyException();

        verify(passwordResetRequestRepository, never()).saveAndReturnId(any());
    }

    @Test
    @DisplayName("A second request while one is pending is accepted silently and creates nothing")
    void requestPasswordResetStaysSilentWhenAlreadyPending() {
        when(userAccountRepository.findByCompanyEmail(EMAIL)).thenReturn(account(true, false));
        when(passwordResetRequestRepository.findLatestPendingByUserAccountId(anyInt()))
                .thenReturn(new PasswordResetRequest(7));

        assertThatCode(() -> passwordResetService.requestPasswordReset(EMAIL))
                .doesNotThrowAnyException();

        verify(passwordResetRequestRepository, never()).saveAndReturnId(any());
    }

    private UserAccount account(boolean active, boolean locked) {
        return new UserAccount(
                7, 101, EMAIL, EMAIL, "hash", "salt",
                List.of(Role.EMPLOYEE), locked, active, null, false, null);
    }
}
