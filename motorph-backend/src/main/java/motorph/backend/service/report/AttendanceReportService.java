package motorph.backend.service.report;

import motorph.backend.dto.report.attendance.AttendanceReportDTO;
import motorph.backend.dto.report.attendance.EmployeeTimecardReportDTO;
import motorph.backend.exception.ValidationException;
import motorph.backend.reporting.JasperReportGenerator;
import motorph.backend.repository.report.AttendanceReportRepository;

import net.sf.jasperreports.engine.JasperPrint;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static motorph.backend.service.report.ReportGuards.requireRows;

@Service
public class AttendanceReportService {

    private static final String ATTENDANCE_REPORT = "/motorph/reports/attendance/attendance_report.jrxml";
    private static final String EMPLOYEE_TIMECARD_REPORT = "/motorph/reports/attendance/employee_timecard_report.jrxml";

    private final AttendanceReportRepository attendanceReportRepository;
    private final JasperReportGenerator jasperReportGenerator;

    public AttendanceReportService(AttendanceReportRepository attendanceReportRepository,
                                   JasperReportGenerator jasperReportGenerator) {
        this.attendanceReportRepository = attendanceReportRepository;
        this.jasperReportGenerator = jasperReportGenerator;
    }

    public List<AttendanceReportDTO> getAttendanceReport(Integer departmentId, Integer employeeId,
                                                          String attendanceStatus, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return attendanceReportRepository.findAttendanceReport(departmentId, employeeId, attendanceStatus, startDate, endDate);
    }

    public byte[] generateAttendanceReportPdf(Integer departmentId, Integer employeeId, String attendanceStatus,
                                              LocalDate startDate, LocalDate endDate) {
        List<AttendanceReportDTO> data = getAttendanceReport(departmentId, employeeId, attendanceStatus, startDate, endDate);
        requireRows(data, "No attendance records match the selected filters.");

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                ATTENDANCE_REPORT,
                buildAttendanceParameters(startDate, endDate),
                data
        );

        return jasperReportGenerator.exportToPdfBytes(jasperPrint);
    }

    public EmployeeTimecardReportDTO getEmployeeTimecardReport(int employeeId, LocalDate startDate, LocalDate endDate) {
        validatePositive(employeeId, "Employee is required.");
        validateDateRange(startDate, endDate);
        return attendanceReportRepository.findEmployeeTimecardReport(employeeId, startDate, endDate);
    }

    public byte[] generateEmployeeTimecardReportPdf(int employeeId, LocalDate startDate, LocalDate endDate) {
        EmployeeTimecardReportDTO data = getEmployeeTimecardReport(employeeId, startDate, endDate);
        requireRows(data == null ? null : data.getEntries(),
                "No timecard entries found for this employee in the selected period.");

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                EMPLOYEE_TIMECARD_REPORT,
                buildTimecardParameters(data),
                data.getEntries()
        );

        return jasperReportGenerator.exportToPdfBytes(jasperPrint);
    }

    private Map<String, Object> buildAttendanceParameters(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("REPORT_TITLE", "Attendance Report");
        parameters.put("START_DATE", startDate);
        parameters.put("END_DATE", endDate);
        return parameters;
    }

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

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start and end date are required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
    }

    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new ValidationException(message);
        }
    }
}
