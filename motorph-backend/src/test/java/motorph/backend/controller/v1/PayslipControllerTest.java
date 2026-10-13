package motorph.backend.controller.v1;

import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.domain.value.Role;
import motorph.backend.repository.PermissionRepository;
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.security.JwtAuthenticationFilter;
import motorph.backend.security.JwtTokenProvider;
import motorph.backend.security.SecurityConfig;
import motorph.backend.service.PayslipService;
import motorph.backend.service.PermissionService;
import motorph.backend.service.UserAccessLogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests payslip access through the real security chain and permission service. Anyone with
 * VIEW_PAYSLIP sees their own payslips; only roles with VIEW_ALL_PAYSLIPS (Admin, Finance, HR) see
 * someone else's.
 */
@WebMvcTest(PayslipController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        CurrentUserProvider.class,
        PermissionService.class
})
@TestPropertySource(properties =
        "motorph.jwt.secret=control-two-test-signing-secret-with-at-least-32-bytes")
class PayslipControllerTest {

    private static final int USER_ACCOUNT_ID = 12;
    private static final int EMPLOYEE_ID = 101;
    private static final int OTHER_EMPLOYEE_ID = 102;
    private static final int PAYRUN_ID = 5;
    private static final String OWN = "/api/v1/payslips/" + EMPLOYEE_ID + "/" + PAYRUN_ID;
    private static final String OTHER = "/api/v1/payslips/" + OTHER_EMPLOYEE_ID + "/" + PAYRUN_ID;

    private static final Map<Role, Set<PermissionKey>> SEEDED_GRANTS = Map.of(
            Role.ADMIN, Set.of(PermissionKey.VIEW_PAYSLIP, PermissionKey.VIEW_ALL_PAYSLIPS),
            Role.FINANCE, Set.of(PermissionKey.VIEW_PAYSLIP, PermissionKey.VIEW_ALL_PAYSLIPS),
            Role.HR, Set.of(PermissionKey.VIEW_PAYSLIP, PermissionKey.VIEW_ALL_PAYSLIPS),
            Role.IT, Set.of(PermissionKey.VIEW_PAYSLIP),
            Role.EMPLOYEE, Set.of(PermissionKey.VIEW_PAYSLIP));

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private TokenRevocationRepository tokenRevocationRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private PermissionRepository permissionRepository;
    @MockitoBean private PayslipService payslipService;
    @MockitoBean private UserAccessLogService userAccessLogService;

    private final Map<Role, Set<PermissionKey>> grants = new EnumMap<>(Role.class);

    @BeforeEach
    void setUp() {
        grants.putAll(SEEDED_GRANTS);

        when(tokenRevocationRepository.isRevoked(anyString())).thenReturn(false);
        when(permissionRepository.roleHasPermission(any(Role.class), anyString())).thenAnswer(invocation ->
                grants.getOrDefault(invocation.<Role>getArgument(0), Set.of())
                        .contains(PermissionKey.valueOf(invocation.getArgument(1))));
        when(payslipService.generatePayslipPdf(anyInt(), anyInt())).thenReturn(new byte[] {'%', 'P', 'D', 'F'});
    }

    @Test
    @DisplayName("A request with no token is rejected with 401 on every payslip endpoint")
    void missingTokenIsUnauthorized() throws Exception {
        for (String path : List.of("/api/v1/payslips", OWN, OWN + "/pdf", OTHER, OTHER + "/pdf")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(""));
        }

        verifyNoInteractions(payslipService);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("Every role can read its own payslip as JSON and PDF, and list its own payslips")
    void everyRoleCanReadItsOwnPayslip(Role role) throws Exception {
        signedInAs(role);

        mockMvc.perform(get(OWN).header("Authorization", bearer())).andExpect(status().isOk());
        mockMvc.perform(get(OWN + "/pdf").header("Authorization", bearer())).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/payslips").header("Authorization", bearer())).andExpect(status().isOk());

        verify(payslipService).getPayslipReport(EMPLOYEE_ID, PAYRUN_ID);
        verify(payslipService).generatePayslipPdf(EMPLOYEE_ID, PAYRUN_ID);
        verify(payslipService).getPayslipSummaries(EMPLOYEE_ID);
    }

