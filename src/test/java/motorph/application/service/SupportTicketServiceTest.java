package motorph.application.service;


import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.UnauthorizedException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.SupportTicket;
import motorph.domain.value.Role;
import motorph.domain.value.RoutingTeam;
import motorph.domain.value.SupportTicketCategory;
import motorph.domain.value.SupportTicketStatus;
import motorph.infrastructure.repository.SupportTicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDateTime;
import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Tests support ticket service using mocked dependencies.
 */
@DisplayName("SupportTicketService Mock Tests")
class SupportTicketServiceTest {


    private static final Logger logger = Logger.getLogger(SupportTicketServiceTest.class.getName());


    // positive: submits payroll ticket routed to payroll team
    @Test
    @DisplayName("Positive - submitTicket should route payroll ticket to payroll team")
    void submitTicketShouldRoutePayrollTicketToPayrollTeam() {
        logger.info("Testing payroll ticket routes to the payroll team.");

        SupportTicketRepository repository = mock(SupportTicketRepository.class);
        SupportTicketService service = new SupportTicketService(repository);


        service.submitTicket(
                10001,
                SupportTicketCategory.PAYROLL,
                "Incorrect payroll deduction",
                "My SSS deduction looks incorrect for this pay period."
        );


        verify(repository).save(argThat(ticket ->
                ticket.getEmployeeId() == 10001
                        && ticket.getCategory() == SupportTicketCategory.PAYROLL
                        && ticket.getRoutingTeam() == RoutingTeam.PAYROLL
                        && ticket.isOpen()
        ));
    }


    // positive: submits attendance ticket routed to HR team
    @Test
    @DisplayName("Positive - submitTicket should route attendance ticket to HR team")
    void submitTicketShouldRouteAttendanceTicketToHrTeam() {
        logger.info("Testing attendance ticket routes to the HR team.");

        SupportTicketRepository repository = mock(SupportTicketRepository.class);
        SupportTicketService service = new SupportTicketService(repository);


        service.submitTicket(
                10001,
                SupportTicketCategory.ATTENDANCE,
                "Missing time out",
                "I forgot to record my time out yesterday."
        );


        verify(repository).save(argThat(ticket ->
                ticket.getRoutingTeam() == RoutingTeam.HR
        ));
    }


    // negative: rejects missing subject
    @Test
    @DisplayName("Negative - submitTicket should reject missing subject")
    void submitTicketShouldRejectMissingSubject() {
        logger.info("Testing ticket submission rejects a missing subject.");

        SupportTicketRepository repository = mock(SupportTicketRepository.class);
        SupportTicketService service = new SupportTicketService(repository);


        assertThrows(
                ValidationException.class,
                () -> service.submitTicket(
                        10001,
                        SupportTicketCategory.PAYROLL,
                        "",
                        "Description is valid."
                )
        );


        verify(repository, never()).save(any(SupportTicket.class));
    }


    // a privileged role resolves an open ticket
    @Test
    @DisplayName("Positive - resolveTicket should mark an open ticket resolved")
    void resolveTicketShouldMarkResolved() {
        logger.info("Testing a privileged role resolves an open ticket.");

        SupportTicketRepository repository = mock(SupportTicketRepository.class);
        when(repository.findById(1)).thenReturn(ticket(SupportTicketStatus.OPEN, 10001));

        SupportTicketService service = new SupportTicketService(repository);

        service.resolveTicket(1, 5, Role.IT);

        verify(repository).markResolved(1, 5);
    }


    // a non-privileged role cannot resolve a ticket
    @Test
    @DisplayName("Negative - resolveTicket should deny an employee role")
    void resolveTicketShouldDenyEmployeeRole() {
        logger.info("Testing an employee role cannot resolve a ticket.");

        SupportTicketRepository repository = mock(SupportTicketRepository.class);

        SupportTicketService service = new SupportTicketService(repository);

        assertThrows(
                UnauthorizedException.class,
                () -> service.resolveTicket(1, 5, Role.EMPLOYEE));
        verify(repository, never()).markResolved(anyInt(), anyInt());
    }


    // an already-resolved ticket cannot be resolved again
    @Test
    @DisplayName("Negative - resolveTicket should reject a non-open ticket")
    void resolveTicketShouldRejectNonOpen() {
        logger.info("Testing an already-resolved ticket cannot be resolved again.");

        SupportTicketRepository repository = mock(SupportTicketRepository.class);
        when(repository.findById(1)).thenReturn(ticket(SupportTicketStatus.RESOLVED, 10001));

        SupportTicketService service = new SupportTicketService(repository);

        assertThrows(
                BusinessRuleException.class,
                () -> service.resolveTicket(1, 5, Role.IT));
    }


    // the owner cancels their own open ticket
    @Test
    @DisplayName("Positive - cancelTicket should let the owner cancel their open ticket")
    void cancelTicketShouldLetOwnerCancel() {
        logger.info("Testing the owner can cancel their open ticket.");

        SupportTicketRepository repository = mock(SupportTicketRepository.class);
        when(repository.findById(1)).thenReturn(ticket(SupportTicketStatus.OPEN, 10001));

        SupportTicketService service = new SupportTicketService(repository);

        service.cancelTicket(1, 10001, Role.EMPLOYEE);

        verify(repository).markCancelled(1);
    }


    // a non-owner employee cannot cancel someone else's ticket
    @Test
    @DisplayName("Negative - cancelTicket should block a non-owner employee")
    void cancelTicketShouldBlockNonOwner() {
        logger.info("Testing a non-owner employee cannot cancel a ticket.");

        SupportTicketRepository repository = mock(SupportTicketRepository.class);
        when(repository.findById(1)).thenReturn(ticket(SupportTicketStatus.OPEN, 10001));

        SupportTicketService service = new SupportTicketService(repository);

        assertThrows(
                UnauthorizedException.class,
                () -> service.cancelTicket(1, 10002, Role.EMPLOYEE));
    }


    // builds a support ticket with the given status and owner
    private SupportTicket ticket(SupportTicketStatus status, int ownerEmployeeId) {
        return new SupportTicket(
                1, ownerEmployeeId, SupportTicketCategory.PAYROLL, RoutingTeam.PAYROLL,
                "Subject", "Description", status, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }
}

