package motorph.api.dto.permission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Client-side copy of the backend's {@code Permission}.
 */
public class PermissionDTO {

    private final Integer permissionId;
    private final String permissionKey;
    private final String module;
    private final String action;
    private final String recordType;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @JsonCreator
    public PermissionDTO(@JsonProperty("permissionId") Integer permissionId,
                         @JsonProperty("permissionKey") String permissionKey,
                         @JsonProperty("module") String module,
                         @JsonProperty("action") String action,
                         @JsonProperty("recordType") String recordType,
                         @JsonProperty("description") String description,
                         @JsonProperty("createdAt") LocalDateTime createdAt,
                         @JsonProperty("updatedAt") LocalDateTime updatedAt) {
        this.permissionId = permissionId;
        this.permissionKey = permissionKey;
        this.module = module;
        this.action = action;
        this.recordType = recordType;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getPermissionId() {
        return permissionId;
    }

    public String getPermissionKey() {
        return permissionKey;
    }

    public String getModule() {
        return module;
    }

    public String getAction() {
        return action;
    }

    public String getRecordType() {
        return recordType;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
