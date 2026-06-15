package motorph.ui.util;

import motorph.domain.model.PayrollResult;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Provides formatting helpers for currency, payslip text, and null-safe values. */
public final class UiFormatter {

    private static final NumberFormat PESO_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("en", "PH"));
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // utility class — no instances
    private UiFormatter() {
    }

    // formats a numeric value as Philippine peso currency
    public static String formatPeso(double amount) {
        return PESO_FORMAT.format(amount);
    }

    // returns the value, or the fallback string when the value is null or blank
    public static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // builds a plain-text payslip from a payroll result
    public static String buildPayslipText(PayrollResult result, String employeeName, String email) {
        String generated = result.getGeneratedAt().format(TIMESTAMP_FORMAT);
        return """
                MOTORPH PAYROLL MANAGEMENT SYSTEM
                Employee Payslip
                ==========================================

                Employee: %s
                Email: %s
                Employee ID: %d
                Payrun ID: %d
                Generated: %s

                ------------------------------------------
                Total Hours Worked: %.2f
                Gross Pay: %s
                Total Deductions: %s
                Net Pay: %s
                ------------------------------------------

                This payslip is generated from persisted MotorPH payroll results.
                For official release, connect the JasperReports template when the
                JasperReports dependency and JRXML design are added to the project.
                """.formatted(
                safe(employeeName, "Employee"),
                safe(email, "employee@motorph.com"),
                result.getEmployeeId(),
                result.getPayrunId(),
                generated,
                result.getTotalHoursWorked(),
                PESO_FORMAT.format(result.getGrossPay()),
                PESO_FORMAT.format(result.getTotalDeductions()),
                PESO_FORMAT.format(result.getNetPay())
        );
    }
}
