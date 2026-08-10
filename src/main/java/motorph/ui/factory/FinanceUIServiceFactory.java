package motorph.ui.factory;

import motorph.application.service.EmployeeService;

/** Provides Finance-role services to Finance UI controllers. Delegates to the shared factory. */
public final class FinanceUIServiceFactory {

    private static FinanceUIServiceFactory instance;

    private final EmployeeService employeeService;

    // constructors
    private FinanceUIServiceFactory(EmployeeUIServiceFactory shared) {
        this.employeeService = shared.getEmployeeService();
    }

    // initializes the singleton from the shared factory; must be called after EmployeeUIServiceFactory.initialize()
    public static void initialize() {
        if (instance == null) {
            instance = new FinanceUIServiceFactory(EmployeeUIServiceFactory.getInstance());
        }
    }

    // returns the singleton instance; throws if initialize() was not called
    public static FinanceUIServiceFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FinanceUIServiceFactory not initialized. Call FinanceUIServiceFactory.initialize() first.");
        }
        return instance;
    }

    // getters
    public EmployeeService getEmployeeService() {
        return employeeService;
    }
}
