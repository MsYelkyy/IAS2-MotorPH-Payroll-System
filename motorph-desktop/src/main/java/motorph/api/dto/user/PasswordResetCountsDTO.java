package motorph.api.dto.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire copy of the backend's password reset request tallies. */
public record PasswordResetCountsDTO(
        int pendingCount,
        int completedCount,
        int cancelledCount,
        int totalCount
) {

    @JsonCreator
    public PasswordResetCountsDTO(@JsonProperty("pendingCount") int pendingCount,
                                  @JsonProperty("completedCount") int completedCount,
                                  @JsonProperty("cancelledCount") int cancelledCount,
                                  @JsonProperty("totalCount") int totalCount) {
        this.pendingCount = pendingCount;
        this.completedCount = completedCount;
        this.cancelledCount = cancelledCount;
        this.totalCount = totalCount;
    }
}
