package motorph.backend.dto.accesslog;

/**
 * Login-activity tallies for the IT/Admin dashboards. Same reasoning as
 * AccountCountsDTO: the dashboards used to count these by fetching every
 * matching log row and calling size() on the list.
 */
public record AccessLogCountsDTO(
        int loginSuccessCount,
        int loginFailedCount
) {
}
