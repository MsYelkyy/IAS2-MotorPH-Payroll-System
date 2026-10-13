package motorph.backend.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import motorph.backend.domain.UserAccount;
import motorph.backend.domain.value.Role;
import motorph.backend.domain.value.SupportTicketCategory;
import motorph.backend.dto.auth.ChangePasswordRequest;
import motorph.backend.dto.auth.LoginRequest;
import motorph.backend.dto.auth.RefreshRequest;
import motorph.backend.dto.auth.TokenResponse;
import motorph.backend.dto.auth.VerifyOtpRequest;
import motorph.backend.dto.support.TicketSubmitRequest;
import motorph.backend.dto.user.UserRolesUpdateRequest;
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
import motorph.backend.service.LoginAttemptOutcome;
import motorph.backend.service.LoginChallengeService;
import motorph.backend.service.PasswordResetService;
import motorph.backend.service.PermissionService;
import motorph.backend.service.SupportTicketService;
import motorph.backend.service.UserAccessLogService;
import motorph.backend.service.UserAccountService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Calls the API like a client would: real servlet, security filters, JSON binding, {@code @Valid}
 * and the real GlobalExceptionHandler. Only the services and repositories are mocked.
 */
@WebMvcTest({AuthController.class, SupportTicketController.class, UserAccountController.class})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        CurrentUserProvider.class
})
@TestPropertySource(properties =
        "motorph.jwt.secret=control-three-test-signing-secret-with-at-least-32-bytes")
class RequestValidationApiTest {

    private static final int USER_ACCOUNT_ID = 12;
    private static final int EMPLOYEE_ID = 101;

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String REFRESH = "/api/v1/auth/refresh";
    private static final String VERIFY_OTP = "/api/v1/auth/verify-otp";
    private static final String CHANGE_PASSWORD = "/api/v1/auth/change-password";
    private static final String TICKETS = "/api/v1/support-tickets";
    private static final String ROLES = "/api/v1/user-accounts/5/roles";

    private static final String SECRET_PASSWORD = "Sup3r-secret-PW!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private RequestMappingHandlerAdapter handlerAdapter;

