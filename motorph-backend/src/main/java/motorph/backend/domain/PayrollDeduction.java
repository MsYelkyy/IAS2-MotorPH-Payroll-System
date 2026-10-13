package motorph.backend.domain;

/**
 * Represents a deduction applied to a payroll result and maps to the payroll_deduction table in the database.
 */
public class PayrollDeduction extends PayrollComponent {

    private int deductionTypeId;
    private boolean taxable;

    public PayrollDeduction(
            Integer payrollDeductionId,
            int payrollResultId,
            int deductionTypeId,
            double amount,
            boolean taxable) {

        super(payrollDeductionId, payrollResultId, amount);
        setDeductionTypeId(deductionTypeId);
        setTaxable(taxable);
    }

    public Integer getPayrollDeductionId() {
        return getPayrollComponentId();
    }

    public int getDeductionTypeId() {
        return deductionTypeId;
    }

    public boolean isTaxable() {
        return taxable;
    }

    public void setDeductionTypeId(int deductionTypeId) {
        if (deductionTypeId <= 0) {
            throw new IllegalArgumentException("Deduction type ID must be positive.");
        }
        this.deductionTypeId = deductionTypeId;
    }

    public void setTaxable(boolean taxable) {
        this.taxable = taxable;
    }

    @Override
    public String getComponentType() {
        return "DEDUCTION";
    }
}
