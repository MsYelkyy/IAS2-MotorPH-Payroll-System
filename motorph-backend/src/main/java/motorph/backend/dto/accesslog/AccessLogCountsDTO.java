package motorph.backend.dto.accesslog;

/**
 * Login success and failure counts for the IT and Admin dashboards.
 */
public record AccessLogCountsDTO(
        int loginSuccessCount,
        int loginFailedCount
) {
}
