package motorph.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing one user access log entry.
 */
public class UserAccessLog {

    public static final String ACTION_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String ACTION_LOGIN_FAILED = "LOGIN_FAILED";
    public static final String ACTION_OPEN_VIEW = "OPEN_VIEW";
    public static final String ACTION_DATABASE_BACKUP = "DATABASE_BACKUP";

    private final LocalDateTime timestamp;
    private final int employeeId;
    private final int roleId;
    private final String action;
    private final String details;
    private final String ipAddress;
    private final String deviceInfo;

    // constructor
    public UserAccessLog(LocalDateTime timestamp,
                         int employeeId,
                         int roleId,
                         String action,
                         String details,
                         String ipAddress,
                         String deviceInfo) {

        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp is required");
        this.employeeId = validateEmployeeId(employeeId);
        this.roleId = roleId;
        this.action = requireNonBlank(action, "Action is required");
        this.details = (details == null) ? "" : details.trim();
        this.ipAddress = clean(ipAddress);
        this.deviceInfo = clean(deviceInfo);
    }

    // getter
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // getter
    public int getEmployeeId() {
        return employeeId;
    }

    // getter
    public int getRoleId() {
        return roleId;
    }

    // getter
    public String getAction() {
        return action;
    }

    // getter
    public String getDetails() {
        return details;
    }

    // getter
    public String getIpAddress() {
        return ipAddress;
    }

    // getter
    public String getDeviceInfo() {
        return deviceInfo;
    }

    // validates employee id
    private int validateEmployeeId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }
        return id;
    }

    // validates required string
    private static String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    // trims string safely
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public String toString() {
        return "UserAccessLog{" +
                "timestamp=" + timestamp +
                ", employeeId=" + employeeId +
                ", roleId=" + roleId +
                ", action='" + action + '\'' +
                ", details='" + details + '\'' +
                ",ipAddress='" + ipAddress + '\'' +
                ", deviceInfo='" + deviceInfo + '\'' +
                '}';
    }
}