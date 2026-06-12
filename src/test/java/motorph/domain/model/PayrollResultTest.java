package motorph.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests payroll result validation and helper methods.
 */
@DisplayName("PayrollResult Tests")
class PayrollResultTest {

    // creates payroll result with valid data
    @Test
    @DisplayName("constructor should create payroll result with valid data")
    void constructorShouldCreatePayrollResultWithValidData() {
        PayrollResult result = createPayrollResult(
                1,
                10001,
                1,
                80,
                30000,
                5000,
                25000,
                LocalDateTime.now()
        );

        assertEquals(10001, result.getEmployeeId());
        assertEquals(30000, result.getGrossPay(), 0.01);
        assertEquals(5000, result.getTotalDeductions(), 0.01);
        assertEquals(25000, result.getNetPay(), 0.01);
    }

    // checks payroll result with deductions
    @Test
    @DisplayName("hasDeductions should return true when deductions exist")
    void hasDeductionsShouldReturnTrueWhenDeductionsExist() {
        PayrollResult result = createPayrollResult(
                1,
                10001,
                1,
                80,
                30000,
                5000,
                25000,
                LocalDateTime.now()
        );

        assertTrue(result.hasDeductions());
    }

    // checks payroll result without deductions
    @Test
    @DisplayName("hasDeductions should return false when deductions are zero")
    void hasDeductionsShouldReturnFalseWhenDeductionsAreZero() {
        PayrollResult result = createPayrollResult(
                1,
                10001,
                1,
                80,
                30000,
                0,
                30000,
                LocalDateTime.now()
        );

        assertFalse(result.hasDeductions());
    }

    // checks payroll result with worked hours
    @Test
    @DisplayName("hasWorkedHours should return true when worked hours exist")
    void hasWorkedHoursShouldReturnTrueWhenWorkedHoursExist() {
        PayrollResult result = createPayrollResult(
                1,
                10001,
                1,
                80,
                30000,
                5000,
                25000,
                LocalDateTime.now()
        );

        assertTrue(result.hasWorkedHours());
    }

    // checks payroll result without worked hours
    @Test
    @DisplayName("hasWorkedHours should return false when worked hours are zero")
    void hasWorkedHoursShouldReturnFalseWhenWorkedHoursAreZero() {
        PayrollResult result = createPayrollResult(
                1,
                10001,
                1,
                0,
                30000,
                5000,
                25000,
                LocalDateTime.now()
        );

        assertFalse(result.hasWorkedHours());
    }

    // rejects invalid employee id
    @Test
    @DisplayName("constructor should reject invalid employee id")
    void constructorShouldRejectInvalidEmployeeId() {
        assertThrows(IllegalArgumentException.class,
                () -> createPayrollResult(
                        1,
                        0,
                        1,
                        80,
                        30000,
                        5000,
                        25000,
                        LocalDateTime.now()
                ));
    }

    // rejects invalid payrun id
    @Test
    @DisplayName("constructor should reject invalid payrun id")
    void constructorShouldRejectInvalidPayrunId() {
        assertThrows(IllegalArgumentException.class,
                () -> createPayrollResult(
                        1,
                        10001,
                        0,
                        80,
                        30000,
                        5000,
                        25000,
                        LocalDateTime.now()
                ));
    }

    // rejects negative total hours worked
    @Test
    @DisplayName("constructor should reject negative total hours worked")
    void constructorShouldRejectNegativeTotalHoursWorked() {
        assertThrows(IllegalArgumentException.class,
                () -> createPayrollResult(
                        1,
                        10001,
                        1,
                        -1,
                        30000,
                        5000,
                        25000,
                        LocalDateTime.now()
                ));
    }

    // rejects negative gross pay
    @Test
    @DisplayName("constructor should reject negative gross pay")
    void constructorShouldRejectNegativeGrossPay() {
        assertThrows(IllegalArgumentException.class,
                () -> createPayrollResult(
                        1,
                        10001,
                        1,
                        80,
                        -1,
                        0,
                        -1,
                        LocalDateTime.now()
                ));
    }

    // rejects negative total deductions
    @Test
    @DisplayName("constructor should reject negative total deductions")
    void constructorShouldRejectNegativeTotalDeductions() {
        assertThrows(IllegalArgumentException.class,
                () -> createPayrollResult(
                        1,
                        10001,
                        1,
                        80,
                        30000,
                        -1,
                        30001,
                        LocalDateTime.now()
                ));
    }

    // rejects null generated date
    @Test
    @DisplayName("constructor should reject null generated date")
    void constructorShouldRejectNullGeneratedDate() {
        assertThrows(IllegalArgumentException.class,
                () -> createPayrollResult(
                        1,
                        10001,
                        1,
                        80,
                        30000,
                        5000,
                        25000,
                        null
                ));
    }

    // rejects mismatched net pay
    @Test
    @DisplayName("constructor should reject mismatched net pay")
    void constructorShouldRejectMismatchedNetPay() {
        assertThrows(IllegalArgumentException.class,
                () -> createPayrollResult(
                        1,
                        10001,
                        1,
                        80,
                        30000,
                        5000,
                        30000,
                        LocalDateTime.now()
                ));
    }

    // creates payroll result test data
    private PayrollResult createPayrollResult(Integer payrollResultId,
                                              int employeeId,
                                              int payrunId,
                                              double totalHoursWorked,
                                              double grossPay,
                                              double totalDeductions,
                                              double netPay,
                                              LocalDateTime generatedAt) {
        return new PayrollResult(
                payrollResultId,
                employeeId,
                payrunId,
                totalHoursWorked,
                grossPay,
                totalDeductions,
                netPay,
                generatedAt
        );
    }
}
