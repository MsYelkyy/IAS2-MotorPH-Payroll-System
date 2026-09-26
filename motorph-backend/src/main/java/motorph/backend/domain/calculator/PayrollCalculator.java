package motorph.backend.domain.calculator;

import motorph.backend.dto.payroll.GovernmentRatesDTO;
import motorph.backend.dto.payroll.PayrollInputDTO;
import motorph.backend.dto.systemconfig.PagIbigRateDTO;
import motorph.backend.dto.systemconfig.WithholdingTaxBracketDTO;
import motorph.backend.domain.Attendance;
import motorph.backend.domain.PayrollBreakdown;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes the payroll breakdown from an employee's payroll data, attendance logs, and the
 * government rates in effect for the cutoff. This is the single source of truth for payroll
 * figures.
 */
@Component
public class PayrollCalculator {

    private static final LocalTime SCHEDULED_IN = LocalTime.of(8, 0);
    private static final LocalTime SCHEDULED_OUT = LocalTime.of(17, 0);
    private static final double UNPAID_LUNCH_HOURS = 1.0;
    private static final double MAX_REGULAR_HOURS_PER_DAY = 8.0;

    /**
     * Computes the full payroll breakdown for one employee using the government rates effective on
     * the cutoff date.
     */
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

            tax = computeMonthlyTax(taxable, rates.getTaxBrackets());
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
     * Returns the paid regular hours for one attendance record, excluding the one-hour unpaid lunch
     * and capped at eight hours.
     */
    public static double calculatePaidRegularHours(Attendance attendance) {
        if (attendance == null) {
            return 0.0;
        }
        return calculatePaidRegularHours(attendance.getTimeIn(), attendance.getTimeOut());
    }

    public static double calculatePaidRegularHours(LocalTime timeIn, LocalTime timeOut) {
        if (timeIn == null || timeOut == null) {
            return 0.0;
        }

        double actualHours = Duration.between(timeIn, timeOut).toMinutes() / 60.0;
        double paidHours = actualHours - UNPAID_LUNCH_HOURS;

        return Math.max(0.0, Math.min(paidHours, MAX_REGULAR_HOURS_PER_DAY));
    }

    /**
     * Computes withholding tax from monthly taxable income using the effective tax brackets.
     */
    public static double computeMonthlyTax(
            double taxableIncome,
            List<WithholdingTaxBracketDTO> brackets) {

        double taxable = Math.max(0.0, taxableIncome);

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

    /**
     * Calculates the employee's SSS share. Salaries above the table's maximum use the fixed capped
     * contribution; otherwise the contribution is prorated.
     */
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

    private static double calculatePhilHealth(double monthlyBasic, GovernmentRatesDTO rates) {
        var rate = rates.getPhilHealthRate();
        if (rate == null) {
            return 0.0;
        }

        double salary = Math.max(rate.getSalaryFloor(), Math.min(monthlyBasic, rate.getSalaryCeiling()));
        return (salary * rate.getPremiumRate()) * rate.getEmployeeShareRatio();
    }

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

    private static double requireNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static class AttendanceDeduction {

        private final double lateDeduction;
        private final double undertimeDeduction;

        AttendanceDeduction(double lateDeduction, double undertimeDeduction) {
            this.lateDeduction = lateDeduction;
            this.undertimeDeduction = undertimeDeduction;
        }

        double getLateDeduction() {
            return lateDeduction;
        }

        double getUndertimeDeduction() {
            return undertimeDeduction;
        }
    }
}
