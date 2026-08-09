package motorph.backend.dto.leave;

/** Dashboard counts used by HR/Admin dashboards. */
public record LeaveSummaryDTO(
        int pendingCount,
        int approvedTodayCount
) {
}
