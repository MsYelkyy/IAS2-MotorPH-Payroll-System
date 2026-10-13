package motorph.backend.controller.v1;

import motorph.backend.domain.UserAccessLog;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.accesslog.AccessLogCountsDTO;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.PermissionService;
import motorph.backend.service.UserAccessLogService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Provides read-only access to audit logs.
 * Only the full log view is audited to avoid unnecessary log entries.
 */
@RestController
@RequestMapping("/api/v1/access-logs")
public class AccessLogController {

    private static final int MAX_RECENT_LIMIT = 100;

    private final UserAccessLogService userAccessLogService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    public AccessLogController(UserAccessLogService userAccessLogService,
                               PermissionService permissionService,
                               CurrentUserProvider currentUserProvider) {
        this.userAccessLogService = userAccessLogService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Returns the full audit log and records the successful view.
     */
    @GetMapping
    public List<UserAccessLog> getAllLogs() {
        permissionService.requirePermission(PermissionKey.VIEW_ACCESS_LOGS);

        List<UserAccessLog> logs = userAccessLogService.getAllLogs();

        userAccessLogService.logEvent(currentUserProvider.getCurrentUserAccountId(),
                UserAccessLogService.ACTION_ACCESS_LOG_VIEWED,
                "Viewed the full audit log (" + logs.size() + " entries).");

        return logs;
    }

    @GetMapping("/recent")
    public List<UserAccessLog> getRecentLogs(@RequestParam(defaultValue = "10") int limit) {
        permissionService.requirePermission(PermissionKey.VIEW_ACCESS_LOGS);
        return userAccessLogService.getRecentLogs(Math.min(limit, MAX_RECENT_LIMIT));
    }

    @GetMapping("/counts")
    public AccessLogCountsDTO getLoginActivityCounts() {
        permissionService.requirePermission(PermissionKey.VIEW_ACCESS_LOGS);
        return userAccessLogService.getLoginActivityCounts();
    }
}
