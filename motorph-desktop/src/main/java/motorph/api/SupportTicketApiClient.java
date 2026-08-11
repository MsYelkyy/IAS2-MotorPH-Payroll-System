package motorph.api;

import motorph.api.dto.support.SupportTicketDTO;
import motorph.api.dto.support.TicketSubmitRequest;
import motorph.domain.value.RoutingTeam;

import java.util.List;

/**
 * Thin wrapper around the backend's /api/v1/support-tickets endpoints.
 * Submitting/cancelling never send an employeeId - always the caller.
 */
public final class SupportTicketApiClient {

    private final HttpApiClient httpApiClient;

    public SupportTicketApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public void submitTicket(TicketSubmitRequest request) {
        httpApiClient.postNoContent("/api/v1/support-tickets", request);
    }

    public List<SupportTicketDTO> getOwnTickets() {
        return httpApiClient.getList("/api/v1/support-tickets/me", SupportTicketDTO.class);
    }

    public List<SupportTicketDTO> getAllTickets() {
        return httpApiClient.getList("/api/v1/support-tickets", SupportTicketDTO.class);
    }

    public List<SupportTicketDTO> getTicketsByRoutingTeam(RoutingTeam routingTeam) {
        return httpApiClient.getList("/api/v1/support-tickets/routing-team/" + routingTeam.name(), SupportTicketDTO.class);
    }

    public int getOpenTicketCount() {
        return httpApiClient.get("/api/v1/support-tickets/open-count", Integer.class);
    }

    public void resolveTicket(int ticketId) {
        httpApiClient.postNoContent("/api/v1/support-tickets/" + ticketId + "/resolve", null);
    }

    public void cancelTicket(int ticketId) {
        httpApiClient.postNoContent("/api/v1/support-tickets/" + ticketId + "/cancel", null);
    }
}
