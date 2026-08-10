package motorph.ui.factory;

import motorph.application.service.DatabaseBackupService;
import motorph.application.service.EmailPreviewService;
import motorph.application.service.EmployeeService;
import motorph.application.service.ITPasswordResetService;
import motorph.application.service.ITUserAccountService;
import motorph.application.service.PasswordResetService;
import motorph.application.service.PermissionService;
import motorph.application.service.UserAccessLogService;
import motorph.application.service.UserAccountService;

/** Provides application services needed by UI controllers. */
public final class EmployeeUIServiceFactory {

    private static EmployeeUIServiceFactory instance;

    private final CoreServiceGroup core;
    private final AuthServiceGroup auth;
    private final ITServiceGroup it;
    private final SystemServiceGroup system;

    // constructors
    private EmployeeUIServiceFactory() {
        this.core       = new CoreServiceGroup();
        this.auth       = new AuthServiceGroup(
                core.getUserAccountRepository(),
                core.getEmployeeService(),
                core.getTwoFactorService(),
                core.getEmailService(),
                core.getUserAccessLogService()
        );
        this.it         = new ITServiceGroup(
                core.getUserAccountRepository(),
                core.getEmployeeService(),
                auth.getPasswordResetService()
        );
        this.system     = new SystemServiceGroup(core.getUserAccessLogService());
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
    public PasswordResetService getPasswordResetService() {
        return auth.getPasswordResetService();
    }

    public EmployeeService getEmployeeService() {
        return core.getEmployeeService();
    }

    public UserAccountService getUserAccountService() {
        return core.getUserAccountService();
    }

    public UserAccessLogService getUserAccessLogService() {
        return core.getUserAccessLogService();
    }

    public ITUserAccountService getITUserAccountService() {
        return it.getITUserAccountService();
    }

    public ITPasswordResetService getITPasswordResetService() {
        return it.getITPasswordResetService();
    }

    public EmailPreviewService getEmailPreviewService() {
        return core.getEmailPreviewService();
    }

    public PermissionService getPermissionService() {
        return system.getPermissionService();
    }

    public DatabaseBackupService getDatabaseBackupService() {
        return system.getDatabaseBackupService();
    }
}
