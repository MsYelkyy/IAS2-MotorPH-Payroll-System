package motorph.ui.employee;

import motorph.domain.value.SupportTicketCategory;
import motorph.domain.value.SupportTicketStatus;
import motorph.api.ApiClientFactory;
import motorph.api.SupportTicketApiClient;
import motorph.api.dto.support.SupportTicketDTO;
import motorph.api.dto.support.TicketSubmitRequest;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiMessageUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Handles support ticket submission and ticket history display for the employee. */
public class EmployeeSupportTicketController {

    private static final Logger logger = Logger.getLogger(EmployeeSupportTicketController.class.getName());

    private static final int MESSAGE_LIMIT = 1000;
    private static final int TITLE_LIMIT = 150;
    private static final int PAGE_SIZE = 5;
    private static final String TITLE_HINT = "Brief summary of your concern";
    private static final String MESSAGE_HINT = "Describe your concern in detail...";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a");

    private SupportTicketApiClient supportTicketApiClient;

    private final Map<String, SupportTicketCategory> categoryMap = new LinkedHashMap<>();
    private final List<SupportTicketDTO> allTickets = new ArrayList<>();
    private int currentPage = 1;
    private boolean titleDirty = false;
    private boolean messageDirty = false;

    // Toast host
    @FXML private StackPane toastRoot;

    // Form
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField titleField;
    @FXML private TextArea messageArea;
    @FXML private Label categoryErrorLabel;
    @FXML private Label titleErrorLabel;
    @FXML private Label messageErrorLabel;
    @FXML private Label messageCounterLabel;
    @FXML private Label formMessageLabel;

    // Table
    @FXML private TableView<SupportTicketDTO> ticketTable;
    @FXML private TableColumn<SupportTicketDTO, String> colTicketId;
    @FXML private TableColumn<SupportTicketDTO, String> colCategory;
    @FXML private TableColumn<SupportTicketDTO, String> colTitle;
    @FXML private TableColumn<SupportTicketDTO, String> colStatus;
    @FXML private TableColumn<SupportTicketDTO, String> colDateCreated;

    // Pagination
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageLabel;
    @FXML private Label recordInfoLabel;

    @FXML
    private void initialize() {
        supportTicketApiClient = ApiClientFactory.getInstance().getSupportTicketApiClient();

        buildCategoryMap();
        setupForm();
        setupTable();
    }

    // reloads ticket list when the view becomes active
    public void refresh() {
        loadTickets();
    }

    // clears the form and validation state; called when leaving the view
    public void resetForm() {
        titleDirty = false;
        messageDirty = false;
        categoryComboBox.getSelectionModel().clearSelection();
        titleField.clear();
        messageArea.clear();
        updateFloatingHint(titleField, TITLE_HINT, false);
        updateFloatingHint(messageArea, MESSAGE_HINT, false);
        updateMessageCounter();
        clearAllErrors();
        UiMessageUtil.clearMessage(formMessageLabel);
    }

