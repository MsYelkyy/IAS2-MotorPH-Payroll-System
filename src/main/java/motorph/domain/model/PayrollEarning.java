package motorph.domain.model;

/**
 * Represents an earning applied to a payroll result and maps to the payroll_earning table in the database.
 */
public class PayrollEarning {

    private Integer payrollEarningId;
    private int payrollResultId;
    private int earningTypeId;
    private double amount;

    // constructors
    public PayrollEarning(
            Integer payrollEarningId,
            int payrollResultId,
            int earningTypeId,
            double amount) {

        setPayrollEarningId(payrollEarningId);
        setPayrollResultId(payrollResultId);
        setEarningTypeId(earningTypeId);
        setAmount(amount);
    }

    // getters
    public Integer getPayrollEarningId() {
        return payrollEarningId;
    }

    public int getPayrollResultId() {
        return payrollResultId;
    }

    public int getEarningTypeId() {
        return earningTypeId;
    }

    public double getAmount() {
        return amount;
    }

    // setters
    public void setPayrollEarningId(Integer payrollEarningId) {
        if (payrollEarningId != null && payrollEarningId <= 0) {
            throw new IllegalArgumentException("Payroll earning ID must be positive.");
        }
        this.payrollEarningId = payrollEarningId;
    }

    public void setPayrollResultId(int payrollResultId) {
        if (payrollResultId <= 0) {
            throw new IllegalArgumentException("Payroll result ID must be positive.");
        }
        this.payrollResultId = payrollResultId;
    }

    public void setEarningTypeId(int earningTypeId) {
        if (earningTypeId <= 0) {
            throw new IllegalArgumentException("Earning type ID must be positive.");
        }
        this.earningTypeId = earningTypeId;
    }

    public void setAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Earning amount cannot be negative.");
        }
        this.amount = amount;
    }

    // returns the payroll earning summary
    @Override
    public String toString() {
        return "PayrollEarning{"
                + "payrollEarningId=" + payrollEarningId
                + ", payrollResultId=" + payrollResultId
                + ", earningTypeId=" + earningTypeId
                + ", amount=" + amount
                + '}';
    }
}