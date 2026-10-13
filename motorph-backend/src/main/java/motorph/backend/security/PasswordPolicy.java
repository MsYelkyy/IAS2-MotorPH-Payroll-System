package motorph.backend.security;

import motorph.backend.exception.ValidationException;

/**
 * Password requirements used by the sign-in screens.
 * Passwords must have at least 8 characters, one uppercase letter, and one number.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final String REQUIREMENTS = "Password must be at least 8 characters and contain one uppercase letter and one number.";

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new ValidationException("New password is required.");
        }

        String trimmed = password.trim();
        boolean hasUppercase = trimmed.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = trimmed.chars().anyMatch(Character::isDigit);

        if (trimmed.length() < MIN_LENGTH || !hasUppercase || !hasDigit) {
            throw new ValidationException(REQUIREMENTS);
        }
    }
}
