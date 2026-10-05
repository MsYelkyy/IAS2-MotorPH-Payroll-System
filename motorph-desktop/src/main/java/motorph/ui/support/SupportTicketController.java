package motorph.ui.support;

import motorph.api.ApiClientFactory;
import motorph.api.SupportTicketApiClient;
import motorph.api.dto.support.SupportTicketDTO;
import motorph.domain.value.Role;
import motorph.domain.value.RoutingTeam;
import motorph.ui.session.CurrentUserSession;

import java.util.List;

/**
 * The Support Tickets screen shared by every staff shell (Admin, HR, Finance, IT).
 * The signed-in role decides which tickets are loaded and which categories can be filtered;
 * only Admin may cancel tickets.
 */
public class SupportTicketController extends BaseSupportTicketController {

    @Override
    protected List<SupportTicketDTO> loadTickets() {
        SupportTicketApiClient api = ApiClientFactory.getInstance().getSupportTicketApiClient();
        return switch (role()) {
            case ADMIN   -> api.getAllTickets();
            case HR      -> api.getTicketsByRoutingTeam(RoutingTeam.HR);
            case FINANCE -> api.getTicketsByRoutingTeam(RoutingTeam.PAYROLL);
            case IT      -> api.getTicketsByRoutingTeam(RoutingTeam.IT);
            default      -> List.of();
        };
    }

    @Override
    protected boolean canCancelTickets() {
        return role() == Role.ADMIN;
    }

    @Override
    protected List<String> categoryFilterOptions() {
        return switch (role()) {
            case ADMIN   -> List.of(CAT_ALL, "PAYROLL", "ATTENDANCE", "ACCOUNT", "SYSTEM", "OTHER");
            case HR      -> List.of(CAT_ALL, "ATTENDANCE");
            case FINANCE -> List.of(CAT_ALL, "PAYROLL");
            case IT      -> List.of(CAT_ALL, "ACCOUNT", "SYSTEM");
            default      -> List.of(CAT_ALL);
        };
    }

    // never null for a signed-in staff user; EMPLOYEE falls through to "no tickets"
    private Role role() {
        Role role = CurrentUserSession.getActiveRole();
        return role != null ? role : Role.EMPLOYEE;
    }
}
