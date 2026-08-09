package motorph.api;

import motorph.api.dto.report.PayrollDashboardDTO;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper mirroring the backend's /api/v1/reports endpoints. Every
 * report is a PDF fetch - the desktop UI never rendered report data in a
 * table, only a Jasper preview/export, so the caller decides what to do
 * with the bytes (open a temp file for "preview", save to a chosen path for
 * "export").
 */
public final class ReportApiClient {

    private final HttpApiClient httpApiClient;

    public ReportApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public byte[] getAttendanceReportPdf(Integer departmentId, Integer employeeId, String status,
                                         LocalDate startDate, LocalDate endDate) {
        String query = query(
                param("departmentId", departmentId),
                param("employeeId", employeeId),
                param("status", status),
                param("startDate", startDate),
                param("endDate", endDate)
        );
        return httpApiClient.getBytes("/api/v1/reports/attendance/pdf" + query);
    }

    public byte[] getEmployeeTimecardReportPdf(int employeeId, LocalDate startDate, LocalDate endDate) {
        String query = query(
                param("employeeId", employeeId),
                param("startDate", startDate),
                param("endDate", endDate)
        );
        return httpApiClient.getBytes("/api/v1/reports/employee-timecard/pdf" + query);
    }

    public byte[] getAuditReportPdf(Integer userAccountId, String action, LocalDate startDate, LocalDate endDate) {
        String query = query(
                param("userAccountId", userAccountId),
                param("action", action),
                param("startDate", startDate),
                param("endDate", endDate)
        );
        return httpApiClient.getBytes("/api/v1/reports/audit/pdf" + query);
    }

    public byte[] getOrganizationPayrollReportPdf(Integer departmentId, String departmentName, Integer employeeId,
                                                  Integer payrunId, LocalDate startDate, LocalDate endDate) {
        String query = query(
                param("departmentId", departmentId),
                param("departmentName", departmentName),
                param("employeeId", employeeId),
                param("payrunId", payrunId),
                param("startDate", startDate),
                param("endDate", endDate)
        );
        return httpApiClient.getBytes("/api/v1/reports/organization-payroll/pdf" + query);
    }

    public byte[] getPayrollSummaryReportPdf(Integer departmentId, Integer payrunId, LocalDate startDate, LocalDate endDate) {
        String query = query(
                param("departmentId", departmentId),
                param("payrunId", payrunId),
                param("startDate", startDate),
                param("endDate", endDate)
        );
        return httpApiClient.getBytes("/api/v1/reports/payroll-summary/pdf" + query);
    }

    public byte[] getTicketReportPdf(Integer departmentId, Integer employeeId, String category, String routingTeam,
                                     String status, LocalDate startDate, LocalDate endDate) {
        String query = query(
                param("departmentId", departmentId),
                param("employeeId", employeeId),
                param("category", category),
                param("routingTeam", routingTeam),
                param("status", status),
                param("startDate", startDate),
                param("endDate", endDate)
        );
        return httpApiClient.getBytes("/api/v1/reports/tickets/pdf" + query);
    }

    public PayrollDashboardDTO getLatestPayrollDashboard() {
        return httpApiClient.get("/api/v1/reports/payroll-dashboard/latest", PayrollDashboardDTO.class);
    }

    private String param(String name, Object value) {
        return value == null ? null : name + "=" + encode(value.toString());
    }

    private String query(String... params) {
        List<String> present = new ArrayList<>();
        for (String p : params) {
            if (p != null) {
                present.add(p);
            }
        }
        return present.isEmpty() ? "" : "?" + String.join("&", present);
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
