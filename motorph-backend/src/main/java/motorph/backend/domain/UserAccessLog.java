package motorph.backend.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a single user access log entry for auditing system activity.
 */
public class UserAccessLog {

    private final Integer logId;
    private final Integer userAccountId;
    private final String action;
    private final String details;
    private final String ipAddress;
    private final String deviceInfo;
    private final LocalDateTime loggedAt;

    public UserAccessLog(
            Integer logId,
            Integer userAccountId,
            String action,
            String details,
            String ipAddress,
            String deviceInfo,
            LocalDateTime loggedAt) {

        this.logId = validateNullableId(logId, "Log ID must be positive.");
        this.userAccountId = validateNullableId(userAccountId, "User account ID must be positive.");
        this.action = requireNonBlank(action, "Action is required.");
        this.details = clean(details);
        this.ipAddress = clean(ipAddress);
        this.deviceInfo = clean(deviceInfo);
        this.loggedAt = Objects.requireNonNull(loggedAt, "Logged date and time is required.");
    }

    public UserAccessLog(
            Integer userAccountId,
            String action,
            String details,
            String ipAddress,
            String deviceInfo) {

        this(
                null,
                userAccountId,
                action,
                details,
                ipAddress,
                deviceInfo,
                LocalDateTime.now()
        );
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

    private Integer validateNullableId(Integer value, String message) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
