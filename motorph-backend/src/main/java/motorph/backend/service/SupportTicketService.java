package motorph.backend.service;

import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.exception.ValidationException;
import motorph.backend.domain.Employee;
import motorph.backend.domain.SupportTicket;
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
 * Handles employee support tickets. Each team sees and resolves only the tickets routed to it
 * (IT, HR, and Finance for payroll), Admin sees and resolves every ticket, and only Admin can
 * cancel one.
 */
@Service
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final EmployeeRepository employeeRepository;

    public SupportTicketService(SupportTicketRepository supportTicketRepository,
                                EmployeeRepository employeeRepository) {
        this.supportTicketRepository = supportTicketRepository;
        this.employeeRepository = employeeRepository;
    }

    public void submitTicket(int employeeId,
                             SupportTicketCategory category,
                             String subject,
                             String description) {

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
     * Counts open tickets the caller can handle: every open ticket for Admin, otherwise their team's.
     */
    public int getOpenTicketCount(Role actorRole) {
        List<SupportTicket> open = supportTicketRepository.findOpenTickets();

        if (actorRole == Role.ADMIN) {
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

        if (actorRole != Role.ADMIN && requireHandlingTeam(actorRole) != routingTeam) {
            throw new UnauthorizedException("You can only view tickets routed to your team.");
        }

        return toRows(supportTicketRepository.findByRoutingTeam(routingTeam));
    }

    public List<SupportTicketRowDTO> getAllTickets(Role actorRole) {
        if (actorRole != Role.ADMIN) {
            throw new UnauthorizedException("Only Admin can view all support tickets.");
        }

        return toRows(supportTicketRepository.findAll());
    }

    /**
     * Resolves an open ticket. Admin can resolve any ticket; a team can resolve only its own.
     */
    public void resolveTicket(int ticketId, int resolvedByUserAccountId, Role resolverRole) {
        validateTicketId(ticketId);

        SupportTicket ticket = getTicketById(ticketId);

        if (resolverRole != Role.ADMIN && requireHandlingTeam(resolverRole) != ticket.getRoutingTeam()) {
            throw new UnauthorizedException("You can only resolve tickets routed to your team.");
        }

        if (ticket.getStatus() != SupportTicketStatus.OPEN) {
            throw new BusinessRuleException("Only open tickets can be resolved.");
        }

        supportTicketRepository.markResolved(ticketId, resolvedByUserAccountId);
    }

    public void cancelTicket(int ticketId, Role actorRole) {
        validateTicketId(ticketId);

        if (actorRole != Role.ADMIN) {
            throw new UnauthorizedException("Only Admin can cancel a support ticket.");
        }

        SupportTicket ticket = getTicketById(ticketId);

        if (ticket.getStatus() != SupportTicketStatus.OPEN) {
            throw new BusinessRuleException("Only open tickets can be cancelled.");
        }

        supportTicketRepository.markCancelled(ticketId);
    }

    private RoutingTeam requireHandlingTeam(Role role) {
        RoutingTeam team = switch (role) {
            case IT -> RoutingTeam.IT;
            case HR -> RoutingTeam.HR;
            case FINANCE -> RoutingTeam.PAYROLL;
            case ADMIN, EMPLOYEE -> null;
        };

        if (team == null) {
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
