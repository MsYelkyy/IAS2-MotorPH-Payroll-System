package motorph.application.service;


import motorph.application.exception.ValidationException;
import motorph.domain.model.SupportTicket;
import motorph.domain.value.RoutingTeam;
import motorph.domain.value.SupportTicketCategory;
import motorph.infrastructure.repository.SupportTicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Tests support ticket service using mocked dependencies.
 */
@DisplayName("SupportTicketService Mock Tests")
class SupportTicketServiceTest {


    // positive: submits payroll ticket routed to payroll team
    @Test
    @DisplayName("Positive - submitTicket should route payroll ticket to payroll team")
    void submitTicketShouldRoutePayrollTicketToPayrollTeam() {
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
}

