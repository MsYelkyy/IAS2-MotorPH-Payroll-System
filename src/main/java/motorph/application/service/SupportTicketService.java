package motorph.application.service;

import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.SupportTicket;
import motorph.domain.value.RoutingTeam;
import motorph.domain.value.SupportTicketCategory;
import motorph.infrastructure.repository.SupportTicketRepository;

import java.util.List;

/**
 * Handles business rules for employee support tickets.
 */
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;

    // constructors
    public SupportTicketService(SupportTicketRepository supportTicketRepository) {
        if (supportTicketRepository == null) {
            throw new IllegalArgumentException("SupportTicketRepository is required.");
        }

        this.supportTicketRepository = supportTicketRepository;
    }

    // submits a new employee support ticket
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

    // returns one support ticket by id
    public SupportTicket getTicketById(int ticketId) {
        validateTicketId(ticketId);

        SupportTicket ticket = supportTicketRepository.findById(ticketId);

        if (ticket == null) {
            throw new EntityNotFoundException("Support ticket not found.");
        }

        return ticket;
    }

    // returns tickets submitted by an employee
    public List<SupportTicket> getTicketsByEmployee(int employeeId) {
        validateEmployeeId(employeeId);
        return supportTicketRepository.findByEmployeeId(employeeId);
    }

    // returns tickets routed to a team
    public List<SupportTicket> getTicketsByRoutingTeam(RoutingTeam routingTeam) {
        if (routingTeam == null) {
            throw new ValidationException("Routing team is required.");
        }

        return supportTicketRepository.findByRoutingTeam(routingTeam);
    }

    // returns all open tickets
    public List<SupportTicket> getOpenTickets() {
        return supportTicketRepository.findOpenTickets();
    }

    // resolves an open support ticket
    public void resolveTicket(int ticketId, int resolvedByUserAccountId) {
        validateTicketId(ticketId);

        if (resolvedByUserAccountId <= 0) {
            throw new ValidationException("Resolved by user account ID is required.");
        }

        supportTicketRepository.markResolved(ticketId, resolvedByUserAccountId);
    }

    // cancels an open support ticket
    public void cancelTicket(int ticketId) {
        validateTicketId(ticketId);
        supportTicketRepository.markCancelled(ticketId);
    }

    // resolves routing team from support ticket category
    private RoutingTeam resolveRoutingTeam(SupportTicketCategory category) {
        return switch (category) {
            case PAYROLL -> RoutingTeam.PAYROLL;
            case ATTENDANCE -> RoutingTeam.HR;
            case ACCOUNT, SYSTEM -> RoutingTeam.IT;
            case OTHER -> RoutingTeam.ADMIN;
        };
    }

    // validates employee id input
    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    // validates ticket id input
    private void validateTicketId(int ticketId) {
        if (ticketId <= 0) {
            throw new ValidationException("Ticket ID is required.");
        }
    }

    // validates ticket category input
    private void validateCategory(SupportTicketCategory category) {
        if (category == null) {
            throw new ValidationException("Support ticket category is required.");
        }
    }

    // validates required text
    private void validateRequired(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(message);
        }
    }
}