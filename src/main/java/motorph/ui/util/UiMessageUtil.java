package motorph.ui.util;

import javafx.scene.control.Label;

/** Provides helpers for showing success and error messages on Labels. */
public final class UiMessageUtil {

    // utility class — no instances
    private UiMessageUtil() {
    }

    // sets an error message on the label
    public static void setError(Label label, String message) {
        label.getStyleClass().remove("success-message");
        if (!label.getStyleClass().contains("error-message")) {
            label.getStyleClass().add("error-message");
        }
        label.setText(message);
    }

    // sets a success message on the label
    public static void setSuccess(Label label, String message) {
        label.getStyleClass().remove("error-message");
        if (!label.getStyleClass().contains("success-message")) {
            label.getStyleClass().add("success-message");
        }
        label.setText(message);
    }

    // clears all message styling and text from the label
    public static void clearMessage(Label label) {
        label.getStyleClass().removeAll("success-message", "error-message");
        label.setText("");
    }

    // converts an exception to a user-friendly message
    public static String toUserMessage(Exception e) {
        String message = e.getMessage();

        if (message == null || message.isBlank()) {
            return "Unable to complete the request. Please try again.";
        }

        if (isDatabaseUnavailable(e)) {
            return "Database connection unavailable. Start MySQL and try again.";
        }

        if (message.startsWith("Failed to ")) {
            return "Unable to complete the request. Please check the database connection.";
        }

        return message;
    }

    // checks if the exception came from database connectivity
    private static boolean isDatabaseUnavailable(Exception e) {
        Throwable current = e;

        while (current != null) {
            String message = current.getMessage();

            if (message != null
                    && (message.contains("Failed to connect to the database")
                    || message.contains("Communications link failure")
                    || message.contains("Connection refused")
                    || message.contains("No suitable driver"))) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
