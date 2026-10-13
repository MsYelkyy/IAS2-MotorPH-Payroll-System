package motorph.backend.controller.v1;

import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.domain.value.Role;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.SupportTicketRepository;
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.security.JwtAuthenticationFilter;
import motorph.backend.security.JwtTokenProvider;
import motorph.backend.security.SecurityConfig;
import motorph.backend.service.PermissionService;
import motorph.backend.service.SupportTicketService;
import motorph.backend.service.UserAccessLogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the support ticket endpoints through the full security filter chain.
 * Invalid or blocked users receive 401, while authenticated users without permission receive 403.
 */
@WebMvcTest(SupportTicketController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        CurrentUserProvider.class,
        SupportTicketService.class
})
@TestPropertySource(properties =
        "motorph.jwt.secret=control-two-test-signing-secret-with-at-least-32-bytes")
class SupportTicketSecurityTest {

    private static final int USER_ACCOUNT_ID = 12;
    private static final int EMPLOYEE_ID = 101;
    private static final String TICKETS = "/api/v1/support-tickets";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private TokenRevocationRepository tokenRevocationRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private PermissionService permissionService;
    @MockitoBean private SupportTicketRepository supportTicketRepository;
    @MockitoBean private EmployeeRepository employeeRepository;
    @MockitoBean private UserAccessLogService userAccessLogService;

    @BeforeEach
    void setUp() {
        when(tokenRevocationRepository.isRevoked(anyString())).thenReturn(false);
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(false, true));
        when(permissionService.getActiveRole()).thenReturn(Role.HR);
    }

    // Authentication: 401

    @Test
    @DisplayName("A request with no token is rejected with 401 before reaching any ticket logic")
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(TICKETS))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));

        verifyNoInteractions(supportTicketRepository, permissionService);
    }

    @Test
    @DisplayName("A malformed token, or one signed with another key, is rejected with 401")
    void invalidTokenIsUnauthorized() throws Exception {
        String foreignToken = new JwtTokenProvider("a-different-signing-secret-of-at-least-32-bytes", 15, 1440)
                .generateAccessToken(USER_ACCOUNT_ID, EMPLOYEE_ID).token();

        mockMvc.perform(get(TICKETS).header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(TICKETS).header("Authorization", "Bearer " + foreignToken))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(supportTicketRepository, permissionService);
    }

    @Test
    @DisplayName("A refresh token cannot be used as an access token")
    void refreshTokenIsNotAnAccessToken() throws Exception {
        String refreshToken = jwtTokenProvider.generateRefreshToken(USER_ACCOUNT_ID, EMPLOYEE_ID).token();

        mockMvc.perform(get(TICKETS).header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(supportTicketRepository, permissionService);
    }

    @Test
    @DisplayName("A revoked access token is rejected with 401")
    void revokedTokenIsUnauthorized() throws Exception {
        when(tokenRevocationRepository.isRevoked(anyString())).thenReturn(true);

        mockMvc.perform(get(TICKETS).header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(supportTicketRepository, permissionService);
    }

    @Test
    @DisplayName("A valid access token for a locked account is rejected with 401")
    void lockedAccountTokenIsUnauthorized() throws Exception {
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(true, true));

        mockMvc.perform(get(TICKETS).header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));

        verifyNoInteractions(supportTicketRepository, permissionService);
    }

    @Test
    @DisplayName("A valid access token for an inactive account is rejected with 401")
    void inactiveAccountTokenIsUnauthorized() throws Exception {
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(false, false));

        mockMvc.perform(get(TICKETS).header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));

        verifyNoInteractions(supportTicketRepository, permissionService);
    }

    @Test
    @DisplayName("A valid access token for an account that no longer exists is rejected with 401")
    void missingAccountTokenIsUnauthorized() throws Exception {
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(null);

        mockMvc.perform(get(TICKETS).header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(supportTicketRepository, permissionService);
    }

    @Test
    @DisplayName("Locking an account takes effect on the very next write request too")
    void lockedAccountCannotWrite() throws Exception {
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(true, true));

        mockMvc.perform(post(TICKETS + "/42/resolve").header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(TICKETS + "/42/cancel").header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isUnauthorized());

        verify(supportTicketRepository, never()).markResolved(anyInt(), anyInt());
        verify(supportTicketRepository, never()).markCancelled(anyInt());
    }

    // Authorization: 403 and allowed access

    @Test
    @DisplayName("An authenticated caller without the permission is refused with 403 and a generic body")
    void missingPermissionIsForbidden() throws Exception {
        denyPermission(PermissionKey.VIEW_ALL_TICKETS);

        mockMvc.perform(get(TICKETS).header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action."));

        verify(supportTicketRepository, never()).findAll();
    }

    @Test
    @DisplayName("Only a caller holding CANCEL_TICKETS can cancel; others get 403")
    void cancelRequiresCancelPermission() throws Exception {
        denyPermission(PermissionKey.CANCEL_TICKETS);

        mockMvc.perform(post(TICKETS + "/42/cancel").header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(supportTicketRepository, never()).markCancelled(anyInt());
    }

    @Test
    @DisplayName("A team without VIEW_ALL_TICKETS cannot read another team's tickets")
    void teamCannotReadAnotherTeamsTickets() throws Exception {
        when(permissionService.hasPermission(PermissionKey.VIEW_ALL_TICKETS)).thenReturn(false);

        mockMvc.perform(get(TICKETS + "/routing-team/IT").header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(supportTicketRepository, never()).findByRoutingTeam(any());
    }

    @Test
    @DisplayName("A caller with the permission, and an active account, is allowed through")
    void permittedCallerIsAllowed() throws Exception {
        when(supportTicketRepository.findAll()).thenReturn(List.of());
        when(employeeRepository.findAllWithInactive()).thenReturn(List.of());

        mockMvc.perform(get(TICKETS).header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(permissionService).requirePermission(PermissionKey.VIEW_ALL_TICKETS);
        verify(supportTicketRepository).findAll();
    }

    private void denyPermission(PermissionKey key) {
        doThrow(new UnauthorizedException("You do not have permission to perform this action."))
                .when(permissionService).requirePermission(key);
    }

    private String accessToken() {
        return jwtTokenProvider.generateAccessToken(USER_ACCOUNT_ID, EMPLOYEE_ID).token();
    }

    private UserAccount account(boolean locked, boolean active) {
        return new UserAccount(
                USER_ACCOUNT_ID,
                EMPLOYEE_ID,
                "hr@motorph.com",
                "hr@motorph.com",
                "password-hash",
                "password-salt",
                List.of(Role.HR),
                locked,
                active,
                null,
                false,
                null
        );
    }
}
