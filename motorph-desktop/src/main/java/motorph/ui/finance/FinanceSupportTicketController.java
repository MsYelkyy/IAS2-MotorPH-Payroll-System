package motorph.ui.finance;

import motorph.domain.value.RoutingTeam;
import motorph.api.ApiClientFactory;
import motorph.api.dto.support.SupportTicketDTO;
import motorph.ui.support.BaseSupportTicketController;

import java.util.List;

/**
 * Finance Support Tickets screen. Shows tickets routed to the PAYROLL team
 * (PAYROLL category) using the shared support ticket pattern.
 */
public class FinanceSupportTicketController extends BaseSupportTicketController {

    @Override
    protected List<SupportTicketDTO> loadTickets() {
        return ApiClientFactory.getInstance().getSupportTicketApiClient()
                .getTicketsByRoutingTeam(RoutingTeam.PAYROLL);
    }

    @Override
    protected List<String> categoryFilterOptions() {
        return List.of(CAT_ALL, "PAYROLL");
    }
}
