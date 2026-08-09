package motorph.backend.domain.value;

/**
 * Represents how an employee salary is computed.
 */
public enum SalaryType {

    MONTHLY,
    HOURLY;

    public static SalaryType fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Salary type is required.");
        }

        try {
            return SalaryType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid salary type: " + value);
        }
    }
}
