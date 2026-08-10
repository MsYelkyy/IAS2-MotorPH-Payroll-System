package motorph.ui.factory;

import motorph.application.service.EmailPreviewService;
import motorph.application.service.EmployeeService;

/** Provides the remaining locally-constructed services needed by UI controllers. */
public final class EmployeeUIServiceFactory {

    private static EmployeeUIServiceFactory instance;

    private final CoreServiceGroup core;

    // constructors
    private EmployeeUIServiceFactory() {
        this.core = new CoreServiceGroup();
    }

    // initializes the singleton; must be called before getInstance()
    public static void initialize() {
        if (instance == null) {
            instance = new EmployeeUIServiceFactory();
        }
    }

    // returns the singleton instance; throws if initialize() was not called
    public static EmployeeUIServiceFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("EmployeeUIServiceFactory not initialized. Call EmployeeUIServiceFactory.initialize() first.");
        }
        return instance;
    }

    // getters
    public EmployeeService getEmployeeService() {
        return core.getEmployeeService();
    }

    public EmailPreviewService getEmailPreviewService() {
        return core.getEmailPreviewService();
    }
}
