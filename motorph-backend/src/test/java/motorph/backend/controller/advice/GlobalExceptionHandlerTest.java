package motorph.backend.controller.advice;

import motorph.backend.domain.UserAccessLog;
import motorph.backend.exception.AuthenticationException;
import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.DataAccessException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.OtpVerificationException;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.UserAccessLogRepository;
import motorph.backend.security.AuthenticatedPrincipal;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.security.RequestClientInfo;
import motorph.backend.service.UserAccessLogService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization failures are audited in one place, the exception handler, instead of in every
 * controller and service (Control 5.4). These requests go through real Spring MVC, the real
 * {@link CurrentUserProvider}, {@link UserAccessLogService} and {@link RequestClientInfo}; only the
 * repository is mocked, so the test sees exactly what would be written to user_access_logs.
 */
class GlobalExceptionHandlerTest {

    private static final int ACTOR_ID = 7;
    private static final String CLIENT_IP = "203.0.113.9";
    private static final String DEVICE = "MotorPH-Desktop/1.0 (Windows 11 10.0; amd64; Java 21)";

    private UserAccessLogRepository repository;
    private GlobalExceptionHandler handler;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository = mock(UserAccessLogRepository.class);
        UserAccessLogService logService = new UserAccessLogService(repository, new RequestClientInfo());
        CurrentUserProvider currentUserProvider = new CurrentUserProvider();

