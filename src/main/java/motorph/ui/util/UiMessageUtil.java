package motorph.ui.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

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

    /**
     * Shows a transient success toast pinned to the bottom-right of {@code root}.
     * The toast is injected into the StackPane, fades in, holds for 2.5 s, then
     * fades out and removes itself — it never participates in layout flow.
     */
    public static void showToast(StackPane root, String message) {
        Label toast = new Label(message);
        toast.getStyleClass().add("toast-label");
        toast.setMouseTransparent(true);
        StackPane.setAlignment(toast, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(toast, new Insets(0, 24, 28, 0));

        root.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.millis(2500));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> root.getChildren().remove(toast));

        new SequentialTransition(fadeIn, hold, fadeOut).play();
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
