package motorph.ui.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Locale;
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

    private static final double TOAST_WIDTH = 380;
    private static final double TOAST_TOP = 28;
    private static final double TOAST_MIN_HEIGHT = 74;
    private static final double TOAST_GAP = 12;

    // the four toast looks: coloured edge + icon, a fixed title, and the message as the body
    private enum ToastType {
        SUCCESS("Success", "\u2713"),
        INFO("Info", "i"),
        WARNING("Warning", "!"),
        ERROR("Error", "\u2715");

        private final String title;
        private final String glyph;

        ToastType(String title, String glyph) {
            this.title = title;
            this.glyph = glyph;
        }

        private String styleClass() {
            return "toast-" + name().toLowerCase(Locale.ROOT);
        }
    }

    // utility class — no instances
    private UiMessageUtil() {
    }

    // shows an error notification, anchored on the given label's window
    public static void setError(Label label, String message) {
        notify(label, message, ToastType.ERROR);
    }

    // shows a success notification, anchored on the given label's window
    public static void setSuccess(Label label, String message) {
        notify(label, message, ToastType.SUCCESS);
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
        notify(anchor, message, ToastType.ERROR);
    }

    /** Shows a success toast anchored on any node in the target window. */
    public static void showSuccess(Node anchor, String message) {
        notify(anchor, message, ToastType.SUCCESS);
    }

    /** Shows an informational toast anchored on any node in the target window. */
    public static void showInfo(Node anchor, String message) {
        notify(anchor, message, ToastType.INFO);
    }

    // resolves the node's window overlay and shows the toast there
    private static void notify(Node anchor, String message, ToastType type) {
        StackPane root = resolveToastRoot(anchor);

        if (root == null) {
            // fired before the node was attached to a scene (e.g. from a controller's
            // initialize) - there is no window to pin a toast to yet
            logger.log(Level.FINE, "Dropped notification with no visible window: {0}", message);
            return;
        }

        showToast(root, message, Pos.TOP_RIGHT, type);
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
     * overlay, fades in, holds, then fades out and removes itself (or closes on
     * its X) — it never participates in layout flow.
     */
    public static void showToast(StackPane root, String message) {
        showToast(root, message, Pos.TOP_RIGHT, ToastType.SUCCESS);
    }

    // shows a success toast pinned to the top-right of the window
    public static void showSuccessToastTop(StackPane root, String message) {
        showToast(root, message, Pos.TOP_RIGHT, ToastType.SUCCESS);
    }

    // shows an error toast pinned to the top-right of the window
    public static void showErrorToastTop(StackPane root, String message) {
        showToast(root, message, Pos.TOP_RIGHT, ToastType.ERROR);
    }

    /**
     * Shows a transient warning toast. Used for validation failures
     * (e.g. missing required fields).
     */
    public static void showErrorToast(StackPane root, String message) {
        showToast(root, message, Pos.TOP_RIGHT, ToastType.WARNING);
    }

    // logs the failure and shows a user-friendly error toast
    public static void handle(StackPane root, String context, Exception e) {
        logger.log(Level.WARNING, context, e);
        showErrorToast(root, toUserMessage(e));
    }

    // injects a transient toast that fades in, holds, then removes itself
    private static void showToast(StackPane root, String message, Pos alignment, ToastType type) {
        if (root == null) return;

        // a view-level toast host still resolves to the window overlay, so toasts
        // from nested screens stack in the same corner as everything else
        StackPane host = root.getScene() != null ? resolveToastRoot(root) : root;

        Label icon = new Label(type.glyph);
        icon.getStyleClass().add("toast-icon");

        Label title = new Label(type.title);
        title.getStyleClass().add("toast-title");

        Label text = new Label(message);
        text.getStyleClass().add("toast-message");
        text.setWrapText(true);

        VBox body = new VBox(title, text);
        body.getStyleClass().add("toast-body");
        HBox.setHgrow(body, Priority.ALWAYS);

        Button close = new Button("\u2715");
        close.getStyleClass().add("toast-close");
        close.setMinWidth(Region.USE_PREF_SIZE);

        HBox toast = new HBox(icon, body, close);
        toast.getStyleClass().addAll("toast", type.styleClass());
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.setPrefWidth(TOAST_WIDTH);
        toast.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(toast, alignment);
        // offset by the toasts already on screen so a burst of messages stacks
        // downwards instead of printing on top of itself
        StackPane.setMargin(toast, new Insets(nextTopOffset(host), 24, 28, 24));

        close.setOnAction(e -> host.getChildren().remove(toast));
        host.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.millis(3500));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> host.getChildren().remove(toast));

        new SequentialTransition(fadeIn, hold, fadeOut).play();
    }

    // top margin for the next toast: stacked below the toasts already in the overlay
    private static double nextTopOffset(StackPane host) {
        double offset = TOAST_TOP;

        for (Node child : host.getChildren()) {
            if (child.getStyleClass().contains("toast")) {
                offset += Math.max(child.getLayoutBounds().getHeight(), TOAST_MIN_HEIGHT) + TOAST_GAP;
            }
        }

        return offset;
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
