package motorph.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

import motorph.domain.value.Role;

/**
 * Represents a single user access log entry for auditing system activity.
 */
public class UserAccessLog {

    private final LocalDateTime timestamp;
    private final Integer employeeId;
    private final Role role;
    private final String action;
    private final String details;
    private final String ipAddress;
    private final String deviceInfo;

    // constructors
    public UserAccessLog(LocalDateTime timestamp,
                         Integer employeeId,
                         Role role,
                         String action,
                         String details,
                         String ipAddress,
                         String deviceInfo) {

        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp is required.");
        this.employeeId = employeeId; // nullable
        this.role = role; // nullable
        this.action = requireNonBlank(action, "Action is required.");
        this.details = clean(details);
        this.ipAddress = clean(ipAddress);
        this.deviceInfo = clean(deviceInfo);
    }

    // getters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // getters
    public Integer getEmployeeId() {
        return employeeId;
    }

    // getters
    public Role getRole() {
        return role;
    }

    // getters
    public String getAction() {
        return action;
    }

    // getters
    public String getDetails() {
        return details;
    }

    // getters
    public String getIpAddress() {
        return ipAddress;
    }

    // getters
    public String getDeviceInfo() {
        return deviceInfo;
    }

    // Validates that a required string is not blank.
    private static String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    // Cleans optional string values.
    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "UserAccessLog{" +
                "timestamp=" + timestamp +
                ", employeeId=" + employeeId +
                ", role=" + role +
                ", action='" + action + '\'' +
                ", details='" + details + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", deviceInfo='" + deviceInfo + '\'' +
                '}';
    }
}