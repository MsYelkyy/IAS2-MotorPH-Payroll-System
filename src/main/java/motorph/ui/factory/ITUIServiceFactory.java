package motorph.ui.factory;

import motorph.application.service.EmployeeService;

/** Provides IT-role services to IT UI controllers. Delegates to the shared factory. */
public final class ITUIServiceFactory {

    private static ITUIServiceFactory instance;

    private final EmployeeService employeeService;

    private ITUIServiceFactory(EmployeeUIServiceFactory shared) {
        this.employeeService = shared.getEmployeeService();
    }

    // initializes the singleton from the shared factory; must be called after EmployeeUIServiceFactory.initialize()
    public static void initialize() {
        if (instance == null) {
            instance = new ITUIServiceFactory(EmployeeUIServiceFactory.getInstance());
        }
    }

    // returns the singleton instance; throws if initialize() was not called
    public static ITUIServiceFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ITUIServiceFactory not initialized. Call ITUIServiceFactory.initialize() first.");
        }
        return instance;
    }

    // getters
    public EmployeeService getEmployeeService() {
        return employeeService;
    }
}