    @FXML
    private void handleSubmitTicket() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            UiMessageUtil.setError(formMessageLabel, "Please log in before using this action.");
            return;
        }

        UiMessageUtil.clearMessage(formMessageLabel);
        if (!validateForm()) return;

        try {
            SupportTicketCategory category = categoryMap.get(categoryComboBox.getValue());
            String subject = titleField.getText().trim();
            String description = messageArea.getText().trim();

            supportTicketApiClient.submitTicket(new TicketSubmitRequest(category.name(), subject, description));

            resetForm();
            loadTickets();
            UiMessageUtil.showToast(toastRoot, "Support ticket submitted successfully.");
        } catch (Exception e) {
            UiMessageUtil.setError(formMessageLabel, userMessage(e));
        }
    }

    @FXML
    private void handleClearForm() {
        resetForm();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages()) {
            currentPage++;
            renderPage();
        }
    }

    private void buildCategoryMap() {
        categoryMap.put("Payroll", SupportTicketCategory.PAYROLL);
        categoryMap.put("Attendance", SupportTicketCategory.ATTENDANCE);
        categoryMap.put("Account", SupportTicketCategory.ACCOUNT);
        categoryMap.put("System", SupportTicketCategory.SYSTEM);
        categoryMap.put("Other", SupportTicketCategory.OTHER);
    }

    private void setupForm() {
        categoryComboBox.getItems().setAll(categoryMap.keySet());
        categoryComboBox.setOnAction(e -> clearFieldError(categoryErrorLabel, categoryComboBox));

        configureFloatingHint(titleField, TITLE_HINT, () -> titleDirty);
        configureFloatingHint(messageArea, MESSAGE_HINT, () -> messageDirty);

        titleField.textProperty().addListener((obs, old, nv) -> {
            if (nv != null && nv.length() > TITLE_LIMIT) {
                titleField.setText(old);
                return;
            }
            if (hasText(nv)) {
                titleDirty = true;
                clearFieldError(titleErrorLabel, titleField);
            }
            UiMessageUtil.clearMessage(formMessageLabel);
        });

        messageArea.textProperty().addListener((obs, old, nv) -> {
            if (nv != null && nv.length() > MESSAGE_LIMIT) {
                messageArea.setText(old);
                return;
            }
            if (hasText(nv)) {
                messageDirty = true;
                clearFieldError(messageErrorLabel, messageArea);
            }
            UiMessageUtil.clearMessage(formMessageLabel);
            updateMessageCounter();
        });
        updateMessageCounter();
    }

    private void setupTable() {
        ticketTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        colTicketId.setCellValueFactory(data -> {
            Integer id = data.getValue().getTicketId();
            return new SimpleStringProperty(id == null ? "-" : "#" + id);
        });

        colCategory.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory().name()));

        colTitle.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSubject()));

        colStatus.setCellValueFactory(data ->
                new SimpleStringProperty(statusDisplay(data.getValue().getStatus())));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll(
                        "ticket-status-open", "ticket-status-resolved", "ticket-status-cancelled");
                if (empty || status == null || getIndex() < 0
                        || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                } else {
                    setText(status);
                    SupportTicketStatus ticketStatus =
                            getTableView().getItems().get(getIndex()).getStatus();
                    getStyleClass().add("ticket-status-" + ticketStatus.name().toLowerCase());
                }
            }
        });

        colDateCreated.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCreatedAt().format(DATE_TIME_FORMAT)));
    }

    private void loadTickets() {
        allTickets.clear();
        int employeeId = CurrentUserSession.getEmployeeId();

        if (employeeId > 0) {
            try {
                List<SupportTicketDTO> tickets = supportTicketApiClient.getOwnTickets();
                tickets.sort(Comparator.comparing(SupportTicketDTO::getCreatedAt).reversed());
                allTickets.addAll(tickets);
            } catch (Exception e) {
                // leaves the table empty on error to avoid crashing the view
                logger.log(Level.WARNING, "Failed to load employee tickets", e);
            }
        }

        currentPage = 1;
        renderPage();
    }

    private void renderPage() {
        int total = allTickets.size();
        int totalPages = totalPages();
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, total);

        ticketTable.getItems().setAll(allTickets.subList(from, to));
        pageLabel.setText("Page " + currentPage + " of " + totalPages);
        recordInfoLabel.setText(total == 0
                ? "No tickets found."
                : "Showing " + (from + 1) + " to " + to + " of " + total + " tickets");
        prevButton.setDisable(currentPage <= 1);
        nextButton.setDisable(currentPage >= totalPages);
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil((double) allTickets.size() / PAGE_SIZE));
    }

    private boolean validateForm() {
        clearAllErrors();
        boolean valid = true;

        if (categoryComboBox.getValue() == null) {
            showFieldError(categoryErrorLabel, categoryComboBox, "Required");
            valid = false;
        }

        if (!hasText(titleField.getText())) {
            showFieldError(titleErrorLabel, titleField, "Required");
            valid = false;
        }

        if (!hasText(messageArea.getText())) {
            showFieldError(messageErrorLabel, messageArea, "Required");
            valid = false;
        }

        return valid;
    }

    private void configureFloatingHint(javafx.scene.control.TextInputControl field, String hint,
                                        BooleanSupplier dirtyCheck) {
        field.focusedProperty().addListener((obs, wasFocused, isFocused) ->
                updateFloatingHint(field, hint, dirtyCheck.getAsBoolean()));
        field.textProperty().addListener((obs, oldValue, newValue) ->
                updateFloatingHint(field, hint, dirtyCheck.getAsBoolean()));
        updateFloatingHint(field, hint, false);
    }

    private void updateFloatingHint(javafx.scene.control.TextInputControl field, String hint, boolean dirty) {
        if (field == null) return;
        field.setPromptText((!dirty && !field.isFocused() && !hasText(field.getText())) ? hint : "");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void showFieldError(Label label, Node field, String message) {
        label.setText(message);
        if (!field.getStyleClass().contains("invalid-field")) {
            field.getStyleClass().add("invalid-field");
        }
    }

    private void clearFieldError(Label label, Node field) {
        if (label != null) label.setText("");
        if (field != null) field.getStyleClass().remove("invalid-field");
    }

    private void clearAllErrors() {
        clearFieldError(categoryErrorLabel, categoryComboBox);
        clearFieldError(titleErrorLabel, titleField);
        clearFieldError(messageErrorLabel, messageArea);
    }

    private void updateMessageCounter() {
        if (messageCounterLabel == null) return;
        int count = messageArea == null || messageArea.getText() == null
                ? 0 : messageArea.getText().length();
        messageCounterLabel.setText(count + " / " + MESSAGE_LIMIT);
    }

    private String statusDisplay(SupportTicketStatus status) {
        return switch (status) {
            case OPEN -> "OPEN";
            case RESOLVED -> "RESOLVED";
            case CANCELLED -> "CLOSED";
        };
    }

    private String userMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) return "Unable to complete the request. Please try again.";
        if (isDatabaseUnavailable(e)) return "Database connection unavailable. Start MySQL and try again.";
        return msg;
    }

    private boolean isDatabaseUnavailable(Exception e) {
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && (msg.contains("Failed to connect to the database")
                    || msg.contains("Communications link failure")
                    || msg.contains("Connection refused"))) return true;
            current = current.getCause();
        }
        return false;
    }
}
