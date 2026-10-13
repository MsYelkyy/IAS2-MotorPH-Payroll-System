package motorph.ui.reports;

import motorph.application.exception.ValidationException;
import motorph.api.ApiClientFactory;
import motorph.api.ApiException;
import motorph.api.EmployeeApiClient;
import motorph.api.PayrunApiClient;
import motorph.api.PayslipApiClient;
import motorph.api.ReportApiClient;
import motorph.api.dto.employee.DepartmentDTO;
import motorph.api.dto.employee.EmployeeSummaryDTO;
import motorph.api.dto.payroll.PayrunDTO;
import motorph.domain.value.Role;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.DatePickerInputHelper;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Shared Reports module: role-neutral, consumed by Admin, IT, and future HR/Finance shells. */
public class ReportsController {

    private static final Logger LOG = Logger.getLogger(ReportsController.class.getName());
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
    @FXML private ComboBox<DepartmentDTO> departmentCombo;

    // Employee filter
    @FXML private VBox employeeRow;
    @FXML private ComboBox<EmployeeSummaryDTO> employeeCombo;

    // Payrun filter
    @FXML private VBox payrunRow;
    @FXML private ComboBox<PayrunDTO> payrunCombo;

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
    @FXML private HBox datePickerRow;
    @FXML private DatePicker dateFromPicker;
    @FXML private DatePicker dateToPicker;

    @FXML private Button previewButton;
    @FXML private Button exportButton;

    // Summary panel: mirrors the current selections and says whether the report can be generated.
    @FXML private Label reportStatusLabel;
    @FXML private Label summaryTitleLabel;
    @FXML private Label summaryDepartmentLabel;
    @FXML private Label summaryEmployeeLabel;
    @FXML private Label summaryDateRangeLabel;
    @FXML private VBox  summaryStatusBox;
    @FXML private Label summaryStatusTitleLabel;
    @FXML private Label summaryStatusNoteLabel;

    // --- Services / API clients --------------------------------------------

    private PayrunApiClient payrunApiClient;
    private EmployeeApiClient employeeApiClient;
    private ReportApiClient reportApiClient;
    private PayslipApiClient payslipApiClient;

    // Latest payrun with results; resolved once on init, used for "Latest Available".
    private PayrunDTO latestPayrun;
    // All payruns with results (DESC by start_date)
    private List<PayrunDTO> allPayrunsWithResults;

    // --- Lifecycle --------------------------------------------------------

    @FXML
    private void initialize() {
        payrunApiClient = ApiClientFactory.getInstance().getPayrunApiClient();

        ApiClientFactory apiClientFactory = ApiClientFactory.getInstance();
        employeeApiClient = apiClientFactory.getEmployeeApiClient();
        reportApiClient   = apiClientFactory.getReportApiClient();
        payslipApiClient  = apiClientFactory.getPayslipApiClient();

        loadPayrunCache();
        setupReportTypeCombo();
        setupDepartmentCombo();
        setupEmployeeCombo();
        setupPayrunCombo();
        setupStaticCombos();
        setupDateRangeCombo();

        DatePickerInputHelper.enableMaskedInput(dateFromPicker);
        DatePickerInputHelper.enableMaskedInput(dateToPicker);

        watchSummaryInputs();
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
            allPayrunsWithResults = payrunApiClient.getPayrunsWithPayrollData();
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
        reportTypeCombo.getItems().setAll(availableReportTypes());
        reportTypeCombo.setOnAction(e -> configureFilters());
    }

    // the backend grants VIEW_ALL_PAYSLIPS to Admin, Finance and HR; other roles would only get a 403
    private List<ReportType> availableReportTypes() {
        boolean canViewAllPayslips = canViewAllPayslips(CurrentUserSession.getActiveRole());

        return Arrays.stream(ReportType.values())
                .filter(type -> type != ReportType.PAYSLIP || canViewAllPayslips)
                .toList();
    }

    static boolean canViewAllPayslips(Role role) {
        return role == Role.ADMIN || role == Role.HR || role == Role.FINANCE;
    }

