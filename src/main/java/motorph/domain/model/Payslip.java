package motorph.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import motorph.domain.value.PayrollType;

/**
 * Represents a generated payslip for printing and does not map directly to a database table.
 */
public class Payslip {

    private final int employeeId;
    private final String employeeName;
    private final int payrunId;
    private final PayrollType payrollType;
    private final LocalDate periodStartDate;
    private final LocalDate periodEndDate;
    private final LocalDate payDate;
    private final double basicPay;
    private final double overtimePay;
    private final double benefitsTotal;
    private final double deductionsTotal;
    private final double grossPay;
    private final double netPay;
    private final LocalDateTime generatedAt;

    // constructors
    public Payslip(
            int employeeId,
            String employeeName,
            int payrunId,
            PayrollType payrollType,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            LocalDate payDate,
            double basicPay,
            double overtimePay,
            double benefitsTotal,
            double deductionsTotal,
            double grossPay,
            double netPay,
            LocalDateTime generatedAt) {

        this.employeeId = validatePositive(employeeId, "Employee ID must be positive.");
        this.employeeName = requireNonBlank(employeeName, "Employee name is required.");
        this.payrunId = validatePositive(payrunId, "Payrun ID must be positive.");
        this.payrollType = requireNonNull(payrollType, "Payroll type is required.");
        this.periodStartDate = requireNonNull(periodStartDate, "Period start date is required.");
        this.periodEndDate = requireNonNull(periodEndDate, "Period end date is required.");
        this.payDate = requireNonNull(payDate, "Pay date is required.");
        this.basicPay = validateNonNegative(basicPay, "Basic pay cannot be negative.");
        this.overtimePay = validateNonNegative(overtimePay, "Overtime pay cannot be negative.");
        this.benefitsTotal = validateNonNegative(benefitsTotal, "Benefits total cannot be negative.");
        this.deductionsTotal = validateNonNegative(deductionsTotal, "Deductions total cannot be negative.");
        this.grossPay = validateNonNegative(grossPay, "Gross pay cannot be negative.");
        this.netPay = netPay;
        this.generatedAt = requireNonNull(generatedAt, "Generated at is required.");

        validateDates();
        validateAmounts();
    }

    // getters
    public int getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public int getPayrunId() {
        return payrunId;
    }

    public PayrollType getPayrollType() {
        return payrollType;
    }

    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }

    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public double getBasicPay() {
        return basicPay;
    }

    public double getOvertimePay() {
        return overtimePay;
    }

    public double getBenefitsTotal() {
        return benefitsTotal;
    }

    public double getDeductionsTotal() {
        return deductionsTotal;
    }

    public double getGrossPay() {
        return grossPay;
    }

    public double getNetPay() {
        return netPay;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    // returns the total earnings before deductions
    public double getTotalEarnings() {
        return basicPay + overtimePay + benefitsTotal;
    }

    // checks if the payslip has overtime pay
    public boolean hasOvertimePay() {
        return overtimePay > 0;
    }

    // checks if the payslip has benefits
    public boolean hasBenefits() {
        return benefitsTotal > 0;
    }

    // checks if the payslip has deductions
    public boolean hasDeductions() {
        return deductionsTotal > 0;
    }

    // validates the date range
    private void validateDates() {
        if (periodEndDate.isBefore(periodStartDate)) {
            throw new IllegalArgumentException("Period end date cannot be before period start date.");
        }
    }

    // validates the computed amounts
    private void validateAmounts() {
        double expectedGrossPay = basicPay + overtimePay + benefitsTotal;
        double expectedNetPay = grossPay - deductionsTotal;
        double tolerance = 0.01;

        if (Math.abs(grossPay - expectedGrossPay) > tolerance) {
            throw new IllegalArgumentException("Gross pay does not match the payslip breakdown.");
        }

        if (Math.abs(netPay - expectedNetPay) > tolerance) {
            throw new IllegalArgumentException("Net pay does not match gross pay minus deductions.");
        }
    }

    // validates a required positive number
    private int validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // validates a non-negative amount
    private double validateNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // validates required text
    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    // validates a required object
    private <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // returns the payslip summary
    @Override
    public String toString() {
        return "Payslip{"
                + "employeeId=" + employeeId
                + ", employeeName='" + employeeName + '\''
                + ", payrunId=" + payrunId
                + ", payrollType=" + payrollType
                + ", payDate=" + payDate
                + ", grossPay=" + grossPay
                + ", netPay=" + netPay
                + '}';
    }
}