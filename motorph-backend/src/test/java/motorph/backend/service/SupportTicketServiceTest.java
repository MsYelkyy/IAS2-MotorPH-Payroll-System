package motorph.backend.service;

import motorph.backend.domain.SupportTicket;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.domain.value.Role;
import motorph.backend.domain.value.RoutingTeam;
import motorph.backend.domain.value.SupportTicketCategory;
import motorph.backend.domain.value.SupportTicketStatus;
import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.SupportTicketRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.mockito.Mockito.doNothing;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Each team sees and resolves only its own tickets, Admin handles every ticket, and only Admin can
 * cancel one. Submitting, resolving and cancelling a ticket are recorded in the audit log.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SupportTicketServiceTest {

    private static final int TICKET_ID = 42;
    private static final int EMPLOYEE_ID = 10028;
    private static final int RESOLVER_ACCOUNT_ID = 7;
    private static final int ADMIN_ACCOUNT_ID = 9;
    private static final int SUBMITTER_ACCOUNT_ID = 15;

    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserAccessLogService userAccessLogService;
    @Mock private PermissionService permissionService;

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
        when(permissionService.hasPermission(PermissionKey.CANCEL_TICKETS)).thenReturn(true);

        supportTicketService.getAllTickets(Role.ADMIN);
        supportTicketService.resolveTicket(TICKET_ID, RESOLVER_ACCOUNT_ID, Role.ADMIN);

        verify(supportTicketRepository).findAll();
        verify(supportTicketRepository).markResolved(TICKET_ID, RESOLVER_ACCOUNT_ID);
    }

    @ParameterizedTest(name = "{0} cannot list all tickets or cancel one")
    @EnumSource(value = Role.class, names = {"HR", "FINANCE", "IT", "EMPLOYEE"})
    void onlyAdminCanListAllOrCancel(Role role) {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(openTicket(RoutingTeam.IT));
        doThrow(new UnauthorizedException("You do not have permission to perform this action."))
                .when(permissionService).requirePermission(PermissionKey.CANCEL_TICKETS);

        assertThatThrownBy(() -> supportTicketService.getAllTickets(role)).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> supportTicketService.cancelTicket(TICKET_ID, role, ADMIN_ACCOUNT_ID))
                .isInstanceOf(UnauthorizedException.class);

        verify(supportTicketRepository, never()).markCancelled(anyInt());
    }

    @Test
    @DisplayName("Admin can cancel an open ticket")
    void adminCanCancel() {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(openTicket(RoutingTeam.PAYROLL));
        when(permissionService.hasPermission(PermissionKey.CANCEL_TICKETS)).thenReturn(true);

        supportTicketService.cancelTicket(TICKET_ID, Role.ADMIN, ADMIN_ACCOUNT_ID);

        verify(supportTicketRepository).markCancelled(TICKET_ID);
    }

    @Test
    @DisplayName("The open count covers only the caller's team, or everything for Admin")
    void openCountIsScopedToTeam() {
        when(supportTicketRepository.findOpenTickets()).thenReturn(List.of(
                openTicket(RoutingTeam.IT), openTicket(RoutingTeam.IT), openTicket(RoutingTeam.PAYROLL)));

        when(permissionService.hasPermission(PermissionKey.CANCEL_TICKETS)).thenReturn(false);
        assertThat(supportTicketService.getOpenTicketCount(Role.IT)).isEqualTo(2);
        assertThat(supportTicketService.getOpenTicketCount(Role.FINANCE)).isEqualTo(1);

        when(permissionService.hasPermission(PermissionKey.CANCEL_TICKETS)).thenReturn(true);
        assertThat(supportTicketService.getOpenTicketCount(Role.ADMIN)).isEqualTo(3);
    }

    // Audit logging tests

    @Test
    @DisplayName("Submitting a ticket is audited with its category and routing team, and no target ID")
    void submittingIsAudited() {
        supportTicketService.submitTicket(
                EMPLOYEE_ID, SupportTicketCategory.PAYROLL, "Wrong deduction", "My payslip is short.",
                SUBMITTER_ACCOUNT_ID);

        InOrder inOrder = inOrder(supportTicketRepository, userAccessLogService);
        inOrder.verify(supportTicketRepository).save(any(SupportTicket.class));
        inOrder.verify(userAccessLogService).logEvent(
                SUBMITTER_ACCOUNT_ID,
                "SUPPORT_TICKET_SUBMITTED",
                "Submitted a PAYROLL support ticket routed to PAYROLL",
                "SUPPORT_TICKET",
                null);
    }

    @Test
    @DisplayName("The subject and description are free text and never reach the audit details")
    void submittedTextIsNotAudited() {
        supportTicketService.submitTicket(
                EMPLOYEE_ID, SupportTicketCategory.ACCOUNT, "My password is hunter2", "Please reset hunter2",
                SUBMITTER_ACCOUNT_ID);

        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(userAccessLogService).logEvent(
                eq(SUBMITTER_ACCOUNT_ID), eq("SUPPORT_TICKET_SUBMITTED"), details.capture(),
                eq("SUPPORT_TICKET"), isNull());

        assertThat(details.getValue()).doesNotContain("hunter2").doesNotContain("password");
    }

    @Test
    @DisplayName("A rejected submission is not audited")
    void rejectedSubmissionIsNotAudited() {
        assertThatThrownBy(() -> supportTicketService.submitTicket(
                EMPLOYEE_ID, SupportTicketCategory.OTHER, " ", "Description", SUBMITTER_ACCOUNT_ID))
                .isInstanceOf(ValidationException.class);

        verify(supportTicketRepository, never()).save(any());
        verifyNoInteractions(userAccessLogService);
    }

    @Test
    @DisplayName("Resolving a ticket is audited against the ticket, after the update")
    void resolvingIsAudited() {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(openTicket(RoutingTeam.PAYROLL));

        supportTicketService.resolveTicket(TICKET_ID, RESOLVER_ACCOUNT_ID, Role.FINANCE);

        InOrder inOrder = inOrder(supportTicketRepository, userAccessLogService);
        inOrder.verify(supportTicketRepository).markResolved(TICKET_ID, RESOLVER_ACCOUNT_ID);
        inOrder.verify(userAccessLogService).logEvent(
                RESOLVER_ACCOUNT_ID,
                "SUPPORT_TICKET_RESOLVED",
                "Resolved support ticket ID 42",
                "SUPPORT_TICKET",
                TICKET_ID);
    }

    @Test
    @DisplayName("A resolve that is refused or fails is not audited")
    void failedResolveIsNotAudited() {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(openTicket(RoutingTeam.IT));

        assertThatThrownBy(() -> supportTicketService.resolveTicket(TICKET_ID, RESOLVER_ACCOUNT_ID, Role.HR))
                .isInstanceOf(UnauthorizedException.class);

        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(ticket(RoutingTeam.IT, SupportTicketStatus.RESOLVED));
        assertThatThrownBy(() -> supportTicketService.resolveTicket(TICKET_ID, RESOLVER_ACCOUNT_ID, Role.IT))
                .isInstanceOf(BusinessRuleException.class);

        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(openTicket(RoutingTeam.IT));
        doThrow(new IllegalArgumentException("Open support ticket not found."))
                .when(supportTicketRepository).markResolved(TICKET_ID, RESOLVER_ACCOUNT_ID);
        assertThatThrownBy(() -> supportTicketService.resolveTicket(TICKET_ID, RESOLVER_ACCOUNT_ID, Role.IT))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(userAccessLogService);
    }

    @Test
    @DisplayName("Cancelling a ticket is audited against the ticket, after the update")
    void cancellingIsAudited() {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(openTicket(RoutingTeam.PAYROLL));
        when(permissionService.hasPermission(PermissionKey.CANCEL_TICKETS)).thenReturn(true);

        supportTicketService.cancelTicket(TICKET_ID, Role.ADMIN, ADMIN_ACCOUNT_ID);

        InOrder inOrder = inOrder(supportTicketRepository, userAccessLogService);
        inOrder.verify(supportTicketRepository).markCancelled(TICKET_ID);
        inOrder.verify(userAccessLogService).logEvent(
                ADMIN_ACCOUNT_ID,
                "SUPPORT_TICKET_CANCELLED",
                "Cancelled support ticket ID 42",
                "SUPPORT_TICKET",
                TICKET_ID);
    }

    @Test
    @DisplayName("A cancel that is refused or fails is not audited")
    void failedCancelIsNotAudited() {
        doThrow(new UnauthorizedException("You do not have permission to perform this action."))
                .when(permissionService).requirePermission(PermissionKey.CANCEL_TICKETS);

        assertThatThrownBy(() -> supportTicketService.cancelTicket(TICKET_ID, Role.HR, RESOLVER_ACCOUNT_ID))
                .isInstanceOf(UnauthorizedException.class);

        doNothing().when(permissionService).requirePermission(PermissionKey.CANCEL_TICKETS);
        when(permissionService.hasPermission(PermissionKey.CANCEL_TICKETS)).thenReturn(true);
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(ticket(RoutingTeam.IT, SupportTicketStatus.CANCELLED));

        assertThatThrownBy(() -> supportTicketService.cancelTicket(TICKET_ID, Role.ADMIN, ADMIN_ACCOUNT_ID))
                .isInstanceOf(BusinessRuleException.class);

        verify(supportTicketRepository, never()).markCancelled(anyInt());
        verifyNoInteractions(userAccessLogService);
    }

    @Test
    @DisplayName("Viewing tickets is not audited")
    void readsAreNotAudited() {
        when(supportTicketRepository.findById(TICKET_ID)).thenReturn(openTicket(RoutingTeam.IT));
        when(permissionService.hasPermission(PermissionKey.CANCEL_TICKETS)).thenReturn(true);

        supportTicketService.getTicketById(TICKET_ID);
        supportTicketService.getAllTickets(Role.ADMIN);
        supportTicketService.getTicketsByEmployee(EMPLOYEE_ID);

        verifyNoInteractions(userAccessLogService);
    }

    private SupportTicket openTicket(RoutingTeam team) {
        return ticket(team, SupportTicketStatus.OPEN);
    }

    private SupportTicket ticket(RoutingTeam team, SupportTicketStatus status) {
        return new SupportTicket(TICKET_ID, EMPLOYEE_ID, SupportTicketCategory.OTHER, team,
                "Subject", "Description", status, null, null,
                LocalDateTime.now(), LocalDateTime.now());
    }
}