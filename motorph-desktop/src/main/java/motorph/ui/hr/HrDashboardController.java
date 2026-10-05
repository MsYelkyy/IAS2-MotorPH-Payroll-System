package motorph.ui.hr;

import motorph.domain.value.RequestStatus;
import motorph.api.ApiClientFactory;
import motorph.api.AttendanceApiClient;
import motorph.api.EmployeeApiClient;
import motorph.api.LeaveApiClient;
import motorph.api.dto.attendance.AttendanceDTO;
import motorph.api.dto.employee.DepartmentDTO;
import motorph.api.dto.employee.EmployeeSummaryDTO;
import motorph.api.dto.leave.LeaveRequestRowDTO;
import motorph.api.dto.leave.LeaveSummaryDTO;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Populates the HR dashboard with workforce metrics, attendance/department charts,
 * a leave summary, and static informational cards. All numbers are aggregated in the
 * controller from existing service results (no SQL here), mirroring AdminDashboardController.
 */
public class HrDashboardController {

    private static final Logger logger = Logger.getLogger(HrDashboardController.class.getName());

    // Leave types shown in the Leave Summary, in display order.
    private static final int[] LEAVE_SUMMARY_TYPE_IDS = {1, 2, 3, 4, 5, 6};

    private EmployeeApiClient   employeeApiClient;
    private LeaveApiClient      leaveApiClient;
    private AttendanceApiClient attendanceApiClient;
    private HrMainController    mainController;

    // Workforce metric labels
    @FXML private Label totalEmployeesLabel;
    @FXML private Label activeEmployeesLabel;
    @FXML private Label presentTodayLabel;
    @FXML private Label onLeaveTodayLabel;
    @FXML private Label pendingLeaveLabel;

    // Charts
    @FXML private LineChart<String, Number> attendanceTrendChart;
    @FXML private BarChart<String, Number>  departmentDistributionChart;
    @FXML private BarChart<String, Number>  attendanceSummaryChart;

    // Leave summary rows container
    @FXML private VBox leaveSummaryList;

    // resolves HR services from the factory
    @FXML
    private void initialize() {
        employeeApiClient   = ApiClientFactory.getInstance().getEmployeeApiClient();
        leaveApiClient      = ApiClientFactory.getInstance().getLeaveApiClient();
        attendanceApiClient = ApiClientFactory.getInstance().getAttendanceApiClient();
    }

    // links this view to its parent shell
    public void setMainController(HrMainController mainController) {
        this.mainController = mainController;
    }

    // refreshes all metrics and charts; called by the shell on navigation
    public void refresh() {
        refreshWorkforceMetrics();
        refreshAttendanceMetrics();
        refreshLeaveMetrics();
        refreshAttendanceTrendChart();
        refreshDepartmentDistributionChart();
        refreshAttendanceSummaryChart();
        refreshLeaveSummary();
    }

    // ----- Metric cards -----

    // loads total and active employee counts
    private void refreshWorkforceMetrics() {
        try {
            List<EmployeeSummaryDTO> summaries = employeeApiClient.getAllEmployeeSummaries();
            long activeCount = summaries.stream().filter(EmployeeSummaryDTO::isActive).count();
            setText(totalEmployeesLabel,  String.valueOf(summaries.size()));
            setText(activeEmployeesLabel, String.valueOf(activeCount));
        } catch (Exception e) {
            setText(totalEmployeesLabel,  "—");
            setText(activeEmployeesLabel, "—");
        }
    }

    // counts employees present or late today from today's attendance records
    private void refreshAttendanceMetrics() {
        try {
            LocalDate today = LocalDate.now();
            long present = attendanceApiClient.getAttendanceByDateRange(today, today).stream()
                    .filter(a -> a.isPresent() || a.isLate())
                    .count();
            setText(presentTodayLabel, String.valueOf(present));
        } catch (Exception e) {
            setText(presentTodayLabel, "—");
        }
    }

    // loads on-leave-today and pending leave counts
    private void refreshLeaveMetrics() {
        try {
            LeaveSummaryDTO summary = leaveApiClient.getSummary();
            setText(onLeaveTodayLabel, String.valueOf(summary.approvedTodayCount()));
            setText(pendingLeaveLabel, String.valueOf(summary.pendingCount()));
        } catch (Exception e) {
            setText(onLeaveTodayLabel, "—");
            setText(pendingLeaveLabel, "—");
        }
    }

    // ----- Charts -----

