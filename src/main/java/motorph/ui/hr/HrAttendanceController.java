package motorph.ui.hr;

import motorph.application.dto.attendance.AttendanceLogRowDTO;
import motorph.application.dto.employee.EmployeeProfileDTO;
import motorph.application.service.AttendanceService;
import motorph.application.service.EmployeeService;
import motorph.domain.model.Attendance;
import motorph.domain.value.AttendanceStatus;
import motorph.ui.factory.HrUIServiceFactory;
import motorph.ui.util.UiMessageUtil;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Manages the HR Attendance module: read-only attendance logs and a full records view. */
public class HrAttendanceController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");
    private static final int HISTORY_DAYS = 90;
    private static final String ALL_DEPARTMENTS = "All Departments";

    private AttendanceService attendanceService;
    private EmployeeService employeeService;

    private final ObservableList<AttendanceLogRowDTO> logItems = FXCollections.observableArrayList();
    private final ObservableList<AttendanceLogRowDTO> allItems = FXCollections.observableArrayList();
    private FilteredList<AttendanceLogRowDTO> filteredLogs;
    private FilteredList<AttendanceLogRowDTO> filteredAllRecords;

    @FXML private StackPane toastRoot;

    // summary cards
    @FXML private Label presentTodayCard;
    @FXML private Label lateTodayCard;
    @FXML private Label onLeaveTodayCard;

    // attendance logs tab
    @FXML private TextField searchField;
    @FXML private ComboBox<String> departmentFilterCombo;
    @FXML private TableView<AttendanceLogRowDTO> logTable;
    @FXML private TableColumn<AttendanceLogRowDTO, LocalDate> logWorkDateColumn;
    @FXML private TableColumn<AttendanceLogRowDTO, String> logEmployeeColumn;
    @FXML private TableColumn<AttendanceLogRowDTO, String> logDepartmentColumn;
    @FXML private TableColumn<AttendanceLogRowDTO, LocalTime> logTimeInColumn;
    @FXML private TableColumn<AttendanceLogRowDTO, LocalTime> logTimeOutColumn;
    @FXML private TableColumn<AttendanceLogRowDTO, Number> logHoursColumn;
    @FXML private TableColumn<AttendanceLogRowDTO, String> logStatusColumn;

    // all records tab
    @FXML private TextField allRecordsSearchField;
    @FXML private ComboBox<String> allRecordsDepartmentCombo;
    @FXML private TableView<AttendanceLogRowDTO> allRecordsTable;
    @FXML private TableColumn<AttendanceLogRowDTO, LocalDate> allWorkDateColumn;
    @FXML private TableColumn<AttendanceLogRowDTO, String> allEmployeeColumn;
    @FXML private TableColumn<AttendanceLogRowDTO, String> allDepartmentColumn;
    @FXML private TableColumn<AttendanceLogRowDTO, LocalTime> allTimeInColumn;
    @FXML private TableColumn<AttendanceLogRowDTO, LocalTime> allTimeOutColumn;
    @FXML private TableColumn<AttendanceLogRowDTO, Number> allHoursColumn;
    @FXML private TableColumn<AttendanceLogRowDTO, String> allStatusColumn;

    // sets up services, tables, filters, and loads the attendance logs
    @FXML
    private void initialize() {
        attendanceService = HrUIServiceFactory.getInstance().getAttendanceService();
        employeeService = HrUIServiceFactory.getInstance().getEmployeeService();

        filteredLogs = setupRecordTable(logItems, logTable, logWorkDateColumn, logEmployeeColumn,
                logDepartmentColumn, logTimeInColumn, logTimeOutColumn, logHoursColumn, logStatusColumn);
        filteredAllRecords = setupRecordTable(allItems, allRecordsTable, allWorkDateColumn, allEmployeeColumn,
                allDepartmentColumn, allTimeInColumn, allTimeOutColumn, allHoursColumn, allStatusColumn);
        setupFilters();
        load();
    }

    // reloads the attendance logs when the nav button is clicked
    public void refresh() {
        load();
    }

    // loads recent and all attendance records, then refreshes both views
    private void load() {
        try {
            Map<Integer, EmployeeProfileDTO> profileCache = new HashMap<>();

            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(HISTORY_DAYS);
            List<AttendanceLogRowDTO> recentRows =
                    toRows(attendanceService.getAttendanceByDateRange(start, end), profileCache);
            List<AttendanceLogRowDTO> allRows =
                    toRows(attendanceService.getAllAttendance(), profileCache);

            logItems.setAll(recentRows);
            allItems.setAll(allRows);
            populateFilters(allRows);
            updateCards(recentRows);
        } catch (Exception e) {
            UiMessageUtil.showErrorToastTop(toastRoot, UiMessageUtil.toUserMessage(e));
        }
        applyFilters();
    }

    // converts attendance records into display rows using a shared profile cache
    private List<AttendanceLogRowDTO> toRows(List<Attendance> records, Map<Integer, EmployeeProfileDTO> profileCache) {
        List<AttendanceLogRowDTO> rows = new ArrayList<>();
        for (Attendance record : records) {
            EmployeeProfileDTO profile = resolveProfile(record.getEmployeeId(), profileCache);
            rows.add(new AttendanceLogRowDTO(
                    record.getEmployeeId(),
                    profile != null ? profile.getFullName() : "Employee #" + record.getEmployeeId(),
                    profile != null && profile.getDepartmentName() != null ? profile.getDepartmentName() : "—",
                    record.getWorkDate(),
                    record.getTimeIn(),
                    record.getTimeOut(),
                    record.getHoursWorked(),
                    record.getAttendanceStatus()));
        }
        return rows;
    }

    // resolves and caches one employee's profile, returning null when unavailable
    private EmployeeProfileDTO resolveProfile(int employeeId, Map<Integer, EmployeeProfileDTO> cache) {
        if (cache.containsKey(employeeId)) {
            return cache.get(employeeId);
        }
        EmployeeProfileDTO profile;
        try {
            profile = employeeService.getEmployeeProfileAdmin(employeeId);
        } catch (Exception e) {
            profile = null;
        }
        cache.put(employeeId, profile);
        return profile;
    }

    // updates the three summary cards from today's actual data
    private void updateCards(List<AttendanceLogRowDTO> rows) {
        LocalDate today = LocalDate.now();
        long presentToday = countToday(rows, today, AttendanceStatus.PRESENT);
        long lateToday = countToday(rows, today, AttendanceStatus.LATE);
        long onLeaveToday = countToday(rows, today, AttendanceStatus.ON_LEAVE);

        presentTodayCard.setText(String.valueOf(presentToday));
        lateTodayCard.setText(String.valueOf(lateToday));
        onLeaveTodayCard.setText(String.valueOf(onLeaveToday));
    }

    // counts today's records matching a status
    private long countToday(List<AttendanceLogRowDTO> rows, LocalDate today, AttendanceStatus status) {
        return rows.stream()
                .filter(r -> today.equals(r.getWorkDate()) && r.getStatus() == status)
                .count();
    }

    // ── Tables ────────────────────────────────────────────────────────

    // configures a read-only attendance record table and returns its filtered list
    private FilteredList<AttendanceLogRowDTO> setupRecordTable(
            ObservableList<AttendanceLogRowDTO> source,
            TableView<AttendanceLogRowDTO> table,
            TableColumn<AttendanceLogRowDTO, LocalDate> workDateColumn,
            TableColumn<AttendanceLogRowDTO, String> employeeColumn,
            TableColumn<AttendanceLogRowDTO, String> departmentColumn,
            TableColumn<AttendanceLogRowDTO, LocalTime> timeInColumn,
            TableColumn<AttendanceLogRowDTO, LocalTime> timeOutColumn,
            TableColumn<AttendanceLogRowDTO, Number> hoursColumn,
            TableColumn<AttendanceLogRowDTO, String> statusColumn) {

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        workDateColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getWorkDate()));
        workDateColumn.setCellFactory(c -> dateCell());
        employeeColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmployeeName()));
        departmentColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDepartment()));
        timeInColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTimeIn()));
        timeInColumn.setCellFactory(c -> timeCell());
        timeOutColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getTimeOut()));
        timeOutColumn.setCellFactory(c -> timeCell());
        hoursColumn.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getHoursWorked()));
        hoursColumn.setCellFactory(c -> hoursCell());
        statusColumn.setCellValueFactory(d -> new SimpleStringProperty(statusLabel(d.getValue().getStatus())));
        statusColumn.setCellFactory(c -> statusBadgeCell());

        FilteredList<AttendanceLogRowDTO> filtered = new FilteredList<>(source, row -> true);
        SortedList<AttendanceLogRowDTO> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        return filtered;
    }

    // ── Filters ───────────────────────────────────────────────────────

    // wires the search fields and department dropdowns to their tables
    private void setupFilters() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, value) -> applyFilters());
        }
        if (departmentFilterCombo != null) {
            departmentFilterCombo.setOnAction(e -> applyFilters());
        }
        if (allRecordsSearchField != null) {
            allRecordsSearchField.textProperty().addListener((obs, old, value) -> applyFilters());
        }
        if (allRecordsDepartmentCombo != null) {
            allRecordsDepartmentCombo.setOnAction(e -> applyFilters());
        }
    }

    // rebuilds both department dropdowns from the loaded rows
    private void populateFilters(List<AttendanceLogRowDTO> rows) {
        List<String> options = new ArrayList<>();
        options.add(ALL_DEPARTMENTS);
        rows.stream()
                .map(AttendanceLogRowDTO::getDepartment)
                .filter(dept -> dept != null && !dept.isBlank() && !options.contains(dept))
                .sorted()
                .forEach(options::add);

        populateDepartmentCombo(departmentFilterCombo, options);
        populateDepartmentCombo(allRecordsDepartmentCombo, options);
    }

    // resets one department dropdown while keeping a valid prior selection
    private void populateDepartmentCombo(ComboBox<String> combo, List<String> options) {
        if (combo == null) return;
        String previous = combo.getValue();
        combo.getItems().setAll(options);
        combo.setValue(options.contains(previous) ? previous : ALL_DEPARTMENTS);
    }

    // applies the search and department filters to both record tables
    private void applyFilters() {
        applyFilter(filteredLogs, searchField, departmentFilterCombo);
        applyFilter(filteredAllRecords, allRecordsSearchField, allRecordsDepartmentCombo);
    }

    // applies one search and department filter to a filtered list
    private void applyFilter(FilteredList<AttendanceLogRowDTO> filtered, TextField search, ComboBox<String> deptCombo) {
        if (filtered == null) return;

        String query = search != null ? search.getText().trim().toLowerCase() : "";
        String dept = deptCombo != null ? deptCombo.getValue() : ALL_DEPARTMENTS;

        filtered.setPredicate(row -> {
            boolean matchesSearch = query.isEmpty()
                    || row.getEmployeeName().toLowerCase().contains(query)
                    || String.valueOf(row.getEmployeeId()).contains(query);
            boolean matchesDept = dept == null || ALL_DEPARTMENTS.equals(dept)
                    || dept.equals(row.getDepartment());
            return matchesSearch && matchesDept;
        });
    }

    // ── Cell factories ────────────────────────────────────────────────

    // renders a work date cell
    private TableCell<AttendanceLogRowDTO, LocalDate> dateCell() {
        return new TableCell<>() {
            @Override protected void updateItem(LocalDate value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.format(DATE_FMT));
            }
        };
    }

    // renders a time-in or time-out cell
    private TableCell<AttendanceLogRowDTO, LocalTime> timeCell() {
        return new TableCell<>() {
            @Override protected void updateItem(LocalTime value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : formatTime(value));
            }
        };
    }

    // renders a worked-hours cell
    private TableCell<AttendanceLogRowDTO, Number> hoursCell() {
        return new TableCell<>() {
            @Override protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : formatHours(value.doubleValue()));
            }
        };
    }

    // renders a coloured status badge
    private TableCell<AttendanceLogRowDTO, String> statusBadgeCell() {
        return new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("status-badge"); }

            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                badge.getStyleClass().removeAll(
                        "status-present", "status-late", "status-absent",
                        "status-half-day", "status-on-leave");
                if (empty || value == null) {
                    setGraphic(null);
                    return;
                }
                AttendanceLogRowDTO row = getTableRow() == null ? null : getTableRow().getItem();
                if (row != null) {
                    badge.getStyleClass().add(statusStyleClass(row.getStatus()));
                }
                badge.setText(value);
                setGraphic(badge);
            }
        };
    }

    // ── Formatting helpers ────────────────────────────────────────────

    // formats a time value, or a dash when missing
    private String formatTime(LocalTime time) {
        return time == null ? "—" : time.format(TIME_FMT);
    }

    // formats worked hours to two decimals
    private String formatHours(double hours) {
        return String.format("%.2f", hours);
    }

    // converts an attendance status to a readable label
    private String statusLabel(AttendanceStatus status) {
        if (status == null) return "—";
        String name = status.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    // maps an attendance status to its badge style class
    private String statusStyleClass(AttendanceStatus status) {
        if (status == null) return "status-absent";
        return switch (status) {
            case PRESENT -> "status-present";
            case LATE -> "status-late";
            case ABSENT -> "status-absent";
            case HALF_DAY -> "status-half-day";
            case ON_LEAVE -> "status-on-leave";
        };
    }
}
