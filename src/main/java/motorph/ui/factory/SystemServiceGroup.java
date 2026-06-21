package motorph.ui.factory;

import motorph.application.service.DatabaseBackupService;
import motorph.application.service.PermissionService;
import motorph.application.service.SystemConfigService;
import motorph.application.service.UserAccessLogService;
import motorph.infrastructure.repository.PagIbigRateRepository;
import motorph.infrastructure.repository.PermissionRepository;
import motorph.infrastructure.repository.PhilHealthRateRepository;
import motorph.infrastructure.repository.SssRateRepository;
import motorph.infrastructure.repository.WithholdingTaxBracketRepository;

/** Owns system administration and configuration services. */
class SystemServiceGroup {

    private final PermissionService permissionService;
    private final SystemConfigService systemConfigService;
    private final DatabaseBackupService databaseBackupService;

    // constructors
    SystemServiceGroup(UserAccessLogService userAccessLogService) {
        this.permissionService = new PermissionService(new PermissionRepository());
        this.databaseBackupService = new DatabaseBackupService(userAccessLogService);
        this.systemConfigService = new SystemConfigService(
                new PhilHealthRateRepository(),
                new SssRateRepository(),
                new PagIbigRateRepository(),
                new WithholdingTaxBracketRepository(),
                userAccessLogService
        );
    }

    // getters
    PermissionService getPermissionService() {
        return permissionService;
    }

    SystemConfigService getSystemConfigService() {
        return systemConfigService;
    }

    DatabaseBackupService getDatabaseBackupService() {
        return databaseBackupService;
    }
}
