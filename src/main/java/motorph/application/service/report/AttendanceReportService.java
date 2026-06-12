package motorph.application.service.report;

import motorph.application.dto.report.attendance.AttendanceReportDTO;
import motorph.application.dto.report.attendance.EmployeeTimecardReportDTO;
import motorph.application.exception.ValidationException;
import motorph.infrastructure.repository.report.AttendanceReportRepository;
import motorph.infrastructure.reporting.JasperReportGenerator;

import net.sf.jasperreports.engine.JasperPrint;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles attendance report generation workflows.
 */
public class AttendanceReportService {

    private static final String ATTENDANCE_REPORT =
            "/motorph/reports/attendance/attendance_report.jrxml";

    private static final String EMPLOYEE_TIMECARD_REPORT =
            "/motorph/reports/attendance/employee_timecard_report.jrxml";

    private final AttendanceReportRepository attendanceReportRepository;
    private final JasperReportGenerator jasperReportGenerator;

    // constructors
    public AttendanceReportService(AttendanceReportRepository attendanceReportRepository,
                                   JasperReportGenerator jasperReportGenerator) {

        this.attendanceReportRepository = require(
                attendanceReportRepository,
                "AttendanceReportRepository is required."
        );

        this.jasperReportGenerator = require(
                jasperReportGenerator,
                "JasperReportGenerator is required."
        );
    }

    // returns attendance report data
    public List<AttendanceReportDTO> getAttendanceReport(Integer departmentId,
                                                         Integer employeeId,
                                                         String attendanceStatus,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {

        validateDateRange(startDate, endDate);

        return attendanceReportRepository.findAttendanceReport(
                departmentId,
                employeeId,
                attendanceStatus,
                startDate,
                endDate
        );
    }

    // previews the attendance report
    public void previewAttendanceReport(Integer departmentId,
                                        Integer employeeId,
                                        String attendanceStatus,
                                        LocalDate startDate,
                                        LocalDate endDate) {

        List<AttendanceReportDTO> reportData = getAttendanceReport(
                departmentId,
                employeeId,
                attendanceStatus,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                ATTENDANCE_REPORT,
                buildAttendanceParameters(startDate, endDate),
                reportData
        );

        jasperReportGenerator.showPreview(jasperPrint);
    }

    // exports the attendance report to PDF
    public void exportAttendanceReportToPdf(Integer departmentId,
                                            Integer employeeId,
                                            String attendanceStatus,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            String outputPath) {

        validateOutputPath(outputPath);

        List<AttendanceReportDTO> reportData = getAttendanceReport(
                departmentId,
                employeeId,
                attendanceStatus,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                ATTENDANCE_REPORT,
                buildAttendanceParameters(startDate, endDate),
                reportData
        );

        jasperReportGenerator.exportToPdf(jasperPrint, outputPath);
    }

    // returns employee timecard report data
    public EmployeeTimecardReportDTO getEmployeeTimecardReport(int employeeId,
                                                               LocalDate startDate,
                                                               LocalDate endDate) {

        validatePositive(employeeId, "Employee ID is required.");
        validateDateRange(startDate, endDate);

        return attendanceReportRepository.findEmployeeTimecardReport(
                employeeId,
                startDate,
                endDate
        );
    }

    // previews the employee timecard report
    public void previewEmployeeTimecardReport(int employeeId,
                                              LocalDate startDate,
                                              LocalDate endDate) {

        EmployeeTimecardReportDTO reportData = getEmployeeTimecardReport(
                employeeId,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                EMPLOYEE_TIMECARD_REPORT,
                buildTimecardParameters(reportData),
                reportData.getEntries()
        );

        jasperReportGenerator.showPreview(jasperPrint);
    }

    // exports the employee timecard report to PDF
    public void exportEmployeeTimecardReportToPdf(int employeeId,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  String outputPath) {

        validateOutputPath(outputPath);

        EmployeeTimecardReportDTO reportData = getEmployeeTimecardReport(
                employeeId,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                EMPLOYEE_TIMECARD_REPORT,
                buildTimecardParameters(reportData),
                reportData.getEntries()
        );

        jasperReportGenerator.exportToPdf(jasperPrint, outputPath);
    }

    // builds attendance report parameters
    private Map<String, Object> buildAttendanceParameters(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("REPORT_TITLE", "Attendance Report");
        parameters.put("START_DATE", startDate);
        parameters.put("END_DATE", endDate);

        return parameters;
    }

    // builds employee timecard report parameters
    private Map<String, Object> buildTimecardParameters(EmployeeTimecardReportDTO reportData) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("REPORT_TITLE", "Employee Timecard Report");
        parameters.put("EMPLOYEE_ID", reportData.getEmployeeId());
        parameters.put("EMPLOYEE_NAME", reportData.getEmployeeName());
        parameters.put("POSITION_NAME", reportData.getPositionName());
        parameters.put("DEPARTMENT_NAME", reportData.getDepartmentName());
        parameters.put("PERIOD_END_DATE", reportData.getPeriodEndDate());

        return parameters;
    }

    // validates date range input
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required.");
        }

        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
    }

    // validates positive id input
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new ValidationException(message);
        }
    }

    // validates output path input
    private void validateOutputPath(String outputPath) {
        if (outputPath == null || outputPath.isBlank()) {
            throw new ValidationException("Output path is required.");
        }
    }

    // validates required dependency
    private <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}