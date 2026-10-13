package motorph.backend.service;

import motorph.backend.domain.value.Role;
import motorph.backend.domain.value.SupportTicketCategory;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.SupportTicketRepository;
import motorph.backend.repository.TwoFactorCodeRepository;
import motorph.backend.repository.UserAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Validates request data at the controller level while keeping the same checks in the service.
 * This ensures the business rules still apply even when a service is called without a REST request.
 */
class ServiceInputValidationTest {

    private static final int EMPLOYEE_ID = 101;
    private static final int ACTOR_ID = 12;
    private static final int ACCOUNT_ID = 7;

    private SupportTicketRepository supportTicketRepository;
    private UserAccountRepository userAccountRepository;
    private UserAccessLogService userAccessLogService;

    private SupportTicketService supportTicketService;
    private UserAccountService userAccountService;
    private AuthenticationService authenticationService;
    private TwoFactorService twoFactorService;

    @BeforeEach
    void setUp() {
        supportTicketRepository = mock(SupportTicketRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        userAccessLogService = mock(UserAccessLogService.class);

        supportTicketService = new SupportTicketService(
                supportTicketRepository, mock(EmployeeRepository.class), userAccessLogService, mock(PermissionService.class));
        userAccountService = new UserAccountService(
                userAccountRepository, mock(EmployeeRepository.class),
                mock(EmployeeCredentialsDeliveryService.class), userAccessLogService);
        authenticationService = new AuthenticationService(
                userAccountRepository, mock(EmployeeRepository.class), mock(TwoFactorService.class),
                mock(OtpDeliveryService.class), userAccessLogService);
        twoFactorService = new TwoFactorService(mock(TwoFactorCodeRepository.class));
    }

    // Support tickets

    @ParameterizedTest(name = "subject \"{0}\"")
    @DisplayName("A ticket service call with a missing or blank subject is rejected")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void ticketSubjectIsStillRequired(String subject) {
        assertThatThrownBy(() -> supportTicketService.submitTicket(
                EMPLOYEE_ID, SupportTicketCategory.PAYROLL, subject, "Description", ACTOR_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Subject is required.");

        verify(supportTicketRepository, never()).save(any());
    }

    @ParameterizedTest(name = "description \"{0}\"")
    @DisplayName("A ticket service call with a missing or blank description is rejected")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void ticketDescriptionIsStillRequired(String description) {
        assertThatThrownBy(() -> supportTicketService.submitTicket(
                EMPLOYEE_ID, SupportTicketCategory.PAYROLL, "Subject", description, ACTOR_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Description is required.");

        verify(supportTicketRepository, never()).save(any());
    }

    @Test
    @DisplayName("A ticket service call with no category or no employee is rejected")
    void ticketCategoryAndEmployeeAreStillRequired() {
        assertThatThrownBy(() -> supportTicketService.submitTicket(EMPLOYEE_ID, null, "Subject", "Description", ACTOR_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Support ticket category is required.");
        assertThatThrownBy(() -> supportTicketService.submitTicket(
                0, SupportTicketCategory.PAYROLL, "Subject", "Description", ACTOR_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Employee ID is required.");

        verify(supportTicketRepository, never()).save(any());
    }

    // Account management

    @Test
    @DisplayName("A role update with no roles is rejected by the service, which the DTO deliberately allows")
    void roleListMustStillHaveAtLeastOneRole() {
        assertThatThrownBy(() -> userAccountService.updateRoles(ACTOR_ID, ACCOUNT_ID, List.of()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("At least one role is required.");
        assertThatThrownBy(() -> userAccountService.updateRoles(ACTOR_ID, ACCOUNT_ID, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("At least one role is required.");

        verify(userAccountRepository, never()).updateRoles(anyInt(), anyList());
    }

    @Test
    @DisplayName("A role update containing a null role is rejected")
    void roleListMustNotContainNull() {
        List<Role> withNull = new ArrayList<>();
        withNull.add(Role.HR);
        withNull.add(null);

        assertThatThrownBy(() -> userAccountService.updateRoles(ACTOR_ID, ACCOUNT_ID, withNull))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Role cannot be null.");
    }

    @Test
    @DisplayName("Changing a password still requires both the current and the new password")
    void changePasswordInputsAreStillRequired() {
        assertThatThrownBy(() -> userAccountService.changeOwnPassword(ACCOUNT_ID, "  ", "BrandNewPass1"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Current password is required.");
        assertThatThrownBy(() -> userAccountService.changeOwnPassword(ACCOUNT_ID, "OldPass123", "  "))
                .isInstanceOf(ValidationException.class)
                .hasMessage("New password is required.");

        verify(userAccountRepository, never()).updatePassword(anyInt(), any(), any());
    }

    @Test
    @DisplayName("Replacing a temporary password still requires the two entries to match")
    void temporaryPasswordConfirmationIsStillChecked() {
        assertThatThrownBy(() -> userAccountService.completeTemporaryPasswordChange(ACCOUNT_ID, "BrandNewPass1", "Different1"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Passwords do not match.");

        verify(userAccountRepository, never()).updatePassword(anyInt(), any(), any());
    }

    // Authentication

    @Test
    @DisplayName("Authenticating without an email or a password is still refused by the service")
    void credentialsAreStillRequired() {
        assertThatThrownBy(() -> authenticationService.authenticate("  ", "password"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Company email is required.");
        assertThatThrownBy(() -> authenticationService.authenticate("a@motorph.com", "  "))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Password is required.");

        verifyNoInteractions(userAccountRepository);
    }

    @ParameterizedTest(name = "otp \"{0}\"")
    @DisplayName("An OTP that is not six digits is still refused by the service")
    @NullAndEmptySource
    @ValueSource(strings = {"12345", "1234567", "12345a", "123 456"})
    void otpFormatIsStillChecked(String otp) {
        assertThatThrownBy(() -> twoFactorService.verifyLoginOtp(ACCOUNT_ID, otp))
                .isInstanceOf(ValidationException.class);
    }
}
