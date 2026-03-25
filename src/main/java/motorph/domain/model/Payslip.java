package motorph.domain.model;

import java.time.LocalDateTime;

/**
 * Domain model representing a payslip record.
 */
public class Payslip {

    private final int payslipId;
    private final int payRunId;
    private final int employeeId;
    private final double basicPay;
    private final double overtimePay;
    private final double allowancesTotal;
    private final double deductionsTotal;
    private final double grossPay;
    private final double netPay;
    private final LocalDateTime generatedAt;

    // constructor
    public Payslip(int payslipId,
                   int payRunId,
                   int employeeId,
                   double basicPay,
                   double overtimePay,
                   double allowancesTotal,
                   double deductionsTotal,
                   double grossPay,
                   double netPay,
                   LocalDateTime generatedAt) {

        this.payslipId = payslipId;
        this.payRunId = payRunId;
        this.employeeId = employeeId;
        this.basicPay = basicPay;
        this.overtimePay = overtimePay;
        this.allowancesTotal = allowancesTotal;
        this.deductionsTotal = deductionsTotal;
        this.grossPay = grossPay;
        this.netPay = netPay;
        this.generatedAt = generatedAt;
    }

    // GETTERS
    public int getPayslipId() {
        return payslipId;
    }

    public int getPayRunId() {
        return payRunId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public double getBasicPay() {
        return basicPay;
    }

    public double getOvertimePay() {
        return overtimePay;
    }

    public double getAllowancesTotal() {
        return allowancesTotal;
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

    @Override
    public String toString() {
        return "Payslip{" +
                "employeeId=" + employeeId +
                ", grossPay=" + grossPay +
                ", netPay=" + netPay +
                ", generatedAt=" + generatedAt +
                '}';
    }
}