    @MockitoBean private AuthenticationService authenticationService;
    @MockitoBean private LoginChallengeService loginChallengeService;
    @MockitoBean private TokenIssuanceService tokenIssuanceService;
    @MockitoBean private LoginRateLimiter loginRateLimiter;
    @MockitoBean private PasswordResetService passwordResetService;
    @MockitoBean private UserAccountService userAccountService;
    @MockitoBean private SupportTicketService supportTicketService;
    @MockitoBean private PermissionService permissionService;
    @MockitoBean private UserAccessLogService userAccessLogService;
    @MockitoBean private TokenRevocationRepository tokenRevocationRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        when(tokenRevocationRepository.isRevoked(anyString())).thenReturn(false);
        when(userAccountRepository.findById(USER_ACCOUNT_ID)).thenReturn(activeAccount());
    }

    // Valid payloads are accepted

    @Nested
    @DisplayName("A valid payload is accepted")
    class ValidPayloads {

        @Test
        @DisplayName("POST /auth/login reaches the authentication service")
        void login() throws Exception {
            when(authenticationService.authenticate("manuel.garcia@motorph.com", SECRET_PASSWORD))
                    .thenReturn(new LoginAttemptOutcome(
                            LoginAttemptOutcome.Type.OTP_SENT, 5, "manuel.garcia@motorph.com", "OTP verification required."));
            when(loginChallengeService.issue(5)).thenReturn("challenge-id");

            mockMvc.perform(json(post(LOGIN), Map.of("companyEmail", "manuel.garcia@motorph.com", "password", SECRET_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.challengeId").value("challenge-id"));
        }

        @Test
        @DisplayName("POST /auth/refresh reaches the token service, whatever the token's shape")
        void refresh() throws Exception {
            when(tokenIssuanceService.refresh("not-a-token")).thenThrow(new AuthenticationException("Invalid refresh token."));

            mockMvc.perform(json(post(REFRESH), Map.of("refreshToken", "not-a-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
        }

        @Test
        @DisplayName("POST /auth/verify-otp reaches the authentication service")
        void verifyOtp() throws Exception {
            when(loginChallengeService.resolveAndConsume("k3J9-_xQ2mP0")).thenReturn(USER_ACCOUNT_ID);
            when(authenticationService.verifyLoginOtp(USER_ACCOUNT_ID, "012345")).thenReturn(activeAccount());
            when(tokenIssuanceService.issueTokenPair(USER_ACCOUNT_ID, EMPLOYEE_ID))
                    .thenReturn(new TokenResponse("access", "refresh", 900));

            mockMvc.perform(json(post(VERIFY_OTP), Map.of("challengeId", "k3J9-_xQ2mP0", "otp", "012345")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access"));
        }

        @Test
        @DisplayName("POST /auth/change-password reaches the account service")
        void changePassword() throws Exception {
            mockMvc.perform(json(post(CHANGE_PASSWORD).header("Authorization", bearer()),
                            Map.of("currentPassword", "OldPass123", "newPassword", "BrandNewPass1")))
                    .andExpect(status().isOk());

            verify(userAccountService).changeOwnPassword(USER_ACCOUNT_ID, "OldPass123", "BrandNewPass1");
        }

        @Test
        @DisplayName("POST /support-tickets stores the text exactly as it was sent, markup included")
        void ticketTextIsPassedThroughUnaltered() throws Exception {
            String subject = "  Hours < 8 & <b>rate</b> > 500  ";
            String description = "<script>alert(1)</script> is what the old template showed.\nSecond line.  ";

            mockMvc.perform(json(post(TICKETS).header("Authorization", bearer()),
                            Map.of("category", "payroll", "subject", subject, "description", description)))
                    .andExpect(status().isOk());

            verify(supportTicketService).submitTicket(
                    EMPLOYEE_ID, SupportTicketCategory.PAYROLL, subject, description, USER_ACCOUNT_ID);
        }

        @Test
        @DisplayName("PUT /user-accounts/{id}/roles reaches the account service")
        void updateRoles() throws Exception {
            mockMvc.perform(json(put(ROLES).header("Authorization", bearer()), Map.of("roles", List.of("HR", "EMPLOYEE"))))
                    .andExpect(status().isOk());

            verify(userAccountService).updateRoles(USER_ACCOUNT_ID, 5, List.of(Role.HR, Role.EMPLOYEE));
        }
    }

    // Invalid payloads are refused before any service runs

    static Stream<Arguments> invalidPayloads() {
        String oversizedSubject = "x".repeat(151);
        String withBell = "Pay" + (char) 7 + "slip";

        return Stream.of(
                Arguments.of("login: blank email", "POST", LOGIN, false, Map.of("companyEmail", "", "password", "pw")),
                Arguments.of("login: malformed email", "POST", LOGIN, false, Map.of("companyEmail", "not-an-email", "password", SECRET_PASSWORD)),
                Arguments.of("login: blank password", "POST", LOGIN, false, Map.of("companyEmail", "a@motorph.com", "password", "   ")),
                Arguments.of("login: password missing", "POST", LOGIN, false, Map.of("companyEmail", "a@motorph.com")),
                Arguments.of("login: empty object", "POST", LOGIN, false, Map.of()),
                Arguments.of("refresh: blank token", "POST", REFRESH, false, Map.of("refreshToken", "")),
                Arguments.of("refresh: token missing", "POST", REFRESH, false, Map.of()),
                Arguments.of("verify-otp: letters in the code", "POST", VERIFY_OTP, false, Map.of("challengeId", "abc", "otp", "12ab56")),
                Arguments.of("verify-otp: short code", "POST", VERIFY_OTP, false, Map.of("challengeId", "abc", "otp", "123")),
                Arguments.of("verify-otp: challenge with illegal characters", "POST", VERIFY_OTP, false, Map.of("challengeId", "../x", "otp", "123456")),
                Arguments.of("verify-otp: blank challenge", "POST", VERIFY_OTP, false, Map.of("challengeId", "", "otp", "123456")),
                Arguments.of("change-password: blank current password", "POST", CHANGE_PASSWORD, true, Map.of("currentPassword", "", "newPassword", "BrandNewPass1")),
                Arguments.of("change-password: new password missing", "POST", CHANGE_PASSWORD, true, Map.of("currentPassword", "OldPass123")),
                Arguments.of("change-password: oversized new password", "POST", CHANGE_PASSWORD, true, Map.of("currentPassword", "OldPass123", "newPassword", "p".repeat(129))),
                Arguments.of("ticket: blank subject", "POST", TICKETS, true, Map.of("category", "PAYROLL", "subject", "  ", "description", "d")),
                Arguments.of("ticket: subject over 150 characters", "POST", TICKETS, true, Map.of("category", "PAYROLL", "subject", oversizedSubject, "description", "d")),
                Arguments.of("ticket: description over 1000 characters", "POST", TICKETS, true, Map.of("category", "PAYROLL", "subject", "s", "description", "d".repeat(1001))),
                Arguments.of("ticket: control character in subject", "POST", TICKETS, true, Map.of("category", "PAYROLL", "subject", withBell, "description", "d")),
                Arguments.of("ticket: blank category", "POST", TICKETS, true, Map.of("category", "", "subject", "s", "description", "d")),
                Arguments.of("roles: list missing", "PUT", ROLES, true, Map.of()),
                Arguments.of("roles: blank role name", "PUT", ROLES, true, Map.of("roles", List.of("HR", ""))),
                Arguments.of("roles: oversized role name", "PUT", ROLES, true, Map.of("roles", List.of("R".repeat(46))))
        );
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("An invalid payload is refused with 400 and a generic body, and no service is called")
    @MethodSource("invalidPayloads")
    void invalidPayloadIsRefusedWith400(String name, String method, String path, boolean authenticated,
                                        Map<String, Object> body) throws Exception {
        MockHttpServletRequestBuilder request = "PUT".equals(method) ? put(path) : post(path);
        if (authenticated) {
            request.header("Authorization", bearer());
        }

        ResultActions result = mockMvc.perform(json(request, body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("The request is invalid."))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());

        assertBodyIsGeneric(result.andReturn(), SECRET_PASSWORD, "not-an-email", "OldPass123", "BrandNewPass1");

        verifyNoInteractions(authenticationService, loginChallengeService, tokenIssuanceService,
                passwordResetService, userAccountService, supportTicketService);
    }

    @Test
    @DisplayName("The failing field is named, but the rejected value never appears in the response")
    void responseNamesTheFieldNotTheValue() throws Exception {
        String raw = mockMvc.perform(json(post(LOGIN), Map.of("companyEmail", "not-an-email", "password", SECRET_PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0]").value("companyEmail"))
                .andReturn().getResponse().getContentAsString();

        assertThat(raw).doesNotContain(SECRET_PASSWORD).doesNotContain("not-an-email");
        assertThat(objectMapper.readTree(raw).fieldNames()).toIterable().containsExactlyInAnyOrder("code", "message", "fieldErrors");
    }

    @Test
    @DisplayName("Rejecting a payload does not reveal whether an account exists: only well-formed emails reach the service")
    void validationDoesNotDependOnAccountExistence() throws Exception {
        when(authenticationService.authenticate(anyString(), anyString()))
                .thenThrow(new AuthenticationException("Invalid email or password."));

        for (String email : List.of("exists@motorph.com", "does.not.exist@motorph.com")) {
            mockMvc.perform(json(post(LOGIN), Map.of("companyEmail", email, "password", "wrong-password")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password."));
        }
    }

    @Test
    @DisplayName("Authentication comes first: an invalid body on a protected endpoint is 401 without a token")
    void authenticationIsCheckedBeforeValidation() throws Exception {
        mockMvc.perform(json(post(TICKETS), Map.of("category", "PAYROLL", "subject", "", "description", "")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));

        verifyNoInteractions(supportTicketService);
    }

    // Malformed JSON

    static Stream<Arguments> malformedBodies() {
        return Stream.of(
                Arguments.of("truncated JSON", "{\"companyEmail\":\"a@motorph.com\",\"password\":"),
                Arguments.of("not JSON at all", "this is not json"),
                Arguments.of("a JSON array instead of an object", "[\"companyEmail\",\"password\"]"),
                Arguments.of("an object where a string belongs", "{\"companyEmail\":{\"nested\":1},\"password\":\"" + SECRET_PASSWORD + "\"}"),
                Arguments.of("an array where a string belongs", "{\"companyEmail\":\"a@motorph.com\",\"password\":[\"" + SECRET_PASSWORD + "\"]}"),
                Arguments.of("unbalanced braces", "{{{{" + SECRET_PASSWORD),
                Arguments.of("an empty body", "")
        );
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Malformed JSON is refused with 400 and a generic body")
    @MethodSource("malformedBodies")
    void malformedJsonIsRefusedWith400(String name, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("The request body is missing or malformed."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andReturn();

        assertBodyIsGeneric(result, SECRET_PASSWORD, "nested", "companyEmail", "a@motorph.com");

        verifyNoInteractions(authenticationService, loginChallengeService);
    }

    @Test
    @DisplayName("Malformed JSON on a protected endpoint is also a clean 400")
    void malformedJsonOnProtectedEndpoint() throws Exception {
        mockMvc.perform(post(TICKETS).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"category\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(supportTicketService);
    }

    @Test
    @DisplayName("A role list of the wrong JSON type is a clean 400")
    void rolesOfTheWrongTypeAreRefused() throws Exception {
        mockMvc.perform(put(ROLES).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(userAccountService, never()).updateRoles(anyInt(), anyInt(), anyList());
    }

    // Unsupported content types

    static Stream<Arguments> unsupportedContentTypes() {
        return Stream.of(
                Arguments.of("text/plain", MediaType.TEXT_PLAIN),
                Arguments.of("text/html", MediaType.TEXT_HTML),
                Arguments.of("application/x-www-form-urlencoded", MediaType.APPLICATION_FORM_URLENCODED),
                Arguments.of("application/octet-stream", MediaType.APPLICATION_OCTET_STREAM),
                Arguments.of("application/yaml", MediaType.parseMediaType("application/yaml")),
                Arguments.of("image/png", MediaType.IMAGE_PNG)
        );
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("A body sent with an unsupported Content-Type is refused with 415 and a generic body")
    @MethodSource("unsupportedContentTypes")
    void unsupportedContentTypeIsRefusedWith415(String name, MediaType contentType) throws Exception {
        String body = "{\"companyEmail\":\"a@motorph.com\",\"password\":\"" + SECRET_PASSWORD + "\"}";

        MvcResult result = mockMvc.perform(post(LOGIN).contentType(contentType).content(body))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("The request content type is not supported."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andReturn();

        assertBodyIsGeneric(result, SECRET_PASSWORD, contentType.getSubtype(), "a@motorph.com");

        verifyNoInteractions(authenticationService, loginChallengeService);
    }

    @Test
    @DisplayName("A body sent with no Content-Type at all is refused with 415")
    void missingContentTypeIsRefusedWith415() throws Exception {
        mockMvc.perform(post(LOGIN).content("{\"companyEmail\":\"a@motorph.com\",\"password\":\"pw\"}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

        verifyNoInteractions(authenticationService);
    }

    @Test
    @DisplayName("A protected endpoint also answers 415 for an authenticated caller, and 401 without a token")
    void unsupportedContentTypeOnProtectedEndpoint() throws Exception {
        mockMvc.perform(post(TICKETS).header("Authorization", bearer())
                        .contentType(MediaType.TEXT_PLAIN).content("subject=x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

        mockMvc.perform(post(TICKETS).contentType(MediaType.TEXT_PLAIN).content("subject=x"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));

        verifyNoInteractions(supportTicketService);
    }

    @Test
    @DisplayName("JSON is still accepted after the 415 handler was added")
    void jsonStillWorks() throws Exception {
        when(tokenIssuanceService.refresh("not-a-token")).thenThrow(new AuthenticationException("Invalid refresh token."));

        mockMvc.perform(post(REFRESH).contentType("application/json;charset=UTF-8").content("{\"refreshToken\":\"not-a-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    // Unsupported HTTP methods

    static Stream<Arguments> unsupportedMethods() {
        return Stream.of(
                Arguments.of("GET /auth/login", "GET", LOGIN, false, Set.of("POST")),
                Arguments.of("PUT /auth/login", "PUT", LOGIN, false, Set.of("POST")),
                Arguments.of("DELETE /auth/refresh", "DELETE", REFRESH, false, Set.of("POST")),
                Arguments.of("GET /auth/verify-otp", "GET", VERIFY_OTP, false, Set.of("POST")),
                Arguments.of("PATCH /auth/change-password", "PATCH", CHANGE_PASSWORD, true, Set.of("POST")),
                Arguments.of("DELETE /support-tickets", "DELETE", TICKETS, true, Set.of("GET", "POST")),
                Arguments.of("POST /user-accounts/{id}/roles", "POST", ROLES, true, Set.of("PUT"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("An unsupported HTTP method is refused with 405, an Allow header and a generic body")
    @MethodSource("unsupportedMethods")
    void unsupportedMethodIsRefusedWith405(String name, String method, String path, boolean authenticated,
                                           Set<String> mustAllow) throws Exception {
        MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(method), path);
        if (authenticated) {
            request.header("Authorization", bearer());
        }

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("The request method is not allowed for this endpoint."))
                .andReturn();

        String allow = result.getResponse().getHeader("Allow");
        assertThat(allow).isNotNull();
        List<String> allowed = Arrays.stream(allow.split(",")).map(String::trim).toList();
        assertThat(allowed).containsAll(mustAllow).doesNotContain(method);

        assertBodyIsGeneric(result, method);

        verifyNoInteractions(authenticationService, supportTicketService, userAccountService);
    }

    @Test
    @DisplayName("Authentication comes first: a wrong method on a protected route is 401 without a token")
    void wrongMethodOnProtectedRouteNeedsAuthenticationFirst() throws Exception {
        mockMvc.perform(request(HttpMethod.DELETE, TICKETS))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }

    // Invalid ticket category

    @ParameterizedTest(name = "category \"{0}\"")
    @DisplayName("An unknown ticket category is refused with 400 and the submitted value is not reflected")
    @ValueSource(strings = {"Banana", "<img src=x>", "PAYROLL; DROP TABLE", "letmein-s3cret", "payroll team"})
    void invalidCategoryIsNotReflected(String category) throws Exception {
        MvcResult result = mockMvc.perform(json(post(TICKETS).header("Authorization", bearer()),
                        Map.of("category", category, "subject", "Subject", "description", "Description")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Invalid support ticket category."))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(category);

        verifyNoInteractions(supportTicketService);
    }

    // Unknown routes

    static Stream<Arguments> unknownRoutes() {
        return Stream.of(
                Arguments.of("GET", "/api/v1/does-not-exist"),
                Arguments.of("POST", "/api/v1/does-not-exist"),
                Arguments.of("PUT", "/api/v1/auth/secret-admin-panel"),
                Arguments.of("DELETE", "/api/v1/support-tickets/nope/nope"),
                Arguments.of("GET", "/api/v2/auth/me"),
                Arguments.of("GET", "/actuator/env"),
                Arguments.of("GET", "/index.html"),
                Arguments.of("GET", "/.git/config"),
                Arguments.of("GET", "/")
        );
    }

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("An authenticated request to an unknown route is refused with 404 and a generic body")
    @MethodSource("unknownRoutes")
    void unknownRouteIsRefusedWith404(String method, String path) throws Exception {
        MvcResult result = mockMvc.perform(request(HttpMethod.valueOf(method), path).header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("The requested resource was not found."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .isEqualTo("{\"code\":\"NOT_FOUND\",\"message\":\"The requested resource was not found.\",\"fieldErrors\":[]}");
        assertBodyIsGeneric(result, path);

        verifyNoInteractions(authenticationService, supportTicketService, userAccountService, tokenIssuanceService);
    }

    @Test
    @DisplayName("Without a valid token an unknown route is still 401, so it does not reveal which routes exist")
    void unknownRouteNeedsAuthenticationFirst() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v1/does-not-exist"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));

        mockMvc.perform(request(HttpMethod.GET, "/api/v1/does-not-exist").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }

    // JSON is still accepted

    @ParameterizedTest(name = "{0}")
    @DisplayName("A JSON request is accepted normally, with or without a charset")
    @ValueSource(strings = {"application/json", "application/json;charset=UTF-8"})
    void jsonRequestIsAccepted(String contentType) throws Exception {
        when(authenticationService.authenticate("manuel.garcia@motorph.com", SECRET_PASSWORD))
                .thenReturn(new LoginAttemptOutcome(
                        LoginAttemptOutcome.Type.OTP_SENT, 5, "manuel.garcia@motorph.com", "OTP verification required."));
        when(loginChallengeService.issue(5)).thenReturn("challenge-id");

        mockMvc.perform(post(LOGIN).contentType(contentType)
                        .content(objectMapper.writeValueAsString(
                                Map.of("companyEmail", "manuel.garcia@motorph.com", "password", SECRET_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challengeId").value("challenge-id"));
    }

    @Test
    @DisplayName("Responses are still JSON")
    void responsesAreStillJson() throws Exception {
        when(supportTicketService.getTicketsByEmployee(EMPLOYEE_ID)).thenReturn(List.of());

        mockMvc.perform(request(HttpMethod.GET, TICKETS + "/me").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("A client that asks for an XML response is told 406, never given XML and never a 500")
    void xmlResponsesAreNotOffered() throws Exception {
        when(supportTicketService.getTicketsByEmployee(EMPLOYEE_ID)).thenReturn(List.of());

        mockMvc.perform(request(HttpMethod.GET, TICKETS + "/me").header("Authorization", bearer())
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().string(""));
    }

    // XML is no longer accepted

    private static final String XML_LOGIN =
            "<LoginRequest><companyEmail>a@motorph.com</companyEmail><password>" + SECRET_PASSWORD + "</password></LoginRequest>";

    static Stream<Arguments> xmlRequests() {
        String ticket = "<TicketSubmitRequest><category>PAYROLL</category><subject>s</subject><description>d</description></TicketSubmitRequest>";
        String roles = "<UserRolesUpdateRequest><roles>HR</roles></UserRolesUpdateRequest>";
        String change = "<ChangePasswordRequest><currentPassword>OldPass123</currentPassword><newPassword>BrandNewPass1</newPassword></ChangePasswordRequest>";

        return Stream.of(
                Arguments.of("login as application/xml", "POST", LOGIN, false, "application/xml", XML_LOGIN),
                Arguments.of("login as text/xml", "POST", LOGIN, false, "text/xml", XML_LOGIN),
                Arguments.of("login as application/xml;charset=UTF-8", "POST", LOGIN, false, "application/xml;charset=UTF-8", XML_LOGIN),
                Arguments.of("login as application/soap+xml", "POST", LOGIN, false, "application/soap+xml", XML_LOGIN),
                Arguments.of("login as application/xhtml+xml", "POST", LOGIN, false, "application/xhtml+xml", XML_LOGIN),
                Arguments.of("refresh as application/xml", "POST", REFRESH, false, "application/xml",
                        "<RefreshRequest><refreshToken>not-a-token</refreshToken></RefreshRequest>"),
                Arguments.of("change-password as application/xml", "POST", CHANGE_PASSWORD, true, "application/xml", change),
                Arguments.of("ticket as application/xml", "POST", TICKETS, true, "application/xml", ticket),
                Arguments.of("roles as text/xml", "PUT", ROLES, true, "text/xml", roles)
        );
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("An XML request is refused with 415 and a generic body, and no service is called")
    @MethodSource("xmlRequests")
    void xmlRequestIsRefusedWith415(String name, String method, String path, boolean authenticated,
                                    String contentType, String body) throws Exception {
        MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(method), path);
        if (authenticated) {
            request.header("Authorization", bearer());
        }

        MvcResult result = mockMvc.perform(request.contentType(contentType).content(body))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("The request content type is not supported."))
                .andReturn();

        assertBodyIsGeneric(result, SECRET_PASSWORD, "a@motorph.com", "not-a-token", "OldPass123", "xml");

        verifyNoInteractions(authenticationService, loginChallengeService, tokenIssuanceService,
                userAccountService, supportTicketService);
    }

    @Test
    @DisplayName("An XML external-entity payload is refused before any XML is parsed")
    void xmlExternalEntityPayloadIsNeverParsed() throws Exception {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY e SYSTEM \"file:///etc/passwd\">]>"
                + "<RefreshRequest><refreshToken>&e;</refreshToken></RefreshRequest>";

        mockMvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_XML).content(xxe))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

        verifyNoInteractions(tokenIssuanceService);
    }

    @Test
    @DisplayName("No message converter in the application can read XML into any request DTO, but JSON still can")
    void onlyJsonConvertersReadRequestDtos() {
        List<Class<?>> requestDtos = List.of(LoginRequest.class, RefreshRequest.class, ChangePasswordRequest.class,
                VerifyOtpRequest.class, TicketSubmitRequest.class, UserRolesUpdateRequest.class);
        List<MediaType> xmlTypes = List.of(MediaType.APPLICATION_XML, MediaType.TEXT_XML,
                MediaType.parseMediaType("application/soap+xml"), MediaType.APPLICATION_XHTML_XML);

        for (Class<?> dto : requestDtos) {
            for (HttpMessageConverter<?> converter : handlerAdapter.getMessageConverters()) {
                for (MediaType xml : xmlTypes) {
                    assertThat(converter.canRead(dto, xml))
                            .as("%s reading %s as %s", converter.getClass().getSimpleName(), dto.getSimpleName(), xml)
                            .isFalse();
                }
            }

            assertThat(handlerAdapter.getMessageConverters())
                    .as("a converter that reads %s from JSON", dto.getSimpleName())
                    .anyMatch(converter -> converter.canRead(dto, MediaType.APPLICATION_JSON));
        }
    }

    // Helpers

    /**
     * The response must not contain anything the client sent, or any server class names or stack details.
     */
    private void assertBodyIsGeneric(MvcResult result, String... submittedFragments) throws Exception {
        String body = result.getResponse().getContentAsString();

        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);

        assertThat(body)
                .doesNotContain(submittedFragments)
                .doesNotContain("Exception")
                .doesNotContain("java.")
                .doesNotContain("javax.")
                .doesNotContain("jakarta.")
                .doesNotContain("org.springframework")
                .doesNotContain("com.fasterxml")
                .doesNotContain("motorph.backend")
                .doesNotContain("JSON parse error")
                .doesNotContain("Cannot deserialize")
                .doesNotContain("Unexpected character")
                .doesNotContain("Source")
                .doesNotContain("stackTrace")
                .doesNotContain("trace")
                .doesNotContain("must not be blank")
                .doesNotContain("size must be");
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    private String bearer() {
        return "Bearer " + jwtTokenProvider.generateAccessToken(USER_ACCOUNT_ID, EMPLOYEE_ID).token();
    }

    private UserAccount activeAccount() {
        return new UserAccount(
                USER_ACCOUNT_ID, EMPLOYEE_ID, "hr@motorph.com", "hr@motorph.com",
                "password-hash", "password-salt", List.of(Role.HR), false, true, null, false, null);
    }
}
