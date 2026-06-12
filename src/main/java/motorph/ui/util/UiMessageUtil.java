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
}
