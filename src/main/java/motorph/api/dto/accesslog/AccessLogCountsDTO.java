package motorph.api.dto.accesslog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire copy of the backend's login-activity tallies. */
public record AccessLogCountsDTO(
        int loginSuccessCount,
        int loginFailedCount
) {

    @JsonCreator
    public AccessLogCountsDTO(@JsonProperty("loginSuccessCount") int loginSuccessCount,
                              @JsonProperty("loginFailedCount") int loginFailedCount) {
        this.loginSuccessCount = loginSuccessCount;
        this.loginFailedCount = loginFailedCount;
    }
}
