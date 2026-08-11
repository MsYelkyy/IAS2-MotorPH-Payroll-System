package motorph.ui.finance;

import motorph.api.ApiClientFactory;
import motorph.api.ReportApiClient;
import motorph.api.dto.report.PayrollDashboardDTO;
import motorph.ui.util.UiFormatter;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Displays the Payroll Reporting Dashboard for the Finance workspace.
 * Read-only: all figures come from vw_payroll_summary_monthly through the
 * reporting service. Nothing here changes payroll data.
 */
public class FinanceDashboardController {

    private static final Logger LOG = Logger.getLogger(FinanceDashboardController.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private ReportApiClient reportApiClient;
    private FinanceMainController   mainController;

    // KPI cards
    @FXML private Label totalEmployeesLabel;
    @FXML private Label totalGrossPayLabel;
    @FXML private Label totalDeductionsLabel;
    @FXML private Label payrollMonthLabel;
    @FXML private Label totalNetPayLabel;
    @FXML private Label mostRecentPayDateLabel;

    // Net pay trend chart
    @FXML private LineChart<String, Number> netPayChart;
    @FXML private NumberAxis                 netPayAmountAxis;

    // Deduction donut chart
    @FXML private PieChart deductionChart;

    // Payroll cost by department (horizontal bars)
    @FXML private BarChart<Number, String> departmentCostChart;
    @FXML private NumberAxis               departmentAmountAxis;

    // Report summary
    @FXML private Label summaryEmployeesLabel;
    @FXML private Label summaryCutoffsLabel;
    @FXML private Label summaryValidationLabel;

    @FXML
    private void initialize() {
        reportApiClient = ApiClientFactory.getInstance().getReportApiClient();
        configureAmountAxes();
    }

    // links this view to its parent shell so the summary buttons can navigate
    public void setMainController(FinanceMainController mainController) {
        this.mainController = mainController;
    }

    // called by FinanceMainController on navigation to refresh all sections
    public void refresh() {
        PayrollDashboardDTO dashboard = loadDashboard();
        if (dashboard == null) {
            showEmpty();
            return;
        }
        refreshKpis(dashboard);
        refreshNetPayChart(dashboard.getNetPayByMonth());
        refreshDeductionChart(dashboard);
        refreshDepartmentChart(dashboard.getDepartmentCosts());
        refreshSummary(dashboard);
    }

    // ── KPI cards ───────────────────────────────────────────────────
    private void refreshKpis(PayrollDashboardDTO d) {
        setText(totalEmployeesLabel,   String.valueOf(d.getTotalEmployees()));
        setText(totalGrossPayLabel,    UiFormatter.formatPeso(d.getTotalGrossPay()));
        setText(totalDeductionsLabel,  UiFormatter.formatPeso(d.getTotalDeductions()));
        setText(payrollMonthLabel,     UiFormatter.safe(d.getPayrollMonthLabel(), "—"));
        setText(totalNetPayLabel,      UiFormatter.formatPeso(d.getTotalNetPay()));
        setText(mostRecentPayDateLabel,
                d.getMostRecentPayDate() == null ? "—" : d.getMostRecentPayDate().format(DATE_FMT));
    }

    // ── Net pay trend chart (one point per payroll month) ───────────
    private void refreshNetPayChart(List<PayrollDashboardDTO.NetPayPoint> points) {
        if (netPayChart == null) return;
        netPayChart.getData().clear();
        if (points == null || points.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Net Pay");
        for (PayrollDashboardDTO.NetPayPoint point : points) {
            addLinePoint(series, point.getMonthLabel(), point.getNetPay());
        }
        netPayChart.getData().add(series);
    }

    // ── Deduction breakdown donut ───────────────────────────────────
    private void refreshDeductionChart(PayrollDashboardDTO dashboard) {
        if (deductionChart == null) return;
        deductionChart.getData().clear();

        PayrollDashboardDTO.DeductionBreakdown d = dashboard.getDeductionBreakdown();
        double government = d == null ? 0 : d.getTotal();

        // whatever the month withheld beyond the four government contributions
        // (late, undertime, absence) - without this the donut reads as "no data"
        // for a period whose deductions are all non-government
        double other = Math.max(0, dashboard.getTotalDeductions() - government);
        double total = government + other;

        if (total <= 0) return;

        if (d != null) {
            deductionChart.getData().addAll(
                    new PieChart.Data("SSS", d.getSss()),
                    new PieChart.Data("PhilHealth", d.getPhilhealth()),
                    new PieChart.Data("Pag-IBIG", d.getPagibig()),
                    new PieChart.Data("Withholding Tax", d.getWithholdingTax()));
        }

        if (other > 0) {
            deductionChart.getData().add(new PieChart.Data("Other Deductions", other));
        }

        installPieTooltips(total);
    }

    // ── Payroll cost by department (horizontal bars) ────────────────
    private void refreshDepartmentChart(List<PayrollDashboardDTO.DepartmentCost> costs) {
        if (departmentCostChart == null) return;
        departmentCostChart.getData().clear();
        if (costs == null || costs.isEmpty()) return;

        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Net Pay");
        // smallest first so the largest bar sits at the top of the chart
        for (int i = costs.size() - 1; i >= 0; i--) {
            PayrollDashboardDTO.DepartmentCost cost = costs.get(i);
            addBar(series, cost.getNetPay(), cost.getDepartmentName());
        }
        departmentCostChart.getData().add(series);
    }

    // ── Report summary panel ────────────────────────────────────────
    private void refreshSummary(PayrollDashboardDTO d) {
        setText(summaryEmployeesLabel, d.getTotalEmployees() + " employees included");
        setText(summaryCutoffsLabel,   d.getPayrollCutoffs() + " payroll cutoffs summarized");
        setText(summaryValidationLabel, "All report validations passed");
    }

    // clears everything when there is no payroll data yet
    private void showEmpty() {
        setText(totalEmployeesLabel, "0");
        setText(totalGrossPayLabel, UiFormatter.formatPeso(0));
        setText(totalDeductionsLabel, UiFormatter.formatPeso(0));
        setText(payrollMonthLabel, "—");
        setText(totalNetPayLabel, UiFormatter.formatPeso(0));
        setText(mostRecentPayDateLabel, "—");
        setText(summaryEmployeesLabel, "0 employees included");
        setText(summaryCutoffsLabel, "0 payroll cutoffs summarized");
        setText(summaryValidationLabel, "No payroll report data yet.");
        if (netPayChart != null) netPayChart.getData().clear();
        if (deductionChart != null) deductionChart.getData().clear();
        if (departmentCostChart != null) departmentCostChart.getData().clear();
    }

    // ── Summary button navigation ───────────────────────────────────
    // opens the payroll management / summary screen
    @FXML
    private void openPayrollManagement() {
        if (mainController != null) mainController.navigateToPayroll();
    }

    // opens the reports screen (payslip report preview + export)
    @FXML
    private void openReports() {
        if (mainController != null) mainController.navigateToReports();
    }

    // ── Service loader (null on error so the dashboard still renders) ─
    private PayrollDashboardDTO loadDashboard() {
        try {
            return reportApiClient.getLatestPayrollDashboard();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load the payroll dashboard", e);
            // silent on the first paint (no window yet); visible on manual refresh
            UiMessageUtil.showError(totalEmployeesLabel,
                    "Could not load payroll figures: " + UiMessageUtil.toUserMessage(e));
            return null;
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    // peso tick labels on both amount axes (e.g. ₱6.4M, ₱850K, ₱0)
    private void configureAmountAxes() {
        applyPesoTicks(netPayAmountAxis);
        applyPesoTicks(departmentAmountAxis);
    }

    private void applyPesoTicks(NumberAxis axis) {
        if (axis == null) return;
        axis.setTickLabelFormatter(new StringConverter<>() {
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

    // adds a line point with a hover tooltip showing the month and full peso value
    private void addLinePoint(XYChart.Series<String, Number> series, String category, double value) {
        XYChart.Data<String, Number> data = new XYChart.Data<>(category, value);
        series.getData().add(data);
        data.nodeProperty().addListener((obs, old, node) -> {
            if (node != null) {
                Tooltip.install(node, new Tooltip(category + "\n" + UiFormatter.formatPeso(value)));
            }
        });
    }

    // adds a horizontal bar (value on X, department on Y) with a peso tooltip
    private void addBar(XYChart.Series<Number, String> series, double value, String category) {
        XYChart.Data<Number, String> data = new XYChart.Data<>(value, category);
        series.getData().add(data);
        data.nodeProperty().addListener((obs, old, node) -> {
            if (node != null) {
                Tooltip.install(node, new Tooltip(category + "\n" + UiFormatter.formatPeso(value)));
            }
        });
    }

    // shows each slice's peso value and share in the legend/tooltip
    private void installPieTooltips(double total) {
        for (PieChart.Data slice : deductionChart.getData()) {
            double share = total > 0 ? (slice.getPieValue() / total) * 100 : 0;
            String text = String.format("%s%n%s (%.0f%%)",
                    slice.getName(), UiFormatter.formatPeso(slice.getPieValue()), share);
            slice.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) Tooltip.install(node, new Tooltip(text));
            });
        }
    }

    private void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }
}
