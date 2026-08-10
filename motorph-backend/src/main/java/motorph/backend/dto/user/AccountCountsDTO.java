package motorph.backend.dto.user;

/**
 * Account status tallies for the IT/Admin dashboards. Computed with COUNT
 * queries so a dashboard never has to pull the whole account list just to
 * size three labels.
 */
public record AccountCountsDTO(
        int activeCount,
        int lockedCount,
        int disabledCount
) {
}
