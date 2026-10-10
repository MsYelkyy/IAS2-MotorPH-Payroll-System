package motorph.backend.controller.v1;

import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.dto.auth.TokenResponse;
import motorph.backend.exception.OtpVerificationException;
import motorph.backend.repository.UserAccountRepository;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.security.JwtTokenProvider;
import motorph.backend.security.LoginRateLimiter;
import motorph.backend.security.TokenIssuanceService;
import motorph.backend.service.AuthenticationService;
import motorph.backend.service.LoginChallengeService;
import motorph.backend.service.PasswordResetService;
import motorph.backend.service.PermissionService;
import motorph.backend.service.UserAccessLogService;
import motorph.backend.service.UserAccountService;
import motorph.backend.controller.advice.GlobalExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String CHALLENGE_ID = "opaque-challenge";
    private static final int ACCOUNT_ID = 12;
    private static final int EMPLOYEE_ID = 101;
    private static final String OTP = "654321";

    @Mock private AuthenticationService authenticationService;
    @Mock private LoginChallengeService loginChallengeService;
    @Mock private TokenIssuanceService tokenIssuanceService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private UserAccessLogService userAccessLogService;
    @Mock private LoginRateLimiter loginRateLimiter;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private PasswordResetService passwordResetService;
    @Mock private UserAccountService userAccountService;
    @Mock private PermissionService permissionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                authenticationService, loginChallengeService, tokenIssuanceService, jwtTokenProvider,
                userAccountRepository, userAccessLogService, loginRateLimiter, currentUserProvider,
                passwordResetService, userAccountService, permissionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json().build()))
                .setControllerAdvice(new GlobalExceptionHandler(userAccessLogService, currentUserProvider))
                .build();
    }

    @Test
    void validOtpConsumesChallengeAndOnlyThenIssuesTokens() throws Exception {
        UserAccount account = account();
        TokenResponse tokens = new TokenResponse("access", "refresh", 3600);
        when(loginChallengeService.resolve(CHALLENGE_ID)).thenReturn(ACCOUNT_ID);
        when(authenticationService.verifyLoginOtp(ACCOUNT_ID, OTP)).thenReturn(account);
        when(tokenIssuanceService.issueTokenPair(ACCOUNT_ID, EMPLOYEE_ID)).thenReturn(tokens);

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"challengeId":"opaque-challenge","otp":"654321"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));

        InOrder order = inOrder(authenticationService, loginChallengeService, tokenIssuanceService);
        order.verify(authenticationService).verifyLoginOtp(ACCOUNT_ID, OTP);
        order.verify(loginChallengeService).consume(CHALLENGE_ID);
        order.verify(tokenIssuanceService).issueTokenPair(ACCOUNT_ID, EMPLOYEE_ID);
    }

    @Test
    void incorrectOtpReturnsControlledErrorAndDoesNotConsumeChallengeOrIssueTokens() throws Exception {
        when(loginChallengeService.resolve(CHALLENGE_ID)).thenReturn(ACCOUNT_ID);
        when(authenticationService.verifyLoginOtp(ACCOUNT_ID, OTP))
                .thenThrow(new OtpVerificationException(
                        OtpVerificationException.Reason.INVALID_CODE, "Invalid OTP code."));

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"challengeId":"opaque-challenge","otp":"654321"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Invalid OTP code."));

        verify(loginChallengeService, never()).consume(CHALLENGE_ID);
        verify(tokenIssuanceService, never()).issueTokenPair(ACCOUNT_ID, EMPLOYEE_ID);
    }

    private UserAccount account() {
        return new UserAccount(
                ACCOUNT_ID, EMPLOYEE_ID, "juan.cruz@motorph.com", "juan.cruz@motorph.com",
                "hash", "salt", List.of(Role.EMPLOYEE), false, true, null, false, null);
    }
}
