package motorph.backend.service;

import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.exception.ValidationException;
import motorph.backend.domain.SupportTicket;
import motorph.backend.domain.value.Role;
import motorph.backend.domain.value.RoutingTeam;
import motorph.backend.domain.value.SupportTicketCategory;
import motorph.backend.domain.value.SupportTicketStatus;
import motorph.backend.repository.SupportTicketRepository;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles business rules for employee support tickets. Ported from the
 * desktop's SupportTicketService. The role checks in resolveTicket/
 * cancelTicket are role-based, not permission-key based, matching the
 * original exactly - there is no dedicated ticket-management permission key
 * in the system, and this preserves the original's precise role set (note
 * the asymmetry: resolve allows IT/HR/ADMIN/FINANCE, cancel's privileged set
 * is IT/ADMIN/HR only - FINANCE cannot cancel another employee's ticket).
 */
@Service
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;

    public SupportTicketService(SupportTicketRepository supportTicketRepository) {
        this.supportTicketRepository = supportTicketRepository;
    }

    // submits a new employee support ticket - employeeId is always the caller's own
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

    public int getOpenTicketCount() {
        return supportTicketRepository.findOpenTickets().size();
    }

    public List<SupportTicket> getTicketsByEmployee(int employeeId) {
        validateEmployeeId(employeeId);
        return supportTicketRepository.findByEmployeeId(employeeId);
    }

    public List<SupportTicket> getTicketsByRoutingTeam(RoutingTeam routingTeam) {
        if (routingTeam == null) {
            throw new ValidationException("Routing team is required.");
        }

        return supportTicketRepository.findByRoutingTeam(routingTeam);
    }

    public List<SupportTicket> getAllTickets() {
        return supportTicketRepository.findAll();
    }

    // resolves an open support ticket; only IT, HR, ADMIN, or FINANCE roles may resolve
    public void resolveTicket(int ticketId, int resolvedByUserAccountId, Role resolverRole) {
        validateTicketId(ticketId);

        if (resolverRole != Role.IT && resolverRole != Role.HR
                && resolverRole != Role.ADMIN && resolverRole != Role.FINANCE) {
            throw new UnauthorizedException("Only IT, HR, Admin, or Finance roles can resolve tickets.");
        }

        SupportTicket ticket = getTicketById(ticketId);

        if (ticket.getStatus() != SupportTicketStatus.OPEN) {
            throw new BusinessRuleException("Only open tickets can be resolved.");
        }

        supportTicketRepository.markResolved(ticketId, resolvedByUserAccountId);
    }

    // cancels an open ticket; owner may cancel their own, IT/Admin/HR may cancel any
    public void cancelTicket(int ticketId, int actorEmployeeId, Role actorRole) {
        validateTicketId(ticketId);

        SupportTicket ticket = getTicketById(ticketId);

        boolean isPrivileged = actorRole == Role.IT
                || actorRole == Role.ADMIN
                || actorRole == Role.HR;

        if (!isPrivileged && ticket.getEmployeeId() != actorEmployeeId) {
            throw new UnauthorizedException("You can only cancel your own support ticket.");
        }

        if (ticket.getStatus() != SupportTicketStatus.OPEN) {
            throw new BusinessRuleException("Only open tickets can be cancelled.");
        }

        supportTicketRepository.markCancelled(ticketId);
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
