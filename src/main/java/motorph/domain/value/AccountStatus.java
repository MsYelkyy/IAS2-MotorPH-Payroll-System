package motorph.domain.value;

public enum AccountStatus {
    ACTIVE,
    DISABLED,
    LOCKED;

    public static AccountStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Account status cannot be null.");
        }
        return AccountStatus.valueOf(value.trim().toUpperCase());

    }
}
