package motorph.backend.controller.v1;

import jakarta.validation.Valid;
import motorph.backend.domain.SupportTicket;
import motorph.backend.domain.value.RoutingTeam;
import motorph.backend.domain.value.SupportTicketCategory;
import motorph.backend.dto.support.SupportTicketRowDTO;
import motorph.backend.dto.support.TicketSubmitRequest;
import motorph.backend.exception.ValidationException;
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
 * Support ticket endpoints. Employees submit and view their own tickets; the team and cancel rules
 * for everything else are enforced in {@link SupportTicketService}.
 * Adds @Valid to TicketSubmitRequest.
 * Validates routingTeam and ticketId path variables.
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
    public void submitTicket(@RequestBody @Valid TicketSubmitRequest request) {
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();

        String cleanSubject = sanitizeText(request.subject());
        String cleanDescription = sanitizeText(request.description());

        supportTicketService.submitTicket(
                principal.employeeId(),
                SupportTicketCategory.fromValue(request.category()),
                cleanSubject,
                cleanDescription,
                principal.userAccountId()
        );
    }

    @GetMapping("/me")
    public List<SupportTicket> getOwnTickets() {
        return supportTicketService.getTicketsByEmployee(callerEmployeeId());
    }

    @GetMapping
    public List<SupportTicketRowDTO> getAllTickets() {
        return supportTicketService.getAllTickets(permissionService.getActiveRole());
    }

    @GetMapping("/routing-team/{routingTeam}")
    public List<SupportTicketRowDTO> getTicketsByRoutingTeam(@PathVariable String routingTeam) {
        if (routingTeam == null || routingTeam.isBlank()) {
            throw new ValidationException("Routing team parameter is required.");
        }
        return supportTicketService.getTicketsByRoutingTeam(
                RoutingTeam.fromValue(routingTeam), permissionService.getActiveRole());
    }

    @GetMapping("/open-count")
    public int getOpenTicketCount() {
        return supportTicketService.getOpenTicketCount(permissionService.getActiveRole());
    }

    @PostMapping("/{ticketId}/resolve")
    public void resolveTicket(@PathVariable int ticketId) {
        validatePositiveId(ticketId, "Ticket ID");
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();
        supportTicketService.resolveTicket(ticketId, principal.userAccountId(), permissionService.getActiveRole());
    }

    @PostMapping("/{ticketId}/cancel")
    public void cancelTicket(@PathVariable int ticketId) {
        validatePositiveId(ticketId, "Ticket ID");
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();
        supportTicketService.cancelTicket(ticketId, permissionService.getActiveRole(), principal.userAccountId());
    }

    private int callerEmployeeId() {
        return currentUserProvider.getCurrentPrincipal().employeeId();
    }

    private void validatePositiveId(int id, String name) {
        if (id <= 0) {
            throw new ValidationException(name + " must be a positive integer.");
        }
    }

    private String sanitizeText(String input) {
        if (input == null) {
            return null;
        }
        // Strips basic HTML/script tags to prevent XSS payloads in persistent ticket fields
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
