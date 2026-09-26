package motorph.backend.service;

import motorph.backend.domain.SupportTicket;
import motorph.backend.domain.value.Role;
import motorph.backend.domain.value.RoutingTeam;
import motorph.backend.domain.value.SupportTicketCategory;
import motorph.backend.domain.value.SupportTicketStatus;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.SupportTicketRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Each team sees and resolves only its own tickets, Admin handles every ticket, and only Admin can
 * cancel one.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SupportTicketServiceTest {

    private static final int TICKET_ID = 42;
    private static final int RESOLVER_ACCOUNT_ID = 7;

    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private EmployeeRepository employeeRepository;

    @InjectMocks private SupportTicketService supportTicketService;

    @Test
    @DisplayName("Finance can list and resolve payroll tickets")
    void financeHandlesPayrollTickets() {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(openTicket(RoutingTeam.PAYROLL));

        assertThat(supportTicketService.getTicketsByRoutingTeam(RoutingTeam.PAYROLL, Role.FINANCE)).isEmpty();
        supportTicketService.resolveTicket(TICKET_ID, RESOLVER_ACCOUNT_ID, Role.FINANCE);

        verify(supportTicketRepository).markResolved(TICKET_ID, RESOLVER_ACCOUNT_ID);
    }

    @Test
    @DisplayName("A team cannot list or resolve another team's tickets")
    void teamCannotReachAnotherTeamsTickets() {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(openTicket(RoutingTeam.IT));

        assertThatThrownBy(() -> supportTicketService.getTicketsByRoutingTeam(RoutingTeam.IT, Role.HR))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> supportTicketService.resolveTicket(TICKET_ID, RESOLVER_ACCOUNT_ID, Role.HR))
                .isInstanceOf(UnauthorizedException.class);

        verify(supportTicketRepository, never()).markResolved(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Admin can list every ticket and resolve any team's ticket")
    void adminHandlesEveryTicket() {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(openTicket(RoutingTeam.HR));

        supportTicketService.getAllTickets(Role.ADMIN);
        supportTicketService.resolveTicket(TICKET_ID, RESOLVER_ACCOUNT_ID, Role.ADMIN);

        verify(supportTicketRepository).findAll();
        verify(supportTicketRepository).markResolved(TICKET_ID, RESOLVER_ACCOUNT_ID);
    }

    @ParameterizedTest(name = "{0} cannot list all tickets or cancel one")
    @EnumSource(value = Role.class, names = {"HR", "FINANCE", "IT", "EMPLOYEE"})
    void onlyAdminCanListAllOrCancel(Role role) {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(openTicket(RoutingTeam.IT));

        assertThatThrownBy(() -> supportTicketService.getAllTickets(role)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> supportTicketService.cancelTicket(TICKET_ID, role))
                .isInstanceOf(UnauthorizedException.class);

        verify(supportTicketRepository, never()).markCancelled(anyInt());
    }

    @Test
    @DisplayName("Admin can cancel an open ticket")
    void adminCanCancel() {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(openTicket(RoutingTeam.PAYROLL));

        supportTicketService.cancelTicket(TICKET_ID, Role.ADMIN);

        verify(supportTicketRepository).markCancelled(TICKET_ID);
    }

    @Test
    @DisplayName("The open count covers only the caller's team, or everything for Admin")
    void openCountIsScopedToTeam() {
        when(supportTicketRepository.findOpenTickets()).thenReturn(List.of(
                openTicket(RoutingTeam.IT), openTicket(RoutingTeam.IT), openTicket(RoutingTeam.PAYROLL)));

        assertThat(supportTicketService.getOpenTicketCount(Role.IT)).isEqualTo(2);
        assertThat(supportTicketService.getOpenTicketCount(Role.FINANCE)).isEqualTo(1);
        assertThat(supportTicketService.getOpenTicketCount(Role.ADMIN)).isEqualTo(3);
    }

    private SupportTicket openTicket(RoutingTeam team) {
        return new SupportTicket(TICKET_ID, 10028, SupportTicketCategory.OTHER, team,
                "Subject", "Description", SupportTicketStatus.OPEN, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
