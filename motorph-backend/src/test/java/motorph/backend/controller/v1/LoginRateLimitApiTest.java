package motorph.backend.controller.v1;

import motorph.backend.configuration.ClockConfig;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.repository.TokenRevocationRepository;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.security.JwtAuthenticationFilter;
import motorph.backend.security.JwtTokenProvider;
import motorph.backend.security.LoginRateLimiter;
import motorph.backend.security.SecurityConfig;
import motorph.backend.security.TokenIssuanceService;
import motorph.backend.service.AuthenticationService;
import motorph.backend.service.LoginChallengeService;
import motorph.backend.service.PasswordRecoveryService;
import motorph.backend.service.PermissionService;
import motorph.backend.service.TotpService;
import motorph.backend.service.UserAccessLogService;
import motorph.backend.service.UserAccountService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real limiter behind the real login endpoint. Each test uses its own client address, so
 * the limiter's counters do not carry over between tests.
 */
@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        CurrentUserProvider.class,
        LoginRateLimiter.class,
        ClockConfig.class
})
@TestPropertySource(properties = {
        "motorph.jwt.secret=control-two-test-signing-secret-with-at-least-32-bytes",
        "motorph.auth.login-rate-limit.max-attempts=2",
        "motorph.auth.login-rate-limit.max-attempts-per-ip=4"
})
class LoginRateLimitApiTest {

    private static final String LOGIN = "/api/v1/auth/login";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TokenRevocationRepository tokenRevocationRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private AuthenticationService authenticationService;
    @MockitoBean private TokenIssuanceService tokenIssuanceService;
    @MockitoBean private LoginChallengeService loginChallengeService;
    @MockitoBean private UserAccessLogService userAccessLogService;
    @MockitoBean private PasswordRecoveryService passwordRecoveryService;
    @MockitoBean private TotpService totpService;
    @MockitoBean private UserAccountService userAccountService;
    @MockitoBean private PermissionService permissionService;

    private ResultActions login(String address, String email, String forwardedFor) throws Exception {
        return mockMvc.perform(post(LOGIN)
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .header("X-Forwarded-For", forwardedFor)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyEmail\":\"" + email + "\",\"password\":\"WrongPassw0rd\"}"));
    }

    @Test
    @DisplayName("The same email is answered with a controlled 429 once its budget is spent")
    void sameEmailGetsTooManyRequests() throws Exception {
        when(authenticationService.authenticate(anyString(), anyString()))
                .thenThrow(new AuthenticationException("Invalid email or password."));

        login("192.0.2.10", "a@motorph.com", "1.1.1.1").andExpect(status().isUnauthorized());
        login("192.0.2.10", "a@motorph.com", "1.1.1.1").andExpect(status().isUnauthorized());

        login("192.0.2.10", "a@motorph.com", "1.1.1.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message").value("Too many attempts. Please try again later."))
                .andExpect(content().string(not(containsString("192.0.2.10"))));

        verify(authenticationService, times(2)).authenticate(anyString(), anyString());
    }

    @Test
    @DisplayName("Changing the email and the X-Forwarded-For header on every request still ends in 429")
    void rotatingEmailsAndHeadersAreStillLimited() throws Exception {
        when(authenticationService.authenticate(anyString(), anyString()))
                .thenThrow(new AuthenticationException("Invalid email or password."));

        for (int i = 0; i < 4; i++) {
            login("192.0.2.20", "user" + i + "@motorph.com", "9.9.9." + i).andExpect(status().isUnauthorized());
        }

        login("192.0.2.20", "someone-new@motorph.com", "9.9.9.99")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));

        verify(authenticationService, times(4)).authenticate(anyString(), anyString());
    }

    @Test
    @DisplayName("A throttled address does not slow down a different one")
    void otherAddressIsNotAffected() throws Exception {
        when(authenticationService.authenticate(anyString(), anyString()))
                .thenThrow(new AuthenticationException("Invalid email or password."));

        for (int i = 0; i < 5; i++) {
            login("192.0.2.30", "user" + i + "@motorph.com", "1.1.1.1");
        }

        login("192.0.2.31", "user0@motorph.com", "1.1.1.1").andExpect(status().isUnauthorized());
    }
}
