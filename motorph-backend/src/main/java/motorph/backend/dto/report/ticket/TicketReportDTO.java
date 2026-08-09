package motorph.backend.dto.report.ticket;

import java.time.LocalDateTime;

/** One row of the support ticket report. */
public class TicketReportDTO {

    private final int ticketId;
    private final int employeeId;
    private final String employeeName;
    private final String departmentName;
    private final String category;
    private final String routingTeam;
    private final String subject;
    private final String status;
    private final String resolvedBy;
    private final LocalDateTime resolvedAt;
    private final LocalDateTime createdAt;

    public TicketReportDTO(int ticketId, int employeeId, String employeeName, String departmentName,
                           String category, String routingTeam, String subject, String status,
                           String resolvedBy, LocalDateTime resolvedAt, LocalDateTime createdAt) {
        this.ticketId = ticketId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.departmentName = departmentName;
        this.category = category;
        this.routingTeam = routingTeam;
        this.subject = subject;
        this.status = status;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.createdAt = createdAt;
    }

    public int getTicketId() {
        return ticketId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getCategory() {
        return category;
    }

    public String getRoutingTeam() {
        return routingTeam;
    }

    public String getSubject() {
        return subject;
    }

    public String getStatus() {
        return status;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
