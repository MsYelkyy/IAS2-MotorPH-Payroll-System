package motorph.domain.model;

import java.time.LocalDateTime;

/**
 * Represents a generated payroll result and maps to the payroll_result table.
 */
public class PayrollResult {

    private Integer payrollResultId;
    private int employeeId;
    private int payrunId;
    private double totalHoursWorked;
    private double grossPay;
    private double totalDeductions;
    private double netPay;
    private LocalDateTime generatedAt;

    // constructors
    public PayrollResult(
            Integer payrollResultId,
            int employeeId,
            int payrunId,
            double totalHoursWorked,
            double grossPay,
            double totalDeductions,
            double netPay,
            LocalDateTime generatedAt) {

        setPayrollResultId(payrollResultId);
        setEmployeeId(employeeId);
        setPayrunId(payrunId);
        setTotalHoursWorked(totalHoursWorked);
        setGrossPay(grossPay);
        setTotalDeductions(totalDeductions);
        setNetPay(netPay);
        setGeneratedAt(generatedAt);
        validateAmounts();
    }

    // getters
    public Integer getPayrollResultId() {
        return payrollResultId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public int getPayrunId() {
        return payrunId;
    }

    public double getTotalHoursWorked() {
        return totalHoursWorked;
    }

    public double getGrossPay() {
        return grossPay;
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNetPay() {
        return netPay;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    // checks if the payroll result has deductions
    public boolean hasDeductions() {
        return totalDeductions > 0;
    }

    // checks if the payroll result has worked hours
    public boolean hasWorkedHours() {
        return totalHoursWorked > 0;
    }

    // setters
    public void setPayrollResultId(Integer payrollResultId) {
        if (payrollResultId != null && payrollResultId <= 0) {
            throw new IllegalArgumentException("Payroll result ID must be positive.");
        }
        this.payrollResultId = payrollResultId;
    }

    public void setEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }
        this.employeeId = employeeId;
    }

    public void setPayrunId(int payrunId) {
        if (payrunId <= 0) {
            throw new IllegalArgumentException("Payrun ID must be positive.");
        }
        this.payrunId = payrunId;
    }

    public void setTotalHoursWorked(double totalHoursWorked) {
        if (totalHoursWorked < 0) {
            throw new IllegalArgumentException("Total hours worked cannot be negative.");
        }
        this.totalHoursWorked = totalHoursWorked;
    }

    public void setGrossPay(double grossPay) {
        if (grossPay < 0) {
            throw new IllegalArgumentException("Gross pay cannot be negative.");
        }
        this.grossPay = grossPay;
    }

    public void setTotalDeductions(double totalDeductions) {
        if (totalDeductions < 0) {
            throw new IllegalArgumentException("Total deductions cannot be negative.");
        }
        this.totalDeductions = totalDeductions;
    }

    public void setNetPay(double netPay) {
        this.netPay = netPay;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        if (generatedAt == null) {
            throw new IllegalArgumentException("Generated at is required.");
        }
        this.generatedAt = generatedAt;
    }

    // validates the payroll amounts
    private void validateAmounts() {
        double expectedNetPay = grossPay - totalDeductions;
        double tolerance = 0.01;

        if (Math.abs(netPay - expectedNetPay) > tolerance) {
            throw new IllegalArgumentException("Net pay does not match gross pay minus total deductions.");
        }
    }

    // returns the payroll result summary
    @Override
    public String toString() {
        return "PayrollResult{"
                + "payrollResultId=" + payrollResultId
                + ", employeeId=" + employeeId
                + ", payrunId=" + payrunId
                + ", totalHoursWorked=" + totalHoursWorked
                + ", grossPay=" + grossPay
                + ", totalDeductions=" + totalDeductions
                + ", netPay=" + netPay
                + ", generatedAt=" + generatedAt
                + '}';
    }
}