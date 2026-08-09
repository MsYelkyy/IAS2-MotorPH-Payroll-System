package motorph.ui.it;

import motorph.domain.value.RoutingTeam;
import motorph.api.ApiClientFactory;
import motorph.api.dto.support.SupportTicketDTO;
import motorph.ui.support.BaseSupportTicketController;

import java.util.List;

/**
 * IT Support Tickets screen. Shows tickets routed to the IT team
 * (ACCOUNT and SYSTEM categories) using the shared support ticket pattern.
 */
public class ITSupportTicketController extends BaseSupportTicketController {

    @Override
    protected List<SupportTicketDTO> loadTickets() {
        return ApiClientFactory.getInstance().getSupportTicketApiClient()
                .getTicketsByRoutingTeam(RoutingTeam.IT);
    }

    @Override
    protected List<String> categoryFilterOptions() {
        return List.of(CAT_ALL, "ACCOUNT", "SYSTEM");
    }
}
