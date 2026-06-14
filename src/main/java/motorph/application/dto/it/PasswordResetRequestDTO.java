package motorph.application.dto.it;

/** Stores password reset request count summary for IT screens. */
public class PasswordResetRequestDTO {

    private final int pendingCount;
    private final int completedCount;
    private final int cancelledCount;
    private final int totalCount;

    // constructors
    public PasswordResetRequestDTO(
            int pendingCount,
            int completedCount,
            int cancelledCount,
            int totalCount) {

        this.pendingCount = pendingCount;
        this.completedCount = completedCount;
        this.cancelledCount = cancelledCount;
        this.totalCount = totalCount;
    }

    // getters
    public int getPendingCount() {
        return pendingCount;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }

    public int getTotalCount() {
        return totalCount;
    }
}