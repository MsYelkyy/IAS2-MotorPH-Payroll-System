package motorph.domain.model;


import motorph.domain.model.PayrollBreakdown;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests payroll breakdown validation and helper methods.
 */
@DisplayName("PayrollBreakdown Tests")
class PayrollBreakdownTest {


    // creates payroll breakdown with valid amounts
    @Test
    @DisplayName("constructor should create payroll breakdown with valid amounts")
    void constructorShouldCreatePayrollBreakdownWithValidAmounts() {
        PayrollBreakdown breakdown = createBreakdown(
                15000,
                750,
                500,
                500,
                200,
                16950,
                100,
                50,
                1125,
                450,
                100,
                500,
                2325,
                14625
        );


        assertEquals(16950, breakdown.getTotalEarnings(), 0.01);
        assertEquals(2325, breakdown.getTotalDeductions(), 0.01);
        assertEquals(14625, breakdown.getNetPay(), 0.01);
    }


    // checks overtime pay helper
    @Test
    @DisplayName("hasOvertimePay should return true when overtime pay exists")
    void hasOvertimePayShouldReturnTrueWhenOvertimePayExists() {
        PayrollBreakdown breakdown = createBreakdown(
                15000,
                0,
                0,
                0,
                200,
                15200,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                15200
        );


        assertTrue(breakdown.hasOvertimePay());
    }


    // checks deductions helper
    @Test
    @DisplayName("hasDeductions should return true when deductions exist")
    void hasDeductionsShouldReturnTrueWhenDeductionsExist() {
        PayrollBreakdown breakdown = createBreakdown(
                15000,
                0,
                0,
                0,
                0,
                15000,
                100,
                0,
                0,
                0,
                0,
                0,
                100,
                14900
        );


        assertTrue(breakdown.hasDeductions());
    }


    // rejects negative amount
    @Test
    @DisplayName("constructor should reject negative basic pay")
    void constructorShouldRejectNegativeBasicPay() {
        assertThrows(IllegalArgumentException.class,
                () -> createBreakdown(
                        -1,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                ));
    }


    // rejects mismatched net pay
    @Test
    @DisplayName("constructor should reject mismatched net pay")
    void constructorShouldRejectMismatchedNetPay() {
        assertThrows(IllegalArgumentException.class,
                () -> createBreakdown(
                        15000,
                        0,
                        0,
                        0,
                        0,
                        15000,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        1000,
                        15000
                ));
    }


    // creates payroll breakdown test data
    private PayrollBreakdown createBreakdown(double basicPay,
                                             double riceSubsidy,
                                             double phoneAllowance,
                                             double clothingAllowance,
                                             double overtimePay,
                                             double totalEarnings,
                                             double lateDeduction,
                                             double undertimeDeduction,
                                             double sssDeduction,
                                             double philhealthDeduction,
                                             double pagibigDeduction,
                                             double withholdingTax,
                                             double totalDeductions,
                                             double netPay) {
        return new PayrollBreakdown(
                basicPay,
                riceSubsidy,
                phoneAllowance,
                clothingAllowance,
                overtimePay,
                totalEarnings,
                lateDeduction,
                undertimeDeduction,
                sssDeduction,
                philhealthDeduction,
                pagibigDeduction,
                withholdingTax,
                totalDeductions,
                netPay
        );
    }
}
