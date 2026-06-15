package motorph.domain.model;

/**
 * Represents a benefit applied to a payroll result and maps to the payroll_benefit table in the database.
 */
public class PayrollBenefit extends PayrollComponent {

    private int benefitTypeId;

    // constructors
    public PayrollBenefit(
            Integer payrollBenefitId,
            int payrollResultId,
            int benefitTypeId,
            double amount) {

        super(payrollBenefitId, payrollResultId, amount);
        setBenefitTypeId(benefitTypeId);
    }

    // getters
    public Integer getPayrollBenefitId() {
        return getPayrollComponentId();
    }

    public int getBenefitTypeId() {
        return benefitTypeId;
    }

    // setters
    public void setBenefitTypeId(int benefitTypeId) {
        if (benefitTypeId <= 0) {
            throw new IllegalArgumentException("Benefit type ID must be positive.");
        }
        this.benefitTypeId = benefitTypeId;
    }

    // returns the payroll benefit summary
    @Override
    public String toString() {
        return "PayrollBenefit{"
                + "payrollBenefitId=" + getPayrollBenefitId()
                + ", payrollResultId=" + getPayrollResultId()
                + ", benefitTypeId=" + benefitTypeId
                + ", amount=" + getAmount()
                + '}';
    }
}