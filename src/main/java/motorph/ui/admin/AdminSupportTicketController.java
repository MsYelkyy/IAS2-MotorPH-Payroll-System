package motorph.ui.admin;

import motorph.api.ApiClientFactory;
import motorph.api.dto.support.SupportTicketDTO;
import motorph.ui.support.BaseSupportTicketController;

import java.util.List;

/**
 * Admin Support Tickets screen. Shows all tickets across every routing team
 * using the shared support ticket pattern.
 */
public class AdminSupportTicketController extends BaseSupportTicketController {

    @Override
    protected List<SupportTicketDTO> loadTickets() {
        return ApiClientFactory.getInstance().getSupportTicketApiClient().getAllTickets();
    }

    @Override
    protected List<String> categoryFilterOptions() {
        return List.of(CAT_ALL, "PAYROLL", "ATTENDANCE", "ACCOUNT", "SYSTEM", "OTHER");
    }
}
