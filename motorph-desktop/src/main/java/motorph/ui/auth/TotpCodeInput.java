package motorph.ui.auth;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.Duration;

import java.time.Instant;

/** Shared behaviour of the six-digit code fields and the "codes refresh every 30 seconds" indicator. */
final class TotpCodeInput {

    static final int DIGITS = 6;
    static final int PERIOD_SECONDS = 30;

    private TotpCodeInput() {
    }

    /** Keeps only digits and at most six of them, so a pasted "123 456" becomes 123456. */
    static void restrictToCode(TextField field) {
        field.setTextFormatter(new TextFormatter<String>(change -> {
            String digits = change.getControlNewText().replaceAll("\\D", "");

            if (digits.length() > DIGITS) {
                digits = digits.substring(0, DIGITS);
            }

            change.setText(digits);
            change.setRange(0, change.getControlText().length());
            change.setCaretPosition(digits.length());
            change.setAnchor(digits.length());
            return change;
        }));
    }

    /**
     * Counts down to the next 30-second step using the computer clock, which authenticator apps use
     * as well. The caller starts and stops the returned timeline with the screen.
     */
    static Timeline countdown(ProgressBar bar, Label label) {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> update(bar, label)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        update(bar, label);
        return timeline;
    }

    private static void update(ProgressBar bar, Label label) {
        long secondsIntoStep = Instant.now().getEpochSecond() % PERIOD_SECONDS;
        long remaining = PERIOD_SECONDS - secondsIntoStep;

        bar.setProgress(remaining / (double) PERIOD_SECONDS);
        label.setText("Codes refresh every " + PERIOD_SECONDS + " seconds. New code in " + remaining + "s.");
    }
}
