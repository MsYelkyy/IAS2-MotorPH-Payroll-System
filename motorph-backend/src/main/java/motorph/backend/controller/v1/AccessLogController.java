package motorph.backend.controller.v1;

import motorph.backend.domain.UserAccessLog;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.accesslog.AccessLogCountsDTO;
import motorph.backend.service.PermissionService;
import motorph.backend.service.UserAccessLogService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only access to the audit trail. Requires {@code VIEW_ACCESS_LOGS}. There are no write
 * endpoints; log entries are only created by the backend itself.
 */
@RestController
@RequestMapping("/api/v1/access-logs")
public class AccessLogController {

    private static final int MAX_RECENT_LIMIT = 100;

    private final UserAccessLogService userAccessLogService;
    private final PermissionService permissionService;

    public AccessLogController(UserAccessLogService userAccessLogService,
                               PermissionService permissionService) {
        this.userAccessLogService = userAccessLogService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<UserAccessLog> getAllLogs() {
        permissionService.requirePermission(PermissionKey.VIEW_ACCESS_LOGS);
        return userAccessLogService.getAllLogs();
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
