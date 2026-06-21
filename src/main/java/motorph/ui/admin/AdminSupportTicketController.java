package motorph.ui.admin;

import motorph.application.service.EmployeeService;
import motorph.application.service.SupportTicketService;
import motorph.domain.model.SupportTicket;
import motorph.ui.factory.ITUIServiceFactory;
import motorph.ui.support.BaseSupportTicketController;

import java.util.List;

/**
 * Admin Support Tickets screen. Shows all tickets across every routing team
 * using the shared support ticket pattern.
 */
public class AdminSupportTicketController extends BaseSupportTicketController {

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
        return supportTicketService().getAllTickets();
    }

    @Override
    protected List<String> categoryFilterOptions() {
        return List.of(CAT_ALL, "PAYROLL", "ATTENDANCE", "ACCOUNT", "SYSTEM", "OTHER");
    }
}
