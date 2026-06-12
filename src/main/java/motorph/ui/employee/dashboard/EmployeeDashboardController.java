package motorph.ui.employee.dashboard;

import motorph.application.service.EmployeeService;
import motorph.application.service.PayrollService;
import motorph.domain.model.Employee;
import motorph.domain.model.PayrollResult;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiFormatter;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import java.util.Comparator;
import java.util.Optional;

/** Displays the employee dashboard: metrics, charts, and quick-action cards. */
public class EmployeeDashboardController {

    private EmployeeService employeeService;
    private PayrollService payrollService;
    private Runnable onNavigateToPayslip;

    @FXML
    private Label dashboardNetPayLabel;

    @FXML
    private Label dashboardLeaveCreditsLabel;

    @FXML
    private Label dashboardTaskPercentLabel;

    @FXML
    private Label dashboardMessageLabel;

    @FXML
    private LineChart<String, Number> payrollTrendChart;

    @FXML
    private BarChart<String, Number> earningsBreakdownChart;

    @FXML
    private BarChart<String, Number> attendanceSummaryChart;

    @FXML
    private void initialize() {
        employeeService = EmployeeUIServiceFactory.getInstance().getEmployeeService();
        payrollService  = EmployeeUIServiceFactory.getInstance().getPayrollService();
        seedCharts();
    }

    // stores a callback invoked when the user clicks "View Payslip"
    public void setOnNavigateToPayslip(Runnable callback) {
        this.onNavigateToPayslip = callback;
    }

    // refreshes all dashboard data for the current session's employee
    public void refresh() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) return;

        try {
            Employee employee = employeeService.getEmployeeById(employeeId);
            if (dashboardTaskPercentLabel != null) {
                dashboardTaskPercentLabel.setText(employee.getFirstName());
            }
        } catch (Exception ignored) {
        }

        refreshNetPay();
    }

    // updates the net pay label from the latest persisted payroll result
    public void refreshNetPay() {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) {
            if (dashboardNetPayLabel != null) dashboardNetPayLabel.setText("PHP 0.00");
            return;
        }
        try {
            Optional<PayrollResult> latest = payrollService.getPayrollResultsByEmployee(employeeId)
                    .stream()
                    .max(Comparator.comparing(PayrollResult::getGeneratedAt));
            if (dashboardNetPayLabel != null) {
                dashboardNetPayLabel.setText(
                        latest.map(r -> UiFormatter.formatPeso(r.getNetPay())).orElse("PHP 0.00"));
            }
        } catch (Exception e) {
            if (dashboardNetPayLabel != null) dashboardNetPayLabel.setText("PHP 0.00");
        }
    }

    // updates the leave credits label; value is pushed from the leave controller
    public void setLeaveCreditsDisplay(String text) {
        if (dashboardLeaveCreditsLabel != null) dashboardLeaveCreditsLabel.setText(text);
    }

    @FXML
    private void navigateToPayslip() {
        if (onNavigateToPayslip != null) onNavigateToPayslip.run();
    }

    // seeds placeholder chart data until real data is loaded from the backend
    private void seedCharts() {
        if (payrollTrendChart != null) {
            payrollTrendChart.getData().setAll(series(
                    "Net Pay (PHP)",
                    new String[]{"Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar", "Apr", "May", "Jun"},
                    new double[]{27000, 34000, 29500, 27500, 12500, 29000, 31500, 28500, 23500, 26000, 24500, 23000}
            ));
        }
        if (earningsBreakdownChart != null) {
            earningsBreakdownChart.getData().setAll(series(
                    "Earnings",
                    new String[]{"Basic", "Allowances", "Overtime", "Night Diff", "Holiday", "Bonus"},
                    new double[]{17500, 6000, 3250, 1750, 1250, 750}
            ));
        }
        if (attendanceSummaryChart != null) {
            String[] dates = {"Jun 1", "Jun 3", "Jun 5", "Jun 7", "Jun 9", "Jun 11", "Jun 13", "Jun 15"};
            attendanceSummaryChart.getData().setAll(
                    series("Present", dates, new double[]{7, 9, 9, 10, 9, 6, 6, 6}),
                    series("Late",    dates, new double[]{2, 3, 6, 5, 0, 1, 0, 4}),
                    series("Absent",  dates, new double[]{0, 0, 0, 0, 0, 1, 0, 0})
            );
        }
    }

    // builds a named chart series with tooltip support
    private XYChart.Series<String, Number> series(String name, String[] categories, double[] values) {
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName(name);
        for (int i = 0; i < categories.length; i++) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(categories[i], values[i]);
            s.getData().add(data);
            data.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) Tooltip.install(node, new Tooltip(data.getXValue() + ": " + data.getYValue()));
            });
        }
        return s;
    }
}
