package motorph.backend.domain;

/**
 * Represents an earning applied to a payroll result and maps to the payroll_earning table in the database.
 */
public class PayrollEarning extends PayrollComponent {

    private int earningTypeId;

    public PayrollEarning(
            Integer payrollEarningId,
            int payrollResultId,
            int earningTypeId,
            double amount) {

        super(payrollEarningId, payrollResultId, amount);
        setEarningTypeId(earningTypeId);
    }

    public Integer getPayrollEarningId() {
        return getPayrollComponentId();
    }

    public int getEarningTypeId() {
        return earningTypeId;
    }

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
}
