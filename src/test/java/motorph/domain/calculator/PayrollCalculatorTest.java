package motorph.domain.calculator;


import motorph.application.dto.payroll.PayrollInputDTO;
import motorph.domain.model.Attendance;
import motorph.domain.model.PayrollBreakdown;
import motorph.domain.value.AttendanceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests payroll calculator computation rules.
 */
@DisplayName("PayrollCalculator Tests")
class PayrollCalculatorTest {


    private final PayrollCalculator payrollCalculator = new PayrollCalculator();


    // computes first cutoff payroll without government deductions
    @Test
    @DisplayName("computeDeductions should compute first cutoff without government deductions")
    void computeDeductionsShouldComputeFirstCutoffWithoutGovernmentDeductions() {
        PayrollInputDTO input = createInput(30000, 1500, 1000, 1000, 100);


        PayrollBreakdown result = payrollCalculator.computeDeductions(
                input,
                List.of(),
                0,
                LocalDate.of(2025, 6, 15)
        );


        assertEquals(15000.00, result.getBasicPay(), 0.01);
        assertEquals(750.00, result.getRiceSubsidy(), 0.01);
        assertEquals(500.00, result.getPhoneAllowance(), 0.01);
        assertEquals(500.00, result.getClothingAllowance(), 0.01);
        assertEquals(16750.00, result.getTotalEarnings(), 0.01);
        assertEquals(0.00, result.getTotalDeductions(), 0.01);
        assertEquals(16750.00, result.getNetPay(), 0.01);
    }


    // computes overtime pay
    @Test
    @DisplayName("computeDeductions should include approved overtime pay")
    void computeDeductionsShouldIncludeApprovedOvertimePay() {
        PayrollInputDTO input = createInput(30000, 0, 0, 0, 100);


        PayrollBreakdown result = payrollCalculator.computeDeductions(
                input,
                List.of(),
                2,
                LocalDate.of(2025, 6, 15)
        );


        assertEquals(200.00, result.getOvertimePay(), 0.01);
        assertEquals(15200.00, result.getTotalEarnings(), 0.01);
        assertEquals(15200.00, result.getNetPay(), 0.01);
    }


    // applies second cutoff government deductions
    @Test
    @DisplayName("computeDeductions should apply government deductions on second cutoff")
    void computeDeductionsShouldApplyGovernmentDeductionsOnSecondCutoff() {
        PayrollInputDTO input = createInput(30000, 0, 0, 0, 100);


        PayrollBreakdown result = payrollCalculator.computeDeductions(
                input,
                List.of(),
                0,
                LocalDate.of(2025, 6, 30)
        );


        assertEquals(1125.00, result.getSssDeduction(), 0.01);
        assertEquals(450.00, result.getPhilhealthDeduction(), 0.01);
        assertEquals(100.00, result.getPagibigDeduction(), 0.01);
        assertTrue(result.getWithholdingTax() > 0);
        assertTrue(result.getTotalDeductions() > 0);
        assertTrue(result.getNetPay() < result.getTotalEarnings());
    }


    // applies late and undertime deductions
    @Test
    @DisplayName("computeDeductions should deduct late and undertime")
    void computeDeductionsShouldDeductLateAndUndertime() {
        PayrollInputDTO input = createInput(30000, 0, 0, 0, 120);


        Attendance attendance = new Attendance(
                1,
                10001,
                LocalDate.of(2025, 6, 10),
                LocalTime.of(8, 30),
                LocalTime.of(16, 30),
                AttendanceStatus.LATE
        );


        PayrollBreakdown result = payrollCalculator.computeDeductions(
                input,
                List.of(attendance),
                0,
                LocalDate.of(2025, 6, 15)
        );


        assertEquals(60.00, result.getLateDeduction(), 0.01);
        assertEquals(60.00, result.getUndertimeDeduction(), 0.01);
        assertEquals(120.00, result.getTotalDeductions(), 0.01);
        assertEquals(14880.00, result.getNetPay(), 0.01);
    }


    // ignores attendance outside cutoff range
    @Test
    @DisplayName("computeDeductions should ignore attendance outside cutoff range")
    void computeDeductionsShouldIgnoreAttendanceOutsideCutoffRange() {
        PayrollInputDTO input = createInput(30000, 0, 0, 0, 120);


        Attendance outsideCutoffAttendance = new Attendance(
                1,
                10001,
                LocalDate.of(2025, 6, 20),
                LocalTime.of(8, 30),
                LocalTime.of(16, 30),
                AttendanceStatus.LATE
        );


        PayrollBreakdown result = payrollCalculator.computeDeductions(
                input,
                List.of(outsideCutoffAttendance),
                0,
                LocalDate.of(2025, 6, 15)
        );


        assertEquals(0.00, result.getLateDeduction(), 0.01);
        assertEquals(0.00, result.getUndertimeDeduction(), 0.01);
        assertEquals(15000.00, result.getNetPay(), 0.01);
    }


    // calculates paid regular hours with lunch deduction and daily cap
    @Test
    @DisplayName("calculatePaidRegularHours should deduct lunch and cap regular hours")
    void calculatePaidRegularHoursShouldDeductLunchAndCapRegularHours() {
        Attendance attendance = new Attendance(
                1,
                10001,
                LocalDate.of(2025, 6, 10),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                AttendanceStatus.PRESENT
        );


        double paidHours = PayrollCalculator.calculatePaidRegularHours(attendance);


        assertEquals(8.00, paidHours, 0.01);
    }


    // computes zero tax for tax-free bracket
    @Test
    @DisplayName("computeMonthlyTax should return zero for tax-free bracket")
    void computeMonthlyTaxShouldReturnZeroForTaxFreeBracket() {
        double tax = PayrollCalculator.computeMonthlyTax(20833, 0);


        assertEquals(0.00, tax, 0.01);
    }


    // rejects null payroll input
    @Test
    @DisplayName("computeDeductions should reject null payroll input")
    void computeDeductionsShouldRejectNullPayrollInput() {
        assertThrows(IllegalArgumentException.class,
                () -> payrollCalculator.computeDeductions(
                        null,
                        List.of(),
                        0,
                        LocalDate.of(2025, 6, 15)
                ));
    }


    // rejects negative overtime hours
    @Test
    @DisplayName("computeDeductions should reject negative overtime hours")
    void computeDeductionsShouldRejectNegativeOvertimeHours() {
        PayrollInputDTO input = createInput(30000, 0, 0, 0, 100);


        assertThrows(IllegalArgumentException.class,
                () -> payrollCalculator.computeDeductions(
                        input,
                        List.of(),
                        -1,
                        LocalDate.of(2025, 6, 15)
                ));
    }


    // creates payroll input test data
    private PayrollInputDTO createInput(double basicSalary,
                                        double riceSubsidy,
                                        double phoneAllowance,
                                        double clothingAllowance,
                                        double hourlyRate) {
        return new PayrollInputDTO(
                10001,
                "Juan",
                "Dela Cruz",
                1,
                null,
                null,
                null,
                "123456789",
                "SSS123",
                "PH123",
                "PAG123",
                basicSalary,
                basicSalary / 2,
                hourlyRate,
                riceSubsidy,
                phoneAllowance,
                clothingAllowance
        );
    }
}
