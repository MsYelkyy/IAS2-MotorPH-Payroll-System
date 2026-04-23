package motorph.domain.model;

/**
 * Represents a deduction applied to a payroll result and maps to the payroll_deduction table in the database.
 */
public class PayrollDeduction {

    private Integer payrollDeductionId;
    private int payrollResultId;
    private int deductionTypeId;
    private double amount;
    private boolean taxable;

    // constructors
    public PayrollDeduction(
            Integer payrollDeductionId,
            int payrollResultId,
            int deductionTypeId,
            double amount,
            boolean taxable) {

        setPayrollDeductionId(payrollDeductionId);
        setPayrollResultId(payrollResultId);
        setDeductionTypeId(deductionTypeId);
        setAmount(amount);
        setTaxable(taxable);
    }

    // getters
    public Integer getPayrollDeductionId() {
        return payrollDeductionId;
    }

    public int getPayrollResultId() {
        return payrollResultId;
    }

    public int getDeductionTypeId() {
        return deductionTypeId;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isTaxable() {
        return taxable;
    }

    // setters
    public void setPayrollDeductionId(Integer payrollDeductionId) {
        if (payrollDeductionId != null && payrollDeductionId <= 0) {
            throw new IllegalArgumentException("Payroll deduction ID must be positive.");
        }
        this.payrollDeductionId = payrollDeductionId;
    }

    public void setPayrollResultId(int payrollResultId) {
        if (payrollResultId <= 0) {
            throw new IllegalArgumentException("Payroll result ID must be positive.");
        }
        this.payrollResultId = payrollResultId;
    }

    public void setDeductionTypeId(int deductionTypeId) {
        if (deductionTypeId <= 0) {
            throw new IllegalArgumentException("Deduction type ID must be positive.");
        }
        this.deductionTypeId = deductionTypeId;
    }

    public void setAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Deduction amount cannot be negative.");
        }
        this.amount = amount;
    }

    public void setTaxable(boolean taxable) {
        this.taxable = taxable;
    }

    // returns the payroll deduction summary
    @Override
    public String toString() {
        return "PayrollDeduction{"
                + "payrollDeductionId=" + payrollDeductionId
                + ", payrollResultId=" + payrollResultId
                + ", deductionTypeId=" + deductionTypeId
                + ", amount=" + amount
                + ", taxable=" + taxable
                + '}';
    }
}