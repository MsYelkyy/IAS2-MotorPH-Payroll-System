package motorph.ui.finance;

import motorph.application.dto.payroll.CompletedPayrunSummaryDTO;
import motorph.application.dto.payroll.PendingPayrunSummaryDTO;
import motorph.application.service.PayrollService;
import motorph.ui.factory.FinanceUIServiceFactory;
import motorph.ui.util.UiFormatter;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Displays finance dashboard metrics, a net-pay trend chart, and a payroll summary.
 * All values are sourced from the latest completed/pending payruns in the database —
 * the chart plots whatever real payroll history exists (no hard-coded sample data).
 */
public class FinanceDashboardController {

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter POINT_FMT = DateTimeFormatter.ofPattern("MMM d");
    private static final int CHART_PAYRUN_LIMIT = 6;

    private PayrollService        payrollService;
    private FinanceMainController  mainController;

    // Row 1 — metric cards
    @FXML private Label pendingPayrunsLabel;
    @FXML private Label readyEmployeesLabel;
    @FXML private Label completedPayrunsLabel;
    @FXML private Label upcomingPayDateLabel;

    // Row 2 — summary cards
    @FXML private Label employeesPaidLabel;
    @FXML private Label totalNetPayLabel;

    // Row 3 — net-pay trend chart
    @FXML private LineChart<String, Number> netPayChart;
    @FXML private NumberAxis                 netPayAmountAxis;

    // Row 3 — payroll summary card
    @FXML private Label summaryNetPayLabel;
    @FXML private Label summaryDescriptionLabel;

    @FXML
    private void initialize() {
        payrollService = FinanceUIServiceFactory.getInstance().getPayrollService();
        configureChartAxis();
    }

    // links this view to its parent shell so the summary buttons can navigate
    public void setMainController(FinanceMainController mainController) {
        this.mainController = mainController;
    }

    // called by FinanceMainController on navigation to refresh all sections
    public void refresh() {
        List<PendingPayrunSummaryDTO> pending = loadPending();
        List<CompletedPayrunSummaryDTO> completed = loadCompleted();
        refreshMetrics(pending, completed);
        refreshChart(completed);
        refreshSummary(completed);
    }

    // ── Metric cards ────────────────────────────────────────────────
    private void refreshMetrics(List<PendingPayrunSummaryDTO> pending,
                                List<CompletedPayrunSummaryDTO> completed) {
        int readyEmployees = pending.stream().mapToInt(PendingPayrunSummaryDTO::getReadyEmployees).sum();
        int employeesPaid  = completed.stream().mapToInt(CompletedPayrunSummaryDTO::getEmployeesPaid).sum();
        double totalNet    = completed.stream().mapToDouble(CompletedPayrunSummaryDTO::getTotalNetPay).sum();

        setText(pendingPayrunsLabel,   String.valueOf(pending.size()));
        setText(readyEmployeesLabel,   String.valueOf(readyEmployees));
        setText(completedPayrunsLabel, String.valueOf(completed.size()));
        setText(upcomingPayDateLabel,  nextPayDate(pending));
        setText(employeesPaidLabel,    String.valueOf(employeesPaid));
        setText(totalNetPayLabel,      UiFormatter.formatPeso(totalNet));
    }

