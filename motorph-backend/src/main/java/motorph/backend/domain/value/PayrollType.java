package motorph.backend.domain.value;

/**
 * Represents the payroll schedule type.
 */
public enum PayrollType {

    SEMI_MONTHLY,
    MONTHLY,
    WEEKLY,
    BI_WEEKLY;

    public static PayrollType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payroll type is required.");
        }

        try {
            return PayrollType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid payroll type: " + value);
        }
    }
}
