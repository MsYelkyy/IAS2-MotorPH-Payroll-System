package motorph.ui.hr;

import motorph.domain.value.RoutingTeam;
import motorph.api.ApiClientFactory;
import motorph.api.dto.support.SupportTicketDTO;
import motorph.ui.support.BaseSupportTicketController;

import java.util.List;

/**
 * HR Support Tickets screen. Shows only tickets routed to the HR team
 * (ATTENDANCE category) using the shared support ticket pattern.
 */
public class HrSupportTicketController extends BaseSupportTicketController {

    @Override
    protected List<SupportTicketDTO> loadTickets() {
        return ApiClientFactory.getInstance().getSupportTicketApiClient()
                .getTicketsByRoutingTeam(RoutingTeam.HR);
    }

    @Override
    protected List<String> categoryFilterOptions() {
        return List.of(CAT_ALL, "ATTENDANCE");
    }
}
