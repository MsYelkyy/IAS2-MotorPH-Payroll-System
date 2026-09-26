package motorph.backend.service;

import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.PasswordHasher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Covers the two self-service password paths and the status-change audit trail. */
@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    private static final int ACCOUNT_ID = 7;
    private static final int ACTOR_ID = 99;
    private static final String CURRENT_PASSWORD = "OldPass123";

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeCredentialsDeliveryService credentialsDeliveryService;
    @Mock private UserAccessLogService userAccessLogService;

    @InjectMocks private UserAccountService userAccountService;

    @Test
    @DisplayName("Changing your own password replaces it once the current one checks out")
    void changeOwnPasswordUpdatesWhenCurrentPasswordMatches() {
        when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(accountWithPassword(CURRENT_PASSWORD));

        userAccountService.changeOwnPassword(ACCOUNT_ID, CURRENT_PASSWORD, "BrandNewPass1");

        verify(userAccountRepository).updatePassword(eq(ACCOUNT_ID), anyString(), anyString());
        verify(userAccessLogService).logEvent(ACCOUNT_ID, UserAccessLogService.ACTION_PASSWORD_CHANGED,
                "Changed own password.");
    }

    @Test
    @DisplayName("A wrong current password leaves the stored password untouched")
    void changeOwnPasswordRejectsWrongCurrentPassword() {
        when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(accountWithPassword(CURRENT_PASSWORD));

        assertThatThrownBy(() -> userAccountService.changeOwnPassword(ACCOUNT_ID, "NotMyPassword", "BrandNewPass1"))
                .isInstanceOf(AuthenticationException.class);

        verify(userAccountRepository, never()).updatePassword(anyInt(), anyString(), anyString());
        verifyNoInteractions(userAccessLogService);
    }

    @Test
    @DisplayName("Completing a temporary password change works only while one is pending")
    void completeTemporaryPasswordChangeAppliesWhenAwaitingChange() {
        when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(account(true, true, false));

        userAccountService.completeTemporaryPasswordChange(ACCOUNT_ID, "BrandNewPass1", "BrandNewPass1");

        verify(userAccountRepository).updatePassword(eq(ACCOUNT_ID), anyString(), anyString());
        verify(userAccessLogService).logEvent(ACCOUNT_ID, UserAccessLogService.ACTION_PASSWORD_CHANGED,
                "Replaced temporary password.");
    }

    @Test
    @DisplayName("A consumed or replayed challenge cannot overwrite a normal account's password")
    void completeTemporaryPasswordChangeRefusesAccountNotAwaitingChange() {
        when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(account(true, false, false));

        assertThatThrownBy(() ->
                userAccountService.completeTemporaryPasswordChange(ACCOUNT_ID, "BrandNewPass1", "BrandNewPass1"))
                .isInstanceOf(BusinessRuleException.class);

        verify(userAccountRepository, never()).updatePassword(anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("Mismatched confirmation is rejected before anything is written")
    void completeTemporaryPasswordChangeRejectsMismatch() {
        assertThatThrownBy(() ->
                userAccountService.completeTemporaryPasswordChange(ACCOUNT_ID, "BrandNewPass1", "Different1"))
                .isInstanceOf(ValidationException.class);

        verify(userAccountRepository, never()).updatePassword(anyInt(), anyString(), anyString());
    }

    @Test
    @DisplayName("An unknown account is a not-found, not a silent no-op")
    void changeOwnPasswordRejectsUnknownAccount() {
        when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(null);

        assertThatThrownBy(() -> userAccountService.changeOwnPassword(ACCOUNT_ID, CURRENT_PASSWORD, "BrandNewPass1"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Locking an account audits the before and after status against the acting user")
    void lockAccountAuditsStatusChange() {
        when(userAccountRepository.findById(ACCOUNT_ID))
                .thenReturn(account(true, false, false))
                .thenReturn(account(true, false, true));

        userAccountService.lockAccount(ACTOR_ID, ACCOUNT_ID);

        verify(userAccountRepository).updateLockStatus(ACCOUNT_ID, true);
        verify(userAccessLogService).logAccountStatusChanged(ACTOR_ID, 101, "ACTIVE", "LOCKED");
    }

    @Test
    @DisplayName("Changing an account's roles audits the old and new roles against the acting user")
    void updateRolesAuditsRoleChange() {
        when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(account(true, false, false));

        userAccountService.updateRoles(ACTOR_ID, ACCOUNT_ID, List.of(Role.EMPLOYEE, Role.HR));

        verify(userAccountRepository).updateRoles(ACCOUNT_ID, List.of(Role.EMPLOYEE, Role.HR));
        verify(userAccessLogService).logEvent(ACTOR_ID, UserAccessLogService.ACTION_USER_ROLES_UPDATED,
                "Changed roles of employee ID 101 from [EMPLOYEE] to [EMPLOYEE, HR]");
    }

    private UserAccount accountWithPassword(String rawPassword) {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(rawPassword, salt);

        return new UserAccount(
                ACCOUNT_ID, 101, "juan.cruz@motorph.com", "juan.cruz@motorph.com", hash, salt,
                List.of(Role.EMPLOYEE), false, true, null, false, null);
    }

    private UserAccount account(boolean active, boolean mustChangePassword, boolean locked) {
        return new UserAccount(
                ACCOUNT_ID, 101, "juan.cruz@motorph.com", "juan.cruz@motorph.com", "hash", "salt",
                List.of(Role.EMPLOYEE), locked, active, null,
                mustChangePassword, mustChangePassword ? LocalDateTime.now().plusHours(1) : null);
    }
}
