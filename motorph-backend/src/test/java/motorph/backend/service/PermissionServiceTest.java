package motorph.backend.service;

import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.repository.PermissionRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.CurrentUserProvider;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * The active role is the one the user selected, but only if their account holds it.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionServiceTest {

    private static final int ACCOUNT_ID = 10;

    @Mock private PermissionRepository permissionRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private UserAccessLogService userAccessLogService;

    @InjectMocks private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.getCurrentUserAccountId()).thenReturn(ACCOUNT_ID);
    }

    @Test
    @DisplayName("Without a selected role, a staff account defaults to its staff role, not EMPLOYEE")
    void defaultsToSeniorRole() {
        givenRoles(Role.EMPLOYEE, Role.FINANCE);

        assertThat(permissionService.getActiveRole()).isEqualTo(Role.FINANCE);
    }

    @Test
    @DisplayName("A selected role the account holds is used")
    void usesSelectedRole() {
        givenRoles(Role.ADMIN, Role.HR, Role.FINANCE);
        when(currentUserProvider.getRequestedRole()).thenReturn(Role.FINANCE);

        assertThat(permissionService.getActiveRole()).isEqualTo(Role.FINANCE);
    }

    @Test
    @DisplayName("A selected role the account does not hold is refused")
    void refusesRoleNotAssigned() {
        givenRoles(Role.FINANCE, Role.EMPLOYEE);
        when(currentUserProvider.getRequestedRole()).thenReturn(Role.ADMIN);

        assertThatThrownBy(() -> permissionService.getActiveRole()).isInstanceOf(UnauthorizedException.class);
    }

    private void givenRoles(Role... roles) {
        when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(new UserAccount(
                ACCOUNT_ID, 10010, "user@motorph.com", "user@motorph.com", "hash", "salt",
                List.of(roles), false, true, null, false, null));
    }
}