    // plots present-or-late counts per month over the trailing 12 months
    private void refreshAttendanceTrendChart() {
        if (attendanceTrendChart == null) return;

        try {
            LocalDate today = LocalDate.now();
            LocalDate start = today.minusMonths(11).withDayOfMonth(1);

            // Seed all 12 months so the line is continuous even with sparse data.
            Map<LocalDate, Integer> byMonth = new LinkedHashMap<>();
            for (int i = 0; i < 12; i++) {
                byMonth.put(start.plusMonths(i), 0);
            }

            for (AttendanceDTO a : attendanceApiClient.getAttendanceByDateRange(start, today)) {
                if (!(a.isPresent() || a.isLate())) continue;
                LocalDate monthKey = a.getWorkDate().withDayOfMonth(1);
                byMonth.computeIfPresent(monthKey, (k, v) -> v + 1);
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Present");
            byMonth.forEach((month, count) -> addPoint(series, monthLabel(month), count));
            attendanceTrendChart.getData().setAll(List.of(series));

        } catch (Exception e) {
            attendanceTrendChart.getData().clear();
        }
    }

    // plots active-employee headcount per department
    private void refreshDepartmentDistributionChart() {
        if (departmentDistributionChart == null) return;

        try {
            List<DepartmentDTO> departments = employeeApiClient.getAllDepartments();
            List<EmployeeSummaryDTO> employees = employeeApiClient.getAllEmployeeSummaries().stream()
                    .filter(EmployeeSummaryDTO::isActive)
                    .toList();

            Map<Integer, Long> countByDept = new LinkedHashMap<>();
            for (EmployeeSummaryDTO e : employees) {
                countByDept.merge(e.getDepartmentId(), 1L, Long::sum);
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Employees");
            for (DepartmentDTO dept : departments) {
                long count = countByDept.getOrDefault(dept.getDepartmentId(), 0L);
                addPoint(series, dept.getDepartmentName(), count);
            }
            departmentDistributionChart.getData().setAll(List.of(series));

        } catch (Exception e) {
            departmentDistributionChart.getData().clear();
        }
    }

    // plots present/late/absent counts per day over the trailing 15 days
    private void refreshAttendanceSummaryChart() {
        if (attendanceSummaryChart == null) return;

        try {
            LocalDate today = LocalDate.now();
            LocalDate start = today.minusDays(14);

            Map<LocalDate, int[]> byDay = new TreeMap<>();
            for (AttendanceDTO a : attendanceApiClient.getAttendanceByDateRange(start, today)) {
                int[] counts = byDay.computeIfAbsent(a.getWorkDate(), k -> new int[3]);
                if (a.isPresent())      counts[0]++;
                else if (a.isLate())    counts[1]++;
                else if (a.isAbsent())  counts[2]++;
            }

            XYChart.Series<String, Number> present = new XYChart.Series<>();
            XYChart.Series<String, Number> late    = new XYChart.Series<>();
            XYChart.Series<String, Number> absent  = new XYChart.Series<>();
            present.setName("Present");
            late.setName("Late");
            absent.setName("Absent");

            byDay.forEach((day, counts) -> {
                String label = dayLabel(day);
                addPoint(present, label, counts[0]);
                addPoint(late,    label, counts[1]);
                addPoint(absent,  label, counts[2]);
            });

            attendanceSummaryChart.getData().setAll(List.of(present, late, absent));

        } catch (Exception e) {
            attendanceSummaryChart.getData().clear();
        }
    }

    // ----- Leave summary -----

    // builds the leave summary list from approved leave usage grouped by leave type
    private void refreshLeaveSummary() {
        if (leaveSummaryList == null) return;
        leaveSummaryList.getChildren().clear();

        try {
            Map<String, Double> daysByType = new LinkedHashMap<>();
            for (int typeId : LEAVE_SUMMARY_TYPE_IDS) {
                daysByType.put(resolveLeaveTypeName(typeId), 0.0);
            }

            for (LeaveRequestRowDTO request : leaveApiClient.getAllLeaveRequests()) {
                if (request.getStatus() != RequestStatus.APPROVED) continue;
                double days = request.getLeaveHours() / 8.0;
                daysByType.computeIfPresent(request.getLeaveTypeName(), (k, v) -> v + days);
            }

            daysByType.forEach((typeName, days) ->
                    leaveSummaryList.getChildren().add(buildLeaveSummaryRow(typeName, days)));
            if (!leaveSummaryList.getChildren().isEmpty()) {
                leaveSummaryList.getChildren().get(leaveSummaryList.getChildren().size() - 1)
                        .getStyleClass().add("leave-summary-row-last");
            }

        } catch (Exception e) {
            // leave summary stays empty on service error
            logger.log(Level.WARNING, "Failed to load leave summary", e);
        }
    }

    // builds a single "Leave Type ......... N days" row
    private HBox buildLeaveSummaryRow(String name, double days) {
        HBox row = new HBox(10);
        row.getStyleClass().add("leave-summary-row");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("leave-summary-name");

        Region leader = new Region();
        leader.getStyleClass().add("leave-summary-leader");
        HBox.setHgrow(leader, Priority.ALWAYS);

        Label valueLabel = new Label(formatDays(days));
        valueLabel.getStyleClass().add("leave-summary-value");

        row.getChildren().addAll(nameLabel, leader, valueLabel);
        return row;
    }

    // ----- Helpers -----

    // adds a data point with a hover tooltip, matching the employee dashboard idiom
    private void addPoint(XYChart.Series<String, Number> series, String category, Number value) {
        XYChart.Data<String, Number> data = new XYChart.Data<>(category, value);
        series.getData().add(data);
        data.nodeProperty().addListener((obs, old, node) -> {
            if (node != null) Tooltip.install(node, new Tooltip(data.getXValue() + ": " + data.getYValue()));
        });
    }

    // resolves a leave type ID to its display name (matches the backend's LeaveService.resolveLeaveTypeName)
    private static String resolveLeaveTypeName(int leaveTypeId) {
        return switch (leaveTypeId) {
            case 1 -> "Vacation Leave";
            case 2 -> "Sick Leave";
            case 3 -> "Emergency Leave";
            case 4 -> "Maternity Leave";
            case 5 -> "Paternity Leave";
            case 6 -> "Solo Parent Leave";
            case 7 -> "Unpaid Leave";
            default -> "Leave Type " + leaveTypeId;
        };
    }

    // "Jun", "Jul" ... for trend chart categories
    private String monthLabel(LocalDate month) {
        return month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    // "Jun 15" for attendance summary categories
    private String dayLabel(LocalDate day) {
        return day.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + day.getDayOfMonth();
    }

    // "1 day" / "12.5 days", trimming trailing ".0"
    private String formatDays(double days) {
        String number = (days == Math.floor(days))
                ? String.valueOf((long) days)
                : String.format(Locale.ENGLISH, "%.1f", days);
        String unit = (days == 1.0) ? " day" : " days";
        return number + unit;
    }

    // sets label text when the label is present
    private void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }
}
