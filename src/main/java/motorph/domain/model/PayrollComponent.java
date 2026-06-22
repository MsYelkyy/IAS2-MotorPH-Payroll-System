package motorph.domain.model;

/**
 * Represents a base payroll component with shared payroll result and amount details.
 */
public abstract class PayrollComponent {

    private final Integer payrollComponentId;
    private final int payrollResultId;

    private double amount;

    // constructors
    protected PayrollComponent(
            Integer payrollComponentId,
            int payrollResultId,
            double amount) {

        this.payrollComponentId = validateNullableId(payrollComponentId, "Payroll component ID must be positive.");
        this.payrollResultId = validatePositive(payrollResultId, "Payroll result ID must be positive.");
        this.amount = validateNonNegative(amount, "Amount cannot be negative.");
    }

    // getters
    public Integer getPayrollComponentId() {
        return payrollComponentId;
    }

    public int getPayrollResultId() {
        return payrollResultId;
    }

    public double getAmount() {
        return amount;
    }

    // setters
    public void setAmount(double amount) {
        this.amount = validateNonNegative(amount, "Amount cannot be negative.");
    }

    // checks if the payroll component has an amount
    public boolean hasAmount() {
        return amount > 0;
    }

    // returns the type label for this payroll component
    public abstract String getComponentType();

    // validates a required positive number
    protected int validatePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // validates an optional positive number
    protected Integer validateNullableId(Integer value, String message) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // validates a non-negative amount
    protected double validateNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}