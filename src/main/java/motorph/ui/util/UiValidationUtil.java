package motorph.ui.util;

import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Locale;

/** Provides helpers for field validation and input parsing in UI controllers. */
public final class UiValidationUtil {

    private static final String MOTORPH_EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@motorph\\.com$";

    // utility class — no instances
    private UiValidationUtil() {
    }

    // adds the invalid-field style class to a text field
    public static void markInvalid(TextField field) {
        if (!field.getStyleClass().contains("invalid-field")) {
            field.getStyleClass().add("invalid-field");
        }
    }

    // adds the invalid-field style class to a password field
    public static void markInvalid(PasswordField field) {
        if (!field.getStyleClass().contains("invalid-field")) {
            field.getStyleClass().add("invalid-field");
        }
    }

    // removes the invalid-field style class from a text field
    public static void clearInvalid(TextField field) {
        field.getStyleClass().remove("invalid-field");
    }

    // removes the invalid-field style class from a password field
    public static void clearInvalid(PasswordField field) {
        field.getStyleClass().remove("invalid-field");
    }

    // returns trimmed text from a field, never null
    public static String getText(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    // returns trimmed text from a password field, never null
    public static String getText(PasswordField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    // returns true if the email matches the MotorPH company domain pattern
    public static boolean isMotorphEmail(String email) {
        return email != null && email.trim().toLowerCase(Locale.ROOT).matches(MOTORPH_EMAIL_PATTERN);
    }

}
