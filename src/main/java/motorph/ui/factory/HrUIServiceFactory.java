package motorph.ui.factory;

import motorph.application.service.AttendanceService;
import motorph.application.service.EmployeeService;
import motorph.application.service.LeaveService;

/** Provides HR-role services to HR UI controllers. Delegates to the shared factory. */
public final class HrUIServiceFactory {

    private static HrUIServiceFactory instance;

    private final EmployeeService employeeService;
    private final LeaveService leaveService;
    private final AttendanceService attendanceService;

    // constructors
    private HrUIServiceFactory(EmployeeUIServiceFactory shared) {
        this.employeeService   = shared.getEmployeeService();
        this.leaveService      = shared.getLeaveService();
        this.attendanceService = shared.getAttendanceService();
    }

    // initializes the singleton from the shared factory; must be called after EmployeeUIServiceFactory.initialize()
    public static void initialize() {
        if (instance == null) {
            instance = new HrUIServiceFactory(EmployeeUIServiceFactory.getInstance());
        }
    }

    // returns the singleton instance; throws if initialize() was not called
    public static HrUIServiceFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("HrUIServiceFactory not initialized. Call HrUIServiceFactory.initialize() first.");
        }
        return instance;
    }

    // getters
    public EmployeeService getEmployeeService() {
        return employeeService;
    }

    public LeaveService getLeaveService() {
        return leaveService;
    }

    public AttendanceService getAttendanceService() {
        return attendanceService;
    }
}
