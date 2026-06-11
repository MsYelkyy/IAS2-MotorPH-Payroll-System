package motorph.application.service;


import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.ValidationException;
import motorph.domain.value.OtpPurpose;
import motorph.infrastructure.repository.TwoFactorCodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests two-factor service validation rules.
 */
@DisplayName("TwoFactorService Database Tests")
class TwoFactorServiceTest {


    private static final Logger logger = Logger.getLogger(TwoFactorServiceTest.class.getName());


    // UT-TFS-001: constructor rejects missing repository
    @Test
    @DisplayName("UT-TFS-001 - Negative - constructor should reject null repository")
    void constructorShouldRejectNullRepository() {
        logger.info("UT-TFS-001 Testing null two-factor repository validation.");


        assertThrows(
                IllegalArgumentException.class,
                () -> new TwoFactorService(null)
        );
    }


    // UT-TFS-002: rejects invalid user account id when creating login otp
    @Test
    @DisplayName("UT-TFS-002 - Negative - createOtp should reject invalid user account ID")
    void createOtpShouldRejectInvalidUserAccountId() {
        TwoFactorService twoFactorService = createTwoFactorService();


        logger.info("UT-TFS-002 Testing invalid user account ID for OTP creation.");


        assertThrows(
                ValidationException.class,
                () -> twoFactorService.createOtp(0, null, OtpPurpose.LOGIN_2FA)
        );
    }


    // UT-TFS-003: rejects reset request id for login otp
    @Test
    @DisplayName("UT-TFS-003 - Business Rule - createOtp should reject reset request ID for login OTP")
    void createOtpShouldRejectResetRequestIdForLoginOtp() {
        TwoFactorService twoFactorService = createTwoFactorService();


        logger.info("UT-TFS-003 Testing reset request ID restriction for login OTP.");


        assertThrows(
                ValidationException.class,
                () -> twoFactorService.createOtp(1, 1, OtpPurpose.LOGIN_2FA)
        );
    }


    // UT-TFS-004: rejects missing reset request id for password reset otp
    @Test
    @DisplayName("UT-TFS-004 - Business Rule - createOtp should require reset request ID for password reset OTP")
    void createOtpShouldRequireResetRequestIdForPasswordResetOtp() {
        TwoFactorService twoFactorService = createTwoFactorService();


        logger.info("UT-TFS-004 Testing required reset request ID for password reset OTP.");


        assertThrows(
                ValidationException.class,
                () -> twoFactorService.createOtp(1, null, OtpPurpose.PASSWORD_RESET)
        );
    }


    // UT-TFS-005: rejects null otp purpose
    @Test
    @DisplayName("UT-TFS-005 - Negative - createOtp should reject null OTP purpose")
    void createOtpShouldRejectNullOtpPurpose() {
        TwoFactorService twoFactorService = createTwoFactorService();


        logger.info("UT-TFS-005 Testing null OTP purpose validation.");


        assertThrows(
                ValidationException.class,
                () -> twoFactorService.createOtp(1, null, null)
        );
    }


    // UT-TFS-006: rejects invalid user account id for login otp verification
    @Test
    @DisplayName("UT-TFS-006 - Negative - verifyLoginOtp should reject invalid user account ID")
    void verifyLoginOtpShouldRejectInvalidUserAccountId() {
        TwoFactorService twoFactorService = createTwoFactorService();


        logger.info("UT-TFS-006 Testing invalid user account ID for login OTP verification.");


        assertThrows(
                ValidationException.class,
                () -> twoFactorService.verifyLoginOtp(0, "123456")
        );
    }


    // UT-TFS-007: rejects blank login otp
    @Test
    @DisplayName("UT-TFS-007 - Negative - verifyLoginOtp should reject blank OTP")
    void verifyLoginOtpShouldRejectBlankOtp() {
        TwoFactorService twoFactorService = createTwoFactorService();


        logger.info("UT-TFS-007 Testing blank login OTP validation.");


        assertThrows(
                ValidationException.class,
                () -> twoFactorService.verifyLoginOtp(1, "")
        );
    }


    // UT-TFS-008: rejects non-numeric login otp
    @Test
    @DisplayName("UT-TFS-008 - Boundary - verifyLoginOtp should reject non-numeric OTP")
    void verifyLoginOtpShouldRejectNonNumericOtp() {
        TwoFactorService twoFactorService = createTwoFactorService();


        logger.info("UT-TFS-008 Testing non-numeric login OTP boundary.");


        assertThrows(
                ValidationException.class,
                () -> twoFactorService.verifyLoginOtp(1, "ABCDEF")
        );
    }


    // UT-TFS-009: rejects short login otp
    @Test
    @DisplayName("UT-TFS-009 - Boundary - verifyLoginOtp should reject short OTP")
    void verifyLoginOtpShouldRejectShortOtp() {
        TwoFactorService twoFactorService = createTwoFactorService();


        logger.info("UT-TFS-009 Testing short login OTP boundary.");


        assertThrows(
                ValidationException.class,
                () -> twoFactorService.verifyLoginOtp(1, "12345")
        );
    }


    // UT-TFS-010: rejects missing valid login otp
    @Test
    @DisplayName("UT-TFS-010 - Edge Case - verifyLoginOtp should reject missing valid OTP")
    void verifyLoginOtpShouldRejectMissingValidOtp() {
        TwoFactorService twoFactorService = createTwoFactorService();


        logger.info("UT-TFS-010 Testing missing valid login OTP lookup.");


        assertThrows(
                BusinessRuleException.class,
                () -> twoFactorService.verifyLoginOtp(99999, "123456")
        );
    }


    // UT-TFS-011: rejects invalid reset request id for password reset otp
    @Test
    @DisplayName("UT-TFS-011 - Negative - verifyPasswordResetOtp should reject invalid reset request ID")
    void verifyPasswordResetOtpShouldRejectInvalidResetRequestId() {
        TwoFactorService twoFactorService = createTwoFactorService();


        logger.info("UT-TFS-011 Testing invalid reset request ID for password reset OTP.");


        assertThrows(
                ValidationException.class,
                () -> twoFactorService.verifyPasswordResetOtp(0, "123456")
        );
    }


    // UT-TFS-012: rejects invalid password reset otp format
    @Test
    @DisplayName("UT-TFS-012 - Boundary - verifyPasswordResetOtp should reject invalid OTP format")
    void verifyPasswordResetOtpShouldRejectInvalidOtpFormat() {
        TwoFactorService twoFactorService = createTwoFactorService();


        logger.info("UT-TFS-012 Testing invalid password reset OTP format.");


        assertThrows(
                ValidationException.class,
                () -> twoFactorService.verifyPasswordResetOtp(1, "123")
        );
    }


    // UT-TFS-013: rejects missing valid password reset otp
    @Test
    @DisplayName("UT-TFS-013 - Edge Case - verifyPasswordResetOtp should reject missing valid OTP")
    void verifyPasswordResetOtpShouldRejectMissingValidOtp() {
        TwoFactorService twoFactorService = createTwoFactorService();


        logger.info("UT-TFS-013 Testing missing valid password reset OTP lookup.");


        assertThrows(
                BusinessRuleException.class,
                () -> twoFactorService.verifyPasswordResetOtp(99999, "123456")
        );
    }


    // creates two-factor service with real dependency
    private TwoFactorService createTwoFactorService() {
        return new TwoFactorService(new TwoFactorCodeRepository());
    }
}

