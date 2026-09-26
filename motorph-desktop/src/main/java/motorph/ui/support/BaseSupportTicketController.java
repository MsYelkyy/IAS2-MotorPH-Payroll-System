package motorph.ui.support;

import motorph.application.dto.it.SupportTicketRowDTO;
import motorph.domain.value.SupportTicketStatus;
import motorph.api.ApiClientFactory;
import motorph.api.SupportTicketApiClient;
import motorph.api.dto.support.SupportTicketDTO;
import motorph.ui.util.UiMessageUtil;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Shared logic for every role's Support Tickets screen.
 * Renders summary cards, status tabs, a paged table, and a detail overlay with
 * resolve/cancel actions. Subclasses supply the services and decide which tickets
 * to load (their routing team, all tickets, or a single employee's tickets) and
 * which category filter options to show.
 */
public abstract class BaseSupportTicketController {


    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    private static final int PAGE_SIZE = 7;

    private static final String STATUS_ALL       = "All";
    private static final String STATUS_OPEN      = "Open";
    private static final String STATUS_RESOLVED  = "Resolved";
    private static final String STATUS_CANCELLED = "Cancelled";

    protected static final String CAT_ALL = "All Categories";

    private SupportTicketApiClient supportTicketApiClient;

    private final ObservableList<SupportTicketRowDTO> allRows      = FXCollections.observableArrayList();
    private final ObservableList<SupportTicketRowDTO> filteredRows = FXCollections.observableArrayList();
    private final ObservableList<SupportTicketRowDTO> pagedRows    = FXCollections.observableArrayList();

    private String activeTab    = STATUS_ALL;
    private int    currentPage  = 1;

    // Summary card labels
    @FXML private Label openTicketsLabel;
    @FXML private Label resolvedTicketsLabel;
    @FXML private Label cancelledTicketsLabel;
    @FXML private Label totalTicketsLabel;
    @FXML private Label messageLabel;

    // Pagination controls
    @FXML private Button ticketsPrevButton;
    @FXML private Button ticketsNextButton;
    @FXML private Label  ticketsPageLabel;
    @FXML private Label  ticketsRecordInfoLabel;

    // Filter controls
    @FXML private HBox     tabsRow;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilterCombo;

    // Table
    @FXML private TableView<SupportTicketRowDTO>                             ticketsTable;
    @FXML private TableColumn<SupportTicketRowDTO, String>                   ticketIdColumn;
    @FXML private TableColumn<SupportTicketRowDTO, String>                   employeeColumn;
    @FXML private TableColumn<SupportTicketRowDTO, String>                   categoryColumn;
    @FXML private TableColumn<SupportTicketRowDTO, String>                   subjectColumn;
    @FXML private TableColumn<SupportTicketRowDTO, SupportTicketStatus>      statusColumn;
    @FXML private TableColumn<SupportTicketRowDTO, String>                   createdAtColumn;
    @FXML private TableColumn<SupportTicketRowDTO, SupportTicketRowDTO>      viewColumn;

    // tab button references for re-styling on selection change
    private Button allTab;
    private Button openTab;
    private Button resolvedTab;
    private Button cancelledTab;

    // Subclass hooks
    /** Loads the tickets this screen should display (routing team, all, or employee-scoped). */
    protected abstract List<SupportTicketDTO> loadTickets();

    // only Admin may cancel tickets; the backend enforces the same rule
    protected boolean canCancelTickets() {
        return false;
    }

    /** Category filter options for this screen; defaults to all categories only. Override to scope. */
    protected List<String> categoryFilterOptions() {
        return List.of(CAT_ALL);
    }

    @FXML
    private void initialize() {
        supportTicketApiClient = ApiClientFactory.getInstance().getSupportTicketApiClient();
        buildTabButtons();
        configureFilters();
        configureTable();
        refresh();
    }

    @FXML
    private void handleRefresh() {
        refresh();
    }

    // loads tickets and refreshes the view
    public void refresh() {
        try {
            List<SupportTicketRowDTO> rows = loadTickets()
                    .stream()
                    .map(this::toRowDTO)
                    .collect(Collectors.toList());
            allRows.setAll(rows);
            updateSummaryCards();
            applyFilters();
            UiMessageUtil.setSuccess(messageLabel, "Support tickets refreshed.");
        } catch (Exception e) {
            allRows.clear();
            filteredRows.clear();
            clearSummaryCards();
            currentPage = 1;
            renderPage();
            UiMessageUtil.setError(messageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    // Tab setup
    private void buildTabButtons() {
        allTab       = tabButton(STATUS_ALL,       true);
        openTab      = tabButton(STATUS_OPEN,      false);
        resolvedTab  = tabButton(STATUS_RESOLVED,  false);
        cancelledTab = tabButton(STATUS_CANCELLED, false);
        tabsRow.getChildren().setAll(allTab, openTab, resolvedTab, cancelledTab);
    }

    private Button tabButton(String label, boolean selected) {
        Button btn = new Button(label);
        btn.getStyleClass().add("ticket-tab-button");
        if (selected) btn.getStyleClass().add("ticket-tab-selected");
        btn.setOnAction(e -> selectTab(label, btn));
        return btn;
    }

    private void selectTab(String label, Button clicked) {
        activeTab = label;
        for (Button b : List.of(allTab, openTab, resolvedTab, cancelledTab)) {
            b.getStyleClass().remove("ticket-tab-selected");
        }
        clicked.getStyleClass().add("ticket-tab-selected");
        applyFilters();
    }

    // Filter setup
    private void configureFilters() {
        categoryFilterCombo.getItems().setAll(categoryFilterOptions());
        categoryFilterCombo.setValue(CAT_ALL);

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        categoryFilterCombo.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    // Table setup
    private void configureTable() {
        ticketsTable.setItems(pagedRows);
        ticketsTable.setPlaceholder(new Label("No support tickets found."));
        ticketsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        ticketIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty("#" + String.format("%04d", data.getValue().getTicketId())));
        employeeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(nullSafe(data.getValue().getEmployeeName())));
        categoryColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory() != null
                        ? data.getValue().getCategory().name() : "-"));
        subjectColumn.setCellValueFactory(data ->
                new SimpleStringProperty(nullSafe(data.getValue().getSubject())));
        statusColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue().getStatus()));
        createdAtColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatDate(data.getValue().getCreatedAt())));
        viewColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue()));

        categoryColumn.setCellFactory(col -> new CategoryBadgeCell());
        statusColumn.setCellFactory(col -> new StatusBadgeCell());
        viewColumn.setCellFactory(col -> new ViewButtonCell());
    }

    // Filtering
    private void applyFilters() {
        String query  = normal(searchField.getText());
        SupportTicketStatus statusFilter = selectedTabStatus();
        String catFilter = categoryFilterCombo.getValue();
        boolean catAll   = CAT_ALL.equals(catFilter);

        filteredRows.setAll(allRows.stream()
                .filter(row -> statusFilter == null || row.getStatus() == statusFilter)
                .filter(row -> catAll
                        || (row.getCategory() != null
                            && row.getCategory().name().equalsIgnoreCase(catFilter)))
                .filter(row -> matchesQuery(row, query))
                .collect(Collectors.toList()));

        currentPage = 1;
        renderPage();
        updateTabLabels();
    }

    private boolean matchesQuery(SupportTicketRowDTO row, String query) {
        if (query.isBlank()) return true;
        String searchable = String.join(" ",
                "#" + row.getTicketId(),
                nullSafe(row.getEmployeeName()),
                nullSafe(row.getSubject()));
        return normal(searchable).contains(query);
    }

    private SupportTicketStatus selectedTabStatus() {
        return switch (activeTab) {
            case STATUS_OPEN      -> SupportTicketStatus.OPEN;
            case STATUS_RESOLVED  -> SupportTicketStatus.RESOLVED;
            case STATUS_CANCELLED -> SupportTicketStatus.CANCELLED;
            default               -> null;
        };
    }

    // Summary cards
    private void updateSummaryCards() {
        long open      = allRows.stream().filter(r -> r.getStatus() == SupportTicketStatus.OPEN).count();
        long resolved  = allRows.stream().filter(r -> r.getStatus() == SupportTicketStatus.RESOLVED).count();
        long cancelled = allRows.stream().filter(r -> r.getStatus() == SupportTicketStatus.CANCELLED).count();

        openTicketsLabel.setText(String.valueOf(open));
        resolvedTicketsLabel.setText(String.valueOf(resolved));
        cancelledTicketsLabel.setText(String.valueOf(cancelled));
        totalTicketsLabel.setText(String.valueOf(allRows.size()));
    }

    private void clearSummaryCards() {
        openTicketsLabel.setText("0");
        resolvedTicketsLabel.setText("0");
        cancelledTicketsLabel.setText("0");
        totalTicketsLabel.setText("0");
    }

    private void renderPage() {
        int total      = filteredRows.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage    = Math.max(1, Math.min(currentPage, totalPages));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        pagedRows.setAll(filteredRows.subList(from, to));
        ticketsPageLabel.setText("Page " + currentPage + " of " + totalPages);
        ticketsRecordInfoLabel.setText(total == 0
                ? "No tickets found."
                : "Showing " + (from + 1) + " to " + to + " of " + total + " tickets");
        ticketsPrevButton.setDisable(currentPage <= 1);
        ticketsNextButton.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void handleTicketsPrevPage() {
        if (currentPage > 1) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void handleTicketsNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredRows.size() / PAGE_SIZE));
        if (currentPage < totalPages) {
            currentPage++;
            renderPage();
        }
    }

    private void updateTabLabels() {
        long open      = allRows.stream().filter(r -> r.getStatus() == SupportTicketStatus.OPEN).count();
        long resolved  = allRows.stream().filter(r -> r.getStatus() == SupportTicketStatus.RESOLVED).count();
        long cancelled = allRows.stream().filter(r -> r.getStatus() == SupportTicketStatus.CANCELLED).count();

        allTab.setText(STATUS_ALL       + " (" + allRows.size() + ")");
        openTab.setText(STATUS_OPEN     + " (" + open + ")");
        resolvedTab.setText(STATUS_RESOLVED  + " (" + resolved + ")");
        cancelledTab.setText(STATUS_CANCELLED + " (" + cancelled + ")");
    }

    // Ticket details popup
    // shows a dimmed overlay with the ticket details card centered on screen
    private void showTicketDetails(SupportTicketRowDTO row) {
        StackPane overlay = buildOverlay(row);
        StackPane root    = (StackPane) ticketsTable.getScene().getRoot();
        root.getChildren().add(overlay);
    }

    private StackPane buildOverlay(SupportTicketRowDTO row) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        VBox card = buildDetailCard(row, overlay);
        overlay.getChildren().add(card);

        // close when clicking the dim background (not the card itself)
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                closeOverlay(overlay);
            }
        });

        return overlay;
    }

    private VBox buildDetailCard(SupportTicketRowDTO row, StackPane overlay) {
        VBox card = new VBox(0);
        card.getStyleClass().add("it-ticket-dialog-card");
        card.setMaxWidth(660);
        card.setMinWidth(520);
        // hug the content's height so the centered card has no empty space below it
        card.setMaxHeight(Region.USE_PREF_SIZE);

        // header row
        HBox header = new HBox(10);
        header.getStyleClass().add("it-ticket-dialog-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("#" + String.format("%04d", row.getTicketId()));
        idLabel.getStyleClass().add("it-ticket-dialog-id");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(displayStatus(row.getStatus()));
        statusBadge.getStyleClass().addAll("status-badge", statusBadgeClass(row.getStatus()));

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("it-ticket-dialog-close");
        closeBtn.setOnAction(e -> closeOverlay(overlay));

        header.getChildren().addAll(idLabel, spacer, statusBadge, closeBtn);

        // body — wraps its content naturally; the popup has no scrollbar of its own
        VBox body = new VBox(16);
        body.getStyleClass().add("it-ticket-dialog-body");

        // two-column info row
        HBox infoRow = new HBox(12);

        VBox empSection = buildInfoSection("Employee Information",
                new String[][]{
                        {"Name",         nullSafe(row.getEmployeeName())},
                        {"Routing Team", row.getRoutingTeam() != null ? row.getRoutingTeam().name() : "-"}
                });

        VBox ticketSection = buildInfoSection("Ticket Information",
                new String[][]{
                        {"Category",     row.getCategory() != null ? row.getCategory().name() : "-"},
                        {"Created",      formatDate(row.getCreatedAt())},
                        {"Last Updated", formatDate(row.getResolvedAt() != null ? row.getResolvedAt() : row.getCreatedAt())}
                });

        HBox.setHgrow(empSection,    Priority.ALWAYS);
        HBox.setHgrow(ticketSection, Priority.ALWAYS);
        infoRow.getChildren().addAll(empSection, ticketSection);

        // subject section
        VBox subjectSection = new VBox(6);
        subjectSection.getStyleClass().add("it-ticket-dialog-section");
        Label subjectLabel = new Label("Title / Subject");
        subjectLabel.getStyleClass().add("it-ticket-dialog-section-label");
        Label subjectValue = new Label(nullSafe(row.getSubject()));
        subjectValue.getStyleClass().add("it-ticket-dialog-section-value");
        subjectValue.setWrapText(true);
        subjectSection.getChildren().addAll(subjectLabel, subjectValue);

        // description section
        VBox descSection = new VBox(6);
        descSection.getStyleClass().add("it-ticket-dialog-section");
        Label descLabel = new Label("Description");
        descLabel.getStyleClass().add("it-ticket-dialog-section-label");
        Label descValue = new Label(nullSafe(row.getDescription()));
        descValue.getStyleClass().add("it-ticket-dialog-section-value");
        descValue.setWrapText(true);
        descSection.getChildren().addAll(descLabel, descValue);

        body.getChildren().addAll(infoRow, subjectSection, descSection);

        // actions footer
        HBox actions = buildActionsBar(row, overlay);

        card.getChildren().addAll(header, body, actions);
        return card;
    }

    private VBox buildInfoSection(String title, String[][] fields) {
        VBox section = new VBox(8);
        section.getStyleClass().add("it-ticket-info-section");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("it-ticket-info-section-title");
        section.getChildren().add(titleLabel);

        for (String[] field : fields) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.TOP_LEFT);

            Label key = new Label(field[0] + ":");
            key.getStyleClass().add("it-ticket-info-key");

            Label val = new Label(field[1]);
            val.getStyleClass().add("it-ticket-info-val");
            val.setWrapText(true);

            row.getChildren().addAll(key, val);
            section.getChildren().add(row);
        }

        return section;
    }

    private HBox buildActionsBar(SupportTicketRowDTO row, StackPane overlay) {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("it-ticket-dialog-actions");
        bar.setAlignment(Pos.CENTER_LEFT);

        if (row.getStatus() == SupportTicketStatus.OPEN) {
            Button resolveBtn = new Button("✓  Resolve");
            resolveBtn.getStyleClass().add("primary-button");
            resolveBtn.setOnAction(e -> performAction(
                    "Resolve Ticket",
                    "Resolve ticket #" + row.getTicketId() + " for " + row.getEmployeeName() + "?",
                    () -> supportTicketApiClient.resolveTicket(row.getTicketId()),
                    "Ticket #" + row.getTicketId() + " resolved.",
                    overlay));

            bar.getChildren().add(resolveBtn);

            if (canCancelTickets()) {
                Button cancelBtn = new Button("✕  Cancel Ticket");
                cancelBtn.getStyleClass().add("danger-outline-button");
                cancelBtn.setOnAction(e -> performAction(
                        "Cancel Ticket",
                        "Cancel ticket #" + row.getTicketId() + " for " + row.getEmployeeName() + "?",
                        () -> supportTicketApiClient.cancelTicket(row.getTicketId()),
                        "Ticket #" + row.getTicketId() + " cancelled.",
                        overlay));
                bar.getChildren().add(cancelBtn);
            }
        } else {
            Label note = new Label(
                    "This ticket is " + displayStatus(row.getStatus()).toLowerCase() + " — no actions available.");
            note.getStyleClass().add("muted-text");
            bar.getChildren().add(note);
        }

        return bar;
    }

    private void performAction(String title,
                               String message,
                               Runnable action,
                               String successMessage,
                               StackPane overlay) {

        if (!confirm(title, message)) return;

        try {
            action.run();
            closeOverlay(overlay);
            refresh();
            UiMessageUtil.setSuccess(messageLabel, successMessage);
        } catch (Exception e) {
            UiMessageUtil.setError(messageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    private void closeOverlay(StackPane overlay) {
        if (overlay != null && overlay.getParent() instanceof StackPane root) {
            root.getChildren().remove(overlay);
        }
    }

    private boolean confirm(String title, String message) {
        ButtonType confirm = new ButtonType(title, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel  = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, confirm, cancel);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (ticketsTable.getScene() != null) {
            alert.initOwner(ticketsTable.getScene().getWindow());
        }
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirm;
    }


    private SupportTicketRowDTO toRowDTO(SupportTicketDTO ticket) {
        String employeeName = ticket.getEmployeeName() != null
                ? ticket.getEmployeeName()
                : "Employee #" + ticket.getEmployeeId();
        return new SupportTicketRowDTO(
                ticket.getTicketId(),
                ticket.getEmployeeId(),
                employeeName,
                ticket.getCategory(),
                ticket.getRoutingTeam(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getResolvedAt()
        );
    }

    private String displayStatus(SupportTicketStatus status) {
        if (status == null) return "-";
        return switch (status) {
            case OPEN      -> "OPEN";
            case RESOLVED  -> "RESOLVED";
            case CANCELLED -> "CANCELLED";
        };
    }

    private String statusBadgeClass(SupportTicketStatus status) {
        if (status == null) return "";
        return switch (status) {
            case OPEN      -> "status-badge-danger";
            case RESOLVED  -> "status-badge-success";
            case CANCELLED -> "status-badge-neutral";
        };
    }

    private String formatDate(LocalDateTime dt) {
        return dt != null ? dt.format(DATE_FMT) : "-";
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String normal(String value) {
        return nullSafe(value).trim().toLowerCase(Locale.ROOT);
    }

    // Custom table cells
    private final class CategoryBadgeCell extends TableCell<SupportTicketRowDTO, String> {
        @Override
        protected void updateItem(String cat, boolean empty) {
            super.updateItem(cat, empty);
            if (empty || cat == null || cat.isBlank() || "-".equals(cat)) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(cat);
            badge.getStyleClass().add("it-ticket-cat-badge");
            setGraphic(badge);
            setText(null);
        }
    }

    private final class StatusBadgeCell extends TableCell<SupportTicketRowDTO, SupportTicketStatus> {
        @Override
        protected void updateItem(SupportTicketStatus status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                return;
            }
            Label badge = new Label(displayStatus(status));
            badge.getStyleClass().addAll("status-badge", statusBadgeClass(status));
            setGraphic(badge);
            setText(null);
        }
    }

    private final class ViewButtonCell extends TableCell<SupportTicketRowDTO, SupportTicketRowDTO> {
        private final Button btn = new Button("›");

        private ViewButtonCell() {
            btn.getStyleClass().add("it-ticket-view-button");
            btn.setOnAction(e -> {
                SupportTicketRowDTO row = getItem();
                if (row != null) showTicketDetails(row);
            });
        }

        @Override
        protected void updateItem(SupportTicketRowDTO row, boolean empty) {
            super.updateItem(row, empty);
            setGraphic(empty || row == null ? null : btn);
            setText(null);
        }
    }
}
