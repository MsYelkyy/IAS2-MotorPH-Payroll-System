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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Two checks now guard every token: the account must be active and unlocked (Control 2), and the
 * token must not be older than the user's last "sign out everywhere", which a password reset
 * triggers (TOTP branch). These tests run both through the real filter and token service, so a
 * future merge cannot drop one of them without a test failing.
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
class TokenAccountAndSessionChecksTest {

    private static final int USER_ACCOUNT_ID = 12;
    private static final int EMPLOYEE_ID = 101;
    private static final String ME = "/api/v1/auth/me";
    private static final String REFRESH = "/api/v1/auth/refresh";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private TokenRevocationRepository tokenRevocationRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private AuthenticationService authenticationService;
    @MockitoBean private LoginChallengeService loginChallengeService;
    @MockitoBean private UserAccessLogService userAccessLogService;
    @MockitoBean private LoginRateLimiter loginRateLimiter;
    @MockitoBean private PasswordRecoveryService passwordRecoveryService;
    @MockitoBean private TotpService totpService;
    @MockitoBean private UserAccountService userAccountService;
    @MockitoBean private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        when(tokenRevocationRepository.isRevoked(anyString())).thenReturn(false);
        when(tokenRevocationRepository.isIssuedBeforeSessionRevocation(anyInt(), any(Instant.class))).thenReturn(false);
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(false, true));
    }

    @Nested
    @DisplayName("Access token")
    class AccessToken {

        @Test
        @DisplayName("An active account whose sessions were not revoked is let in")
        void activeAccountIsAccepted() throws Exception {
            mockMvc.perform(get(ME).header("Authorization", accessBearer()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.companyEmail").value("employee@motorph.com"));
        }

        @Test
        @DisplayName("A locked account is refused with 401")
        void lockedAccountIsRefused() throws Exception {
            when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(true, true));

            mockMvc.perform(get(ME).header("Authorization", accessBearer()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("An inactive account is refused with 401")
        void inactiveAccountIsRefused() throws Exception {
            when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(false, false));

            mockMvc.perform(get(ME).header("Authorization", accessBearer()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("A deleted account is refused with 401")
        void missingAccountIsRefused() throws Exception {
            when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(null);

            mockMvc.perform(get(ME).header("Authorization", accessBearer()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("A token issued before the user's sessions were revoked is refused, even for an active account")
        void tokenFromBeforeSessionRevocationIsRefused() throws Exception {
            when(tokenRevocationRepository.isIssuedBeforeSessionRevocation(eqUser(), any(Instant.class))).thenReturn(true);

            mockMvc.perform(get(ME).header("Authorization", accessBearer()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("The filter checks the revocation against the token's own issue time")
        void filterUsesTheTokenIssueTime() throws Exception {
            Instant before = Instant.now().minusSeconds(5);

            mockMvc.perform(get(ME).header("Authorization", accessBearer())).andExpect(status().isOk());

            ArgumentCaptor<Instant> issuedAt = ArgumentCaptor.forClass(Instant.class);
            verify(tokenRevocationRepository).isIssuedBeforeSessionRevocation(anyInt(), issuedAt.capture());
            assertThat(issuedAt.getValue()).isAfter(before).isBeforeOrEqualTo(Instant.now().plusSeconds(1));
        }

        @Test
        @DisplayName("A revoked token ID is still refused")
        void revokedTokenIdIsRefused() throws Exception {
            when(tokenRevocationRepository.isRevoked(anyString())).thenReturn(true);

            mockMvc.perform(get(ME).header("Authorization", accessBearer()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("A refresh token cannot be used as an access token")
        void refreshTokenIsNotAnAccessToken() throws Exception {
            String refresh = jwtTokenProvider.generateRefreshToken(USER_ACCOUNT_ID, EMPLOYEE_ID).token();

            mockMvc.perform(get(ME).header("Authorization", "Bearer " + refresh))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Refresh token")
    class RefreshToken {

        @Test
        @DisplayName("An active account whose sessions were not revoked gets a new access token")
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
                    .andExpect(jsonPath("$.message").value("Account is not available."))
                    .andExpect(jsonPath("$.accessToken").doesNotExist());
        }

        @Test
        @DisplayName("An inactive account is refused with 401 and no token")
        void inactiveAccountIsRefused() throws Exception {
            when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(account(false, false));

            mockMvc.perform(refreshRequest())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.accessToken").doesNotExist());
        }

        @Test
        @DisplayName("A deleted account is refused with 401 and no token")
        void missingAccountIsRefused() throws Exception {
            when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(null);

            mockMvc.perform(refreshRequest())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.accessToken").doesNotExist());
        }

        @Test
        @DisplayName("A refresh token issued before the user's sessions were revoked is refused, even for an active account")
        void tokenFromBeforeSessionRevocationIsRefused() throws Exception {
            when(tokenRevocationRepository.isIssuedBeforeSessionRevocation(eqUser(), any(Instant.class))).thenReturn(true);

            mockMvc.perform(refreshRequest())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("Refresh token has been revoked."))
                    .andExpect(jsonPath("$.accessToken").doesNotExist());
        }

        @Test
        @DisplayName("A revoked refresh token ID is still refused")
        void revokedTokenIdIsRefused() throws Exception {
            when(tokenRevocationRepository.isRevoked(anyString())).thenReturn(true);

            mockMvc.perform(refreshRequest())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.accessToken").doesNotExist());
        }

        @Test
        @DisplayName("An access token cannot be used to refresh")
        void accessTokenCannotRefresh() throws Exception {
            String access = jwtTokenProvider.generateAccessToken(USER_ACCOUNT_ID, EMPLOYEE_ID).token();

            mockMvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + access + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    private static int eqUser() {
        return org.mockito.ArgumentMatchers.eq(USER_ACCOUNT_ID);
    }

    private String accessBearer() {
        return "Bearer " + jwtTokenProvider.generateAccessToken(USER_ACCOUNT_ID, EMPLOYEE_ID).token();
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
