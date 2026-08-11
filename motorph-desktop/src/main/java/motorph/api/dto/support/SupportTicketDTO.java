package motorph.api.dto.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import motorph.domain.value.RoutingTeam;
import motorph.domain.value.SupportTicketCategory;
import motorph.domain.value.SupportTicketStatus;

import java.time.LocalDateTime;

/**
 * Wire copy of the backend's SupportTicket domain model, mirroring the old
 * motorph.domain.model.SupportTicket getter names.
 */
public class SupportTicketDTO {

    private final Integer ticketId;
    private final int employeeId;
    private final SupportTicketCategory category;
    private final RoutingTeam routingTeam;
    private final String subject;
    private final String description;
    private final SupportTicketStatus status;
    private final Integer resolvedByUserAccountId;
    private final LocalDateTime resolvedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @JsonCreator
    public SupportTicketDTO(@JsonProperty("ticketId") Integer ticketId,
                            @JsonProperty("employeeId") int employeeId,
                            @JsonProperty("category") SupportTicketCategory category,
                            @JsonProperty("routingTeam") RoutingTeam routingTeam,
                            @JsonProperty("subject") String subject,
                            @JsonProperty("description") String description,
                            @JsonProperty("status") SupportTicketStatus status,
                            @JsonProperty("resolvedByUserAccountId") Integer resolvedByUserAccountId,
                            @JsonProperty("resolvedAt") LocalDateTime resolvedAt,
                            @JsonProperty("createdAt") LocalDateTime createdAt,
                            @JsonProperty("updatedAt") LocalDateTime updatedAt) {
        this.ticketId = ticketId;
        this.employeeId = employeeId;
        this.category = category;
        this.routingTeam = routingTeam;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.resolvedByUserAccountId = resolvedByUserAccountId;
        this.resolvedAt = resolvedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
}
