package motorph.backend.security;

import motorph.backend.controller.v1.AuthController;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.service.AuthenticationService;
import motorph.backend.service.LoginChallengeService;
import motorph.backend.service.PasswordRecoveryService;
import motorph.backend.service.PermissionService;
import motorph.backend.service.TotpService;
import motorph.backend.service.UserAccessLogService;
import motorph.backend.service.UserAccountService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        CurrentUserProvider.class
})
@TestPropertySource(properties =
        "motorph.jwt.secret=control-two-test-signing-secret-with-at-least-32-bytes")
class AuthMeSecurityTest {

    private static final int USER_ACCOUNT_ID = 12;
    private static final int EMPLOYEE_ID = 101;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private TokenRevocationRepository tokenRevocationRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private AuthenticationService authenticationService;
    @MockitoBean private LoginChallengeService loginChallengeService;
    @MockitoBean private TokenIssuanceService tokenIssuanceService;
    @MockitoBean private UserAccessLogService userAccessLogService;
    @MockitoBean private LoginRateLimiter loginRateLimiter;
    @MockitoBean private PasswordRecoveryService passwordRecoveryService;
    @MockitoBean private TotpService totpService;
    @MockitoBean private UserAccountService userAccountService;
    @MockitoBean private PermissionService permissionService;

    @Test
    void validAccessTokenReturnsAuthenticatedUser() throws Exception {
        when(tokenRevocationRepository.isRevoked(anyString())).thenReturn(false);
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account());
        JwtTokenProvider.IssuedToken accessToken =
                jwtTokenProvider.generateAccessToken(USER_ACCOUNT_ID, EMPLOYEE_ID);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken.token())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userAccountId").value(USER_ACCOUNT_ID))
                .andExpect(jsonPath("$.employeeId").value(EMPLOYEE_ID))
                .andExpect(jsonPath("$.companyEmail").value("employee@motorph.com"))
                .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"))
                .andExpect(jsonPath("$.activeRole").value("EMPLOYEE"));

        verify(tokenRevocationRepository).isRevoked(accessToken.jti());
        // Once in the filter, which checks the account is active and unlocked, and once in the controller.
        verify(userAccountRepository, times(2)).findById(USER_ACCOUNT_ID);
    }

    @Test
    void missingAuthorizationHeaderReturnsUnauthorizedWithoutUserDetails() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));

        verifyNoInteractions(tokenRevocationRepository, userAccountRepository);
    }

    private UserAccount account() {
        return new UserAccount(
                USER_ACCOUNT_ID,
                EMPLOYEE_ID,
                "employee@motorph.com",
                "employee@motorph.com",
                "password-hash",
                "password-salt",
                List.of(Role.EMPLOYEE),
                false,
                true,
                null,
                false,
                null
        );
    }
}
