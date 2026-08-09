package motorph.backend.controller.v1;

import motorph.backend.domain.SupportTicket;
import motorph.backend.domain.value.Role;
import motorph.backend.domain.value.RoutingTeam;
import motorph.backend.domain.value.SupportTicketCategory;
import motorph.backend.dto.support.TicketSubmitRequest;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.security.AuthenticatedPrincipal;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.PermissionService;
import motorph.backend.service.SupportTicketService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Support ticket endpoints. Submitting always acts on the caller's own
 * employeeId. Viewing the cross-employee ticket lists (all tickets / one
 * routing team / open count) requires an IT/HR/ADMIN/FINANCE active role -
 * a check the original desktop service never had at all (only its UI menu
 * structure restricted access). Since this API is reachable directly, that
 * gate is added here as a hardening step; resolve/cancel keep the original's
 * exact role logic, enforced in the service.
 */
@RestController
@RequestMapping("/api/v1/support-tickets")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    public SupportTicketController(SupportTicketService supportTicketService,
                                   PermissionService permissionService,
                                   CurrentUserProvider currentUserProvider) {
        this.supportTicketService = supportTicketService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public void submitTicket(@RequestBody TicketSubmitRequest request) {
        supportTicketService.submitTicket(
                callerEmployeeId(),
                SupportTicketCategory.fromValue(request.category()),
                request.subject(),
                request.description()
        );
    }

    @GetMapping("/me")
    public List<SupportTicket> getOwnTickets() {
        return supportTicketService.getTicketsByEmployee(callerEmployeeId());
    }

    @GetMapping
    public List<SupportTicket> getAllTickets() {
        requireTicketManagerRole();
        return supportTicketService.getAllTickets();
    }

    @GetMapping("/routing-team/{routingTeam}")
    public List<SupportTicket> getTicketsByRoutingTeam(@PathVariable String routingTeam) {
        requireTicketManagerRole();
        return supportTicketService.getTicketsByRoutingTeam(RoutingTeam.fromValue(routingTeam));
    }

    @GetMapping("/open-count")
    public int getOpenTicketCount() {
        requireTicketManagerRole();
        return supportTicketService.getOpenTicketCount();
    }

    @PostMapping("/{ticketId}/resolve")
    public void resolveTicket(@PathVariable int ticketId) {
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();
        supportTicketService.resolveTicket(ticketId, principal.userAccountId(), permissionService.getActiveRole());
    }

    @PostMapping("/{ticketId}/cancel")
    public void cancelTicket(@PathVariable int ticketId) {
        supportTicketService.cancelTicket(ticketId, callerEmployeeId(), permissionService.getActiveRole());
    }

    private void requireTicketManagerRole() {
        Role role = permissionService.getActiveRole();
        boolean allowed = role == Role.IT || role == Role.HR || role == Role.ADMIN || role == Role.FINANCE;

        if (!allowed) {
            throw new UnauthorizedException("You do not have permission to perform this action.");
        }
    }

    private int callerEmployeeId() {
        return currentUserProvider.getCurrentPrincipal().employeeId();
    }
}
