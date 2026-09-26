package motorph.backend.dto.user;

/**
 * Account status counts for the IT and Admin dashboards, computed with COUNT queries instead of
 * loading every account.
 */
public record AccountCountsDTO(
        int activeCount,
        int lockedCount,
        int disabledCount
) {
}
