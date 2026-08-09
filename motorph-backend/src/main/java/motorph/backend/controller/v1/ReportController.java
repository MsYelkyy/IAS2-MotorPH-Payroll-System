package motorph.backend.controller.v1;

import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.report.payroll.PayrollDashboardDTO;
import motorph.backend.service.PermissionService;
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
 * Report generation endpoints. Every report is a PDF download - the desktop
 * UI never showed report data in a table, only a Jasper preview/export, so
 * there is no JSON list endpoint here (the payroll dashboard is the one
 * exception: it always was pure JSON feeding JavaFX charts, no PDF).
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

    public ReportController(AttendanceReportService attendanceReportService,
                            AuditReportService auditReportService,
                            PayrollDashboardService payrollDashboardService,
                            PayrollReportService payrollReportService,
                            TicketReportService ticketReportService,
                            PermissionService permissionService) {
        this.attendanceReportService = attendanceReportService;
        this.auditReportService = auditReportService;
        this.payrollDashboardService = payrollDashboardService;
        this.payrollReportService = payrollReportService;
        this.ticketReportService = ticketReportService;
        this.permissionService = permissionService;
    }

    @GetMapping(value = "/attendance/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> attendancePdf(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) String status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        requireReportsPermission();
        byte[] pdf = attendanceReportService.generateAttendanceReportPdf(departmentId, employeeId, status, startDate, endDate);
        return pdfResponse(pdf, "attendance-report.pdf");
    }

    @GetMapping(value = "/employee-timecard/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> employeeTimecardPdf(
            @RequestParam int employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        requireReportsPermission();
        byte[] pdf = attendanceReportService.generateEmployeeTimecardReportPdf(employeeId, startDate, endDate);
        return pdfResponse(pdf, "employee-timecard-" + employeeId + ".pdf");
    }

    @GetMapping(value = "/audit/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> auditPdf(
            @RequestParam(required = false) Integer userAccountId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        requireReportsPermission();
        byte[] pdf = auditReportService.generateAuditReportPdf(userAccountId, action, startDate, endDate);
        return pdfResponse(pdf, "audit-report.pdf");
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
        byte[] pdf = payrollReportService.generateOrganizationPayrollReportPdf(
                departmentId, departmentName, employeeId, payrunId, startDate, endDate);
        return pdfResponse(pdf, "organization-payroll-report.pdf");
    }

    @GetMapping(value = "/payroll-summary/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> payrollSummaryPdf(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer payrunId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        requireReportsPermission();
        byte[] pdf = payrollReportService.generatePayrollSummaryReportPdf(departmentId, payrunId, startDate, endDate);
        return pdfResponse(pdf, "payroll-summary-report.pdf");
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
        byte[] pdf = ticketReportService.generateTicketReportPdf(
                departmentId, employeeId, category, routingTeam, status, startDate, endDate);
        return pdfResponse(pdf, "ticket-report.pdf");
    }

    @GetMapping("/payroll-dashboard/latest")
    public PayrollDashboardDTO latestPayrollDashboard() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_PAYROLL, PermissionKey.VIEW_REPORTS, PermissionKey.GENERATE_REPORTS);
        return payrollDashboardService.getLatestDashboard();
    }

    @GetMapping("/payroll-dashboard/{payrollMonth}")
    public PayrollDashboardDTO payrollDashboard(@PathVariable String payrollMonth) {
        permissionService.requireAnyPermission(PermissionKey.VIEW_PAYROLL, PermissionKey.VIEW_REPORTS, PermissionKey.GENERATE_REPORTS);
        return payrollDashboardService.getDashboard(payrollMonth);
    }

    private void requireReportsPermission() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_REPORTS, PermissionKey.GENERATE_REPORTS);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
