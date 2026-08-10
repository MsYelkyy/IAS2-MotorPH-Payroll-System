package motorph.ui.factory;

import motorph.application.service.EmployeeService;
import motorph.application.service.ITPasswordResetService;
import motorph.application.service.ITUserAccountService;
import motorph.application.service.PasswordResetService;
import motorph.application.service.PermissionService;
import motorph.application.service.UserAccessLogService;
import motorph.application.service.UserAccountService;

/** Provides IT-role services to IT UI controllers. Delegates to the shared factory. */
public final class ITUIServiceFactory {

    private static ITUIServiceFactory instance;

    private final UserAccountService userAccountService;
    private final ITUserAccountService itUserAccountService;
    private final PasswordResetService passwordResetService;
    private final ITPasswordResetService itPasswordResetService;
    private final UserAccessLogService userAccessLogService;
    private final PermissionService permissionService;
    private final EmployeeService employeeService;

    private ITUIServiceFactory(EmployeeUIServiceFactory shared) {
        this.userAccountService    = shared.getUserAccountService();
        this.itUserAccountService  = shared.getITUserAccountService();
        this.passwordResetService  = shared.getPasswordResetService();
        this.itPasswordResetService = shared.getITPasswordResetService();
        this.userAccessLogService  = shared.getUserAccessLogService();
        this.permissionService     = shared.getPermissionService();
        this.employeeService       = shared.getEmployeeService();
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
    public UserAccountService getUserAccountService() {
        return userAccountService;
    }

    public ITUserAccountService getITUserAccountService() {
        return itUserAccountService;
    }

    public PasswordResetService getPasswordResetService() {
        return passwordResetService;
    }

    public ITPasswordResetService getITPasswordResetService() {
        return itPasswordResetService;
    }

    public UserAccessLogService getUserAccessLogService() {
        return userAccessLogService;
    }

    public PermissionService getPermissionService() {
        return permissionService;
    }

    public EmployeeService getEmployeeService() {
        return employeeService;
    }
}
