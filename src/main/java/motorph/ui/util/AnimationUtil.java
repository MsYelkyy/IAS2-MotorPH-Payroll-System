package motorph.ui.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/** Provides reusable JavaFX animation helpers. */
public final class AnimationUtil {

    // utility class — no instances
    private AnimationUtil() {
    }

    // plays a fade-in transition on the node
    public static void fadeIn(Node node, double durationMillis, double delayMillis) {
        if (node == null) return;
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(durationMillis), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(delayMillis));
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.play();
    }

    // plays a scale-in transition on the node
    public static void scaleIn(Node node, double durationMillis, double delayMillis) {
        if (node == null) return;
        node.setScaleX(0.98);
        node.setScaleY(0.98);
        ScaleTransition scale = new ScaleTransition(Duration.millis(durationMillis), node);
        scale.setFromX(0.98);
        scale.setFromY(0.98);
        scale.setToX(1);
        scale.setToY(1);
        scale.setDelay(Duration.millis(delayMillis));
        scale.setInterpolator(Interpolator.EASE_OUT);
        scale.play();
    }

    // plays an infinite float animation on the node
    public static void floatNode(Node node, double offsetY, double seconds, double delaySeconds) {
        if (node == null) return;
        TranslateTransition floatAnim = new TranslateTransition(Duration.seconds(seconds), node);
        floatAnim.setFromY(0);
        floatAnim.setByY(offsetY);
        floatAnim.setDelay(Duration.seconds(delaySeconds));
        floatAnim.setAutoReverse(true);
        floatAnim.setCycleCount(TranslateTransition.INDEFINITE);
        floatAnim.setInterpolator(Interpolator.EASE_BOTH);
        floatAnim.play();
    }

    // slides a node in from the right edge (shows it)
    public static void slideInFromRight(Node node, double width) {
        if (node == null) return;
        node.setTranslateX(width);
        node.setVisible(true);
        node.setManaged(true);
        TranslateTransition tt = new TranslateTransition(Duration.millis(260), node);
        tt.setFromX(width);
        tt.setToX(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        tt.play();
    }

    // slides a node out to the right edge (hides it when done)
    public static void slideOutToRight(Node node, double width, Runnable onFinished) {
        if (node == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.millis(260), node);
        tt.setFromX(0);
        tt.setToX(width);
        tt.setInterpolator(Interpolator.EASE_IN);
        tt.setOnFinished(e -> {
            node.setVisible(false);
            node.setManaged(false);
            if (onFinished != null) onFinished.run();
        });
        tt.play();
    }
}
