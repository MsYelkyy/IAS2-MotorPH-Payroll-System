package motorph.ui.common;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.ArrayList;

/** Pages a filtered list through a table and drives the shared Previous / Next footer. */
public final class TablePager<T> {

    private final TableView<T> table;
    private final Label pageLabel;
    private final Label infoLabel;
    private final Button prevButton;
    private final Button nextButton;
    private final String noun;
    private final String emptyNote;
    private final int pageSize;
    private final ObservableList<T> page = FXCollections.observableArrayList();

    private SortedList<T> sorted;
    private ObservableList<?> unfiltered;
    private int currentPage = 1;

    public TablePager(TableView<T> table, Label pageLabel, Label infoLabel, Button prevButton, Button nextButton,
                      String noun, String emptyNote, int pageSize) {
        this.table = table;
        this.pageLabel = pageLabel;
        this.infoLabel = infoLabel;
        this.prevButton = prevButton;
        this.nextButton = nextButton;
        this.noun = noun;
        this.emptyNote = emptyNote;
        this.pageSize = pageSize;

        table.setItems(page);
        // the pager sorts across every page, so the table must not re-sort only the visible slice
        table.setSortPolicy(t -> true);
        prevButton.setOnAction(e -> goTo(currentPage - 1));
        nextButton.setOnAction(e -> goTo(currentPage + 1));
    }

    /** Pages {@code filtered}, sorted by the table's column sort; {@code unfiltered} supplies the overall count. */
    public void bind(ObservableList<T> filtered, ObservableList<?> unfiltered) {
        this.unfiltered = unfiltered;
        sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        sorted.addListener((ListChangeListener<T>) change -> render());
        render();
    }

    public void firstPage() {
        goTo(1);
    }

    private void goTo(int target) {
        currentPage = target;
        render();
    }

    private void render() {
        int total = sorted.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int from = (currentPage - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        page.setAll(new ArrayList<>(sorted.subList(from, to)));

        pageLabel.setText("Page " + currentPage + " of " + totalPages);
        infoLabel.setText(EmptyState.recordInfo(from, to, total, unfiltered.size(), noun));
        prevButton.setDisable(currentPage <= 1);
        nextButton.setDisable(currentPage >= totalPages);
        EmptyState.apply(table, !unfiltered.isEmpty(), noun, emptyNote);
    }
}