    private void setupDepartmentCombo() {
        departmentCombo.setConverter(new StringConverter<>() {
            @Override public String toString(DepartmentDTO d) {
                return d == null ? "All Departments" : d.getDepartmentName();
            }
            @Override public DepartmentDTO fromString(String s) { return null; }
        });

        try {
            List<DepartmentDTO> departments = employeeApiClient.getAllDepartments();
            departmentCombo.getItems().add(null);
            departmentCombo.getItems().addAll(departments);
        } catch (Exception ex) {
            departmentCombo.getItems().add(null);
        }
        departmentCombo.setValue(null);
    }

    private void setupEmployeeCombo() {
        employeeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(EmployeeSummaryDTO e) {
                return e == null ? "Select Employee" : e.getFullName() + " (#" + e.getEmployeeId() + ")";
            }
            @Override public EmployeeSummaryDTO fromString(String s) { return null; }
        });

        try {
            List<EmployeeSummaryDTO> employees = employeeApiClient.getAllEmployeeSummaries().stream()
                    .filter(EmployeeSummaryDTO::isActive)
                    .toList();
            employeeCombo.getItems().add(null);
            employeeCombo.getItems().addAll(employees);
        } catch (Exception ex) {
            employeeCombo.getItems().add(null);
        }
        employeeCombo.setValue(null);
    }

    private void setupPayrunCombo() {
        payrunCombo.setConverter(new StringConverter<>() {
            @Override public String toString(PayrunDTO p) {
                if (p == null) return "Any Payrun";
                return DATE_FMT.format(p.getStartDate())
                        + " – " + DATE_FMT.format(p.getEndDate())
                        + "  (Pay date: " + DATE_FMT.format(p.getPayDate()) + ")";
            }
            @Override public PayrunDTO fromString(String s) { return null; }
        });

        try {
            List<PayrunDTO> payruns = payrunApiClient.getPayrunsWithPayrollData();
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
        refreshSummary();
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

    // --- Summary panel ----------------------------------------------------

    private void watchSummaryInputs() {
        for (ComboBox<?> combo : List.of(reportTypeCombo, departmentCombo, employeeCombo, payrunCombo, dateRangeCombo)) {
            combo.valueProperty().addListener((obs, was, now) -> refreshSummary());
        }
        dateFromPicker.valueProperty().addListener((obs, was, now) -> refreshSummary());
        dateToPicker.valueProperty().addListener((obs, was, now) -> refreshSummary());
    }

    // mirrors the current selections and whether the report can be generated
    private void refreshSummary() {
        ReportType type = reportTypeCombo.getValue();
        summaryTitleLabel.setText(type == null ? "Select a report" : type.toString());

        summaryDepartmentLabel.setText(departmentCombo.isDisabled() ? "Not used"
                : departmentCombo.getValue() == null ? "All Departments" : comboText(departmentCombo));

        boolean employeeRequired = type == ReportType.EMPLOYEE_TIMECARD || type == ReportType.PAYSLIP;
        summaryEmployeeLabel.setText(employeeCombo.isDisabled() ? "Not used"
                : employeeCombo.getValue() != null ? comboText(employeeCombo)
                : employeeRequired ? "Not selected" : "All employees");

        summaryDateRangeLabel.setText(dateRangeCombo.isDisabled() || dateRangeCombo.getValue() == null ? "Not used"
                : DR_CUSTOM.equals(dateRangeCombo.getValue()) ? customRangeText()
                : dateRangeCombo.getValue());

        String issue = configurationIssue();
        boolean ready = issue == null;
        reportStatusLabel.setText(ready ? "Required fields are ready" : "Required fields are missing");
        summaryStatusTitleLabel.setText(ready ? "Preview ready" : "Needs attention");
        summaryStatusNoteLabel.setText(ready ? "Your report configuration is valid and ready to export." : issue);
        summaryStatusBox.getStyleClass().removeAll("report-status-ready", "report-status-warning");
        summaryStatusBox.getStyleClass().add(ready ? "report-status-ready" : "report-status-warning");
    }

    // what is still missing for the current report, or null when it can be previewed/exported;
    // the fetch methods below remain the source of truth and re-validate on click
    private String configurationIssue() {
        ReportType type = reportTypeCombo.getValue();
        if (type == null) return "Select a report type.";

        if ((type == ReportType.EMPLOYEE_TIMECARD || type == ReportType.PAYSLIP) && employeeCombo.getValue() == null) {
            return "Select an employee.";
        }
        if (type == ReportType.PAYSLIP && payrunCombo.getValue() == null) {
            return "Select a payroll run.";
        }
        if (!usesDateRange(type)) return null;

        String mode = dateRangeCombo.getValue();
        if (DR_CUSTOM.equals(mode)) {
            LocalDate from = dateFromPicker.getValue();
            LocalDate to = dateToPicker.getValue();
            boolean datesOptional = type == ReportType.AUDIT_LOG || type == ReportType.SUPPORT_TICKET;
            if (from == null && to == null) return datesOptional ? null : "Choose a start and end date.";
            if (from == null || to == null) return "Choose both a start and end date.";
            if (to.isBefore(from)) return "The end date is before the start date.";
        } else if (isPayrollReport(type) && DR_LATEST.equals(mode)
                && payrunCombo.getValue() == null && latestPayrun == null) {
            return "No payroll data is available yet.";
        }
        return null;
    }

    private String customRangeText() {
        LocalDate from = dateFromPicker.getValue();
        LocalDate to = dateToPicker.getValue();
        return from != null && to != null ? DATE_FMT.format(from) + " – " + DATE_FMT.format(to) : "Custom";
    }

    private <T> String comboText(ComboBox<T> combo) {
        T value = combo.getValue();
        StringConverter<T> converter = combo.getConverter();
        return converter == null ? value.toString() : converter.toString(value);
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
            PayrunDTO ref = payrunCombo.getValue();
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
            byte[] pdf = switch (type) {
                case ATTENDANCE        -> fetchAttendancePdf();
                case EMPLOYEE_TIMECARD -> fetchEmployeeTimecardPdf();
                case ORG_PAYROLL       -> fetchOrgPayrollPdf();
                case PAYROLL_SUMMARY   -> fetchPayrollSummaryPdf();
                case PAYSLIP           -> fetchPayslipPdf();
                case AUDIT_LOG         -> fetchAuditLogPdf();
                case SUPPORT_TICKET    -> fetchSupportTicketPdf();
            };
            openPdf(pdf, type.name().toLowerCase());
        } catch (ValidationException ex) {
            showError("Validation Error", ex.getMessage());
        } catch (ApiException ex) {
            // an empty report is not a failure - say so instead of opening a blank PDF
            showError("Report", isEmptyReport(ex)
                    ? ex.getMessage() + " There is nothing to preview."
                    : "Could not generate report: " + ex.getMessage());
        } catch (Exception ex) {
            showError("Report Error", "Could not generate report: " + ex.getMessage());
        }
    }

    // true when the backend ran the report fine but it matched no rows
    private boolean isEmptyReport(ApiException ex) {
        return "NO_REPORT_DATA".equals(ex.getCode());
    }

    // --- Per-report PDF fetch methods (shared by preview and export) ------

    private byte[] fetchAttendancePdf() {
        LocalDate[] dates = resolveDates();
        LocalDate from = requireDate(dates[0], "Date From is required (select Custom to enter a specific date).");
        LocalDate to   = requireDateRange(from, dates[1]);

        Integer deptId = selectedDeptId();
        Integer empId  = selectedEmpId();
        String  status = toAttendanceStatusRaw(statusCombo.getValue());

        return reportApiClient.getAttendanceReportPdf(deptId, empId, status, from, to);
    }

    private byte[] fetchEmployeeTimecardPdf() {
        int empId = requireEmployee();

        LocalDate[] dates = resolveDates();
        LocalDate from = requireDate(dates[0], "Date From is required (select Custom to enter a specific date).");
        LocalDate to   = requireDateRange(from, dates[1]);

        return reportApiClient.getEmployeeTimecardReportPdf(empId, from, to);
    }

    private byte[] fetchOrgPayrollPdf() {
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

        return reportApiClient.getOrganizationPayrollReportPdf(deptId, deptName, empId, payrunId, from, to);
    }

    private byte[] fetchPayrollSummaryPdf() {
        LocalDate[] dates = resolveDates();
        if (dates[0] == null) {
            throw new ValidationException("No payrun data found. Cannot resolve date range.");
        }
        LocalDate from = dates[0];
        LocalDate to   = requireDateRange(from, dates[1]);

        Integer deptId   = selectedDeptId();
        Integer payrunId = selectedPayrunId();

        return reportApiClient.getPayrollSummaryReportPdf(deptId, payrunId, from, to);
    }

    private byte[] fetchPayslipPdf() {
        int empId    = requireEmployee();
        int payrunId = requirePayrun();
        return payslipApiClient.getPayslipPdf(empId, payrunId);
    }

    private byte[] fetchAuditLogPdf() {
        // Dates are optional for audit log — null is valid
        LocalDate[] dates = resolveDates();
        LocalDate from = dates[0];
        LocalDate to   = dates[1];

        // When custom, enforce both-or-neither rule
        if (DR_CUSTOM.equals(dateRangeCombo.getValue()) && (from == null) != (to == null)) {
            throw new ValidationException("Provide both Date From and Date To, or leave both empty.");
        }

        String action = toAuditActionRaw(actionCombo.getValue());
        return reportApiClient.getAuditReportPdf(null, action, from, to);
    }

    private byte[] fetchSupportTicketPdf() {
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

        return reportApiClient.getTicketReportPdf(deptId, empId, category, routingTeam, status, from, to);
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
            String defaultName = switch (type) {
                case ATTENDANCE        -> "Attendance_Report.pdf";
                case EMPLOYEE_TIMECARD -> "Employee_Timecard_Report.pdf";
                case ORG_PAYROLL       -> "Monthly_Payroll_Summary_Report.pdf";
                case PAYROLL_SUMMARY   -> "Department_Payroll_Report.pdf";
                case PAYSLIP           -> "Payslip_" + requireEmployee() + "_Payrun" + requirePayrun() + ".pdf";
                case AUDIT_LOG         -> "Audit_Log_Report.pdf";
                case SUPPORT_TICKET    -> "Support_Ticket_Report.pdf";
            };

            String path = chooseExportFile(defaultName);
            if (path == null) return;

            byte[] pdf = switch (type) {
                case ATTENDANCE        -> fetchAttendancePdf();
                case EMPLOYEE_TIMECARD -> fetchEmployeeTimecardPdf();
                case ORG_PAYROLL       -> fetchOrgPayrollPdf();
                case PAYROLL_SUMMARY   -> fetchPayrollSummaryPdf();
                case PAYSLIP           -> fetchPayslipPdf();
                case AUDIT_LOG         -> fetchAuditLogPdf();
                case SUPPORT_TICKET    -> fetchSupportTicketPdf();
            };

            Files.write(Path.of(path), pdf);
            reportExported(path);
        } catch (ValidationException ex) {
            showError("Validation Error", ex.getMessage());
        } catch (ApiException ex) {
            showError("Report", isEmptyReport(ex)
                    ? ex.getMessage() + " There is nothing to export."
                    : "Could not export report: " + ex.getMessage());
        } catch (Exception ex) {
            showError("Report Error", "Could not export report: " + ex.getMessage());
        }
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

    // opens the PDF bytes in the OS's default viewer via a temp file - there is no
    // in-app preview window anymore now that generation happens server-side
    private void openPdf(byte[] pdf, String namePrefix) {
        try {
            File tempFile = File.createTempFile(namePrefix + "-", ".pdf");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), pdf);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(tempFile);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to save or open report PDF", e);
            showError("Report Error", "Could not open the generated report.");
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
        EmployeeSummaryDTO e = employeeCombo.getValue();
        if (e == null) throw new ValidationException("Please select an employee.");
        return e.getEmployeeId();
    }

    private int requirePayrun() {
        PayrunDTO p = payrunCombo.getValue();
        if (p == null) throw new ValidationException("Please select a payrun.");
        return p.getPayrunId();
    }

    private Integer selectedDeptId() {
        DepartmentDTO d = departmentCombo.getValue();
        return d == null ? null : d.getDepartmentId();
    }

    // null when "All Departments" is selected; the report label falls back accordingly
    private String selectedDeptName() {
        DepartmentDTO d = departmentCombo.getValue();
        return d == null ? null : d.getDepartmentName();
    }

    private Integer selectedEmpId() {
        EmployeeSummaryDTO e = employeeCombo.getValue();
        return e == null ? null : e.getEmployeeId();
    }

    private Integer selectedPayrunId() {
        PayrunDTO p = payrunCombo.getValue();
        return p == null ? null : p.getPayrunId();
    }

    // --- UI helpers -------------------------------------------------------

    private void showError(String title, String message) {
        UiMessageUtil.setError(moduleMessageLabel, message);
    }
}
