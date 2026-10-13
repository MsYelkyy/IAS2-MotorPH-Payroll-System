package motorph.backend.controller.v1;

import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.report.payroll.PayrollDashboardDTO;
import motorph.backend.exception.ValidationException;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.PermissionService;
import motorph.backend.service.UserAccessLogService;
import motorph.backend.service.report.AttendanceReportService;
import motorph.backend.service.report.AuditReportService;
import motorph.backend.service.report.PayrollDashboardService;
import motorph.backend.service.report.PayrollReportService;
import motorph.backend.service.report.TicketReportService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Report endpoints. Reports are returned as PDF downloads, except the payroll dashboard, which
 * returns JSON for the client's charts.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final AttendanceReportService attendanceReportService;
    private final AuditReportService auditReportService;
    private final PayrollDashboardService payrollDashboardService;
    private final PayrollReportService payrollReportService;
    private final TicketReportService ticketReportService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;
    private final UserAccessLogService userAccessLogService;

    public ReportController(AttendanceReportService attendanceReportService,
                            AuditReportService auditReportService,
                            PayrollDashboardService payrollDashboardService,
                            PayrollReportService payrollReportService,
                            TicketReportService ticketReportService,
                            PermissionService permissionService,
                            CurrentUserProvider currentUserProvider,
                            UserAccessLogService userAccessLogService) {
        this.attendanceReportService = attendanceReportService;
        this.auditReportService = auditReportService;
        this.payrollDashboardService = payrollDashboardService;
        this.payrollReportService = payrollReportService;
        this.ticketReportService = ticketReportService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
        this.userAccessLogService = userAccessLogService;
    }

    @GetMapping(value = "/attendance/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> attendancePdf(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) String status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        requireReportsPermission();
        validateOptionalPositiveId(departmentId, "Department ID");
        validateOptionalPositiveId(employeeId, "Employee ID");
        validateDateRange(startDate, endDate);

        byte[] pdf = attendanceReportService.generateAttendanceReportPdf(
                departmentId, employeeId, sanitizeString(status), startDate, endDate);
        return pdfResponse(pdf, "attendance-report.pdf", "Exported attendance report" + period(startDate, endDate));
    }

    @GetMapping(value = "/employee-timecard/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> employeeTimecardPdf(
            @RequestParam int employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        requireReportsPermission();
        validatePositiveId(employeeId, "Employee ID");
        validateDateRange(startDate, endDate);

        byte[] pdf = attendanceReportService.generateEmployeeTimecardReportPdf(employeeId, startDate, endDate);
        return pdfResponse(pdf, "employee-timecard-" + employeeId + ".pdf",
                "Exported timecard report of employee ID " + employeeId + period(startDate, endDate));
    }

    @GetMapping(value = "/audit/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> auditPdf(
            @RequestParam(required = false) Integer userAccountId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        requireReportsPermission();
        validateOptionalPositiveId(userAccountId, "User Account ID");
        validateOptionalDateRange(startDate, endDate);

        byte[] pdf = auditReportService.generateAuditReportPdf(userAccountId, sanitizeString(action), startDate, endDate);
        return pdfResponse(pdf, "audit-report.pdf", "Exported audit report" + period(startDate, endDate));
    }

    @GetMapping(value = "/organization-payroll/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> organizationPayrollPdf(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) Integer payrunId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        requireReportsPermission();
        validateOptionalPositiveId(departmentId, "Department ID");
        validateOptionalPositiveId(employeeId, "Employee ID");
        validateOptionalPositiveId(payrunId, "Payrun ID");
        validateOptionalDateRange(startDate, endDate);

        byte[] pdf = payrollReportService.generateOrganizationPayrollReportPdf(
                departmentId, sanitizeString(departmentName), employeeId, payrunId, startDate, endDate);
        return pdfResponse(pdf, "organization-payroll-report.pdf",
                "Exported organization payroll report" + period(startDate, endDate));
    }

    @GetMapping(value = "/payroll-summary/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> payrollSummaryPdf(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer payrunId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        requireReportsPermission();
        validateOptionalPositiveId(departmentId, "Department ID");
        validateOptionalPositiveId(payrunId, "Payrun ID");
        validateOptionalDateRange(startDate, endDate);

        byte[] pdf = payrollReportService.generatePayrollSummaryReportPdf(departmentId, payrunId, startDate, endDate);
        return pdfResponse(pdf, "payroll-summary-report.pdf",
                "Exported payroll summary report" + (payrunId == null ? "" : " for payrun ID " + payrunId)
                        + period(startDate, endDate));
    }

    @GetMapping(value = "/tickets/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> ticketsPdf(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String routingTeam,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        requireReportsPermission();
        validateOptionalPositiveId(departmentId, "Department ID");
        validateOptionalPositiveId(employeeId, "Employee ID");
        validateOptionalDateRange(startDate, endDate);

        byte[] pdf = ticketReportService.generateTicketReportPdf(
                departmentId, employeeId, sanitizeString(category), sanitizeString(routingTeam),
                sanitizeString(status), startDate, endDate);
        return pdfResponse(pdf, "ticket-report.pdf", "Exported support ticket report" + period(startDate, endDate));
    }

    @GetMapping(value = "/payroll-dashboard/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public PayrollDashboardDTO latestPayrollDashboard() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_PAYROLL, PermissionKey.VIEW_REPORTS, PermissionKey.GENERATE_REPORTS);
        return payrollDashboardService.getLatestDashboard();
    }

    @GetMapping(value = "/payroll-dashboard/{payrollMonth}", produces = MediaType.APPLICATION_JSON_VALUE)
    public PayrollDashboardDTO payrollDashboard(@PathVariable String payrollMonth) {
        permissionService.requireAnyPermission(PermissionKey.VIEW_PAYROLL, PermissionKey.VIEW_REPORTS, PermissionKey.GENERATE_REPORTS);
        validatePayrollMonth(payrollMonth);
        return payrollDashboardService.getDashboard(payrollMonth);
    }

    private void requireReportsPermission() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_REPORTS, PermissionKey.GENERATE_REPORTS);
    }

    /**
     * Records the export in the audit log and wraps the PDF in the download response. Only reached
     * when a report was actually generated, so empty or rejected requests are not logged.
     */
    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename, String auditDetails) {
        userAccessLogService.logEvent(currentUserProvider.getCurrentUserAccountId(),
                UserAccessLogService.ACTION_REPORT_EXPORTED, auditDetails,
                UserAccessLogService.TARGET_REPORT, filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private void validatePositiveId(int id, String name) {
        if (id <= 0) {
            throw new ValidationException(name + " must be a positive integer.");
        }
    }

    private void validateOptionalPositiveId(Integer id, String name) {
        if (id != null && id <= 0) {
            throw new ValidationException(name + " must be a positive integer.");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required.");
        }
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date.");
        }
    }

    private void validateOptionalDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date.");
        }
    }

    private void validatePayrollMonth(String month) {
        if (month == null || !month.matches("^\\d{4}-\\d{2}$")) {
            throw new ValidationException("Payroll month must be formatted as YYYY-MM.");
        }
    }

    private String sanitizeString(String input) {
        return (input == null || input.isBlank()) ? null : input.trim();
    }

    private String period(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "";
        }
        return " for " + startDate + " to " + endDate;
    }
}
