package motorph.ui.it.dashboard;

import motorph.application.service.PasswordResetService;
import motorph.application.service.SupportTicketService;
import motorph.application.service.UserAccessLogService;
import motorph.application.service.UserAccountService;
import motorph.domain.model.PasswordResetRequest;
import motorph.domain.model.SupportTicket;
import motorph.ui.factory.EmployeeUIServiceFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Displays IT dashboard metrics, login-activity stats, and summary tables. */
public class ITDashboardController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    private UserAccountService   userAccountService;
    private PasswordResetService passwordResetService;
    private UserAccessLogService userAccessLogService;
    private SupportTicketService supportTicketService;

    // ── Account metric labels ─────────────────────────────────────────────────
    @FXML private Label activeAccountsLabel;
    @FXML private Label lockedAccountsLabel;
    @FXML private Label disabledAccountsLabel;
    @FXML private Label pendingResetRequestsLabel;
    @FXML private Label openTicketsLabel;

    // ── Login-activity stat labels ────────────────────────────────────────────
    @FXML private Label successLoginsLabel;
    @FXML private Label failedLoginsLabel;

    // ── Table row containers (populated programmatically) ─────────────────────
    @FXML private VBox pendingResetRows;
    @FXML private VBox recentTicketsRows;

    @FXML
    private void initialize() {
        userAccountService   = EmployeeUIServiceFactory.getInstance().getUserAccountService();
        passwordResetService = EmployeeUIServiceFactory.getInstance().getPasswordResetService();
        userAccessLogService = EmployeeUIServiceFactory.getInstance().getUserAccessLogService();
        supportTicketService = EmployeeUIServiceFactory.getInstance().getSupportTicketService();
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

    // ── Metric refresh methods ────────────────────────────────────────────────

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

    // ── Table row population ──────────────────────────────────────────────────

    private void refreshPendingResets() {
        if (pendingResetRows == null) return;
        pendingResetRows.getChildren().clear();
        try {
            List<PasswordResetRequest> resets = passwordResetService.listPendingRequests();
            resets.stream()
                  .limit(5)
                  .map(this::buildResetRow)
                  .forEach(pendingResetRows.getChildren()::add);
        } catch (Exception ignored) {
            // leave table empty on service error
        }
    }

    private void refreshRecentTickets() {
        if (recentTicketsRows == null) return;
        recentTicketsRows.getChildren().clear();
        try {
            List<SupportTicket> tickets = supportTicketService.getOpenTickets();
            tickets.stream()
                   .limit(5)
                   .map(this::buildTicketRow)
                   .forEach(recentTicketsRows.getChildren()::add);
        } catch (Exception ignored) {
            // leave table empty on service error
        }
    }

    // ── Row builders ──────────────────────────────────────────────────────────

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

    // ── View-all stubs (wired later by parent shell) ──────────────────────────

    @FXML
    private void viewAllResets() {
        // navigation to Password Reset Requests is delegated to ITMainController
    }

    @FXML
    private void viewAllTickets() {
        // navigation to Support Tickets is delegated to ITMainController
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

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
