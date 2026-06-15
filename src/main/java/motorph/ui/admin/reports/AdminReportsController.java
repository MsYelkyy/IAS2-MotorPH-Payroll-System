package motorph.ui.admin.reports;

import motorph.ui.util.UiMessageUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminReportsController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a");
    private static final String GENERATED_BY = "Admin";

    private enum ReportType {
        PAYROLL("Payroll Report"),
        ATTENDANCE("Attendance Report"),
        LEAVE("Leave Report"),
        OVERTIME("Overtime Report"),
        EMPLOYEE("Employee Report"),
        SYSTEM("System / Security Report");

        private final String title;

        ReportType(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private record GeneratedReport(
            String reportName,
            String reportType,
            String department,
            String dateRange,
            String generatedBy,
            String generatedAt,
            String status,
            String groupBy) {
    }

    @FXML private ComboBox<ReportType> reportTypeCombo;
    @FXML private ComboBox<String> departmentFilterCombo;
    @FXML private ComboBox<String> employeeFilterCombo;
    @FXML private ComboBox<String> dateRangeCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> groupByCombo;
    @FXML private DatePicker dateFromPicker;
    @FXML private DatePicker dateToPicker;
    @FXML private Label moduleMessageLabel;

    @FXML private TableView<GeneratedReport> reportTable;
    @FXML private TableColumn<GeneratedReport, String> reportNameColumn;
    @FXML private TableColumn<GeneratedReport, String> reportTypeColumn;
    @FXML private TableColumn<GeneratedReport, String> departmentColumn;
    @FXML private TableColumn<GeneratedReport, String> dateRangeColumn;
    @FXML private TableColumn<GeneratedReport, String> generatedByColumn;
    @FXML private TableColumn<GeneratedReport, String> generatedAtColumn;
    @FXML private TableColumn<GeneratedReport, Void> actionColumn;

    private final ObservableList<GeneratedReport> generatedReports = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        setupFilters();
        setupTable();
        seedGeneratedReports();
        UiMessageUtil.clearMessage(moduleMessageLabel);
    }

    public void refresh() {
        UiMessageUtil.clearMessage(moduleMessageLabel);
    }

    @FXML
    private void handleGenerate() {
        ReportType type = reportTypeCombo.getValue();
        if (type == null) {
            UiMessageUtil.setError(moduleMessageLabel, "Select a report type first.");
            return;
        }

        String department = departmentFilterCombo.isDisable()
                ? "N/A"
                : valueOrDefault(departmentFilterCombo.getValue(), "All Departments");
        String dateRange = buildDateRangeLabel();
        GeneratedReport report = new GeneratedReport(
                type.title + " - " + dateRange,
                type.title,
                department,
                dateRange,
                GENERATED_BY,
                DATE_TIME_FMT.format(LocalDateTime.now()),
                valueOrDefault(statusFilterCombo.getValue(), "All Statuses"),
                valueOrDefault(groupByCombo.getValue(), "Department"));

        generatedReports.add(0, report);
        reportTable.getSelectionModel().select(report);
        UiMessageUtil.setSuccess(moduleMessageLabel, type.title + " generated.");
    }

    private void setupFilters() {
        reportTypeCombo.getItems().setAll(ReportType.values());
        reportTypeCombo.setValue(ReportType.ATTENDANCE);
        reportTypeCombo.setOnAction(event -> configureFiltersForReport());

        departmentFilterCombo.getItems().setAll(
                "All Departments", "Executive Management", "Human Resources",
                "Finance", "Marketing", "Information Technology", "Operations");
        departmentFilterCombo.setValue("All Departments");

        employeeFilterCombo.getItems().setAll("All Employees");
        employeeFilterCombo.setValue("All Employees");

        dateRangeCombo.getItems().setAll("This Month", "This Week", "Year to Date", "Custom");
        dateRangeCombo.setValue("This Month");
        dateRangeCombo.setOnAction(event -> applyDateRangePreset());

        configureFiltersForReport();
        applyDateRangePreset();
    }

    private void configureFiltersForReport() {
        ReportType type = reportTypeCombo.getValue();
        if (type == null) return;

        switch (type) {
            case PAYROLL -> {
                statusFilterCombo.getItems().setAll("All Statuses", "Generated", "Pending", "Approved");
                groupByCombo.getItems().setAll("Department", "Pay Period", "Employee");
                setFilterEnabled(departmentFilterCombo, true);
                setFilterEnabled(employeeFilterCombo, true);
            }
            case ATTENDANCE -> {
                statusFilterCombo.getItems().setAll("All Statuses", "Present", "Late", "Absent");
                groupByCombo.getItems().setAll("Department", "Employee", "Date");
                setFilterEnabled(departmentFilterCombo, true);
                setFilterEnabled(employeeFilterCombo, true);
            }
            case LEAVE -> {
                statusFilterCombo.getItems().setAll("All Statuses", "Approved", "Pending", "Rejected");
                groupByCombo.getItems().setAll("Department", "Leave Type", "Employee");
                setFilterEnabled(departmentFilterCombo, true);
                setFilterEnabled(employeeFilterCombo, true);
            }
            case OVERTIME -> {
                statusFilterCombo.getItems().setAll("All Statuses", "Approved", "Pending", "Rejected");
                groupByCombo.getItems().setAll("Department", "Employee", "Date");
                setFilterEnabled(departmentFilterCombo, true);
                setFilterEnabled(employeeFilterCombo, true);
            }
            case EMPLOYEE -> {
                statusFilterCombo.getItems().setAll("All Statuses", "Active", "Inactive");
                groupByCombo.getItems().setAll("Department", "Status", "Position");
                setFilterEnabled(departmentFilterCombo, true);
                setFilterEnabled(employeeFilterCombo, true);
            }
            case SYSTEM -> {
                statusFilterCombo.getItems().setAll("All Statuses", "Success", "Failed", "Locked");
                groupByCombo.getItems().setAll("Role", "User", "Action");
                setFilterEnabled(departmentFilterCombo, false);
                setFilterEnabled(employeeFilterCombo, false);
            }
        }

        statusFilterCombo.setValue("All Statuses");
        groupByCombo.setValue(groupByCombo.getItems().isEmpty() ? null : groupByCombo.getItems().get(0));
    }

    private void setFilterEnabled(ComboBox<String> comboBox, boolean enabled) {
        comboBox.setDisable(!enabled);
        comboBox.setOpacity(enabled ? 1.0 : 0.55);
    }

    private void applyDateRangePreset() {
        String preset = dateRangeCombo.getValue();
        LocalDate today = LocalDate.now();

        if ("This Week".equals(preset)) {
            dateFromPicker.setValue(today.with(DayOfWeek.MONDAY));
            dateToPicker.setValue(today);
        } else if ("Year to Date".equals(preset)) {
            dateFromPicker.setValue(LocalDate.of(today.getYear(), 1, 1));
            dateToPicker.setValue(today);
        } else if ("This Month".equals(preset)) {
            dateFromPicker.setValue(today.withDayOfMonth(1));
            dateToPicker.setValue(today);
        }
    }

    private void setupTable() {
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        reportNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().reportName()));
        reportTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().reportType()));
        departmentColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().department()));
        dateRangeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().dateRange()));
        generatedByColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().generatedBy()));
        generatedAtColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().generatedAt()));
        actionColumn.setSortable(false);
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button previewButton = new Button("Preview");

            {
                previewButton.getStyleClass().add("table-action-button");
                previewButton.setOnAction(event -> {
                    GeneratedReport report = getTableRow().getItem();
                    if (report != null) {
                        showInfo("Preview Report",
                                report.reportName() + "\nStatus: " + report.status()
                                        + "\nGroup by: " + report.groupBy());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_LEFT);
                setGraphic(empty ? null : previewButton);
            }
        });

        reportTable.setItems(generatedReports);
    }

    private void seedGeneratedReports() {
        LocalDate today = LocalDate.now();
        String monthRange = DATE_FMT.format(today.withDayOfMonth(1)) + " - " + DATE_FMT.format(today);
        generatedReports.setAll(
                new GeneratedReport("Attendance Report - " + monthRange, "Attendance Report",
                        "All Departments", monthRange, GENERATED_BY,
                        DATE_TIME_FMT.format(LocalDateTime.now().minusHours(1)), "All Statuses", "Department"),
                new GeneratedReport("Leave Report - " + monthRange, "Leave Report",
                        "All Departments", monthRange, GENERATED_BY,
                        DATE_TIME_FMT.format(LocalDateTime.now().minusHours(2)), "All Statuses", "Department"),
                new GeneratedReport("Employee Report - Active Employees", "Employee Report",
                        "Human Resources", "All periods", GENERATED_BY,
                        DATE_TIME_FMT.format(LocalDateTime.now().minusDays(1)), "Active", "Department"));
    }

    private String buildDateRangeLabel() {
        LocalDate from = dateFromPicker.getValue();
        LocalDate to = dateToPicker.getValue();

        if (from != null && to != null) {
            return DATE_FMT.format(from) + " - " + DATE_FMT.format(to);
        }
        if (from != null) {
            return "From " + DATE_FMT.format(from);
        }
        if (to != null) {
            return "To " + DATE_FMT.format(to);
        }
        return "All periods";
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
