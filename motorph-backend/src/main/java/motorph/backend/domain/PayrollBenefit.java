package motorph.backend.domain;

/**
 * Represents a benefit applied to a payroll result and maps to the payroll_benefit table in the database.
 */
public class PayrollBenefit extends PayrollComponent {

    private int benefitTypeId;

    public PayrollBenefit(
            Integer payrollBenefitId,
            int payrollResultId,
            int benefitTypeId,
            double amount) {

        super(payrollBenefitId, payrollResultId, amount);
        setBenefitTypeId(benefitTypeId);
    }

    public Integer getPayrollBenefitId() {
        return getPayrollComponentId();
    }

    public int getBenefitTypeId() {
        return benefitTypeId;
    }

    public void setBenefitTypeId(int benefitTypeId) {
        if (benefitTypeId <= 0) {
            throw new IllegalArgumentException("Benefit type ID must be positive.");
        }
        this.benefitTypeId = benefitTypeId;
    }

    @Override
    public String getComponentType() {
        return "BENEFIT";
    }
}
