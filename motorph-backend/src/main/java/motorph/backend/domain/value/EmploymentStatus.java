package motorph.backend.domain.value;

/**
 * Represents the employment status of an employee.
 */
public enum EmploymentStatus {

    REGULAR,
    PROBATIONARY,
    CONTRACTUAL;

    public static EmploymentStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Employment status is required.");
        }

        try {
            return EmploymentStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid employment status: " + value);
        }
    }
}
