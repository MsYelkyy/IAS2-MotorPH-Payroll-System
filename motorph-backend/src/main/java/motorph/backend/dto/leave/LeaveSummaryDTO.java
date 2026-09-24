package motorph.backend.dto.leave;

/** Leave counts for the HR and Admin dashboards. */
public record LeaveSummaryDTO(
        int pendingCount,
        int approvedTodayCount
) {
}