        handler = new GlobalExceptionHandler(logService, currentUserProvider);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProbeController(currentUserProvider))
                .setControllerAdvice(handler)
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("A refused request is audited as ACCESS_DENIED under the caller, with the method and route")
    void deniedRequestIsAudited() throws Exception {
        signInAs(ACTOR_ID);

        mockMvc.perform(fromClient(get("/api/v1/probe/denied")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        UserAccessLog log = savedLog();
        assertThat(log.getAction()).isEqualTo("ACCESS_DENIED");
        assertThat(log.getUserAccountId()).isEqualTo(ACTOR_ID);
        assertThat(log.getDetails()).isEqualTo("Access denied: GET /api/v1/probe/denied");
        assertThat(log.getTargetType()).isNull();
        assertThat(log.getTargetId()).isNull();
    }

    @Test
    @DisplayName("IP and device come from RequestClientInfo, and the timestamp is left to the database")
    void ipAndDeviceComeFromTheRequest() throws Exception {
        signInAs(ACTOR_ID);

        mockMvc.perform(fromClient(get("/api/v1/probe/denied")).header("X-Forwarded-For", "10.9.9.9"))
                .andExpect(status().isForbidden());

        UserAccessLog log = savedLog();
        assertThat(log.getIpAddress()).isEqualTo(CLIENT_IP);
        assertThat(log.getDeviceInfo()).isEqualTo(DEVICE);
        assertThat(log.getLoggedAt()).isNull();
    }

    @Test
    @DisplayName("The route template is recorded, so path values never reach the audit details")
    void routeTemplateKeepsPathValuesOut() throws Exception {
        signInAs(ACTOR_ID);

        mockMvc.perform(fromClient(post("/api/v1/probe/items/12345/cancel")))
                .andExpect(status().isForbidden());

        assertThat(savedLog().getDetails())
                .isEqualTo("Access denied: POST /api/v1/probe/items/{itemId}/cancel")
                .doesNotContain("12345");
    }

    @Test
    @DisplayName("The exception message and client-supplied text are not recorded")
    void clientSuppliedTextIsNotRecorded() throws Exception {
        signInAs(ACTOR_ID);

        mockMvc.perform(fromClient(get("/api/v1/probe/role")).header("X-Active-Role", "<script>alert(1)</script>"))
                .andExpect(status().isForbidden());

        assertThat(savedLog().getDetails())
                .isEqualTo("Access denied: GET /api/v1/probe/role")
                .doesNotContain("script")
                .doesNotContain("Unknown role");
    }

    @Test
    @DisplayName("Each denial writes exactly one entry")
    void eachDenialWritesOneEntry() throws Exception {
        signInAs(ACTOR_ID);

        mockMvc.perform(fromClient(get("/api/v1/probe/denied"))).andExpect(status().isForbidden());

        verify(repository).save(any(UserAccessLog.class));
    }

    @Test
    @DisplayName("A denial with no authenticated caller has nobody to attribute it to, and is skipped")
    void unauthenticatedDenialIsSkipped() throws Exception {
        mockMvc.perform(fromClient(get("/api/v1/probe/unauthenticated")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("A failure to write the audit entry does not turn the 403 into a 500")
    void auditFailureDoesNotChangeTheResponse() throws Exception {
        signInAs(ACTOR_ID);
        doThrow(new DataAccessException("Failed to save user access log.", new SQLException("database down")))
                .when(repository).save(any());

        mockMvc.perform(fromClient(get("/api/v1/probe/denied")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have permission to perform this action."));

        verify(repository).save(any(UserAccessLog.class));
    }

    @Test
    @DisplayName("An invalid caller ID that the log service rejects does not change the response either")
    void rejectedAuditEntryDoesNotChangeTheResponse() throws Exception {
        signInAs(0);

        mockMvc.perform(fromClient(get("/api/v1/probe/denied")))
                .andExpect(status().isForbidden());

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("The route falls back to a fixed label when Spring recorded no template")
    void routeFallsBackToAFixedLabel() {
        signInAs(ACTOR_ID);

        ResponseEntity<?> response = handler.handleUnauthorized(
                new UnauthorizedException("anything"), new MockHttpServletRequest("GET", "/attacker/chosen/path"));

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(savedLog().getDetails()).isEqualTo("Access denied: GET (unmatched route)");
    }

    @ParameterizedTest(name = "{0} responds {1} and is not audited as ACCESS_DENIED")
    @CsvSource({
            "/api/v1/probe/validation,   400",
            "/api/v1/probe/conflict,     409",
            "/api/v1/probe/otp,          409",
            "/api/v1/probe/login-failure, 401",
            "/api/v1/probe/missing,      404"
    })
    void otherErrorsAreNotAuditedAsAccessDenied(String path, int expectedStatus) throws Exception {
        signInAs(ACTOR_ID);

        mockMvc.perform(fromClient(get(path))).andExpect(status().is(expectedStatus));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("A failed OTP check keeps its existing 409 response")
    void otpFailureKeepsItsResponse() throws Exception {
        signInAs(ACTOR_ID);

        mockMvc.perform(fromClient(get("/api/v1/probe/otp")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Invalid OTP code."));
    }

    private MockHttpServletRequestBuilder fromClient(MockHttpServletRequestBuilder builder) {
        return builder
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, DEVICE)
                .with(request -> {
                    request.setRemoteAddr(CLIENT_IP);
                    return request;
                });
    }

    private void signInAs(int userAccountId) {
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(userAccountId, 101, "test-jti", Instant.now().plusSeconds(300));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private UserAccessLog savedLog() {
        ArgumentCaptor<UserAccessLog> captor = ArgumentCaptor.forClass(UserAccessLog.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    /** Endpoints that fail in the ways the handler has to tell apart. */
    @RestController
    @RequestMapping("/api/v1/probe")
    static class ProbeController {

        private final CurrentUserProvider currentUserProvider;

        ProbeController(CurrentUserProvider currentUserProvider) {
            this.currentUserProvider = currentUserProvider;
        }

        @GetMapping("/denied")
        void denied() {
            throw new UnauthorizedException("You do not have permission to perform this action.");
        }

        @PostMapping("/items/{itemId}/cancel")
        void cancel(@PathVariable int itemId) {
            throw new UnauthorizedException("Only Admin can cancel this item " + itemId);
        }

        // Throws UnauthorizedException("Unknown role: ...") for an unrecognised X-Active-Role header.
        @GetMapping("/role")
        void role() {
            currentUserProvider.getRequestedRole();
        }

        // Throws UnauthorizedException("No authenticated user for this request.") when not signed in.
        @GetMapping("/unauthenticated")
        void unauthenticated() {
            currentUserProvider.getCurrentPrincipal();
        }

        @GetMapping("/validation")
        void validation() {
            throw new ValidationException("Invalid input.");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new BusinessRuleException("Conflicting state.");
        }

        @GetMapping("/otp")
        void otp() {
            throw new OtpVerificationException(OtpVerificationException.Reason.INVALID_CODE, "Invalid OTP code.");
        }

        @GetMapping("/login-failure")
        void loginFailure() {
            throw new AuthenticationException("Invalid email or password.");
        }

        @GetMapping("/missing")
        void missing() {
            throw new EntityNotFoundException("Not found.");
        }
    }
}
