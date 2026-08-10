package motorph.ui.factory;

import motorph.application.service.EmailPreviewService;

/**
 * The desktop's remaining composition root. Everything it used to build -
 * employee, account, permission, audit, password-reset and backup services,
 * and the JDBC repositories under them - now lives behind the backend API and
 * is reached through ApiClientFactory. What is left is the in-app email
 * preview, which is pure UI state and never touched the database.
 */
public final class EmployeeUIServiceFactory {

    private static EmployeeUIServiceFactory instance;

    private final EmailPreviewService emailPreviewService;

    // constructors
    private EmployeeUIServiceFactory() {
        this.emailPreviewService = new EmailPreviewService();
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
    public EmailPreviewService getEmailPreviewService() {
        return emailPreviewService;
    }
}
