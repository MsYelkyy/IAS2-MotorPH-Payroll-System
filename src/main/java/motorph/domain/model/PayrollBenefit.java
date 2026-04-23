package motorph.domain.model;

/**
 * Represents a benefit applied to a payroll result and maps to the payroll_benefit table in the database.
 */
public class PayrollBenefit {

    private Integer payrollBenefitId;
    private int payrollResultId;
    private int benefitTypeId;
    private double amount;

    // constructors
    public PayrollBenefit(
            Integer payrollBenefitId,
            int payrollResultId,
            int benefitTypeId,
            double amount) {

        setPayrollBenefitId(payrollBenefitId);
        setPayrollResultId(payrollResultId);
        setBenefitTypeId(benefitTypeId);
        setAmount(amount);
    }

    // getters
    public Integer getPayrollBenefitId() {
        return payrollBenefitId;
    }

    public int getPayrollResultId() {
        return payrollResultId;
    }

    public int getBenefitTypeId() {
        return benefitTypeId;
    }

    public double getAmount() {
        return amount;
    }

    // setters
    public void setPayrollBenefitId(Integer payrollBenefitId) {
        if (payrollBenefitId != null && payrollBenefitId <= 0) {
            throw new IllegalArgumentException("Payroll benefit ID must be positive.");
        }
        this.payrollBenefitId = payrollBenefitId;
    }

    public void setPayrollResultId(int payrollResultId) {
        if (payrollResultId <= 0) {
            throw new IllegalArgumentException("Payroll result ID must be positive.");
        }
        this.payrollResultId = payrollResultId;
    }

    public void setBenefitTypeId(int benefitTypeId) {
        if (benefitTypeId <= 0) {
            throw new IllegalArgumentException("Benefit type ID must be positive.");
        }
        this.benefitTypeId = benefitTypeId;
    }

    public void setAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Benefit amount cannot be negative.");
        }
        this.amount = amount;
    }

    // returns the payroll benefit summary
    @Override
    public String toString() {
        return "PayrollBenefit{"
                + "payrollBenefitId=" + payrollBenefitId
                + ", payrollResultId=" + payrollResultId
                + ", benefitTypeId=" + benefitTypeId
                + ", amount=" + amount
                + '}';
    }
}