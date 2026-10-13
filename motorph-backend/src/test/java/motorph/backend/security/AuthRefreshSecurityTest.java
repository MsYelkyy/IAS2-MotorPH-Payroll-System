package motorph.backend.security;

import motorph.backend.controller.v1.AuthController;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.service.AuthenticationService;
import motorph.backend.service.LoginChallengeService;
import motorph.backend.service.PasswordResetService;
import motorph.backend.service.PermissionService;
import motorph.backend.service.UserAccessLogService;
import motorph.backend.service.UserAccountService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Calls {@code POST /api/v1/auth/refresh} directly, through the real security chain and token
 * service, to show a locked or inactive account cannot renew its session by skipping the client.
 */
@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        CurrentUserProvider.class,
        TokenIssuanceService.class
})
@TestPropertySource(properties =
        "motorph.jwt.secret=control-two-test-signing-secret-with-at-least-32-bytes")
class AuthRefreshSecurityTest {

    private static final int USER_ACCOUNT_ID = 12;
    private static final int EMPLOYEE_ID = 101;
    private static final String REFRESH = "/api/v1/auth/refresh";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private TokenRevocationRepository tokenRevocationRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private AuthenticationService authenticationService;
    @MockitoBean private LoginChallengeService loginChallengeService;
    @MockitoBean private UserAccessLogService userAccessLogService;
    @MockitoBean private LoginRateLimiter loginRateLimiter;
    @MockitoBean private PasswordResetService passwordResetService;
    @MockitoBean private UserAccountService userAccountService;
    @MockitoBean private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        when(tokenRevocationRepository.isRevoked(anyString())).thenReturn(false);
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(false, true));
    }

    @Test
    @DisplayName("An active, unlocked account gets a new access token")
    void activeAccountRefreshes() throws Exception {
        mockMvc.perform(refreshRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("A locked account is refused with 401 and no token")
    void lockedAccountIsRefused() throws Exception {
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(true, true));

        mockMvc.perform(refreshRequest())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("An inactive account is refused with 401 and no token")
    void inactiveAccountIsRefused() throws Exception {
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(false, false));

        mockMvc.perform(refreshRequest())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("A garbage refresh token is refused with 401")
    void invalidRefreshTokenIsRefused() throws Exception {
        mockMvc.perform(post(REFRESH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"not-a-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    private MockHttpServletRequestBuilder refreshRequest() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(USER_ACCOUNT_ID, EMPLOYEE_ID).token();

        return post(REFRESH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}");
    }

    private UserAccount account(boolean locked, boolean active) {
        return new UserAccount(
                USER_ACCOUNT_ID,
                EMPLOYEE_ID,
                "employee@motorph.com",
                "employee@motorph.com",
                "password-hash",
                "password-salt",
                List.of(Role.EMPLOYEE),
                locked,
                active,
                null,
                false,
                null
        );
    }
}
