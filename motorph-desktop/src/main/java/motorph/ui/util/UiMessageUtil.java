package motorph.ui.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single entry point for user-facing notifications. Every message - success or
 * error - surfaces as a transient toast pinned to the top-right of the window,
 * never as text wedged into a container next to the button that triggered it.
 * The label-taking methods are kept so existing call sites read naturally; the
 * label is only used to locate the window, its text is never written to.
 */
public final class UiMessageUtil {

    private static final Logger logger = Logger.getLogger(UiMessageUtil.class.getName());

    // utility class — no instances
    private UiMessageUtil() {
    }

    // shows an error notification, anchored on the given label's window
    public static void setError(Label label, String message) {
        notify(label, message, "toast-label-error");
    }

    // shows a success notification, anchored on the given label's window
    public static void setSuccess(Label label, String message) {
        notify(label, message, "toast-label");
    }

    /**
     * Writes an error message into the label itself instead of toasting it.
     * Reserved for the login card, where the message has to stay on screen
     * next to the field the user is about to correct.
     */
    public static void setInlineError(Label label, String message) {
        label.getStyleClass().remove("success-message");
        if (!label.getStyleClass().contains("error-message")) {
            label.getStyleClass().add("error-message");
        }
        label.setText(message);
    }

    /** Inline counterpart of {@link #setInlineError} — see that method for why. */
    public static void setInlineSuccess(Label label, String message) {
        label.getStyleClass().remove("error-message");
        if (!label.getStyleClass().contains("success-message")) {
            label.getStyleClass().add("success-message");
        }
        label.setText(message);
    }

    /** Shows an error toast anchored on any node in the target window. */
    public static void showError(Node anchor, String message) {
        notify(anchor, message, "toast-label-error");
    }

    /** Shows a success toast anchored on any node in the target window. */
    public static void showSuccess(Node anchor, String message) {
        notify(anchor, message, "toast-label");
    }

    // resolves the node's window overlay and shows the toast there
    private static void notify(Node anchor, String message, String styleClass) {
        StackPane root = resolveToastRoot(anchor);

        if (root == null) {
            // fired before the node was attached to a scene (e.g. from a controller's
            // initialize) - there is no window to pin a toast to yet
            logger.log(Level.FINE, "Dropped notification with no visible window: {0}", message);
            return;
        }

        showToast(root, message, Pos.TOP_RIGHT, styleClass);
    }

    // walks up to the outermost StackPane so every toast lands in the same corner
    private static StackPane resolveToastRoot(Node anchor) {
        if (anchor == null || anchor.getScene() == null) {
            return null;
        }

        StackPane outermost = null;

        for (Node current = anchor; current != null; current = current.getParent()) {
            if (current instanceof StackPane stackPane) {
                outermost = stackPane;
            }
        }

        return outermost;
    }

    // clears all message styling and text from the label
    public static void clearMessage(Label label) {
        label.getStyleClass().removeAll("success-message", "error-message");
        label.setText("");
    }

    /**
     * Shows a transient success toast. The toast is injected into the window's
     * overlay, fades in, holds for 2.5 s, then fades out and removes itself — it
     * never participates in layout flow.
     */
    public static void showToast(StackPane root, String message) {
        showToast(root, message, Pos.TOP_RIGHT, "toast-label");
    }

    // shows a success toast pinned to the top-right of the window
    public static void showSuccessToastTop(StackPane root, String message) {
        showToast(root, message, Pos.TOP_RIGHT, "toast-label");
    }

    // shows an error toast pinned to the top-right of the window
    public static void showErrorToastTop(StackPane root, String message) {
        showToast(root, message, Pos.TOP_RIGHT, "toast-label-error");
    }

    /**
     * Shows a transient red error toast. Used for validation failures
     * (e.g. missing required fields).
     */
    public static void showErrorToast(StackPane root, String message) {
        showToast(root, message, Pos.TOP_RIGHT, "toast-label-danger");
    }

    // logs the failure and shows a user-friendly error toast
    public static void handle(StackPane root, String context, Exception e) {
        logger.log(Level.WARNING, context, e);
        showErrorToast(root, toUserMessage(e));
    }

    // injects a transient toast that fades in, holds, then removes itself
    private static void showToast(StackPane root, String message, Pos alignment, String styleClass) {
        if (root == null) return;

        // a view-level toast host still resolves to the window overlay, so toasts
        // from nested screens stack in the same corner as everything else
        StackPane host = root.getScene() != null ? resolveToastRoot(root) : root;

        Label toast = new Label(message);
        toast.getStyleClass().add(styleClass);
        toast.setWrapText(true);
        toast.setMouseTransparent(true);
        StackPane.setAlignment(toast, alignment);
        // offset by the toasts already on screen so a burst of messages stacks
        // downwards instead of printing on top of itself
        StackPane.setMargin(toast, new Insets(28 + (visibleToasts(host) * 54), 24, 28, 24));

        host.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.millis(2500));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> host.getChildren().remove(toast));

        new SequentialTransition(fadeIn, hold, fadeOut).play();
    }

    // counts the toasts currently living in the host overlay
    private static int visibleToasts(StackPane host) {
        int count = 0;

        for (Node child : host.getChildren()) {
            if (child.getStyleClass().stream().anyMatch(style -> style.startsWith("toast-label"))) {
                count++;
            }
        }

        return count;
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
