package motorph.ui.employee;

import motorph.api.dto.payroll.PayrunDTO;
import motorph.api.dto.payroll.PayslipSummaryDTO;
import motorph.domain.value.RequestStatus;
import motorph.api.ApiClientFactory;
import motorph.api.AttendanceApiClient;
import motorph.api.LeaveApiClient;
import motorph.api.PayrunApiClient;
import motorph.api.PayslipApiClient;
import motorph.api.dto.attendance.AttendanceDTO;
import motorph.api.dto.leave.LeaveCreditRowDTO;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiFormatter;

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
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Displays the employee dashboard: metrics, charts, and quick-action cards, all backed by real data for the logged-in employee. */
public class EmployeeDashboardController {

    private static final Logger logger = Logger.getLogger(EmployeeDashboardController.class.getName());

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);
    private static final DateTimeFormatter PERIOD_FMT_YEAR = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
    private static final int VACATION_LEAVE_TYPE_ID = 1;
    private static final int ATTENDANCE_WINDOW_DAYS = 30;

    private LeaveApiClient leaveApiClient;
    private PayslipApiClient payslipApiClient;
    private PayrunApiClient payrunApiClient;
    private AttendanceApiClient attendanceApiClient;
    private Runnable onNavigateToPayslip;

    // Metric cards
    @FXML private Label dashboardNetPayLabel;
    @FXML private Label dashboardAttendanceRateLabel;
    @FXML private Label dashboardDaysWorkedLabel;
    @FXML private Label dashboardLeaveCreditsLabel;
    @FXML private Label dashboardTaskPercentLabel;

    // Charts
    @FXML private LineChart<String, Number> payrollTrendChart;
    @FXML private BarChart<String, Number> earningsBreakdownChart;
    @FXML private BarChart<String, Number> attendanceSummaryChart;

    // Leave summary
    @FXML private VBox leaveSummaryList;

    // Bottom cards
    @FXML private Label upcomingPaydayLabel;
    @FXML private Label upcomingPaydayCountdownLabel;
    @FXML private Label recentPayslipPeriodLabel;
    @FXML private Label payrollStatusLabel;
    @FXML private Label payrollStatusDetailLabel;

    @FXML private Label dashboardMessageLabel;

    @FXML
    private void initialize() {
        ApiClientFactory factory = ApiClientFactory.getInstance();
        payslipApiClient    = factory.getPayslipApiClient();
        payrunApiClient      = factory.getPayrunApiClient();
        leaveApiClient      = factory.getLeaveApiClient();
        attendanceApiClient = factory.getAttendanceApiClient();
    }

    // stores a callback invoked when the user clicks "View Payslip"
    public void setOnNavigateToPayslip(Runnable callback) {
        this.onNavigateToPayslip = callback;
    }

    // refreshes all dashboard data for the current session's employee
    public void refresh() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            return;
        }

        // refreshes each widget independently so one failure does not break the dashboard
        refreshWidget("net pay", this::refreshNetPay);
        refreshWidget("pending requests", this::refreshPendingRequests);
        refreshWidget("leave credits", this::refreshLeaveCredits);
        refreshWidget("attendance metrics", () -> refreshAttendanceMetrics(employeeId));
        refreshWidget("payroll trend chart", this::refreshPayrollTrendChart);
        refreshWidget("earnings breakdown chart", this::refreshEarningsBreakdownChart);
        refreshWidget("attendance summary chart", () -> refreshAttendanceSummaryChart(employeeId));
        refreshWidget("leave summary", () -> refreshLeaveSummary(employeeId));
        refreshWidget("bottom cards", this::refreshBottomCards);
    }

    // runs a single widget refresh, logging any failure instead of swallowing it
    private void refreshWidget(String widgetName, Runnable refreshAction) {
        try {
            refreshAction.run();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to refresh dashboard widget: " + widgetName, e);
        }
    }

    // ----- Metric cards -----

    // updates the net pay label from the latest persisted payroll result
    public void refreshNetPay() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            setText(dashboardNetPayLabel, "PHP 0.00");
            return;
        }
        try {
            Optional<PayslipSummaryDTO> latest = latestResult();
            setText(dashboardNetPayLabel,
                    latest.map(r -> UiFormatter.formatPeso(r.getNetPay())).orElse("PHP 0.00"));
        } catch (Throwable t) {
            setText(dashboardNetPayLabel, "PHP 0.00");
        }
    }

    // updates the pending request count for the current employee
    private void refreshPendingRequests() {
        try {
            long pendingCount = leaveApiClient.getOwnLeaveRequests()
                    .stream()
                    .filter(request -> request.getStatus() == RequestStatus.PENDING)
                    .count();
            setText(dashboardTaskPercentLabel, String.valueOf(pendingCount));
        } catch (Exception e) {
            setText(dashboardTaskPercentLabel, "0");
        }
    }

    // updates the leave credits label; value is pushed from the leave controller
    public void setLeaveCreditsDisplay(String text) {
        if (dashboardLeaveCreditsLabel != null) dashboardLeaveCreditsLabel.setText(text);
    }

    // updates available vacation leave credits for the current employee
    private void refreshLeaveCredits() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            setText(dashboardLeaveCreditsLabel, "0");
            return;
        }
        try {
            double credits = leaveApiClient.getOwnLeaveCredits().stream()
                    .filter(row -> row.getLeaveTypeId() == VACATION_LEAVE_TYPE_ID)
                    .mapToDouble(LeaveCreditRowDTO::getRemainingCredits)
                    .findFirst()
                    .orElse(0.0);
            setText(dashboardLeaveCreditsLabel, String.format(Locale.ENGLISH, "%.0f", credits));
        } catch (Throwable t) {
            setText(dashboardLeaveCreditsLabel, "0");
        }
    }

    // computes attendance rate and days worked over the trailing window from real attendance logs
    private void refreshAttendanceMetrics(int employeeId) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate start = today.minusDays(ATTENDANCE_WINDOW_DAYS - 1);
            List<AttendanceDTO> logs = attendanceApiClient.getOwnAttendanceRange(start, today);

            long total   = logs.size();
            long worked  = logs.stream().filter(a -> a.isPresent() || a.isLate()).count();

            if (total == 0) {
                setText(dashboardAttendanceRateLabel, "—");
                setText(dashboardDaysWorkedLabel, "0 / 0");
                return;
            }

            double rate = (worked * 100.0) / total;
            setText(dashboardAttendanceRateLabel, String.format(Locale.ENGLISH, "%.2f%%", rate));
            setText(dashboardDaysWorkedLabel, worked + " / " + total);
        } catch (Exception e) {
            setText(dashboardAttendanceRateLabel, "—");
            setText(dashboardDaysWorkedLabel, "—");
        }
    }

    // ----- Charts -----

    // plots net pay across the employee's payroll history, oldest to newest
    private void refreshPayrollTrendChart() {
        if (payrollTrendChart == null) return;
        try {
            List<PayslipSummaryDTO> results = payslipApiClient.getOwnPayslips().stream()
                    .sorted(Comparator.comparing(PayslipSummaryDTO::getPeriodStart))
                    .toList();

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Net Pay (PHP)");
            for (PayslipSummaryDTO r : results) {
                addPoint(series, PERIOD_FMT.format(r.getPeriodEnd()), r.getNetPay());
            }
            payrollTrendChart.getData().setAll(List.of(series));
        } catch (Exception e) {
            payrollTrendChart.getData().clear();
        }
    }

    // plots gross / deductions / net for the latest payroll result
    private void refreshEarningsBreakdownChart() {
        if (earningsBreakdownChart == null) return;
        try {
            Optional<PayslipSummaryDTO> latest = latestResult();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Amount (PHP)");
            if (latest.isPresent()) {
                PayslipSummaryDTO r = latest.get();
                addPoint(series, "Gross Pay",  r.getGrossPay());
                addPoint(series, "Deductions", r.getDeductions());
                addPoint(series, "Net Pay",    r.getNetPay());
            }
            earningsBreakdownChart.getData().setAll(List.of(series));
        } catch (Exception e) {
            earningsBreakdownChart.getData().clear();
        }
    }

    // plots present/late/absent per day over the trailing window from real attendance logs
    private void refreshAttendanceSummaryChart(int employeeId) {
        if (attendanceSummaryChart == null) return;
        try {
            LocalDate today = LocalDate.now();
            LocalDate start = today.minusDays(ATTENDANCE_WINDOW_DAYS - 1);
            List<AttendanceDTO> logs = attendanceApiClient.getOwnAttendanceRange(start, today);

            // For a single employee each day is at most one record; counts are 0/1 per status.
            Map<LocalDate, int[]> byDay = new TreeMap<>();
            for (AttendanceDTO a : logs) {
                int[] counts = byDay.computeIfAbsent(a.getWorkDate(), k -> new int[3]);
                if (a.isPresent())     counts[0]++;
                else if (a.isLate())   counts[1]++;
                else if (a.isAbsent()) counts[2]++;
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

    // builds the leave summary list from this employee's remaining leave credits per type
    private void refreshLeaveSummary(int employeeId) {
        if (leaveSummaryList == null) return;
        leaveSummaryList.getChildren().clear();
        try {
            List<LeaveCreditRowDTO> credits = leaveApiClient.getOwnLeaveCredits();
            for (LeaveCreditRowDTO row : credits) {
                leaveSummaryList.getChildren().add(
                        buildLeaveSummaryRow(row.getLeaveTypeName(), row.getRemainingCredits()));
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

    // ----- Bottom cards -----

    // populates upcoming payday, recent payslip period, and payroll status from real payrun/result data
    private void refreshBottomCards() {
        List<PayrunDTO> payruns;
        try {
            payruns = payrunApiClient.getAllPayruns();
        } catch (Exception e) {
            payruns = List.of();
        }

        refreshUpcomingPayday(payruns);
        refreshRecentPayslipAndStatus(payruns);
    }

    // shows the nearest future pay date from the payrun table
    private void refreshUpcomingPayday(List<PayrunDTO> payruns) {
        try {
            LocalDate today = LocalDate.now();
            Optional<PayrunDTO> next = payruns.stream()
                    .filter(p -> p.getPayDate() != null && !p.getPayDate().isBefore(today))
                    .min(Comparator.comparing(PayrunDTO::getPayDate));

            if (next.isEmpty()) {
                setText(upcomingPaydayLabel, "No scheduled payday");
                setText(upcomingPaydayCountdownLabel, "");
                return;
            }

            LocalDate payDate = next.get().getPayDate();
            setText(upcomingPaydayLabel, PERIOD_FMT_YEAR.format(payDate));
            long days = ChronoUnit.DAYS.between(today, payDate);
            setText(upcomingPaydayCountdownLabel,
                    days == 0 ? "Today" : ("In " + days + (days == 1 ? " day" : " days")));
        } catch (Exception e) {
            setText(upcomingPaydayLabel, "—");
            setText(upcomingPaydayCountdownLabel, "");
        }
    }

    // shows the latest payslip period and derives payroll status for the latest payrun
    private void refreshRecentPayslipAndStatus(List<PayrunDTO> payruns) {
        Optional<PayslipSummaryDTO> latest = Optional.empty();
        try {
            latest = latestResult();
        } catch (Exception e) {
            // leaves latest empty so the labels fall back to placeholders
            logger.log(Level.FINE, "Could not load latest payroll result", e);
        }

        // Recent payslip period label
        if (latest.isPresent()) {
            setText(recentPayslipPeriodLabel, periodLabel(latest.get().getPeriodStart(), latest.get().getPeriodEnd()));
        } else {
            setText(recentPayslipPeriodLabel, "No payslip yet");
        }

        // Payroll status: derive from the latest payrun and whether this employee has a result for it
        if (payruns.isEmpty()) {
            setText(payrollStatusLabel, "No Payroll Data");
            setText(payrollStatusDetailLabel, "There is no payroll period available yet.");
            return;
        }

        PayrunDTO latestPayrun = payruns.stream()
                .max(Comparator.comparing(PayrunDTO::getEndDate))
                .orElse(null);

        if (latestPayrun == null) {
            setText(payrollStatusLabel, "No Payroll Data");
            setText(payrollStatusDetailLabel, "There is no payroll period available yet.");
            return;
        }

        boolean processed = latest.isPresent() && latest.get().getPayrunId() == latestPayrun.getPayrunId();

        if (processed) {
            setText(payrollStatusLabel, "Processed");
            setText(payrollStatusDetailLabel,
                    "Your payroll for " + periodLabel(latestPayrun.getStartDate(), latestPayrun.getEndDate()) + " has been processed.");
        } else {
            setText(payrollStatusLabel, "Pending");
            setText(payrollStatusDetailLabel,
                    "Your payroll for " + periodLabel(latestPayrun.getStartDate(), latestPayrun.getEndDate()) + " is still being processed.");
        }
    }

    @FXML
    private void navigateToPayslip() {
        if (onNavigateToPayslip != null) onNavigateToPayslip.run();
    }

    // ----- Helpers -----

    // returns the latest payroll result for the current employee - the backend
    // already returns the caller's own payslips sorted newest-first
    private Optional<PayslipSummaryDTO> latestResult() {
        List<PayslipSummaryDTO> own = payslipApiClient.getOwnPayslips();
        return own.isEmpty() ? Optional.empty() : Optional.of(own.get(0));
    }

    // "May 16 – May 31, 2026" for a payroll period
    private String periodLabel(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return "—";
        }
        return PERIOD_FMT.format(start) + " – " + PERIOD_FMT_YEAR.format(end);
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

    // adds a data point with a hover tooltip, matching the existing chart idiom
    private void addPoint(XYChart.Series<String, Number> series, String category, Number value) {
        XYChart.Data<String, Number> data = new XYChart.Data<>(category, value);
        series.getData().add(data);
        data.nodeProperty().addListener((obs, old, node) -> {
            if (node != null) Tooltip.install(node, new Tooltip(data.getXValue() + ": " + data.getYValue()));
        });
    }

    // sets label text when the label is present
    private void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }
}
