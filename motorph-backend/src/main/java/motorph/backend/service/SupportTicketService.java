package motorph.backend.service;

import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.exception.ValidationException;
import motorph.backend.domain.Employee;
import motorph.backend.domain.SupportTicket;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.domain.value.Role;
import motorph.backend.domain.value.RoutingTeam;
import motorph.backend.domain.value.SupportTicketCategory;
import motorph.backend.domain.value.SupportTicketStatus;
import motorph.backend.dto.support.SupportTicketRowDTO;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.SupportTicketRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles employee support tickets using centralized RBAC permission checks.
 */
@Service
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final EmployeeRepository employeeRepository;
    private final UserAccessLogService userAccessLogService;
    private final PermissionService permissionService;

    public SupportTicketService(SupportTicketRepository supportTicketRepository,
                                EmployeeRepository employeeRepository,
                                UserAccessLogService userAccessLogService,
                                PermissionService permissionService) {
        this.supportTicketRepository = supportTicketRepository;
        this.employeeRepository = employeeRepository;
        this.userAccessLogService = userAccessLogService;
        this.permissionService = permissionService;
    }

    /**
     * Submits a support ticket and records it in the audit log.
     * Ignore missing @param tags hahaha
     */
    public void submitTicket(int employeeId,
                             SupportTicketCategory category,
                             String subject,
                             String description,
                             int actorUserAccountId) {

        validateEmployeeId(employeeId);
        validateCategory(category);
        validateRequired(subject, "Subject is required.");
        validateRequired(description, "Description is required.");

        RoutingTeam routingTeam = resolveRoutingTeam(category);

        SupportTicket ticket = new SupportTicket(
                employeeId,
                category,
                routingTeam,
                subject,
                description
        );

        supportTicketRepository.save(ticket);

        userAccessLogService.logEvent(actorUserAccountId, UserAccessLogService.ACTION_SUPPORT_TICKET_SUBMITTED,
                "Submitted a " + category.name() + " support ticket routed to " + routingTeam.name(),
                UserAccessLogService.TARGET_SUPPORT_TICKET, null);
    }

    public SupportTicket getTicketById(int ticketId) {
        validateTicketId(ticketId);

        SupportTicket ticket = supportTicketRepository.findById(ticketId);

        if (ticket == null) {
            throw new EntityNotFoundException("Support ticket not found.");
        }

        return ticket;
    }

    /**
     * Counts open tickets the caller can handle based on permissions and active team role.
     * Used VIEW_TICKET token
     */
    public int getOpenTicketCount(Role actorRole) {
        permissionService.requirePermission(PermissionKey.VIEW_TICKETS);
        List<SupportTicket> open = supportTicketRepository.findOpenTickets();

        if (permissionService.hasPermission(PermissionKey.CANCEL_TICKETS)) {
            return open.size();
        }

        RoutingTeam team = requireHandlingTeam(actorRole);
        return (int) open.stream().filter(ticket -> ticket.getRoutingTeam() == team).count();
    }

    public List<SupportTicket> getTicketsByEmployee(int employeeId) {
        validateEmployeeId(employeeId);
        return supportTicketRepository.findByEmployeeId(employeeId);
    }

    public List<SupportTicketRowDTO> getTicketsByRoutingTeam(RoutingTeam routingTeam, Role actorRole) {
        if (routingTeam == null) {
            throw new ValidationException("Routing team is required.");
        }

        permissionService.requirePermission(PermissionKey.VIEW_TICKETS);

        if (!permissionService.hasPermission(PermissionKey.CANCEL_TICKETS) 
                && requireHandlingTeam(actorRole) != routingTeam) {
            throw new UnauthorizedException("You can only view tickets routed to your team.");
        }

        return toRows(supportTicketRepository.findByRoutingTeam(routingTeam));
    }

    public List<SupportTicketRowDTO> getAllTickets(Role actorRole) {
        permissionService.requirePermission(PermissionKey.CANCEL_TICKETS);
        return toRows(supportTicketRepository.findAll());
    }

    /**
     * Resolves an open ticket after validating the RESOLVE_TICKETS permission key.
     */
    public void resolveTicket(int ticketId, int resolvedByUserAccountId, Role resolverRole) {
        validateTicketId(ticketId);
        permissionService.requirePermission(PermissionKey.RESOLVE_TICKETS);

        SupportTicket ticket = getTicketById(ticketId);

        if (!permissionService.hasPermission(PermissionKey.CANCEL_TICKETS) 
                && requireHandlingTeam(resolverRole) != ticket.getRoutingTeam()) {
            throw new UnauthorizedException("You can only resolve tickets routed to your team.");
        }

        if (ticket.getStatus() != SupportTicketStatus.OPEN) {
            throw new BusinessRuleException("Only open tickets can be resolved.");
        }

        supportTicketRepository.markResolved(ticketId, resolvedByUserAccountId);

        userAccessLogService.logEvent(resolvedByUserAccountId, UserAccessLogService.ACTION_SUPPORT_TICKET_RESOLVED,
                "Resolved support ticket ID " + ticketId,
                UserAccessLogService.TARGET_SUPPORT_TICKET, ticketId);
    }

    /**
     * Cancels an open ticket after enforcing the CANCEL_TICKETS permission key.
     */
    public void cancelTicket(int ticketId, Role actorRole, int actorUserAccountId) {
        validateTicketId(ticketId);
        permissionService.requirePermission(PermissionKey.CANCEL_TICKETS);

        SupportTicket ticket = getTicketById(ticketId);

        if (ticket.getStatus() != SupportTicketStatus.OPEN) {
            throw new BusinessRuleException("Only open tickets can be cancelled.");
        }

        supportTicketRepository.markCancelled(ticketId);

        userAccessLogService.logEvent(actorUserAccountId, UserAccessLogService.ACTION_SUPPORT_TICKET_CANCELLED,
                "Cancelled support ticket ID " + ticketId,
                UserAccessLogService.TARGET_SUPPORT_TICKET, ticketId);
    }

    private RoutingTeam requireHandlingTeam(Role role) {
        RoutingTeam team = switch (role) {
            case IT -> RoutingTeam.IT;
            case HR -> RoutingTeam.HR;
            case FINANCE -> RoutingTeam.PAYROLL;
            case ADMIN, EMPLOYEE -> null;
        };

        if (team == null && !permissionService.hasPermission(PermissionKey.CANCEL_TICKETS)) {
            throw new UnauthorizedException("You do not have permission to perform this action.");
        }

        return team;
    }

    private List<SupportTicketRowDTO> toRows(List<SupportTicket> tickets) {
        Map<Integer, String> namesByEmployeeId = employeeRepository.findAllWithInactive()
                .stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, Employee::getFullName, (a, b) -> a));

        return tickets.stream()
                .map(ticket -> new SupportTicketRowDTO(
                        ticket.getTicketId(),
                        ticket.getEmployeeId(),
                        namesByEmployeeId.getOrDefault(ticket.getEmployeeId(), "Employee #" + ticket.getEmployeeId()),
                        ticket.getCategory(),
                        ticket.getRoutingTeam(),
                        ticket.getSubject(),
                        ticket.getDescription(),
                        ticket.getStatus(),
                        ticket.getResolvedByUserAccountId(),
                        ticket.getResolvedAt(),
                        ticket.getCreatedAt(),
                        ticket.getUpdatedAt()))
                .toList();
    }

    private RoutingTeam resolveRoutingTeam(SupportTicketCategory category) {
        return switch (category) {
            case PAYROLL -> RoutingTeam.PAYROLL;
            case ATTENDANCE -> RoutingTeam.HR;
            case ACCOUNT, SYSTEM -> RoutingTeam.IT;
            case OTHER -> RoutingTeam.ADMIN;
        };
    }

    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    private void validateTicketId(int ticketId) {
        if (ticketId <= 0) {
            throw new ValidationException("Ticket ID is required.");
        }
    }

    private void validateCategory(SupportTicketCategory category) {
        if (category == null) {
            throw new ValidationException("Support ticket category is required.");
        }
    }

    private void validateRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(message);
        }
    }
}