package motorph.domain.value;

/**
 * Represents the attendance status of an employee.
 */
public enum AttendanceStatus {

    PRESENT,
    LATE,
    ABSENT,
    HALF_DAY,
    ON_LEAVE;

    // Converts text to attendance status.
    public static AttendanceStatus fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Attendance status is required.");
        }

        try {
            return AttendanceStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid attendance status: " + value);
        }
    }
}