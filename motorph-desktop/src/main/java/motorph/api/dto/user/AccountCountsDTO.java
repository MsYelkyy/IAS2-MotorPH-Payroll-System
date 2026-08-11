package motorph.api.dto.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire copy of the backend's account status tallies. */
public record AccountCountsDTO(
        int activeCount,
        int lockedCount,
        int disabledCount
) {

    @JsonCreator
    public AccountCountsDTO(@JsonProperty("activeCount") int activeCount,
                            @JsonProperty("lockedCount") int lockedCount,
                            @JsonProperty("disabledCount") int disabledCount) {
        this.activeCount = activeCount;
        this.lockedCount = lockedCount;
        this.disabledCount = disabledCount;
    }
}
