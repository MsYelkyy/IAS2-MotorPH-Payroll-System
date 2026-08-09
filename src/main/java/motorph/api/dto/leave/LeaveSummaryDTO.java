package motorph.api.dto.leave;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire copy of the backend's LeaveSummaryDTO for HR/Admin dashboard counts. */
public record LeaveSummaryDTO(int pendingCount, int approvedTodayCount) {

    @JsonCreator
    public LeaveSummaryDTO(@JsonProperty("pendingCount") int pendingCount,
                           @JsonProperty("approvedTodayCount") int approvedTodayCount) {
        this.pendingCount = pendingCount;
        this.approvedTodayCount = approvedTodayCount;
    }
}
