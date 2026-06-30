package motorph.ui.it;

import motorph.application.service.PasswordResetService;
import motorph.application.service.SupportTicketService;
import motorph.application.service.UserAccessLogService;
import motorph.application.service.UserAccountService;
import motorph.domain.model.PasswordResetRequest;
import motorph.domain.model.SupportTicket;
import motorph.domain.value.RoutingTeam;
import motorph.ui.factory.ITUIServiceFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Displays IT dashboard metrics, login-activity stats, and summary tables. */
public class ITDashboardController {

    private static final Logger logger = Logger.getLogger(ITDashboardController.class.getName());

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    private UserAccountService   userAccountService;
    private PasswordResetService passwordResetService;
    private UserAccessLogService userAccessLogService;
    private SupportTicketService supportTicketService;
    private ITMainController     mainController;

    // Account metric labels
    @FXML private Label activeAccountsLabel;
    @FXML private Label lockedAccountsLabel;
    @FXML private Label disabledAccountsLabel;
    @FXML private Label pendingResetRequestsLabel;
    @FXML private Label openTicketsLabel;

    // Login-activity stat labels
    @FXML private Label successLoginsLabel;
    @FXML private Label failedLoginsLabel;

    // Table row containers (populated programmatically)
    @FXML private VBox pendingResetRows;
    @FXML private VBox recentTicketsRows;

    @FXML
    private void initialize() {
        userAccountService   = ITUIServiceFactory.getInstance().getUserAccountService();
        passwordResetService = ITUIServiceFactory.getInstance().getPasswordResetService();
        userAccessLogService = ITUIServiceFactory.getInstance().getUserAccessLogService();
        supportTicketService = ITUIServiceFactory.getInstance().getSupportTicketService();
    }

    public void setMainController(ITMainController mainController) {
        this.mainController = mainController;
    }

    // called by ITMainController on navigation to refresh all sections
    public void refresh() {
        refreshAccountMetrics();
        refreshPasswordResetMetrics();
        refreshTicketMetrics();
        refreshLoginActivity();
        refreshPendingResets();
        refreshRecentTickets();
    }

    // Metric refresh methods
    private void refreshAccountMetrics() {
        try {
            int active   = userAccountService.getActiveAccountCount();
            int locked   = userAccountService.getLockedAccountCount();
            int disabled = userAccountService.getDisabledAccountCount();
            setText(activeAccountsLabel,   String.valueOf(active));
            setText(lockedAccountsLabel,   String.valueOf(locked));
            setText(disabledAccountsLabel, String.valueOf(disabled));
        } catch (Exception e) {
            setText(activeAccountsLabel,   "0");
            setText(lockedAccountsLabel,   "0");
            setText(disabledAccountsLabel, "0");
        }
    }

    private void refreshPasswordResetMetrics() {
        try {
            setText(pendingResetRequestsLabel, String.valueOf(passwordResetService.getPendingRequestCount()));
        } catch (Exception e) {
            setText(pendingResetRequestsLabel, "0");
        }
    }

    private void refreshTicketMetrics() {
        try {
            setText(openTicketsLabel, String.valueOf(supportTicketService.getOpenTicketCount()));
        } catch (Exception e) {
            setText(openTicketsLabel, "0");
        }
    }

    private void refreshLoginActivity() {
        try {
            int success = userAccessLogService
                    .getLogsByAction(UserAccessLogService.ACTION_LOGIN_SUCCESS).size();
            int failed = userAccessLogService
                    .getLogsByAction(UserAccessLogService.ACTION_LOGIN_FAILED).size();
            setText(successLoginsLabel, String.valueOf(success));
            setText(failedLoginsLabel,  String.valueOf(failed));
        } catch (Exception e) {
            setText(successLoginsLabel, "0");
            setText(failedLoginsLabel,  "0");
        }
    }

    // Table row population
    private void refreshPendingResets() {
        if (pendingResetRows == null) return;
        pendingResetRows.getChildren().clear();
        try {
            List<PasswordResetRequest> resets = passwordResetService.listPendingRequests();
            resets.stream()
                  .limit(5)
                  .map(this::buildResetRow)
                  .forEach(pendingResetRows.getChildren()::add);
        } catch (Exception e) {
            // leaves the table empty on service error
            logger.log(Level.WARNING, "Failed to load pending password resets", e);
        }
    }

    private void refreshRecentTickets() {
        if (recentTicketsRows == null) return;
        recentTicketsRows.getChildren().clear();
        try {
            List<SupportTicket> tickets = supportTicketService
                    .getTicketsByRoutingTeam(RoutingTeam.IT);
            tickets.stream()
                   .limit(5)
                   .map(this::buildTicketRow)
                   .forEach(recentTicketsRows.getChildren()::add);
        } catch (Exception e) {
            // leaves the table empty on service error
            logger.log(Level.WARNING, "Failed to load recent IT tickets", e);
        }
    }

    // Row builders
    private HBox buildResetRow(PasswordResetRequest req) {
        HBox row = new HBox();
        row.getStyleClass().add("table-data-row");

        Label id      = cell("REQ-" + (req.getResetRequestId() != null ? req.getResetRequestId() : "?"), 90);
        Label account = cell("Account #" + req.getUserAccountId(), 100);

        Label status = new Label(req.getRequestStatus().name());
        status.getStyleClass().add("status-badge");
        status.setPrefWidth(90);

        Label date = cell(formatDate(req.getRequestedAt()), 0);
        HBox.setHgrow(date, Priority.ALWAYS);

        row.getChildren().addAll(id, account, status, date);
        return row;
    }

    private HBox buildTicketRow(SupportTicket ticket) {
        HBox row = new HBox();
        row.getStyleClass().add("table-data-row");

        Label id = cell(
                "TK-" + (ticket.getTicketId() != null
                        ? String.format("%04d", ticket.getTicketId()) : "?"),
                80);
        Label category = cell(ticket.getCategory().name().replace("_", " "), 110);

        Label subject = new Label(ticket.getSubject());
        subject.getStyleClass().add("data-line");
        subject.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(subject, Priority.ALWAYS);

        Label status = new Label(ticket.getStatus().name());
        status.getStyleClass().add("status-badge");
        status.setPrefWidth(75);

        Label date = cell(formatDate(ticket.getCreatedAt()), 140);

        row.getChildren().addAll(id, category, subject, status, date);
        return row;
    }

    // View-all stubs (wired later by parent shell)
    @FXML
    private void viewAllResets() {
        if (mainController != null) mainController.navigateToPasswordReset();
    }

    @FXML
    private void viewAllTickets() {
        if (mainController != null) mainController.navigateToSupportTickets();
    }

    // Utilities
    private Label cell(String text, double prefWidth) {
        Label l = new Label(text);
        l.getStyleClass().add("data-line");
        if (prefWidth > 0) l.setPrefWidth(prefWidth);
        return l;
    }

    private String formatDate(LocalDateTime dt) {
        return dt != null ? dt.format(DATE_FMT) : "—";
    }

    private void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }
}
