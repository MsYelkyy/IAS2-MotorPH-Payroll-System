package motorph.ui.factory;

import motorph.application.service.AttendanceService;
import motorph.application.service.EmployeeService;
import motorph.application.service.LeaveService;
import motorph.infrastructure.repository.LeaveCreditRepository;
import motorph.infrastructure.repository.LeaveRepository;

/** Owns leave and attendance operations services. */
class OperationsServiceGroup {

    private final LeaveService leaveService;

    // constructors
    OperationsServiceGroup(AttendanceService attendanceService, EmployeeService employeeService) {
        this.leaveService = new LeaveService(
                new LeaveRepository(),
                new LeaveCreditRepository(),
                employeeService
        );
    }

    // getters
    LeaveService getLeaveService() {
        return leaveService;
    }
}
