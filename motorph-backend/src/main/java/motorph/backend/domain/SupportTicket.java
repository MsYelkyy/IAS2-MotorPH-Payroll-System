package motorph.backend.domain;

import motorph.backend.domain.value.RoutingTeam;
import motorph.backend.domain.value.SupportTicketCategory;
import motorph.backend.domain.value.SupportTicketStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents an employee support ticket.
 */
public class SupportTicket {

    private Integer ticketId;
    private int employeeId;
    private SupportTicketCategory category;
    private RoutingTeam routingTeam;
    private String subject;
    private String description;
    private SupportTicketStatus status;
    private Integer resolvedByUserAccountId;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SupportTicket(Integer ticketId,
                         int employeeId,
                         SupportTicketCategory category,
                         RoutingTeam routingTeam,
                         String subject,
                         String description,
                         SupportTicketStatus status,
                         Integer resolvedByUserAccountId,
                         LocalDateTime resolvedAt,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt) {

        setTicketId(ticketId);
        setEmployeeId(employeeId);
        setCategory(category);
        setRoutingTeam(routingTeam);
        setSubject(subject);
        setDescription(description);
        setStatus(status);
        setResolvedByUserAccountId(resolvedByUserAccountId);
        setResolvedAt(resolvedAt);
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
    }

    public SupportTicket(int employeeId,
                         SupportTicketCategory category,
                         RoutingTeam routingTeam,
                         String subject,
                         String description) {

        this(
                null,
                employeeId,
                category,
                routingTeam,
                subject,
                description,
                SupportTicketStatus.OPEN,
                null,
                null,
                LocalDateTime.now(),
                null
        );
    }

    public Integer getTicketId() {
        return ticketId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public SupportTicketCategory getCategory() {
        return category;
    }

    public RoutingTeam getRoutingTeam() {
        return routingTeam;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public SupportTicketStatus getStatus() {
        return status;
    }

    public Integer getResolvedByUserAccountId() {
        return resolvedByUserAccountId;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOpen() {
        return status == SupportTicketStatus.OPEN;
    }

    public boolean isResolved() {
        return status == SupportTicketStatus.RESOLVED;
    }

    public boolean isCancelled() {
        return status == SupportTicketStatus.CANCELLED;
    }

    public void markResolved(int resolvedByUserAccountId) {
        if (!isOpen()) {
            throw new IllegalStateException("Only open tickets can be resolved.");
        }

        setResolvedByUserAccountId(resolvedByUserAccountId);
        this.status = SupportTicketStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markCancelled() {
        if (!isOpen()) {
            throw new IllegalStateException("Only open tickets can be cancelled.");
        }

        this.status = SupportTicketStatus.CANCELLED;
    }

    public void setTicketId(Integer ticketId) {
        if (ticketId != null && ticketId <= 0) {
            throw new IllegalArgumentException("Ticket ID must be positive.");
        }

        this.ticketId = ticketId;
    }

    public void setEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }

        this.employeeId = employeeId;
    }

    public void setCategory(SupportTicketCategory category) {
        this.category = Objects.requireNonNull(category, "Support ticket category is required.");
    }

    public void setRoutingTeam(RoutingTeam routingTeam) {
        this.routingTeam = Objects.requireNonNull(routingTeam, "Routing team is required.");
    }

    public void setSubject(String subject) {
        String value = requireNonBlank(subject, "Subject is required.");

        if (value.length() > 150) {
            throw new IllegalArgumentException("Subject cannot exceed 150 characters.");
        }

        this.subject = value;
    }

    public void setDescription(String description) {
        String value = requireNonBlank(description, "Description is required.");

        if (value.length() > 1000) {
            throw new IllegalArgumentException("Description cannot exceed 1000 characters.");
        }

        this.description = value;
    }

    public void setStatus(SupportTicketStatus status) {
        this.status = Objects.requireNonNull(status, "Support ticket status is required.");
    }

    public void setResolvedByUserAccountId(Integer resolvedByUserAccountId) {
        if (resolvedByUserAccountId != null && resolvedByUserAccountId <= 0) {
            throw new IllegalArgumentException("Resolved by user account ID must be positive.");
        }

        this.resolvedByUserAccountId = resolvedByUserAccountId;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = Objects.requireNonNull(createdAt, "Created date and time is required.");
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}
