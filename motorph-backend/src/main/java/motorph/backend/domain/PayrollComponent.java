package motorph.backend.domain;

/**
 * Represents a base payroll component with shared payroll result and amount details.
 */
public abstract class PayrollComponent {

    private final Integer payrollComponentId;
    private final int payrollResultId;

    private double amount;

    protected PayrollComponent(
            Integer payrollComponentId,
            int payrollResultId,
            double amount) {

        this.payrollComponentId = validateNullableId(payrollComponentId, "Payroll component ID must be positive.");
        this.payrollResultId = validatePositive(payrollResultId, "Payroll result ID must be positive.");
        this.amount = validateNonNegative(amount, "Amount cannot be negative.");
    }

    public Integer getPayrollComponentId() {
        return payrollComponentId;
    }

    public int getPayrollResultId() {
        return payrollResultId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = validateNonNegative(amount, "Amount cannot be negative.");
    }

    public boolean hasAmount() {
        return amount > 0;
    }

    /**
     * Returns the component category: {@code EARNING}, {@code BENEFIT}, or {@code DEDUCTION}.
     */
    public abstract String getComponentType();

    protected int validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    protected Integer validateNullableId(Integer value, String message) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    protected double validateNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
