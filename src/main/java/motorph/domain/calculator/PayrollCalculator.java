package motorph.domain.calculator;

import motorph.application.dto.payroll.PayrollInputDTO;
import motorph.domain.model.Attendance;
import motorph.domain.model.PayrollBreakdown;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes payroll breakdown from payroll input data and attendance logs.
 */
public class PayrollCalculator {

    // Computes payroll breakdown for the given cutoff date.
    public PayrollBreakdown computeDeductions(
            PayrollInputDTO input,
            List<Attendance> logs,
            LocalDate cutoffDate) {

        if (input == null) {
            throw new IllegalArgumentException("Payroll input is required.");
        }

        if (logs == null) {
            throw new IllegalArgumentException("Logs are required.");
        }

        if (cutoffDate == null) {
            throw new IllegalArgumentException("Cutoff date is required.");
        }

        requireNonNegative(input.getBasicSalary(), "Basic salary cannot be negative.");
        requireNonNegative(input.getRiceSubsidy(), "Rice subsidy cannot be negative.");
        requireNonNegative(input.getPhoneAllowance(), "Phone allowance cannot be negative.");
        requireNonNegative(input.getClothingAllowance(), "Clothing allowance cannot be negative.");
        requireNonNegative(input.getHourlyRate(), "Hourly rate cannot be negative.");

        YearMonth yearMonth = YearMonth.from(cutoffDate);
        int lastDayOfMonth = yearMonth.lengthOfMonth();
        int payPeriod = (cutoffDate.getDayOfMonth() <= 15) ? 1 : 2;

        double semiMonthlyBasic = input.getBasicSalary() / 2.0;
        double rice = input.getRiceSubsidy() / 2.0;
        double phone = input.getPhoneAllowance() / 2.0;
        double clothing = input.getClothingAllowance() / 2.0;
        double totalEarnings = semiMonthlyBasic + rice + phone + clothing;

        List<Attendance> filteredLogs = filterLogsByDateRange(
                logs,
                cutoffDate,
                (payPeriod == 1) ? 1 : 16,
                (payPeriod == 1) ? 15 : lastDayOfMonth
        );

        AttendanceDeduction attendanceDeduction = calculateAttendanceDeductions(input, filteredLogs);

        double lateDeduction = attendanceDeduction.getLateDeduction();
        double undertimeDeduction = attendanceDeduction.getUndertimeDeduction();

        double sss = 0.0;
        double philhealth = 0.0;
        double pagibig = 0.0;
        double tax = 0.0;

        // Applies monthly government deductions on the second cutoff.
        if (payPeriod == 2) {
            double monthlyBasic = input.getBasicSalary();

            sss = calculateSSS(monthlyBasic);
            philhealth = calculatePhilHealth(monthlyBasic);
            pagibig = calculatePagIbig(monthlyBasic);

            double mandatory = sss + philhealth + pagibig;
            double taxable = Math.max(
                    0.0,
                    monthlyBasic - mandatory - lateDeduction - undertimeDeduction
            );

            tax = computeMonthlyTax(taxable, 0.0);
        }

        double totalDeductions = lateDeduction + undertimeDeduction + sss + philhealth + pagibig + tax;
        double netPay = totalEarnings - totalDeductions;

        return new PayrollBreakdown(
                semiMonthlyBasic,
                rice,
                phone,
                clothing,
                totalEarnings,
                lateDeduction,
                undertimeDeduction,
                sss,
                philhealth,
                pagibig,
                tax,
                totalDeductions,
                netPay
        );
    }

    /**
     * Holds the computed attendance deductions.
     */
    private static class AttendanceDeduction {

        private final double lateDeduction;
        private final double undertimeDeduction;

        // constructors
        AttendanceDeduction(double lateDeduction, double undertimeDeduction) {
            this.lateDeduction = lateDeduction;
            this.undertimeDeduction = undertimeDeduction;
        }

        // getters
        double getLateDeduction() {
            return lateDeduction;
        }

        // getters
        double getUndertimeDeduction() {
            return undertimeDeduction;
        }
    }

    // Filters attendance logs within the current cutoff range.
    private static List<Attendance> filterLogsByDateRange(
            List<Attendance> logs,
            LocalDate cutoffDate,
            int startDay,
            int endDay) {

        List<Attendance> filtered = new ArrayList<>();

        for (Attendance log : logs) {
            LocalDate logDate = log.getWorkDate();

            if (logDate != null
                    && logDate.getMonth() == cutoffDate.getMonth()
                    && logDate.getYear() == cutoffDate.getYear()
                    && logDate.getDayOfMonth() >= startDay
                    && logDate.getDayOfMonth() <= endDay) {
                filtered.add(log);
            }
        }

        return filtered;
    }

    // Calculates late and undertime deductions from attendance logs.
    private static AttendanceDeduction calculateAttendanceDeductions(
            PayrollInputDTO input,
            List<Attendance> logs) {

        LocalTime scheduledIn = LocalTime.of(10, 0);
        LocalTime scheduledOut = LocalTime.of(19, 0);

        double hourlyRate = input.getHourlyRate();
        double lateTotal = 0.0;
        double undertimeTotal = 0.0;

        for (Attendance log : logs) {
            LocalTime actualIn = log.getTimeIn();
            LocalTime actualOut = log.getTimeOut();

            if (actualIn == null || actualOut == null) {
                continue;
            }

            long minutesLate = Math.max(0, Duration.between(scheduledIn, actualIn).toMinutes());
            long minutesUndertime = Math.max(0, Duration.between(actualOut, scheduledOut).toMinutes());

            lateTotal += (minutesLate / 60.0) * hourlyRate;
            undertimeTotal += (minutesUndertime / 60.0) * hourlyRate;
        }

        return new AttendanceDeduction(lateTotal, undertimeTotal);
    }

    // Calculates the employee share of SSS.
    private static double calculateSSS(double monthlyBasic) {
        if (monthlyBasic > 24750.0) {
            return 1125.00;
        }

        return monthlyBasic * 0.045;
    }

    // Calculates the employee share of PhilHealth.
    private static double calculatePhilHealth(double monthlyBasic) {
        double salary = Math.max(10000.0, Math.min(monthlyBasic, 60000.0));
        return (salary * 0.03) / 2.0;
    }

    // Calculates the employee share of Pag-IBIG.
    private static double calculatePagIbig(double monthlySalary) {
        if (monthlySalary < 1000) {
            return 0.0;
        }

        double rate = (monthlySalary <= 1500) ? 0.01 : 0.02;
        return Math.min(monthlySalary * rate, 100.0);
    }

    // Validates that a numeric value is not negative.
    private static double requireNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    // Computes withholding tax from monthly taxable income.
    public static double computeMonthlyTax(double monthlyGross, double monthlyDeductions) {
        double taxable = Math.max(0.0, monthlyGross - monthlyDeductions);

        if (taxable <= 20833) {
            return 0.0;
        }

        if (taxable <= 33333) {
            return (taxable - 20833) * 0.15;
        }

        if (taxable <= 66667) {
            return 1875 + (taxable - 33333) * 0.20;
        }

        if (taxable <= 166667) {
            return 8541.67 + (taxable - 66667) * 0.25;
        }

        if (taxable <= 666667) {
            return 33541.67 + (taxable - 166667) * 0.30;
        }

        return 183541.67 + (taxable - 666667) * 0.35;
    }
}