package motorph.ui.common;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

/** Builds the centred title-and-note placeholder shown inside an empty table. */
public final class EmptyState {

    private EmptyState() {
    }

    /** Shows "No matching X" when records exist but none pass the filters, otherwise "No X yet". */
    public static void apply(TableView<?> table, boolean anyRecords, String noun, String emptyNote) {
        if (anyRecords) {
            apply(table, "No matching " + noun, "Try changing the search or filters.");
        } else {
            apply(table, "No " + noun + " yet", emptyNote);
        }
    }

    public static void apply(TableView<?> table, String title, String note) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("table-empty-title");
        Label noteLabel = new Label(note);
        noteLabel.getStyleClass().add("table-empty-note");
        VBox box = new VBox(titleLabel, noteLabel);
        box.getStyleClass().add("table-empty");
        table.setPlaceholder(box);
    }

    /** "Showing 1 to 10 of 24 tickets", or "Showing 0 of 24 tickets" when nothing is on the page. */
    public static String recordInfo(int from, int to, int total, int unfilteredTotal, String noun) {
        return total == 0
                ? "Showing 0 of " + unfilteredTotal + " " + noun
                : "Showing " + (from + 1) + " to " + to + " of " + total + " " + noun;
    }
}
