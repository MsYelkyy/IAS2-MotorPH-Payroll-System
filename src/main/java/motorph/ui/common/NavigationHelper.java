package motorph.ui.common;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** Provides shared helpers for topbar header text and sidebar nav button highlighting. */
public final class NavigationHelper {

    // utility class — no instances
    private NavigationHelper() {
    }

    // Removes "selected-nav" from every button then adds it to the chosen one.
    public static void setNavSelected(Button selected, Button... allButtons) {
        for (Button btn : allButtons) {
            if (btn != null) btn.getStyleClass().remove("selected-nav");
        }
        if (selected != null && !selected.getStyleClass().contains("selected-nav")) {
            selected.getStyleClass().add("selected-nav");
        }
    }

    // Sets the topbar page title and subtitle labels.
    public static void setHeader(Label titleLabel, Label subtitleLabel, String title, String subtitle) {
        if (titleLabel    != null) titleLabel.setText(title);
        if (subtitleLabel != null) subtitleLabel.setText(subtitle);
    }
}
