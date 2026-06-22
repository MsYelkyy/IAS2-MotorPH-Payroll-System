package motorph.domain.calculator;

import motorph.application.dto.payroll.GovernmentRatesDTO;
import motorph.application.dto.payroll.PayrollInputDTO;
import motorph.application.dto.systemconfig.PagIbigRateDTO;
import motorph.application.dto.systemconfig.WithholdingTaxBracketDTO;
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

    private static final LocalTime SCHEDULED_IN = LocalTime.of(8, 0);
    private static final LocalTime SCHEDULED_OUT = LocalTime.of(17, 0);
    private static final double UNPAID_LUNCH_HOURS = 1.0;
    private static final double MAX_REGULAR_HOURS_PER_DAY = 8.0;

    // computes payroll breakdown for the given cutoff date using effective government rates
    public PayrollBreakdown computeDeductions(
            PayrollInputDTO input,
            List<Attendance> logs,
            double approvedOvertimeHours,
            LocalDate cutoffDate,
            GovernmentRatesDTO rates) {

        if (input == null) {
            throw new IllegalArgumentException("Payroll input is required.");
        }

        if (logs == null) {
            throw new IllegalArgumentException("Logs are required.");
        }

        if (cutoffDate == null) {
            throw new IllegalArgumentException("Cutoff date is required.");
        }

        if (rates == null) {
            throw new IllegalArgumentException("Government rates are required.");
        }

        requireNonNegative(input.getBasicSalary(), "Basic salary cannot be negative.");
        requireNonNegative(input.getRiceSubsidy(), "Rice subsidy cannot be negative.");
        requireNonNegative(input.getPhoneAllowance(), "Phone allowance cannot be negative.");
        requireNonNegative(input.getClothingAllowance(), "Clothing allowance cannot be negative.");
        requireNonNegative(input.getHourlyRate(), "Hourly rate cannot be negative.");
        requireNonNegative(approvedOvertimeHours, "Approved overtime hours cannot be negative.");

        YearMonth yearMonth = YearMonth.from(cutoffDate);
        int lastDayOfMonth = yearMonth.lengthOfMonth();
        int payPeriod = cutoffDate.getDayOfMonth() <= 15 ? 1 : 2;

        double semiMonthlyBasic = input.getBasicSalary() / 2.0;
        double rice = input.getRiceSubsidy() / 2.0;
        double phone = input.getPhoneAllowance() / 2.0;
        double clothing = input.getClothingAllowance() / 2.0;
        double overtimePay = approvedOvertimeHours * input.getHourlyRate();

        double totalEarnings = semiMonthlyBasic + rice + phone + clothing + overtimePay;

        List<Attendance> filteredLogs = filterLogsByDateRange(
                logs,
                cutoffDate,
                payPeriod == 1 ? 1 : 16,
                payPeriod == 1 ? 15 : lastDayOfMonth
        );

        AttendanceDeduction attendanceDeduction = calculateAttendanceDeductions(input, filteredLogs);

        double lateDeduction = attendanceDeduction.getLateDeduction();
        double undertimeDeduction = attendanceDeduction.getUndertimeDeduction();

        double sss = 0.0;
        double philhealth = 0.0;
        double pagibig = 0.0;
        double tax = 0.0;

        if (payPeriod == 2) {
            double monthlyBasic = input.getBasicSalary();

            sss = calculateSSS(monthlyBasic, rates);
            philhealth = calculatePhilHealth(monthlyBasic, rates);
            pagibig = calculatePagIbig(monthlyBasic, rates);

            double mandatory = sss + philhealth + pagibig;
            double taxable = Math.max(
                    0.0,
                    monthlyBasic + overtimePay - mandatory - lateDeduction - undertimeDeduction
            );

            tax = computeMonthlyTax(taxable, 0.0, rates.getTaxBrackets());
        }

        double totalDeductions = lateDeduction + undertimeDeduction + sss + philhealth + pagibig + tax;
        double netPay = totalEarnings - totalDeductions;

        return new PayrollBreakdown(
                semiMonthlyBasic,
                rice,
                phone,
                clothing,
                overtimePay,
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

    // filters attendance logs within the current cutoff range
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

    // calculates late and undertime deductions from attendance logs
    private static AttendanceDeduction calculateAttendanceDeductions(
            PayrollInputDTO input,
            List<Attendance> logs) {

        double hourlyRate = input.getHourlyRate();
        double lateTotal = 0.0;
        double undertimeTotal = 0.0;

        for (Attendance log : logs) {
            LocalTime actualIn = log.getTimeIn();
            LocalTime actualOut = log.getTimeOut();

            if (actualIn == null || actualOut == null) {
                continue;
            }

            long minutesLate = Math.max(0, Duration.between(SCHEDULED_IN, actualIn).toMinutes());
            long minutesUndertime = Math.max(0, Duration.between(actualOut, SCHEDULED_OUT).toMinutes());

            lateTotal += (minutesLate / 60.0) * hourlyRate;
            undertimeTotal += (minutesUndertime / 60.0) * hourlyRate;
        }

        return new AttendanceDeduction(lateTotal, undertimeTotal);
    }

    // calculates paid regular hours with unpaid lunch deducted
    public static double calculatePaidRegularHours(Attendance attendance) {
        if (attendance == null || attendance.getTimeIn() == null || attendance.getTimeOut() == null) {
            return 0.0;
        }

        double actualHours = Duration.between(
                attendance.getTimeIn(),
                attendance.getTimeOut()
        ).toMinutes() / 60.0;

        double paidHours = actualHours - UNPAID_LUNCH_HOURS;

        return Math.max(0.0, Math.min(paidHours, MAX_REGULAR_HOURS_PER_DAY));
    }

    // calculates the employee SSS share; uses fixed cap contribution when salary exceeds max_salary, otherwise prorates
    private static double calculateSSS(double monthlyBasic, GovernmentRatesDTO rates) {
        var rate = rates.getSssRate();
        if (rate == null) {
            return 0.0;
        }

        double maxContribution = rate.getEmployeeShare();

        if (monthlyBasic >= rate.getMaxSalary()) {
            return maxContribution;
        }

        double prorated = (rate.getMaxSalary() > 0)
                ? monthlyBasic * (maxContribution / rate.getMaxSalary())
                : 0.0;

        return Math.min(prorated, maxContribution);
    }

    // calculates the employee PhilHealth share using the effective rate
    private static double calculatePhilHealth(double monthlyBasic, GovernmentRatesDTO rates) {
        var rate = rates.getPhilHealthRate();
        if (rate == null) {
            return 0.0;
        }

        double salary = Math.max(rate.getSalaryFloor(), Math.min(monthlyBasic, rate.getSalaryCeiling()));
        return (salary * rate.getPremiumRate()) * rate.getEmployeeShareRatio();
    }

    // calculates the employee Pag-IBIG share using the effective salary tiers
    private static double calculatePagIbig(double monthlySalary, GovernmentRatesDTO rates) {
        List<PagIbigRateDTO> tiers = rates.getPagIbigRates();
        if (tiers == null || tiers.isEmpty()) {
            return 0.0;
        }

        for (PagIbigRateDTO tier : tiers) {
            if (monthlySalary >= tier.getMinSalary() && monthlySalary <= tier.getMaxSalary()) {
                double share = monthlySalary * tier.getEmployeeShareRate();
                Double cap = tier.getMaxEmployeeShare();
                return cap != null ? Math.min(share, cap) : share;
            }
        }

        return 0.0;
    }

    // validates that a numeric value is not negative
    private static double requireNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    // computes withholding tax from monthly taxable income using the effective tax brackets
    public static double computeMonthlyTax(
            double monthlyGross,
            double monthlyDeductions,
            List<WithholdingTaxBracketDTO> brackets) {

        double taxable = Math.max(0.0, monthlyGross - monthlyDeductions);

        if (brackets == null || brackets.isEmpty()) {
            return 0.0;
        }

        for (WithholdingTaxBracketDTO bracket : brackets) {
            boolean withinMin = taxable >= bracket.getMinSalary();
            boolean withinMax = bracket.getMaxSalary() <= 0 || taxable <= bracket.getMaxSalary();

            if (withinMin && withinMax) {
                return bracket.getBaseTax() + (taxable - bracket.getExcessOver()) * bracket.getTaxRate();
            }
        }

        return 0.0;
    }
}
