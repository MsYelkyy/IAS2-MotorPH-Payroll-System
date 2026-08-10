package motorph.ui.factory;

import motorph.application.service.DatabaseBackupService;
import motorph.application.service.PermissionService;
import motorph.application.service.UserAccessLogService;
import motorph.infrastructure.repository.PermissionRepository;

/** Owns system administration services. */
class SystemServiceGroup {

    private final PermissionService permissionService;
    private final DatabaseBackupService databaseBackupService;

    // constructors
    SystemServiceGroup(UserAccessLogService userAccessLogService) {
        this.permissionService = new PermissionService(new PermissionRepository());
        this.databaseBackupService = new DatabaseBackupService(userAccessLogService);
    }

    // getters
    PermissionService getPermissionService() {
        return permissionService;
    }

    DatabaseBackupService getDatabaseBackupService() {
        return databaseBackupService;
    }
}
