package motorph.backend.dto.user;

/** Password reset request tallies for the IT screens' summary cards. */
public record PasswordResetCountsDTO(
        int pendingCount,
        int completedCount,
        int cancelledCount,
        int totalCount
) {
}
