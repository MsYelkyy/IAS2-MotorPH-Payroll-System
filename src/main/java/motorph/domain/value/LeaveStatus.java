package motorph.domain.value;

public enum LeaveStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED;

    public static LeaveStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Leave status cannot be null.");
        }

        try {
            return LeaveStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid leave status: " + value);
        }
    }

    public boolean isFinal() {
        return this == APPROVED || this == REJECTED || this == CANCELLED;
    }
}
