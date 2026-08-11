package motorph.api.dto.accesslog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Wire copy of the backend's UserAccessLog domain model, mirroring the old
 * motorph.domain.model.UserAccessLog getter names.
 */
public class UserAccessLogDTO {

    private final Integer logId;
    private final Integer userAccountId;
    private final String action;
    private final String details;
    private final String ipAddress;
    private final String deviceInfo;
    private final LocalDateTime loggedAt;

    @JsonCreator
    public UserAccessLogDTO(@JsonProperty("logId") Integer logId,
                            @JsonProperty("userAccountId") Integer userAccountId,
                            @JsonProperty("action") String action,
                            @JsonProperty("details") String details,
                            @JsonProperty("ipAddress") String ipAddress,
                            @JsonProperty("deviceInfo") String deviceInfo,
                            @JsonProperty("loggedAt") LocalDateTime loggedAt) {
        this.logId = logId;
        this.userAccountId = userAccountId;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.loggedAt = loggedAt;
    }

    public Integer getLogId() {
        return logId;
    }

    public Integer getUserAccountId() {
        return userAccountId;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }
}
