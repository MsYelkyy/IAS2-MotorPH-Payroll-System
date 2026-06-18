package motorph.application.service.report;

import motorph.application.dto.report.leave.LeaveReportDTO;
import motorph.application.exception.ValidationException;
import motorph.infrastructure.repository.report.LeaveReportRepository;
import motorph.infrastructure.reporting.JasperReportGenerator;

import net.sf.jasperreports.engine.JasperPrint;

import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles leave report generation workflows.
 */
public class LeaveReportService {

    private static final String LEAVE_REPORT =
            "/motorph/reports/leave/leave_report.jrxml";

    private final LeaveReportRepository leaveReportRepository;
    private final JasperReportGenerator jasperReportGenerator;

    // constructors
    public LeaveReportService(LeaveReportRepository leaveReportRepository,
                              JasperReportGenerator jasperReportGenerator) {

        this.leaveReportRepository = require(
                leaveReportRepository,
                "LeaveReportRepository is required."
        );

        this.jasperReportGenerator = require(
                jasperReportGenerator,
                "JasperReportGenerator is required."
        );
    }

    // returns leave report data
    public List<LeaveReportDTO> getLeaveReport(Integer departmentId,
                                               Integer employeeId,
                                               String leaveType,
                                               String leaveStatus,
                                               LocalDate startDate,
                                               LocalDate endDate) {

        validateDateRange(startDate, endDate);

        return leaveReportRepository.findLeaveReport(
                departmentId,
                employeeId,
                leaveType,
                leaveStatus,
                Date.valueOf(startDate),
                Date.valueOf(endDate)
        );
    }

    // previews the leave report
    public void previewLeaveReport(Integer departmentId,
                                   Integer employeeId,
                                   String leaveType,
                                   String leaveStatus,
                                   LocalDate startDate,
                                   LocalDate endDate) {

        List<LeaveReportDTO> reportData = getLeaveReport(
                departmentId,
                employeeId,
                leaveType,
                leaveStatus,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                LEAVE_REPORT,
                buildReportParameters(startDate, endDate),
                reportData
        );

        jasperReportGenerator.showPreview(jasperPrint);
    }

    // exports the leave report to PDF
    public void exportLeaveReportToPdf(Integer departmentId,
                                       Integer employeeId,
                                       String leaveType,
                                       String leaveStatus,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       String outputPath) {

        validateOutputPath(outputPath);

        List<LeaveReportDTO> reportData = getLeaveReport(
                departmentId,
                employeeId,
                leaveType,
                leaveStatus,
                startDate,
                endDate
        );

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                LEAVE_REPORT,
                buildReportParameters(startDate, endDate),
                reportData
        );

        jasperReportGenerator.exportToPdf(jasperPrint, outputPath);
    }

    // builds leave report parameters
    private Map<String, Object> buildReportParameters(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("REPORT_TITLE", "Leave Report");
        parameters.put("START_DATE", startDate);
        parameters.put("END_DATE", endDate);

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