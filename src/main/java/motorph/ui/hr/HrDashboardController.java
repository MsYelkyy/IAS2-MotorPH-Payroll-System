package motorph.ui.hr;

import motorph.application.service.EmployeeService;
import motorph.application.service.LeaveService;
import motorph.domain.model.LeaveRequest;
import motorph.ui.factory.HrUIServiceFactory;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Displays HR dashboard metrics and a summary of pending leave requests. */
public class HrDashboardController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private EmployeeService employeeService;
    private LeaveService    leaveService;
    private HrMainController mainController;

    // Workforce metric labels
    @FXML private Label totalEmployeesLabel;
    @FXML private Label activeEmployeesLabel;
    @FXML private Label pendingLeaveLabel;
    @FXML private Label onLeaveTodayLabel;

    // Pending leave table rows (populated programmatically)
    @FXML private VBox pendingLeaveRows;

    // resolves HR services from the factory
    @FXML
    private void initialize() {
        employeeService = HrUIServiceFactory.getInstance().getEmployeeService();
        leaveService    = HrUIServiceFactory.getInstance().getLeaveService();
    }

    // links this view to its parent shell
    public void setMainController(HrMainController mainController) {
        this.mainController = mainController;
    }

    // refreshes all metrics and tables; called by the shell on navigation
    public void refresh() {
        refreshWorkforceMetrics();
        refreshLeaveMetrics();
        refreshPendingLeave();
    }

    // loads total and active employee counts
    private void refreshWorkforceMetrics() {
        try {
            int total  = employeeService.getAllEmployees().size();
            int active = employeeService.getAllActiveEmployees().size();
            setText(totalEmployeesLabel,  String.valueOf(total));
            setText(activeEmployeesLabel, String.valueOf(active));
        } catch (Exception e) {
            setText(totalEmployeesLabel,  "0");
            setText(activeEmployeesLabel, "0");
        }
    }

    // loads pending leave and on-leave-today counts
    private void refreshLeaveMetrics() {
        try {
            setText(pendingLeaveLabel,  String.valueOf(leaveService.countPendingRequests()));
            setText(onLeaveTodayLabel,  String.valueOf(leaveService.countApprovedLeavesToday()));
        } catch (Exception e) {
            setText(pendingLeaveLabel, "0");
            setText(onLeaveTodayLabel, "0");
        }
    }

    // populates the pending leave request table
    private void refreshPendingLeave() {
        if (pendingLeaveRows == null) return;
        pendingLeaveRows.getChildren().clear();
        try {
            List<LeaveRequest> pending = leaveService.getPendingLeaveRequests();
            pending.stream()
                   .limit(5)
                   .map(this::buildLeaveRow)
                   .forEach(pendingLeaveRows.getChildren()::add);
        } catch (Exception ignored) {
            // leave table empty on service error
        }
    }

    // builds a single pending leave request row
    private HBox buildLeaveRow(LeaveRequest request) {
        HBox row = new HBox();
        row.getStyleClass().add("table-data-row");

        Label id       = cell("REQ-" + (request.getRequestId() != null ? request.getRequestId() : "?"), 90);
        Label employee = cell("Employee #" + request.getEmployeeId(), 120);
        Label dates    = cell(formatDate(request.getStartDate()) + " – " + formatDate(request.getEndDate()), 0);
        HBox.setHgrow(dates, Priority.ALWAYS);

        Label status = new Label(request.getStatus().name());
        status.getStyleClass().add("status-badge");
        status.setPrefWidth(90);

        row.getChildren().addAll(id, employee, dates, status);
        return row;
    }

    // builds a fixed-width data cell
    private Label cell(String text, double prefWidth) {
        Label l = new Label(text);
        l.getStyleClass().add("data-line");
        if (prefWidth > 0) l.setPrefWidth(prefWidth);
        return l;
    }

    // formats a date or returns a dash when missing
    private String formatDate(java.time.LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "—";
    }

    // sets label text when the label is present
    private void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }
}
