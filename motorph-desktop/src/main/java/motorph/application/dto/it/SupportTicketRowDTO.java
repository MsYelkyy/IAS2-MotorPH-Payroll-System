package motorph.application.dto.it;

import motorph.domain.value.RoutingTeam;
import motorph.domain.value.SupportTicketCategory;
import motorph.domain.value.SupportTicketStatus;

import java.time.LocalDateTime;

/** Carries support ticket row data for the IT support ticket screen. */
public class SupportTicketRowDTO {

    private final int ticketId;
    private final int employeeId;
    private final String employeeName;
    private final SupportTicketCategory category;
    private final RoutingTeam routingTeam;
    private final String subject;
    private final String description;
    private final SupportTicketStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime resolvedAt;

    // constructors
    public SupportTicketRowDTO(int ticketId,
                                int employeeId,
                                String employeeName,
                                SupportTicketCategory category,
                                RoutingTeam routingTeam,
                                String subject,
                                String description,
                                SupportTicketStatus status,
                                LocalDateTime createdAt,
                                LocalDateTime resolvedAt) {

        this.ticketId     = ticketId;
        this.employeeId   = employeeId;
        this.employeeName = employeeName;
        this.category     = category;
        this.routingTeam  = routingTeam;
        this.subject      = subject;
        this.description  = description;
        this.status       = status;
        this.createdAt    = createdAt;
        this.resolvedAt   = resolvedAt;
    }

    // getters and setters
    public int getTicketId()                  { return ticketId; }
    public int getEmployeeId()                { return employeeId; }
    public String getEmployeeName()           { return employeeName; }
    public SupportTicketCategory getCategory(){ return category; }
    public RoutingTeam getRoutingTeam()       { return routingTeam; }
    public String getSubject()                { return subject; }
    public String getDescription()            { return description; }
    public SupportTicketStatus getStatus()    { return status; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public LocalDateTime getResolvedAt()      { return resolvedAt; }
}