    @Test
    @DisplayName("A role without VIEW_PAYSLIP is refused with 403 even for its own payslip")
    void ownPayslipNeedsViewPayslip() throws Exception {
        signedInAs(Role.EMPLOYEE);
        grants.put(Role.EMPLOYEE, Set.of());

        for (String path : List.of("/api/v1/payslips", OWN, OWN + "/pdf")) {
            mockMvc.perform(get(path).header("Authorization", bearer()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        verifyNoInteractions(payslipService);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "FINANCE", "HR"})
    @DisplayName("Admin, Finance and HR can read another employee's payslip as JSON and PDF")
    void adminFinanceAndHrCanReadOtherPayslips(Role role) throws Exception {
        signedInAs(role);

        mockMvc.perform(get(OTHER).header("Authorization", bearer())).andExpect(status().isOk());
        mockMvc.perform(get(OTHER + "/pdf").header("Authorization", bearer())).andExpect(status().isOk());

        verify(payslipService).getPayslipReport(OTHER_EMPLOYEE_ID, PAYRUN_ID);
        verify(payslipService).generatePayslipPdf(OTHER_EMPLOYEE_ID, PAYRUN_ID);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"IT", "EMPLOYEE"})
    @DisplayName("IT and Employee are refused with 403 on another employee's payslip, JSON and PDF")
    void otherRolesCannotReadOtherPayslips(Role role) throws Exception {
        signedInAs(role);

        for (String path : List.of(OTHER, OTHER + "/pdf")) {
            mockMvc.perform(get(path).header("Authorization", bearer()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }

        verifyNoInteractions(payslipService);
    }

    @Test
    @DisplayName("A role that only holds VIEW_PAYSLIP can still read its own payslip but nobody else's")
    void viewPayslipAloneIsSelfOnly() throws Exception {
        signedInAs(Role.HR);
        grants.put(Role.HR, Set.of(PermissionKey.VIEW_PAYSLIP));

        mockMvc.perform(get(OWN).header("Authorization", bearer())).andExpect(status().isOk());
        mockMvc.perform(get(OTHER).header("Authorization", bearer())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A multi-role account loses the cross-employee access when it switches to the Employee role")
    void switchingToEmployeeRoleDropsCrossEmployeeAccess() throws Exception {
        signedInAs(Role.FINANCE, Role.EMPLOYEE);

        mockMvc.perform(get(OTHER).header("Authorization", bearer())).andExpect(status().isOk());
        mockMvc.perform(get(OTHER).header("Authorization", bearer()).header("X-Active-Role", "EMPLOYEE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(OWN).header("Authorization", bearer()).header("X-Active-Role", "EMPLOYEE"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("VIEW_PAYSLIP alone never opens another employee's payslip")
    void viewPayslipDoesNotOpenOtherPayslips() throws Exception {
        signedInAs(Role.EMPLOYEE);
        grants.put(Role.EMPLOYEE, Set.of(PermissionKey.VIEW_PAYSLIP));

        mockMvc.perform(get(OTHER).header("Authorization", bearer())).andExpect(status().isForbidden());

        verifyNoInteractions(payslipService);
    }

    @Test
    @DisplayName("Choosing a role in X-Active-Role does not grant the permission of a role the account lacks")
    void forgedActiveRoleIsRefused() throws Exception {
        signedInAs(Role.EMPLOYEE);

        mockMvc.perform(get(OTHER).header("Authorization", bearer()).header("X-Active-Role", "FINANCE"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(payslipService);
    }

    private void signedInAs(Role... roles) {
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(new UserAccount(
                USER_ACCOUNT_ID, EMPLOYEE_ID, "user@motorph.com", "user@motorph.com", "hash", "salt",
                List.of(roles), false, true, null, false, null));
    }

    private String bearer() {
        return "Bearer " + jwtTokenProvider.generateAccessToken(USER_ACCOUNT_ID, EMPLOYEE_ID).token();
    }
}
