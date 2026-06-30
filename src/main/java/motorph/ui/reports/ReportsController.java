package motorph.ui.reports;

import motorph.application.exception.ValidationException;
import motorph.application.service.EmployeeService;
import motorph.application.service.PayrollService;
import motorph.application.service.report.AttendanceReportService;
import motorph.application.service.report.AuditReportService;
import motorph.application.service.report.PayrollReportService;
import motorph.application.service.report.TicketReportService;
import motorph.domain.model.Department;
import motorph.domain.model.Employee;
import motorph.domain.model.Payrun;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.util.DatePickerInputHelper;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Shared Reports module: role-neutral, consumed by Admin, IT, and future HR/Finance shells. */
public class ReportsController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private static final String DR_LATEST    = "Latest Available";
    private static final String DR_THIS_MONTH = "This Month";
    private static final String DR_THIS_YEAR  = "This Year";
    private static final String DR_CUSTOM     = "Custom";

    // --- Report type enum -------------------------------------------------

    private enum ReportType {
        ATTENDANCE("Attendance Report"),
        EMPLOYEE_TIMECARD("Employee Timecard Report"),
        ORG_PAYROLL("Payroll Summary Report"),
        PAYROLL_SUMMARY("Department Payroll Report"),
        PAYSLIP("Payslip"),
        AUDIT_LOG("Audit Trail Report"),
        SUPPORT_TICKET("Support Ticket Report");

        private final String label;

        ReportType(String label) { this.label = label; }

        @Override
        public String toString() { return label; }
    }

    // --- FXML controls ----------------------------------------------------

    @FXML private Label moduleMessageLabel;

    @FXML private ComboBox<ReportType> reportTypeCombo;

    // Department filter
    @FXML private VBox departmentRow;
    @FXML private ComboBox<Department> departmentCombo;

    // Employee filter
    @FXML private VBox employeeRow;
    @FXML private ComboBox<Employee> employeeCombo;

    // Payrun filter
    @FXML private VBox payrunRow;
    @FXML private ComboBox<Payrun> payrunCombo;

    // Attendance status filter
    @FXML private VBox statusRow;
    @FXML private ComboBox<String> statusCombo;

    // Audit log action filter
    @FXML private VBox actionRow;
    @FXML private ComboBox<String> actionCombo;

    // Support ticket filters
    @FXML private VBox categoryRow;
    @FXML private ComboBox<String> categoryCombo;

    @FXML private VBox routingTeamRow;
    @FXML private ComboBox<String> routingTeamCombo;

    @FXML private VBox ticketStatusRow;
    @FXML private ComboBox<String> ticketStatusCombo;

    // Date range slot; disabled for reports that do not use dates.
    @FXML private VBox dateRangeSection;
    @FXML private ComboBox<String> dateRangeCombo;

    // Custom date pickers; kept in the grid and enabled only for DR_CUSTOM.
    @FXML private VBox datePickerRow;
    @FXML private DatePicker dateFromPicker;
    @FXML private DatePicker dateToPicker;

    @FXML private Button previewButton;
    @FXML private Button exportButton;

    // --- Services ---------------------------------------------------------

    private EmployeeService          employeeService;
    private PayrollService           payrollService;
    private AttendanceReportService  attendanceReportService;
    private AuditReportService       auditReportService;
    private PayrollReportService     payrollReportService;
    private TicketReportService      ticketReportService;

    // Latest payrun with results; resolved once on init, used for "Latest Available".
    private Payrun latestPayrun;
    // All payruns with results (DESC by start_date)
    private List<Payrun> allPayrunsWithResults;

    // --- Lifecycle --------------------------------------------------------

    @FXML
    private void initialize() {
        EmployeeUIServiceFactory factory = EmployeeUIServiceFactory.getInstance();
        employeeService = factory.getEmployeeService();
        payrollService  = factory.getPayrollService();

        attendanceReportService = factory.getAttendanceReportService();
        auditReportService      = factory.getAuditReportService();
        payrollReportService    = factory.getPayrollReportService();
        ticketReportService     = factory.getTicketReportService();

        loadPayrunCache();
        setupReportTypeCombo();
        setupDepartmentCombo();
        setupEmployeeCombo();
        setupPayrunCombo();
        setupStaticCombos();
        setupDateRangeCombo();

        DatePickerInputHelper.enableMaskedInput(dateFromPicker);
        DatePickerInputHelper.enableMaskedInput(dateToPicker);

        reportTypeCombo.setValue(ReportType.ATTENDANCE);
        configureFilters();

        UiMessageUtil.clearMessage(moduleMessageLabel);
    }

    public void refresh() {
        UiMessageUtil.clearMessage(moduleMessageLabel);
    }

    // --- Data loading -----------------------------------------------------

    private void loadPayrunCache() {
        try {
            allPayrunsWithResults = payrollService.getPayrunsWithPayrollData();
            latestPayrun = (allPayrunsWithResults != null && !allPayrunsWithResults.isEmpty())
                    ? allPayrunsWithResults.get(0)
                    : null;
        } catch (Exception ex) {
            allPayrunsWithResults = List.of();
            latestPayrun = null;
        }
    }

    // --- Setup helpers ----------------------------------------------------

    private void setupReportTypeCombo() {
        reportTypeCombo.getItems().setAll(ReportType.values());
        reportTypeCombo.setOnAction(e -> configureFilters());
    }

    private void setupDepartmentCombo() {
        departmentCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Department d) {
                return d == null ? "All Departments" : d.getDepartmentName();
            }
            @Override public Department fromString(String s) { return null; }
        });

        try {
            List<Department> departments = employeeService.getAllDepartments();
            departmentCombo.getItems().add(null);
            departmentCombo.getItems().addAll(departments);
        } catch (Exception ex) {
            departmentCombo.getItems().add(null);
        }
        departmentCombo.setValue(null);
    }

    private void setupEmployeeCombo() {
        employeeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Employee e) {
                return e == null ? "Select Employee" : e.getFullName() + " (#" + e.getEmployeeId() + ")";
            }
            @Override public Employee fromString(String s) { return null; }
        });

        try {
            List<Employee> employees = employeeService.getAllActiveEmployees();
            employeeCombo.getItems().add(null);
            employeeCombo.getItems().addAll(employees);
        } catch (Exception ex) {
            employeeCombo.getItems().add(null);
        }
        employeeCombo.setValue(null);
    }

    private void setupPayrunCombo() {
        payrunCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Payrun p) {
                if (p == null) return "Any Payrun";
                return DATE_FMT.format(p.getStartDate())
                        + " – " + DATE_FMT.format(p.getEndDate())
                        + "  (Pay date: " + DATE_FMT.format(p.getPayDate()) + ")";
            }
            @Override public Payrun fromString(String s) { return null; }
        });

        try {
            List<Payrun> payruns = payrollService.getPayrunsWithPayrollData();
            payrunCombo.getItems().add(null);
            payrunCombo.getItems().addAll(payruns);
        } catch (Exception ex) {
            payrunCombo.getItems().add(null);
        }
        payrunCombo.setValue(null);

        // When a payrun is selected, switch date range to Latest Available so dates resolve
        // from the selected payrun's period rather than a hardcoded preset.
        payrunCombo.setOnAction(e -> {
            ReportType type = reportTypeCombo.getValue();
            if (type != null && isPayrollReport(type) && payrunCombo.getValue() != null) {
                if (!DR_CUSTOM.equals(dateRangeCombo.getValue())) {
                    dateRangeCombo.setValue(DR_LATEST);
                }
            }
        });
    }

    private void setupStaticCombos() {
        // Attendance status — user-friendly labels with stored raw values
        statusCombo.getItems().setAll(
                "All Statuses", "Present", "Late", "Absent", "Half Day", "On Leave");
        statusCombo.setValue("All Statuses");

        // Audit log action
        actionCombo.getItems().setAll(
                "All Actions", "Login", "Logout", "Create", "Update", "Delete", "Reset Password");
        actionCombo.setValue("All Actions");

        // Support ticket category
        categoryCombo.getItems().setAll(
                "All Categories", "Payroll", "Attendance", "Account", "System", "Other");
        categoryCombo.setValue("All Categories");

        // Routing team
        routingTeamCombo.getItems().setAll(
                "All Teams", "HR", "Payroll", "IT", "Admin");
        routingTeamCombo.setValue("All Teams");

        // Routing team follows the chosen category: tickets are routed to a fixed team
        // at creation time, so a free-choice team would let users build a contradictory
        // filter (e.g. Payroll + IT) that returns nothing. Auto-set and lock the team
        // when a specific category is picked; re-enable it for "All Categories".
        categoryCombo.setOnAction(e -> syncRoutingTeamToCategory());

        // Ticket status
        ticketStatusCombo.getItems().setAll(
                "All Statuses", "Open", "Resolved", "Cancelled");
        ticketStatusCombo.setValue("All Statuses");
    }

    // Drives the routing team from the selected category, mirroring the routing rule
    // applied when a ticket is created (see SupportTicketService.resolveRoutingTeam).
    // For "All Categories" the team is freed and reset to "All Teams".
    private void syncRoutingTeamToCategory() {
        String team = routingTeamForCategory(categoryCombo.getValue());

        if (team == null) {
            // "All Categories" — let the user pick any team
            routingTeamCombo.setValue("All Teams");
            routingTeamCombo.setDisable(false);
        } else {
            routingTeamCombo.setValue(team);
            routingTeamCombo.setDisable(true);
        }
        refreshComboButtonText(routingTeamCombo);
    }

    // Maps a category label to its routing team label, or null for "All Categories".
    // Mirrors SupportTicketService.resolveRoutingTeam so the report filter stays
    // consistent with how tickets were actually routed.
    private String routingTeamForCategory(String category) {
        if (category == null || "All Categories".equals(category)) return null;
        return switch (category) {
            case "Payroll"    -> "Payroll";
            case "Attendance" -> "HR";
            case "Account",
                 "System"     -> "IT";
            case "Other"      -> "Admin";
            default           -> null;
        };
    }

    private void setupDateRangeCombo() {
        dateRangeCombo.getItems().setAll(DR_LATEST, DR_THIS_MONTH, DR_THIS_YEAR, DR_CUSTOM);
        dateRangeCombo.setValue(DR_LATEST);
        dateRangeCombo.setOnAction(e -> updateDatePickerVisibility());
        updateDatePickerVisibility();
    }

    private void updateDatePickerVisibility() {
        boolean dateRangeApplies = usesDateRange(reportTypeCombo.getValue());
        boolean custom = dateRangeApplies && DR_CUSTOM.equals(dateRangeCombo.getValue());

        datePickerRow.setVisible(true);
        datePickerRow.setManaged(true);
        datePickerRow.setDisable(!custom);

        dateFromPicker.setDisable(!custom);
        dateToPicker.setDisable(!custom);
        dateFromPicker.setPromptText(custom ? "Date From" : datePickerPrompt(dateRangeApplies));
        dateToPicker.setPromptText(custom ? "Date To" : datePickerPrompt(dateRangeApplies));

        if (!custom) {
            dateFromPicker.setValue(null);
            dateToPicker.setValue(null);
        }
    }

    // --- Filter visibility ------------------------------------------------

    private void configureFilters() {
        ReportType type = reportTypeCombo.getValue();
        if (type == null) return;

        boolean usesDepartment = false;
        boolean usesEmployee = false;
        boolean usesPayrun = false;
        boolean usesStatus = false;
        boolean usesAction = false;
        boolean usesDateRange = true;
        boolean usesCategory = false;
        boolean usesRoutingTeam = false;
        boolean usesTicketStatus = false;

        switch (type) {
            case ATTENDANCE -> {
                usesDepartment = true;
                usesEmployee = true;
                usesStatus = true;
                setDateRangeOptions(false);
            }
            case EMPLOYEE_TIMECARD -> {
                usesEmployee = true;
                setDateRangeOptions(false);
            }
            case ORG_PAYROLL -> {
                // Monthly Payroll Summary merges both semi-monthly cutoffs per month,
                // so it is driven by the date range, not a single payrun. The payrun
                // filter is intentionally hidden here to avoid implying it has an effect.
                usesDepartment = true;
                setDateRangeOptions(true);
            }
            case PAYROLL_SUMMARY -> {
                usesDepartment = true;
                usesPayrun = true;
                setDateRangeOptions(true);
            }
            case PAYSLIP -> {
                usesEmployee = true;
                usesPayrun = true;
                usesDateRange = false;
            }
            case AUDIT_LOG -> {
                usesAction = true;
                setDateRangeOptions(false);
            }
            case SUPPORT_TICKET -> {
                usesDepartment = true;
                usesEmployee = true;
                usesCategory = true;
                usesRoutingTeam = true;
                usesTicketStatus = true;
                setDateRangeOptions(false);
            }
        }

        applyComboState(departmentRow, departmentCombo, usesDepartment, "All Departments", null);
        applyComboState(employeeRow, employeeCombo, usesEmployee, "Select Employee", null);
        applyComboState(payrunRow, payrunCombo, usesPayrun, "Any Payrun", null);
        applyComboState(statusRow, statusCombo, usesStatus, "All Statuses", "All Statuses");
        applyComboState(actionRow, actionCombo, usesAction, "All Actions", "All Actions");
        applyComboState(dateRangeSection, dateRangeCombo, usesDateRange, "Select date range", null);
        applyComboState(categoryRow, categoryCombo, usesCategory, "All Categories", "All Categories");
        applyComboState(routingTeamRow, routingTeamCombo, usesRoutingTeam, "All Teams", "All Teams");
        applyComboState(ticketStatusRow, ticketStatusCombo, usesTicketStatus, "All Statuses", "All Statuses");

        actionRow.setVisible(usesAction);
        actionRow.setManaged(usesAction);
        statusRow.setVisible(!usesAction);
        statusRow.setManaged(!usesAction);

        updateDatePickerVisibility();
    }

    private void setDateRangeOptions(boolean payrollMode) {
        String current = dateRangeCombo.getValue();
        dateRangeCombo.getItems().setAll(DR_LATEST, DR_THIS_MONTH, DR_THIS_YEAR, DR_CUSTOM);

        if (DR_CUSTOM.equals(current)) {
            dateRangeCombo.setValue(DR_CUSTOM);
        } else if (payrollMode) {
            dateRangeCombo.setValue(DR_LATEST);
        } else {
            // attendance / audit / ticket default to This Month
            dateRangeCombo.setValue(
                    DR_THIS_MONTH.equals(current) || DR_THIS_YEAR.equals(current) ? current : DR_THIS_MONTH);
        }
    }

    private boolean isPayrollReport(ReportType type) {
        return type == ReportType.ORG_PAYROLL
                || type == ReportType.PAYROLL_SUMMARY;
    }

    private boolean usesDateRange(ReportType type) {
        return type != null && type != ReportType.PAYSLIP;
    }

    private String datePickerPrompt(boolean dateRangeApplies) {
        return dateRangeApplies ? "Use Custom date range" : "Not used for this report";
    }

    private <T> void applyComboState(VBox row,
                                     ComboBox<T> combo,
                                     boolean enabled,
                                     String activePrompt,
                                     T activeDefault) {
        row.setVisible(true);
        row.setManaged(true);
        row.setDisable(!enabled);
        combo.setDisable(!enabled);

        if (enabled) {
            combo.setPromptText(activePrompt);
            if (combo.getValue() == null && activeDefault != null) {
                combo.setValue(activeDefault);
            }
        } else {
            combo.setValue(null);
            combo.setPromptText("Not used for this report");
        }

        refreshComboButtonText(combo);
    }

    private <T> void refreshComboButtonText(ComboBox<T> combo) {
        ListCell<T> buttonCell = new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(combo.getPromptText());
                    return;
                }

                StringConverter<T> converter = combo.getConverter();
                setText(converter == null ? item.toString() : converter.toString(item));
            }
        };

        // Let the button cell shrink and clip long values (e.g. a payrun period string)
        // instead of widening the combo and shifting the surrounding grid layout.
        buttonCell.setMinWidth(0);
        buttonCell.setPrefWidth(0);
        buttonCell.setTextOverrun(OverrunStyle.ELLIPSIS);
        combo.setButtonCell(buttonCell);
    }

    // --- Date resolution --------------------------------------------------

    /**
     * Resolves the effective [from, to] date pair for the current date range selection.
     * For payroll reports, "Latest Available" uses the selected payrun's period if one
     * is chosen, otherwise falls back to the most recent payrun with results.
     * Returns null array entries when the date range is intentionally open (e.g. Audit Log
     * with no selection), which is valid for services that accept null dates.
     */
    private LocalDate[] resolveDates() {
        String mode = dateRangeCombo.getValue();
        ReportType type = reportTypeCombo.getValue();

        if (DR_CUSTOM.equals(mode)) {
            return new LocalDate[]{ dateFromPicker.getValue(), dateToPicker.getValue() };
        }

        if (DR_THIS_MONTH.equals(mode)) {
            LocalDate today = LocalDate.now();
            return new LocalDate[]{ today.withDayOfMonth(1), today };
        }

        if (DR_THIS_YEAR.equals(mode)) {
            LocalDate today = LocalDate.now();
            return new LocalDate[]{ LocalDate.of(today.getYear(), 1, 1), today };
        }

        // DR_LATEST
        if (isPayrollReport(type)) {
            // Prefer selected payrun; fall back to latest payrun with results
            Payrun ref = payrunCombo.getValue();
            if (ref == null) ref = latestPayrun;
            if (ref != null) {
                return new LocalDate[]{ ref.getStartDate(), ref.getEndDate() };
            }
            // No payrun data at all — return null so service throws a clear message
            return new LocalDate[]{ null, null };
        }

        // For attendance/audit/ticket "Latest Available" = most recent full month
        LocalDate today = LocalDate.now();
        return new LocalDate[]{ today.withDayOfMonth(1), today };
    }

    // --- Preview action ---------------------------------------------------

    @FXML
    private void handlePreview() {
        UiMessageUtil.clearMessage(moduleMessageLabel);
        ReportType type = reportTypeCombo.getValue();
        if (type == null) {
            UiMessageUtil.setError(moduleMessageLabel, "Select a report type first.");
            return;
        }

        try {
            switch (type) {
                case ATTENDANCE        -> previewAttendance();
                case EMPLOYEE_TIMECARD -> previewEmployeeTimecard();
                case ORG_PAYROLL       -> previewOrgPayroll();
                case PAYROLL_SUMMARY   -> previewPayrollSummary();
                case PAYSLIP           -> previewPayslip();
                case AUDIT_LOG         -> previewAuditLog();
                case SUPPORT_TICKET    -> previewSupportTicket();
            }
        } catch (ValidationException ex) {
            showError("Validation Error", ex.getMessage());
        } catch (Exception ex) {
            showError("Report Error", "Could not generate report:\n" + ex.getMessage());
        }
    }

    // --- Per-report preview methods ---------------------------------------

    private void previewAttendance() {
        LocalDate[] dates = resolveDates();
        LocalDate from = requireDate(dates[0], "Date From is required (select Custom to enter a specific date).");
        LocalDate to   = requireDateRange(from, dates[1]);

        Integer deptId = selectedDeptId();
        Integer empId  = selectedEmpId();
        String  status = toAttendanceStatusRaw(statusCombo.getValue());

        attendanceReportService.previewAttendanceReport(deptId, empId, status, from, to);
    }

    private void previewEmployeeTimecard() {
        int empId = requireEmployee();

        LocalDate[] dates = resolveDates();
        LocalDate from = requireDate(dates[0], "Date From is required (select Custom to enter a specific date).");
        LocalDate to   = requireDateRange(from, dates[1]);

        attendanceReportService.previewEmployeeTimecardReport(empId, from, to);
    }

    private void previewOrgPayroll() {
        LocalDate[] dates = resolveDates();
        if (dates[0] == null) {
            throw new ValidationException("No payrun data found. Cannot resolve date range.");
        }
        LocalDate from = dates[0];
        LocalDate to   = requireDateRange(from, dates[1]);

        Integer deptId   = selectedDeptId();
        String  deptName = selectedDeptName();
        Integer empId    = selectedEmpId();
        Integer payrunId = selectedPayrunId();

        payrollReportService.previewOrganizationPayrollReport(deptId, deptName, empId, payrunId, from, to);
    }

    private void previewPayrollSummary() {
        LocalDate[] dates = resolveDates();
        if (dates[0] == null) {
            throw new ValidationException("No payrun data found. Cannot resolve date range.");
        }
        LocalDate from = dates[0];
        LocalDate to   = requireDateRange(from, dates[1]);

        Integer deptId   = selectedDeptId();
        Integer payrunId = selectedPayrunId();

        payrollReportService.previewPayrollSummaryReport(deptId, payrunId, from, to);
    }

    private void previewPayslip() {
        int empId    = requireEmployee();
        int payrunId = requirePayrun();
        payrollReportService.previewPayslipReport(empId, payrunId);
    }

    private void previewAuditLog() {
        // Dates are optional for audit log — null is valid
        LocalDate[] dates = resolveDates();
        LocalDate from = dates[0];
        LocalDate to   = dates[1];

        // When custom, enforce both-or-neither rule
        if (DR_CUSTOM.equals(dateRangeCombo.getValue()) && (from == null) != (to == null)) {
            throw new ValidationException("Provide both Date From and Date To, or leave both empty.");
        }

        String action = toAuditActionRaw(actionCombo.getValue());
        auditReportService.previewAuditReport(null, action, from, to);
    }

    private void previewSupportTicket() {
        // Dates are optional for ticket report
        LocalDate[] dates = resolveDates();
        LocalDate from = dates[0];
        LocalDate to   = dates[1];

        if (DR_CUSTOM.equals(dateRangeCombo.getValue()) && (from != null) != (to != null)) {
            throw new ValidationException("Provide both Date From and Date To, or leave both empty.");
        }

        Integer deptId      = selectedDeptId();
        Integer empId       = selectedEmpId();
        String  category    = toRawEnumValue(categoryCombo.getValue(), "All Categories");
        String  routingTeam = toRawEnumValue(routingTeamCombo.getValue(), "All Teams");
        String  status      = toRawEnumValue(ticketStatusCombo.getValue(), "All Statuses");

        ticketReportService.previewTicketReport(deptId, empId, category, routingTeam, status, from, to);
    }

    // --- Export action ----------------------------------------------------

    @FXML
    private void handleExport() {
        UiMessageUtil.clearMessage(moduleMessageLabel);
        ReportType type = reportTypeCombo.getValue();
        if (type == null) {
            UiMessageUtil.setError(moduleMessageLabel, "Select a report type first.");
            return;
        }

        try {
            switch (type) {
                case ATTENDANCE        -> exportAttendance();
                case EMPLOYEE_TIMECARD -> exportEmployeeTimecard();
                case ORG_PAYROLL       -> exportOrgPayroll();
                case PAYROLL_SUMMARY   -> exportPayrollSummary();
                case PAYSLIP           -> exportPayslip();
                case AUDIT_LOG         -> exportAuditLog();
                case SUPPORT_TICKET    -> exportSupportTicket();
            }
        } catch (ValidationException ex) {
            showError("Validation Error", ex.getMessage());
        } catch (Exception ex) {
            showError("Report Error", "Could not export report:\n" + ex.getMessage());
        }
    }

    // --- Per-report export methods ----------------------------------------

    private void exportAttendance() {
        LocalDate[] dates = resolveDates();
        LocalDate from = requireDate(dates[0], "Date From is required (select Custom to enter a specific date).");
        LocalDate to   = requireDateRange(from, dates[1]);

        Integer deptId = selectedDeptId();
        Integer empId  = selectedEmpId();
        String  status = toAttendanceStatusRaw(statusCombo.getValue());

        String path = chooseExportFile("Attendance_Report.pdf");
        if (path == null) return;
        attendanceReportService.exportAttendanceReportToPdf(deptId, empId, status, from, to, path);
        reportExported(path);
    }

    private void exportEmployeeTimecard() {
        int empId = requireEmployee();

        LocalDate[] dates = resolveDates();
        LocalDate from = requireDate(dates[0], "Date From is required (select Custom to enter a specific date).");
        LocalDate to   = requireDateRange(from, dates[1]);

        String path = chooseExportFile("Employee_Timecard_Report.pdf");
        if (path == null) return;
        attendanceReportService.exportEmployeeTimecardReportToPdf(empId, from, to, path);
        reportExported(path);
    }

    private void exportOrgPayroll() {
        LocalDate[] dates = resolveDates();
        if (dates[0] == null) {
            throw new ValidationException("No payrun data found. Cannot resolve date range.");
        }
        LocalDate from = dates[0];
        LocalDate to   = requireDateRange(from, dates[1]);

        Integer deptId   = selectedDeptId();
        String  deptName = selectedDeptName();
        Integer empId    = selectedEmpId();
        Integer payrunId = selectedPayrunId();

        String path = chooseExportFile("Monthly_Payroll_Summary_Report.pdf");
        if (path == null) return;
        payrollReportService.exportOrganizationPayrollReportToPdf(deptId, deptName, empId, payrunId, from, to, path);
        reportExported(path);
    }

    private void exportPayrollSummary() {
        LocalDate[] dates = resolveDates();
        if (dates[0] == null) {
            throw new ValidationException("No payrun data found. Cannot resolve date range.");
        }
        LocalDate from = dates[0];
        LocalDate to   = requireDateRange(from, dates[1]);

        Integer deptId   = selectedDeptId();
        Integer payrunId = selectedPayrunId();

        String path = chooseExportFile("Department_Payroll_Report.pdf");
        if (path == null) return;
        payrollReportService.exportPayrollSummaryReportToPdf(deptId, payrunId, from, to, path);
        reportExported(path);
    }

    private void exportPayslip() {
        int empId    = requireEmployee();
        int payrunId = requirePayrun();

        String path = chooseExportFile("Payslip_" + empId + "_Payrun" + payrunId + ".pdf");
        if (path == null) return;
        payrollReportService.exportPayslipReportToPdf(empId, payrunId, path);
        reportExported(path);
    }

    private void exportAuditLog() {
        LocalDate[] dates = resolveDates();
        LocalDate from = dates[0];
        LocalDate to   = dates[1];

        if (DR_CUSTOM.equals(dateRangeCombo.getValue()) && (from == null) != (to == null)) {
            throw new ValidationException("Provide both Date From and Date To, or leave both empty.");
        }

        String action = toAuditActionRaw(actionCombo.getValue());

        String path = chooseExportFile("Audit_Log_Report.pdf");
        if (path == null) return;
        auditReportService.exportAuditReportToPdf(null, action, from, to, path);
        reportExported(path);
    }

    private void exportSupportTicket() {
        LocalDate[] dates = resolveDates();
        LocalDate from = dates[0];
        LocalDate to   = dates[1];

        if (DR_CUSTOM.equals(dateRangeCombo.getValue()) && (from != null) != (to != null)) {
            throw new ValidationException("Provide both Date From and Date To, or leave both empty.");
        }

        Integer deptId      = selectedDeptId();
        Integer empId       = selectedEmpId();
        String  category    = toRawEnumValue(categoryCombo.getValue(), "All Categories");
        String  routingTeam = toRawEnumValue(routingTeamCombo.getValue(), "All Teams");
        String  status      = toRawEnumValue(ticketStatusCombo.getValue(), "All Statuses");

        String path = chooseExportFile("Support_Ticket_Report.pdf");
        if (path == null) return;
        ticketReportService.exportTicketReportToPdf(deptId, empId, category, routingTeam, status, from, to, path);
        reportExported(path);
    }

    // opens a save dialog and returns the chosen path, or null when cancelled
    private String chooseExportFile(String defaultName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Report PDF");
        chooser.setInitialFileName(defaultName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        File target = exportButton.getScene() != null
                ? chooser.showSaveDialog(exportButton.getScene().getWindow())
                : chooser.showSaveDialog(null);
        return target != null ? target.getAbsolutePath() : null;
    }

    // shows a success toast after a report PDF is written
    private void reportExported(String path) {
        String message = "Report exported to " + new File(path).getName() + ".";
        if (exportButton.getScene() != null
                && exportButton.getScene().getRoot() instanceof StackPane root) {
            UiMessageUtil.showToast(root, message);
        } else {
            UiMessageUtil.setSuccess(moduleMessageLabel, message);
        }
    }

    // --- Value translation helpers ----------------------------------------

    // Maps user-friendly attendance status labels back to DB enum values
    private String toAttendanceStatusRaw(String label) {
        if (label == null || "All Statuses".equals(label)) return null;
        return switch (label) {
            case "Present"   -> "PRESENT";
            case "Late"      -> "LATE";
            case "Absent"    -> "ABSENT";
            case "Half Day"  -> "HALF_DAY";
            case "On Leave"  -> "ON_LEAVE";
            default          -> null;
        };
    }

    // Maps user-friendly action labels back to DB values
    private String toAuditActionRaw(String label) {
        if (label == null || "All Actions".equals(label)) return null;
        return switch (label) {
            case "Login"          -> "LOGIN";
            case "Logout"         -> "LOGOUT";
            case "Create"         -> "CREATE";
            case "Update"         -> "UPDATE";
            case "Delete"         -> "DELETE";
            case "Reset Password" -> "RESET_PASSWORD";
            default               -> null;
        };
    }

    // Maps user-friendly combo labels to raw DB enum strings; returns null when "All *"
    private String toRawEnumValue(String label, String allSentinel) {
        if (label == null || label.equals(allSentinel)) return null;
        // Category: "Payroll" → "PAYROLL", "Attendance" → "ATTENDANCE", etc.
        // RoutingTeam: "HR" → "HR" (already uppercase)
        // TicketStatus: "Open" → "OPEN", "Resolved" → "RESOLVED", etc.
        return label.toUpperCase().replace(" ", "_");
    }

    // --- Validation helpers -----------------------------------------------

    private LocalDate requireDate(LocalDate d, String message) {
        if (d == null) throw new ValidationException(message);
        return d;
    }

    private LocalDate requireDateRange(LocalDate from, LocalDate to) {
        if (to == null) throw new ValidationException("Date To could not be resolved.");
        if (to.isBefore(from)) throw new ValidationException("Date To cannot be before Date From.");
        return to;
    }

    private int requireEmployee() {
        Employee e = employeeCombo.getValue();
        if (e == null) throw new ValidationException("Please select an employee.");
        return e.getEmployeeId();
    }

    private int requirePayrun() {
        Payrun p = payrunCombo.getValue();
        if (p == null) throw new ValidationException("Please select a payrun.");
        return p.getPayrunId();
    }

    private Integer selectedDeptId() {
        Department d = departmentCombo.getValue();
        return d == null ? null : d.getDepartmentId();
    }

    // null when "All Departments" is selected; the report label falls back accordingly
    private String selectedDeptName() {
        Department d = departmentCombo.getValue();
        return d == null ? null : d.getDepartmentName();
    }

    private Integer selectedEmpId() {
        Employee e = employeeCombo.getValue();
        return e == null ? null : e.getEmployeeId();
    }

    private Integer selectedPayrunId() {
        Payrun p = payrunCombo.getValue();
        return p == null ? null : p.getPayrunId();
    }

    // --- UI helpers -------------------------------------------------------

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
