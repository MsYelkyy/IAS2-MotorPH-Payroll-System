package motorph.ui.it;

import motorph.application.service.EmployeeService;
import motorph.application.service.SupportTicketService;
import motorph.domain.model.SupportTicket;
import motorph.domain.value.RoutingTeam;
import motorph.ui.factory.ITUIServiceFactory;
import motorph.ui.support.BaseSupportTicketController;

import java.util.List;

/**
 * IT Support Tickets screen. Shows tickets routed to the IT team
 * (ACCOUNT and SYSTEM categories) using the shared support ticket pattern.
 */
public class ITSupportTicketController extends BaseSupportTicketController {

    @Override
    protected SupportTicketService supportTicketService() {
        return ITUIServiceFactory.getInstance().getSupportTicketService();
    }

    @Override
    protected EmployeeService employeeService() {
        return ITUIServiceFactory.getInstance().getEmployeeService();
    }

    @Override
    protected List<SupportTicket> loadTickets() {
        return supportTicketService().getTicketsByRoutingTeam(RoutingTeam.IT);
    }

    @Override
    protected List<String> categoryFilterOptions() {
        return List.of(CAT_ALL, "ACCOUNT", "SYSTEM");
    }
}
