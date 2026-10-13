package motorph.backend.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import motorph.backend.dto.auth.ChangePasswordRequest;
import motorph.backend.dto.auth.LoginRequest;
import motorph.backend.dto.auth.RefreshRequest;
import motorph.backend.dto.auth.CompleteTemporaryPasswordRequest;
import motorph.backend.dto.auth.ResetPasswordRequest;
import motorph.backend.dto.auth.TotpCodeRequest;
import motorph.backend.dto.auth.TotpSetupStartRequest;
import motorph.backend.dto.support.TicketSubmitRequest;
import motorph.backend.dto.user.UserRolesUpdateRequest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the validation rules on the request DTOs. For each DTO, a normal request passes and each
 * checked field has a case that fails. These rules only check the shape of the request. Business
 * rules stay in the services.
 */
class RequestDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    private static Set<String> failingFields(Object dto) {
        Set<String> fields = new TreeSet<>();

        for (ConstraintViolation<Object> violation : validator.validate(dto)) {
            fields.add(violation.getPropertyPath().toString());
        }

        return fields;
    }

    private static String repeat(char c, int times) {
        return String.valueOf(c).repeat(times);
    }

    @Nested
    @DisplayName("LoginRequest")
    class Login {

        @Test
        @DisplayName("An ordinary request is accepted")
        void validRequestIsAccepted() {
            assertThat(failingFields(new LoginRequest("manuel.garcia@motorph.com", "Sup3r-secret pass!"))).isEmpty();
        }

        @ParameterizedTest(name = "email \"{0}\"")
        @DisplayName("A missing, blank, malformed or oversized email is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "not-an-email", "two@@motorph.com", "no spaces@motorph.com", "@motorph.com"})
        void invalidEmailIsRejected(String email) {
            assertThat(failingFields(new LoginRequest(email, "password"))).containsExactly("companyEmail");
        }

        @Test
        @DisplayName("An email longer than the 255-character column is rejected")
        void oversizedEmailIsRejected() {
            String tooLong = repeat('a', 250) + "@motorph.com";

            assertThat(failingFields(new LoginRequest(tooLong, "password"))).containsExactly("companyEmail");
        }

        @ParameterizedTest(name = "password \"{0}\"")
        @DisplayName("A missing or blank password is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void blankPasswordIsRejected(String password) {
            assertThat(failingFields(new LoginRequest("manuel.garcia@motorph.com", password)))
                    .containsExactly("password");
        }

        @Test
        @DisplayName("A password is accepted up to 128 characters and rejected beyond it")
        void passwordLengthLimit() {
            assertThat(failingFields(new LoginRequest("a@motorph.com", repeat('p', 128)))).isEmpty();
            assertThat(failingFields(new LoginRequest("a@motorph.com", repeat('p', 129)))).containsExactly("password");
        }

        @Test
        @DisplayName("Every failing field is reported, not only the first")
        void allFailingFieldsAreReported() {
            assertThat(failingFields(new LoginRequest("", ""))).containsExactly("companyEmail", "password");
        }
    }

    @Nested
    @DisplayName("RefreshRequest")
    class Refresh {

        @Test
        @DisplayName("A token-shaped value is accepted; its signature is checked later by the token service")
        void tokenIsAccepted() {
            assertThat(failingFields(new RefreshRequest("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJl"))).isEmpty();
            assertThat(failingFields(new RefreshRequest("not-a-token"))).isEmpty();
        }

        @ParameterizedTest(name = "token \"{0}\"")
        @DisplayName("A missing or blank refresh token is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\n"})
        void blankTokenIsRejected(String token) {
            assertThat(failingFields(new RefreshRequest(token))).containsExactly("refreshToken");
        }

        @Test
        @DisplayName("A refresh token is accepted up to 4096 characters and rejected beyond it")
        void tokenLengthLimit() {
            assertThat(failingFields(new RefreshRequest(repeat('t', 4096)))).isEmpty();
            assertThat(failingFields(new RefreshRequest(repeat('t', 4097)))).containsExactly("refreshToken");
        }
    }

    @Nested
    @DisplayName("ChangePasswordRequest")
    class ChangePassword {

        @Test
        @DisplayName("An ordinary request is accepted")
        void validRequestIsAccepted() {
            assertThat(failingFields(new ChangePasswordRequest("OldPass123", "BrandNewPass1"))).isEmpty();
        }

        @ParameterizedTest(name = "current password \"{0}\"")
        @DisplayName("A missing or blank current password is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void blankCurrentPasswordIsRejected(String value) {
            assertThat(failingFields(new ChangePasswordRequest(value, "BrandNewPass1")))
                    .containsExactly("currentPassword");
        }

        @ParameterizedTest(name = "new password \"{0}\"")
        @DisplayName("A missing or blank new password is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void blankNewPasswordIsRejected(String value) {
            assertThat(failingFields(new ChangePasswordRequest("OldPass123", value)))
                    .containsExactly("newPassword");
        }

        @Test
        @DisplayName("Each password is accepted up to 128 characters and rejected beyond it")
        void passwordLengthLimit() {
            assertThat(failingFields(new ChangePasswordRequest(repeat('a', 128), repeat('b', 128)))).isEmpty();
            assertThat(failingFields(new ChangePasswordRequest(repeat('a', 129), repeat('b', 129))))
                    .containsExactly("currentPassword", "newPassword");
        }

        @Test
        @DisplayName("Password strength is not a request-shape rule, so a short new password reaches the service")
        void strengthIsLeftToTheService() {
            assertThat(failingFields(new ChangePasswordRequest("OldPass123", "short"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("TotpCodeRequest")
    class TotpCode {

        private static final String CHALLENGE = "k3J9-_xQ2mP0aBcDeFgHiJkLmNoPqRsTuVwXyZ01234";

        @Test
        @DisplayName("An ordinary request is accepted")
        void validRequestIsAccepted() {
            assertThat(failingFields(new TotpCodeRequest(CHALLENGE, "012345"))).isEmpty();
        }

        @Test
        @DisplayName("A challenge ID from the server, real or decoy, passes: both are 43 URL-safe Base64 characters")
        void serverGeneratedChallengeIdsPass() {
            String generated = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);

            assertThat(generated).hasSize(43);
            assertThat(failingFields(new TotpCodeRequest(generated, "123456"))).isEmpty();
        }

        @ParameterizedTest(name = "code \"{0}\"")
        @DisplayName("An authenticator code that is not exactly six digits is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "12345", "1234567", "12345a", "abcdef", "123 456", " 123456", "123456 ", "123456\n", "-12345", "\u0661\u0662\u0663\u0664\u0665\u0666"})
        void invalidCodeIsRejected(String code) {
            assertThat(failingFields(new TotpCodeRequest(CHALLENGE, code))).containsExactly("code");
        }

        @ParameterizedTest(name = "challenge \"{0}\"")
        @DisplayName("A missing, blank or oddly shaped challenge ID is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "has space", "dot.separated", "../../etc/passwd", "semi;colon", "quote'", "<script>", "pad=="})
        void invalidChallengeIsRejected(String challengeId) {
            assertThat(failingFields(new TotpCodeRequest(challengeId, "123456"))).containsExactly("challengeId");
        }

        @Test
        @DisplayName("A challenge ID is accepted up to 128 characters and rejected beyond it")
        void challengeLengthLimit() {
            assertThat(failingFields(new TotpCodeRequest(repeat('A', 128), "123456"))).isEmpty();
            assertThat(failingFields(new TotpCodeRequest(repeat('A', 129), "123456"))).containsExactly("challengeId");
        }
    }

    @Nested
    @DisplayName("Other requests in the active authenticator flow")
    class OtherAuthenticatorFlowRequests {

        private static final String ID = "k3J9-_xQ2mP0aBcDeFgHiJkLmNoPqRsTuVwXyZ01234";

        @Test
        @DisplayName("Ordinary requests are accepted")
        void validRequestsAreAccepted() {
            assertThat(failingFields(new TotpSetupStartRequest(ID))).isEmpty();
            assertThat(failingFields(new ResetPasswordRequest(ID, "NewPassw0rd1", "NewPassw0rd1"))).isEmpty();
            assertThat(failingFields(new CompleteTemporaryPasswordRequest(ID, "NewPassw0rd1", "NewPassw0rd1"))).isEmpty();
        }

        @ParameterizedTest(name = "challenge \"{0}\"")
        @DisplayName("Setup start: a missing, blank or oddly shaped challenge ID is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "has space", "dot.separated", "../../etc/passwd", "semi;colon", "<script>", "pad=="})
        void setupStartRejectsBadChallenge(String challengeId) {
            assertThat(failingFields(new TotpSetupStartRequest(challengeId))).containsExactly("challengeId");
        }

        @ParameterizedTest(name = "reset token \"{0}\"")
        @DisplayName("Password reset: a missing, blank or oddly shaped reset token is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "has space", "dot.separated", "../../etc/passwd", "semi;colon", "<script>", "pad=="})
        void resetRejectsBadToken(String resetToken) {
            assertThat(failingFields(new ResetPasswordRequest(resetToken, "NewPassw0rd1", "NewPassw0rd1")))
                    .containsExactly("resetToken");
        }

        @ParameterizedTest(name = "challenge \"{0}\"")
        @DisplayName("Temporary password change: a missing, blank or oddly shaped challenge ID is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "has space", "dot.separated", "../../etc/passwd", "semi;colon", "<script>", "pad=="})
        void temporaryPasswordRejectsBadChallenge(String challengeId) {
            assertThat(failingFields(new CompleteTemporaryPasswordRequest(challengeId, "NewPassw0rd1", "NewPassw0rd1")))
                    .containsExactly("challengeId");
        }

        @Test
        @DisplayName("Each ID is accepted up to 128 characters and rejected beyond it")
        void idLengthLimit() {
            assertThat(failingFields(new TotpSetupStartRequest(repeat('A', 128)))).isEmpty();
            assertThat(failingFields(new TotpSetupStartRequest(repeat('A', 129)))).containsExactly("challengeId");
            assertThat(failingFields(new ResetPasswordRequest(repeat('A', 129), "x", "x"))).containsExactly("resetToken");
            assertThat(failingFields(new CompleteTemporaryPasswordRequest(repeat('A', 129), "x", "x")))
                    .containsExactly("challengeId");
        }

        @Test
        @DisplayName("Password rules are not request-shape rules, so they still reach the service")
        void passwordRulesAreLeftToTheService() {
            assertThat(failingFields(new ResetPasswordRequest(ID, "short", "different"))).isEmpty();
            assertThat(failingFields(new CompleteTemporaryPasswordRequest(ID, null, null))).isEmpty();
        }
    }

    @Nested
    @DisplayName("TicketSubmitRequest")
    class TicketSubmit {

        @Test
        @DisplayName("An ordinary request is accepted")
        void validRequestIsAccepted() {
            assertThat(failingFields(new TicketSubmitRequest(
                    "PAYROLL", "Missing overtime", "My overtime for 3 March is not on my payslip."))).isEmpty();
        }

        @Test
        @DisplayName("Markup, quotes, symbols and non-English text are legitimate and are accepted as they are")
        void markupAndUnicodeAreAccepted() {
            assertThat(failingFields(new TicketSubmitRequest(
                    "OTHER",
                    "Hours < 8 & rate > \u20B1500 for \"Ni\u00F1o\" \u65E5\u672C\u8A9E \uD83D\uDE00",
                    "<b>Pay</b> is wrong; the <script> tag in the template shows as text. It's 5 > 3."))).isEmpty();
        }

        @Test
        @DisplayName("A description may span several lines and contain tabs; a subject may contain tabs")
        void lineBreaksAreAllowedInTheDescription() {
            assertThat(failingFields(new TicketSubmitRequest(
                    "SYSTEM", "Tabbed\tsubject", "Line one\nLine two\r\nLine\tthree"))).isEmpty();
        }

        @ParameterizedTest(name = "category \"{0}\"")
        @DisplayName("A missing or blank category is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void blankCategoryIsRejected(String category) {
            assertThat(failingFields(new TicketSubmitRequest(category, "Subject", "Description")))
                    .containsExactly("category");
        }

        @Test
        @DisplayName("An oversized category is rejected, which also bounds what an invalid value can echo back")
        void oversizedCategoryIsRejected() {
            assertThat(failingFields(new TicketSubmitRequest(repeat('C', 31), "Subject", "Description")))
                    .containsExactly("category");
        }

        @ParameterizedTest(name = "subject \"{0}\"")
        @DisplayName("A missing or blank subject is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void blankSubjectIsRejected(String subject) {
            assertThat(failingFields(new TicketSubmitRequest("PAYROLL", subject, "Description")))
                    .contains("subject");
        }

        @ParameterizedTest(name = "description \"{0}\"")
        @DisplayName("A missing or blank description is rejected")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\n\n"})
        void blankDescriptionIsRejected(String description) {
            assertThat(failingFields(new TicketSubmitRequest("PAYROLL", "Subject", description)))
                    .contains("description");
        }

        @Test
        @DisplayName("The subject matches its 150-character column and the description its 1000-character column")
        void lengthLimitsMatchTheColumns() {
            assertThat(failingFields(new TicketSubmitRequest("PAYROLL", repeat('s', 150), repeat('d', 1000)))).isEmpty();
            assertThat(failingFields(new TicketSubmitRequest("PAYROLL", repeat('s', 151), repeat('d', 1001))))
                    .containsExactly("description", "subject");
        }

        @ParameterizedTest(name = "subject with control character U+{0}")
        @DisplayName("Control characters, and line breaks in the subject, are rejected")
        @ValueSource(strings = {"0000", "0007", "001B", "007F", "0085", "000A", "000D"})
        void controlCharactersInSubjectAreRejected(String codePoint) {
            String subject = "Pay" + (char) Integer.parseInt(codePoint, 16) + "slip";

            assertThat(failingFields(new TicketSubmitRequest("PAYROLL", subject, "Description")))
                    .containsExactly("subject");
        }

        @ParameterizedTest(name = "description with control character U+{0}")
        @DisplayName("Control characters other than tab and line breaks are rejected in the description")
        @ValueSource(strings = {"0000", "0007", "001B", "007F", "0085"})
        void controlCharactersInDescriptionAreRejected(String codePoint) {
            String description = "Pay" + (char) Integer.parseInt(codePoint, 16) + "slip";

            assertThat(failingFields(new TicketSubmitRequest("PAYROLL", "Subject", description)))
                    .containsExactly("description");
        }
    }

    @Nested
    @DisplayName("UserRolesUpdateRequest")
    class UserRolesUpdate {

        @Test
        @DisplayName("An ordinary request is accepted")
        void validRequestIsAccepted() {
            assertThat(failingFields(new UserRolesUpdateRequest(List.of("HR", "EMPLOYEE")))).isEmpty();
        }

        @ParameterizedTest
        @DisplayName("A missing role list is rejected")
        @NullSource
        void missingListIsRejected(List<String> roles) {
            assertThat(failingFields(new UserRolesUpdateRequest(roles))).containsExactly("roles");
        }

        @Test
        @DisplayName("An empty list passes the request check; 'at least one role' is a service rule")
        void emptyListIsLeftToTheService() {
            assertThat(failingFields(new UserRolesUpdateRequest(List.of()))).isEmpty();
        }

        @Test
        @DisplayName("A blank or null role name is rejected and the failing element is identified")
        void blankElementsAreRejected() {
            assertThat(failingFields(new UserRolesUpdateRequest(List.of("HR", " "))))
                    .containsExactly("roles[1].<list element>");

            assertThat(failingFields(new UserRolesUpdateRequest(Arrays.asList("HR", null))))
                    .containsExactly("roles[1].<list element>");
        }

        @Test
        @DisplayName("A role name longer than the 45-character column is rejected")
        void oversizedRoleNameIsRejected() {
            assertThat(failingFields(new UserRolesUpdateRequest(List.of(repeat('R', 46)))))
                    .containsExactly("roles[0].<list element>");
        }

        @Test
        @DisplayName("More than ten roles is rejected")
        void tooManyRolesAreRejected() {
            List<String> eleven = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                eleven.add("ROLE" + i);
            }

            assertThat(failingFields(new UserRolesUpdateRequest(eleven))).containsExactly("roles");
        }
    }
}
