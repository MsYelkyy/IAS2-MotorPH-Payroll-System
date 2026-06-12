package motorph.application.service;

import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.Payslip;
import motorph.infrastructure.config.EmailConfig;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.PayrollBenefitRepository;
import motorph.infrastructure.repository.PayrollDeductionRepository;
import motorph.infrastructure.repository.PayrollEarningRepository;
import motorph.infrastructure.repository.PayrollOvertimeRepository;
import motorph.infrastructure.repository.PayrollResultRepository;
import motorph.infrastructure.repository.PayrunRepository;
import motorph.infrastructure.repository.UserAccountRepository;
import motorph.infrastructure.reporting.JasperReportGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests payslip service using real database dependencies.
 */
@DisplayName("PayslipService Database Tests")
class PayslipServiceTest {

    private static final Logger logger = Logger.getLogger(PayslipServiceTest.class.getName());

    // UT-PSS-001: constructor rejects missing payroll result repository
    @Test
    @DisplayName("UT-PSS-001 - Negative - constructor should reject null payroll result repository")
    void constructorShouldRejectNullPayrollResultRepository() {
        logger.info("UT-PSS-001 Testing null payroll result repository validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PayslipService(
                        null,
                        new PayrollEarningRepository(),
                        new PayrollBenefitRepository(),
                        new PayrollDeductionRepository(),
                        new PayrollOvertimeRepository(),
                        new PayrunRepository(),
                        createEmployeeService(),
                        new JasperReportGenerator()
                )
        );
    }

    // UT-PSS-002: constructor rejects missing payroll earning repository
    @Test
    @DisplayName("UT-PSS-002 - Negative - constructor should reject null payroll earning repository")
    void constructorShouldRejectNullPayrollEarningRepository() {
        logger.info("UT-PSS-002 Testing null payroll earning repository validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PayslipService(
                        new PayrollResultRepository(),
                        null,
                        new PayrollBenefitRepository(),
                        new PayrollDeductionRepository(),
                        new PayrollOvertimeRepository(),
                        new PayrunRepository(),
                        createEmployeeService(),
                        new JasperReportGenerator()
                )
        );
    }

    // UT-PSS-003: constructor rejects missing payroll benefit repository
    @Test
    @DisplayName("UT-PSS-003 - Negative - constructor should reject null payroll benefit repository")
    void constructorShouldRejectNullPayrollBenefitRepository() {
        logger.info("UT-PSS-003 Testing null payroll benefit repository validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PayslipService(
                        new PayrollResultRepository(),
                        new PayrollEarningRepository(),
                        null,
                        new PayrollDeductionRepository(),
                        new PayrollOvertimeRepository(),
                        new PayrunRepository(),
                        createEmployeeService(),
                        new JasperReportGenerator()
                )
        );
    }

    // UT-PSS-004: constructor rejects missing payroll deduction repository
    @Test
    @DisplayName("UT-PSS-004 - Negative - constructor should reject null payroll deduction repository")
    void constructorShouldRejectNullPayrollDeductionRepository() {
        logger.info("UT-PSS-004 Testing null payroll deduction repository validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PayslipService(
                        new PayrollResultRepository(),
                        new PayrollEarningRepository(),
                        new PayrollBenefitRepository(),
                        null,
                        new PayrollOvertimeRepository(),
                        new PayrunRepository(),
                        createEmployeeService(),
                        new JasperReportGenerator()
                )
        );
    }

    // UT-PSS-005: constructor rejects missing payroll overtime repository
    @Test
    @DisplayName("UT-PSS-005 - Negative - constructor should reject null payroll overtime repository")
    void constructorShouldRejectNullPayrollOvertimeRepository() {
        logger.info("UT-PSS-005 Testing null payroll overtime repository validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PayslipService(
                        new PayrollResultRepository(),
                        new PayrollEarningRepository(),
                        new PayrollBenefitRepository(),
                        new PayrollDeductionRepository(),
                        null,
                        new PayrunRepository(),
                        createEmployeeService(),
                        new JasperReportGenerator()
                )
        );
    }

    // UT-PSS-006: constructor rejects missing payrun repository
    @Test
    @DisplayName("UT-PSS-006 - Negative - constructor should reject null payrun repository")
    void constructorShouldRejectNullPayrunRepository() {
        logger.info("UT-PSS-006 Testing null payrun repository validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PayslipService(
                        new PayrollResultRepository(),
                        new PayrollEarningRepository(),
                        new PayrollBenefitRepository(),
                        new PayrollDeductionRepository(),
                        new PayrollOvertimeRepository(),
                        null,
                        createEmployeeService(),
                        new JasperReportGenerator()
                )
        );
    }

    // UT-PSS-007: constructor rejects missing employee service
    @Test
    @DisplayName("UT-PSS-007 - Negative - constructor should reject null employee service")
    void constructorShouldRejectNullEmployeeService() {
        logger.info("UT-PSS-007 Testing null employee service validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PayslipService(
                        new PayrollResultRepository(),
                        new PayrollEarningRepository(),
                        new PayrollBenefitRepository(),
                        new PayrollDeductionRepository(),
                        new PayrollOvertimeRepository(),
                        new PayrunRepository(),
                        null,
                        new JasperReportGenerator()
                )
        );
    }

    // UT-PSS-008: constructor rejects missing jasper report generator
    @Test
    @DisplayName("UT-PSS-008 - Negative - constructor should reject null jasper report generator")
    void constructorShouldRejectNullJasperReportGenerator() {
        logger.info("UT-PSS-008 Testing null Jasper report generator validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new PayslipService(
                        new PayrollResultRepository(),
                        new PayrollEarningRepository(),
                        new PayrollBenefitRepository(),
                        new PayrollDeductionRepository(),
                        new PayrollOvertimeRepository(),
                        new PayrunRepository(),
                        createEmployeeService(),
                        null
                )
        );
    }

    // UT-PSS-009: rejects missing payroll result
        @Test
        @DisplayName("UT-PSS-009 - Edge Case - generatePayslip should reject missing payroll result")
        void generatePayslipShouldRejectMissingPayrollResultForExistingEmployeeAndPayrun() {
        PayslipService payslipService = createPayslipService();

        logger.info("UT-PSS-009 Testing missing payroll result for existing employee and payrun.");

        assertThrows(
                EntityNotFoundException.class,
                () -> payslipService.generatePayslip(10001, 1)
        );
        }

    // UT-PSS-010: rejects invalid employee id
    @Test
    @DisplayName("UT-PSS-010 - Negative - generatePayslip should reject invalid employee ID")
    void generatePayslipShouldRejectInvalidEmployeeId() {
        PayslipService payslipService = createPayslipService();

        logger.info("UT-PSS-010 Testing invalid employee ID validation.");

        assertThrows(
                ValidationException.class,
                () -> payslipService.generatePayslip(0, 1)
        );
    }

    // UT-PSS-011: rejects invalid payrun id
    @Test
    @DisplayName("UT-PSS-011 - Negative - generatePayslip should reject invalid payrun ID")
    void generatePayslipShouldRejectInvalidPayrunId() {
        PayslipService payslipService = createPayslipService();

        logger.info("UT-PSS-011 Testing invalid payrun ID validation.");

        assertThrows(
                ValidationException.class,
                () -> payslipService.generatePayslip(10001, 0)
        );
    }

    // UT-PSS-013: rejects negative employee id boundary
    @Test
    @DisplayName("UT-PSS-013 - Boundary - generatePayslip should reject negative employee ID")
    void generatePayslipShouldRejectNegativeEmployeeId() {
        PayslipService payslipService = createPayslipService();

        logger.info("UT-PSS-013 Testing negative employee ID boundary.");

        assertThrows(
                ValidationException.class,
                () -> payslipService.generatePayslip(-1, 1)
        );
    }

    // UT-PSS-014: rejects negative payrun id boundary
    @Test
    @DisplayName("UT-PSS-014 - Boundary - generatePayslip should reject negative payrun ID")
    void generatePayslipShouldRejectNegativePayrunId() {
        PayslipService payslipService = createPayslipService();

        logger.info("UT-PSS-014 Testing negative payrun ID boundary.");

        assertThrows(
                ValidationException.class,
                () -> payslipService.generatePayslip(10001, -1)
        );
    }

    // UT-PSS-015: rejects blank output path before export
    @Test
    @DisplayName("UT-PSS-015 - Negative - exportPayslipToPdf should reject blank output path")
    void exportPayslipToPdfShouldRejectBlankOutputPath() {
        PayslipService payslipService = createPayslipService();

        logger.info("UT-PSS-015 Testing blank output path validation.");

        assertThrows(
                ValidationException.class,
                () -> payslipService.exportPayslipToPdf(10001, 1, "")
        );
    }

    // UT-PSS-016: rejects null output path before export
    @Test
    @DisplayName("UT-PSS-016 - Negative - exportPayslipToPdf should reject null output path")
    void exportPayslipToPdfShouldRejectNullOutputPath() {
        PayslipService payslipService = createPayslipService();

        logger.info("UT-PSS-016 Testing null output path validation.");

        assertThrows(
                ValidationException.class,
                () -> payslipService.exportPayslipToPdf(10001, 1, null)
        );
    }

    // creates payslip service with real dependencies
    private PayslipService createPayslipService() {
        return new PayslipService(
                new PayrollResultRepository(),
                new PayrollEarningRepository(),
                new PayrollBenefitRepository(),
                new PayrollDeductionRepository(),
                new PayrollOvertimeRepository(),
                new PayrunRepository(),
                createEmployeeService(),
                new JasperReportGenerator()
        );
    }

    // creates employee service with real dependencies
    private EmployeeService createEmployeeService() {
        return new EmployeeService(
                new EmployeeRepository(),
                createUserAccountService()
        );
    }

    // creates user account service dependency
    private UserAccountService createUserAccountService() {
        return new UserAccountService(
                new UserAccountRepository(),
                new EmailService(createEmailConfig())
        );
    }

    // creates email config test data
    private EmailConfig createEmailConfig() {
        return new EmailConfig(
                "sandbox.smtp.mailtrap.io",
                2525,
                "test_username",
                "test_password",
                "no-reply@motorph.com",
                true,
                true
        );
    }
}