    // ── Net-pay trend chart ─────────────────────────────────────────
    // Plots the latest completed payruns (oldest → newest) using each payrun's
    // pay date as the category and its total net pay as the value.
    private void refreshChart(List<CompletedPayrunSummaryDTO> completed) {
        if (netPayChart == null) return;
        netPayChart.getData().clear();

        List<CompletedPayrunSummaryDTO> latest = latestChronological(completed);
        if (latest.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Net Pay");
        for (CompletedPayrunSummaryDTO payrun : latest) {
            addPoint(series, pointLabel(payrun), payrun.getTotalNetPay());
        }
        netPayChart.getData().add(series);
    }

    // returns the most recent payruns sorted oldest → newest for left-to-right plotting
    private List<CompletedPayrunSummaryDTO> latestChronological(List<CompletedPayrunSummaryDTO> completed) {
        List<CompletedPayrunSummaryDTO> sorted = completed.stream()
                .sorted(Comparator.comparing(this::sortDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        int from = Math.max(0, sorted.size() - CHART_PAYRUN_LIMIT);
        return sorted.subList(from, sorted.size());
    }

    // ── Payroll summary card ────────────────────────────────────────
    private void refreshSummary(List<CompletedPayrunSummaryDTO> completed) {
        double totalNet = completed.stream().mapToDouble(CompletedPayrunSummaryDTO::getTotalNetPay).sum();
        setText(summaryNetPayLabel, UiFormatter.formatPeso(totalNet));

        completed.stream()
                .max(Comparator.comparing(this::sortDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .ifPresentOrElse(
                        latest -> setText(summaryDescriptionLabel, String.format(
                                "Latest payrun %d generated payroll results for %d employees.",
                                latest.getPayrunId(), latest.getEmployeesPaid())),
                        () -> setText(summaryDescriptionLabel, "No completed payruns yet."));
    }

    // ── Summary button navigation ───────────────────────────────────
    @FXML
    private void openPayrollManagement() {
        if (mainController != null) mainController.navigateToPayroll();
    }

    @FXML
    private void openReports() {
        if (mainController != null) mainController.navigateToReports();
    }

    // ── Service loaders (empty on error so the dashboard still renders) ─
    private List<PendingPayrunSummaryDTO> loadPending() {
        try {
            return payrollService.getPendingPayruns();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<CompletedPayrunSummaryDTO> loadCompleted() {
        try {
            return payrollService.getCompletedPayruns();
        } catch (Exception e) {
            return List.of();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    // shows peso amounts compactly on the Y-axis (e.g. ₱6.4M, ₱850K, ₱0)
    private void configureChartAxis() {
        if (netPayAmountAxis == null) return;
        netPayAmountAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return compactPeso(value == null ? 0 : value.doubleValue());
            }

            @Override
            public Number fromString(String text) {
                return 0;
            }
        });
    }

    // "₱6.4M" / "₱850K" / "₱0"
    private String compactPeso(double amount) {
        if (amount >= 1_000_000) return String.format("₱%.1fM", amount / 1_000_000);
        if (amount >= 1_000)     return String.format("₱%.0fK", amount / 1_000);
        return String.format("₱%.0f", amount);
    }

    // adds a chart point with a hover tooltip showing the period and full peso value
    private void addPoint(XYChart.Series<String, Number> series, String category, double value) {
        XYChart.Data<String, Number> data = new XYChart.Data<>(category, value);
        series.getData().add(data);
        data.nodeProperty().addListener((obs, old, node) -> {
            if (node != null) {
                Tooltip.install(node, new Tooltip(category + "\n" + UiFormatter.formatPeso(value)));
            }
        });
    }

    // X-axis category: prefer the pay date, fall back to the period end month
    private String pointLabel(CompletedPayrunSummaryDTO payrun) {
        if (payrun.getPayDate() != null) return payrun.getPayDate().format(POINT_FMT);
        if (payrun.getEndDate() != null) return payrun.getEndDate().format(POINT_FMT);
        return "Payrun " + payrun.getPayrunId();
    }

    // chronological sort key: pay date, then period end, then period start
    private LocalDate sortDate(CompletedPayrunSummaryDTO payrun) {
        if (payrun.getPayDate() != null)   return payrun.getPayDate();
        if (payrun.getEndDate() != null)   return payrun.getEndDate();
        return payrun.getStartDate();
    }

    // returns the earliest pending pay date, or a placeholder when none
    private String nextPayDate(List<PendingPayrunSummaryDTO> pending) {
        return pending.stream()
                .map(PendingPayrunSummaryDTO::getPayDate)
                .filter(date -> date != null)
                .min(LocalDate::compareTo)
                .map(date -> date.format(DATE_FMT))
                .orElse("—");
    }

    private void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }
}
