package motorph.ui.employee;

import motorph.application.dto.leave.LeaveCreditRowDTO;
import motorph.application.service.AttendanceService;
import motorph.application.service.LeaveService;
import motorph.application.service.PayrollService;
import motorph.domain.model.Attendance;
import motorph.domain.model.PayrollResult;
import motorph.domain.model.Payrun;
import motorph.domain.value.RequestStatus;
import motorph.ui.factory.EmployeeUIServiceFactory;
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

    private LeaveService leaveService;
    private PayrollService payrollService;
    private AttendanceService attendanceService;
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
        payrollService    = EmployeeUIServiceFactory.getInstance().getPayrollService();
        leaveService      = EmployeeUIServiceFactory.getInstance().getLeaveService();
        attendanceService = EmployeeUIServiceFactory.getInstance().getAttendanceService();
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
        refreshWidget("payroll trend chart", () -> refreshPayrollTrendChart(employeeId));
        refreshWidget("earnings breakdown chart", () -> refreshEarningsBreakdownChart(employeeId));
        refreshWidget("attendance summary chart", () -> refreshAttendanceSummaryChart(employeeId));
        refreshWidget("leave summary", () -> refreshLeaveSummary(employeeId));
        refreshWidget("bottom cards", () -> refreshBottomCards(employeeId));
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
            Optional<PayrollResult> latest = latestResult(employeeId);
            setText(dashboardNetPayLabel,
                    latest.map(r -> UiFormatter.formatPeso(r.getNetPay())).orElse("PHP 0.00"));
        } catch (Throwable t) {
            setText(dashboardNetPayLabel, "PHP 0.00");
        }
    }

    // updates the pending request count for the current employee
    private void refreshPendingRequests() {
        int employeeId = CurrentUserSession.getEmployeeId();
        try {
            long pendingCount = leaveService.getLeaveRequestsByEmployeeId(employeeId)
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
            double credits = leaveService.getRemainingLeaveCredits(
                    employeeId, VACATION_LEAVE_TYPE_ID, LocalDate.now().getYear());
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
            List<Attendance> logs = attendanceService.getAttendanceByEmployeeAndDateRange(employeeId, start, today);

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
    private void refreshPayrollTrendChart(int employeeId) {
        if (payrollTrendChart == null) return;
        try {
            List<PayrollResult> results = payrollService.getPayrollResultsByEmployee(employeeId).stream()
                    .sorted(Comparator.comparing(PayrollResult::getGeneratedAt))
                    .toList();

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Net Pay (PHP)");
            for (PayrollResult r : results) {
                addPoint(series, periodLabelForResult(r), r.getNetPay());
            }
            payrollTrendChart.getData().setAll(List.of(series));
        } catch (Exception e) {
            payrollTrendChart.getData().clear();
        }
    }

    // plots gross / deductions / net for the latest payroll result
    private void refreshEarningsBreakdownChart(int employeeId) {
        if (earningsBreakdownChart == null) return;
        try {
            Optional<PayrollResult> latest = latestResult(employeeId);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Amount (PHP)");
            if (latest.isPresent()) {
                PayrollResult r = latest.get();
                addPoint(series, "Gross Pay",  r.getGrossPay());
                addPoint(series, "Deductions", r.getTotalDeductions());
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
            List<Attendance> logs = attendanceService.getAttendanceByEmployeeAndDateRange(employeeId, start, today);

            // For a single employee each day is at most one record; counts are 0/1 per status.
            Map<LocalDate, int[]> byDay = new TreeMap<>();
            for (Attendance a : logs) {
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
            List<LeaveCreditRowDTO> credits = leaveService.getLeaveCreditsForEmployee(employeeId);
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
    private void refreshBottomCards(int employeeId) {
        List<Payrun> payruns;
        try {
            payruns = payrollService.getAllPayruns();
        } catch (Exception e) {
            payruns = List.of();
        }

        refreshUpcomingPayday(payruns);
        refreshRecentPayslipAndStatus(employeeId, payruns);
    }

    // shows the nearest future pay date from the payrun table
    private void refreshUpcomingPayday(List<Payrun> payruns) {
        try {
            LocalDate today = LocalDate.now();
            Optional<Payrun> next = payruns.stream()
                    .filter(p -> p.getPayDate() != null && !p.getPayDate().isBefore(today))
                    .min(Comparator.comparing(Payrun::getPayDate));

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
    private void refreshRecentPayslipAndStatus(int employeeId, List<Payrun> payruns) {
        Optional<PayrollResult> latest = Optional.empty();
        try {
            latest = latestResult(employeeId);
        } catch (Exception e) {
            // leaves latest empty so the labels fall back to placeholders
            logger.log(Level.FINE, "Could not load latest payroll result", e);
        }

        // Recent payslip period label
        if (latest.isPresent()) {
            try {
                Payrun payrun = payrollService.getPayrun(latest.get().getPayrunId());
                setText(recentPayslipPeriodLabel, periodLabel(payrun));
            } catch (Exception e) {
                setText(recentPayslipPeriodLabel, "Latest payslip");
            }
        } else {
            setText(recentPayslipPeriodLabel, "No payslip yet");
        }

        // Payroll status: derive from the latest payrun and whether this employee has a result for it
        if (payruns.isEmpty()) {
            setText(payrollStatusLabel, "No Payroll Data");
            setText(payrollStatusDetailLabel, "There is no payroll period available yet.");
            return;
        }

        Payrun latestPayrun = payruns.stream()
                .max(Comparator.comparing(Payrun::getEndDate))
                .orElse(null);

        if (latestPayrun == null) {
            setText(payrollStatusLabel, "No Payroll Data");
            setText(payrollStatusDetailLabel, "There is no payroll period available yet.");
            return;
        }

        boolean processed = latest.isPresent()
                && latestPayrun.getPayrunId() != null
                && latest.get().getPayrunId() == latestPayrun.getPayrunId();

        if (processed) {
            setText(payrollStatusLabel, "Processed");
            setText(payrollStatusDetailLabel,
                    "Your payroll for " + periodLabel(latestPayrun) + " has been processed.");
        } else {
            setText(payrollStatusLabel, "Pending");
            setText(payrollStatusDetailLabel,
                    "Your payroll for " + periodLabel(latestPayrun) + " is still being processed.");
        }
    }

    @FXML
    private void navigateToPayslip() {
        if (onNavigateToPayslip != null) onNavigateToPayslip.run();
    }

    // ----- Helpers -----

    // returns the latest payroll result for the employee, by generation time
    private Optional<PayrollResult> latestResult(int employeeId) {
        return payrollService.getPayrollResultsByEmployee(employeeId).stream()
                .max(Comparator.comparing(PayrollResult::getGeneratedAt));
    }

    // "May 16 – May 31, 2026" for a payrun period
    private String periodLabel(Payrun payrun) {
        if (payrun == null || payrun.getStartDate() == null || payrun.getEndDate() == null) {
            return "—";
        }
        return PERIOD_FMT.format(payrun.getStartDate()) + " – " + PERIOD_FMT_YEAR.format(payrun.getEndDate());
    }

    // short period label for a result's payrun, used as a chart category; falls back to generation date
    private String periodLabelForResult(PayrollResult result) {
        try {
            Payrun payrun = payrollService.getPayrun(result.getPayrunId());
            if (payrun != null && payrun.getEndDate() != null) {
                return PERIOD_FMT.format(payrun.getEndDate());
            }
        } catch (Exception e) {
            // falls back to the generation date below
            logger.log(Level.FINE, "Could not resolve payrun period for chart label", e);
        }
        return result.getGeneratedAt().toLocalDate().format(PERIOD_FMT);
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
