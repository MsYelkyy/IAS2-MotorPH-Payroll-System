package motorph.domain.value;

/**
 * Represents the gender of an employee.
 */
public enum Gender {

    MALE,
    FEMALE;

    // Converts text to gender.
    public static Gender fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Gender is required.");
        }

        try {
            return Gender.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid gender: " + value);
        }
    }
}