package motorph.ui.common;

import motorph.ui.util.ThemeManager;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/** Builds the language and theme combo boxes into the topbar global-controls host. */
public final class GlobalControlsHelper {

    // utility class — no instances
    private GlobalControlsHelper() {
    }

    /**
     * Populates {@code host} with a language combo, a theme combo, and — if {@code onMoreClicked}
     * is non-null — a "⋮" overflow button wired to that action.
     */
    public static void build(HBox host, StackPane rootPane, Runnable onMoreClicked) {
        if (host == null) return;

        ComboBox<String> languageCombo = new ComboBox<>();
        languageCombo.getStyleClass().add("global-combo");
        languageCombo.setPrefWidth(150);
        languageCombo.getItems().setAll("English", "Filipino");
        languageCombo.setValue(ThemeManager.getInstance().getCurrentLanguage());
        languageCombo.setOnAction(e -> ThemeManager.getInstance().setLanguage(languageCombo.getValue(), rootPane));

        ComboBox<String> themeCombo = new ComboBox<>();
        themeCombo.getStyleClass().add("global-combo");
        themeCombo.setPrefWidth(150);
        themeCombo.getItems().setAll("System", "Light", "Dark");
        themeCombo.setValue(ThemeManager.getInstance().getCurrentTheme());
        themeCombo.setOnAction(e -> ThemeManager.getInstance().setTheme(themeCombo.getValue(), rootPane));

        host.setPickOnBounds(false);

        if (onMoreClicked != null) {
            Button moreBtn = new Button("⋮");
            moreBtn.getStyleClass().add("more-button");
            moreBtn.setOnAction(e -> onMoreClicked.run());
            host.getChildren().setAll(languageCombo, themeCombo, moreBtn);
        } else {
            host.getChildren().setAll(languageCombo, themeCombo);
        }
    }
}
