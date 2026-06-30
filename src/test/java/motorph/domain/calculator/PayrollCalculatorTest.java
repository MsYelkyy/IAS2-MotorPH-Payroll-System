package motorph.domain.calculator;


import motorph.application.dto.payroll.GovernmentRatesDTO;
import motorph.application.dto.payroll.PayrollInputDTO;
import motorph.application.dto.systemconfig.PagIbigRateDTO;
import motorph.application.dto.systemconfig.PhilHealthRateDTO;
import motorph.application.dto.systemconfig.SssRateDTO;
import motorph.application.dto.systemconfig.WithholdingTaxBracketDTO;
import motorph.domain.model.Attendance;
import motorph.domain.model.PayrollBreakdown;
import motorph.domain.value.AttendanceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
                LocalDate.of(2025, 6, 15),
                createRates()
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
                LocalDate.of(2025, 6, 15),
                createRates()
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
                LocalDate.of(2025, 6, 30),
                createRates()
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
                LocalDate.of(2025, 6, 15),
                createRates()
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
                LocalDate.of(2025, 6, 15),
                createRates()
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
        double tax = PayrollCalculator.computeMonthlyTax(20833, 0, createRates().getTaxBrackets());


        assertEquals(0.00, tax, 0.01);
    }


    // computes gross semi-monthly earnings from basic pay and allowances
    @Test
    @DisplayName("computeDeductions should compute gross earnings from basic and allowances")
    void computeDeductionsShouldComputeGrossEarnings() {
        PayrollInputDTO input = createInput(30000, 1500, 1000, 1000, 100);

        PayrollBreakdown result = payrollCalculator.computeDeductions(
                input, List.of(), 0, LocalDate.of(2025, 6, 15), createRates());

        // 15000 basic + 750 rice + 500 phone + 500 clothing
        assertEquals(16750.00, result.getTotalEarnings(), 0.01);
    }


    // computes net pay after government deductions on the second cutoff
    @Test
    @DisplayName("computeDeductions should compute net pay with tax deductions")
    void computeDeductionsShouldComputeNetPayWithTax() {
        PayrollInputDTO input = createInput(30000, 0, 0, 0, 100);

        PayrollBreakdown result = payrollCalculator.computeDeductions(
                input, List.of(), 0, LocalDate.of(2025, 6, 30), createRates());

        assertEquals(result.getTotalEarnings() - result.getTotalDeductions(), result.getNetPay(), 0.01);
        assertTrue(result.getWithholdingTax() > 0);
    }


    // computes overtime pay from approved hours and hourly rate
    @Test
    @DisplayName("computeDeductions should compute overtime pay from approved hours")
    void computeDeductionsShouldComputeOvertimePay() {
        PayrollInputDTO input = createInput(30000, 0, 0, 0, 150);

        PayrollBreakdown result = payrollCalculator.computeDeductions(
                input, List.of(), 4, LocalDate.of(2025, 6, 15), createRates());

        // 4 hours * 150 hourly rate
        assertEquals(600.00, result.getOvertimePay(), 0.01);
    }


    // computes payroll correctly when no allowances are provided
    @Test
    @DisplayName("computeDeductions should compute correctly with no allowances")
    void computeDeductionsShouldComputeWithNoAllowances() {
        PayrollInputDTO input = createInput(30000, 0, 0, 0, 100);

        PayrollBreakdown result = payrollCalculator.computeDeductions(
                input, List.of(), 0, LocalDate.of(2025, 6, 15), createRates());

        assertEquals(0.00, result.getRiceSubsidy(), 0.01);
        assertEquals(0.00, result.getPhoneAllowance(), 0.01);
        assertEquals(0.00, result.getClothingAllowance(), 0.01);
        assertEquals(15000.00, result.getTotalEarnings(), 0.01);
    }


    // keeps monetary values free of rounding drift over repeated computation
    @Test
    @DisplayName("computeDeductions should not accumulate rounding errors")
    void computeDeductionsShouldNotAccumulateRoundingErrors() {
        PayrollInputDTO input = createInput(33333, 1500, 1000, 1000, 100);

        PayrollBreakdown first = payrollCalculator.computeDeductions(
                input, List.of(), 0, LocalDate.of(2025, 6, 15), createRates());
        PayrollBreakdown second = payrollCalculator.computeDeductions(
                input, List.of(), 0, LocalDate.of(2025, 6, 15), createRates());

        // identical inputs must yield identical results (no drift)
        assertEquals(first.getNetPay(), second.getNetPay(), 0.001);
    }


    // stress: stays stable and correct across a high volume of computations
    @Test
    @DisplayName("Stress - computeDeductions should stay stable for high volume")
    void computeDeductionsShouldStayStableForHighVolume() {
        PayrollInputDTO input = createInput(30000, 1500, 1000, 1000, 100);
        GovernmentRatesDTO rates = createRates();

        for (int i = 0; i < 10_000; i++) {
            PayrollBreakdown result = payrollCalculator.computeDeductions(
                    input, List.of(), 0, LocalDate.of(2025, 6, 15), rates);
            assertEquals(16750.00, result.getNetPay(), 0.01);
        }
    }


    // stress: handles a large attendance list without failure
    @Test
    @DisplayName("Stress - computeDeductions should handle a large attendance list")
    void computeDeductionsShouldHandleLargeAttendanceList() {
        List<Attendance> bigList = new ArrayList<>();
        for (int i = 1; i <= 5000; i++) {
            bigList.add(new Attendance(
                    i, 10001, LocalDate.of(2025, 6, 10),
                    LocalTime.of(8, 0), LocalTime.of(17, 0), AttendanceStatus.PRESENT));
        }

        PayrollBreakdown result = payrollCalculator.computeDeductions(
                createInput(30000, 0, 0, 0, 100), bigList, 0,
                LocalDate.of(2025, 6, 15), createRates());

        assertNotNull(result);
    }


    // overloaded computeMonthlyTax matches the full version with zero deductions
    @Test
    @DisplayName("computeMonthlyTax overload should match the full method with zero deductions")
    void computeMonthlyTaxOverloadShouldMatchFullMethodWithZeroDeductions() {
        List<WithholdingTaxBracketDTO> brackets = createRates().getTaxBrackets();

        double overloaded = PayrollCalculator.computeMonthlyTax(30000, brackets);
        double full = PayrollCalculator.computeMonthlyTax(30000, 0.0, brackets);

        assertEquals(full, overloaded, 0.01);
    }


    // overloaded calculatePaidRegularHours matches the attendance version
    @Test
    @DisplayName("calculatePaidRegularHours overload should match the attendance version")
    void calculatePaidRegularHoursOverloadShouldMatchAttendanceVersion() {
        Attendance attendance = new Attendance(
                1,
                10001,
                LocalDate.of(2025, 6, 10),
                LocalTime.of(8, 0),
                LocalTime.of(18, 0),
                AttendanceStatus.PRESENT
        );

        double fromTimes = PayrollCalculator.calculatePaidRegularHours(LocalTime.of(8, 0), LocalTime.of(18, 0));
        double fromAttendance = PayrollCalculator.calculatePaidRegularHours(attendance);

        assertEquals(8.00, fromTimes, 0.01);
        assertEquals(fromAttendance, fromTimes, 0.01);
    }


    // overloaded calculatePaidRegularHours returns zero for null times
    @Test
    @DisplayName("calculatePaidRegularHours overload should return zero for null times")
    void calculatePaidRegularHoursOverloadShouldReturnZeroForNullTimes() {
        assertEquals(0.00, PayrollCalculator.calculatePaidRegularHours(null, LocalTime.of(17, 0)), 0.01);
        assertEquals(0.00, PayrollCalculator.calculatePaidRegularHours(LocalTime.of(8, 0), null), 0.01);
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
                        LocalDate.of(2025, 6, 15),
                        createRates()
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
                        LocalDate.of(2025, 6, 15),
                        createRates()
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

    // creates government rates matching the values previously hardcoded in PayrollCalculator
    private GovernmentRatesDTO createRates() {
        LocalDate start = LocalDate.of(2020, 1, 1);

        // employeeShare stores the fixed max SSS contribution (1125.00); calculator prorates below maxSalary
        SssRateDTO sss = new SssRateDTO(
                1, 1,
                0.0, 24750.0,
                1125.0, 1125.0,
                start, null
        );

        PhilHealthRateDTO philHealth = new PhilHealthRateDTO(
                1, 2,
                0.03,
                10000.0, 60000.0,
                0.5, 0.5,
                start, null
        );

        PagIbigRateDTO tierLow = new PagIbigRateDTO(
                1, 3,
                1000.0, 1500.0,
                0.01, 0.02,
                100.0, null,
                start, null
        );

        PagIbigRateDTO tierHigh = new PagIbigRateDTO(
                2, 3,
                1500.01, Double.MAX_VALUE,
                0.02, 0.02,
                100.0, null,
                start, null
        );

        WithholdingTaxBracketDTO b1 = new WithholdingTaxBracketDTO(1, 4, 0.0,     20833.0,  0.0,     0.00, 0.0,     start, null);
        WithholdingTaxBracketDTO b2 = new WithholdingTaxBracketDTO(2, 4, 20833.0, 33333.0,  0.0,     0.15, 20833.0, start, null);
        WithholdingTaxBracketDTO b3 = new WithholdingTaxBracketDTO(3, 4, 33333.0, 66667.0,  1875.0,  0.20, 33333.0, start, null);
        WithholdingTaxBracketDTO b4 = new WithholdingTaxBracketDTO(4, 4, 66667.0, 166667.0, 8541.67, 0.25, 66667.0, start, null);
        WithholdingTaxBracketDTO b5 = new WithholdingTaxBracketDTO(5, 4, 166667.0,666667.0, 33541.67,0.30, 166667.0,start, null);
        WithholdingTaxBracketDTO b6 = new WithholdingTaxBracketDTO(6, 4, 666667.0,0.0,      183541.67,0.35,666667.0,start, null);

        return new GovernmentRatesDTO(sss, philHealth, List.of(tierLow, tierHigh), List.of(b1, b2, b3, b4, b5, b6));
    }
}
