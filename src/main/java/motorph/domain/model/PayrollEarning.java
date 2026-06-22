package motorph.domain.model;

/**
 * Represents an earning applied to a payroll result and maps to the payroll_earning table in the database.
 */
public class PayrollEarning extends PayrollComponent {

    private int earningTypeId;

    // constructors
    public PayrollEarning(
            Integer payrollEarningId,
            int payrollResultId,
            int earningTypeId,
            double amount) {

        super(payrollEarningId, payrollResultId, amount);
        setEarningTypeId(earningTypeId);
    }

    // getters
    public Integer getPayrollEarningId() {
        return getPayrollComponentId();
    }

    public int getEarningTypeId() {
        return earningTypeId;
    }

    // setters
    public void setEarningTypeId(int earningTypeId) {
        if (earningTypeId <= 0) {
            throw new IllegalArgumentException("Earning type ID must be positive.");
        }
        this.earningTypeId = earningTypeId;
    }

    @Override
    public String getComponentType() {
        return "EARNING";
    }

    // returns the payroll earning summary
    @Override
    public String toString() {
        return "PayrollEarning{"
                + "payrollEarningId=" + getPayrollEarningId()
                + ", payrollResultId=" + getPayrollResultId()
                + ", earningTypeId=" + earningTypeId
                + ", amount=" + getAmount()
                + '}';
    }
}