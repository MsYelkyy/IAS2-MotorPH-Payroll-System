package motorph.ui.hr;

import motorph.application.service.EmployeeService;
import motorph.application.service.SupportTicketService;
import motorph.domain.model.SupportTicket;
import motorph.domain.value.RoutingTeam;
import motorph.ui.factory.ITUIServiceFactory;
import motorph.ui.support.BaseSupportTicketController;

import java.util.List;

/**
 * HR Support Tickets screen. Shows only tickets routed to the HR team
 * (ATTENDANCE category) using the shared support ticket pattern.
 */
public class HrSupportTicketController extends BaseSupportTicketController {

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
        return supportTicketService().getTicketsByRoutingTeam(RoutingTeam.HR);
    }

    @Override
    protected List<String> categoryFilterOptions() {
        return List.of(CAT_ALL, "ATTENDANCE");
    }
}
