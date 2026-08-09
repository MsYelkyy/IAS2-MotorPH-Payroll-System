package motorph.backend.domain;

import java.time.LocalDateTime;

/**
 * Represents a system permission assigned to roles.
 */
public class Permission {

    private Integer permissionId;
    private String permissionKey;
    private String module;
    private String action;
    private String recordType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Permission(Integer permissionId, String permissionKey, String module, String action,
                      String recordType, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        setPermissionId(permissionId);
        setPermissionKey(permissionKey);
        setModule(module);
        setAction(action);
        setRecordType(recordType);
        setDescription(description);
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
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

    public void setPermissionId(Integer permissionId) {
        if (permissionId != null && permissionId <= 0) {
            throw new IllegalArgumentException("Permission ID must be positive.");
        }
        this.permissionId = permissionId;
    }

    public void setPermissionKey(String permissionKey) {
        this.permissionKey = requireNonBlank(permissionKey, "Permission key is required.");
    }

    public void setModule(String module) {
        this.module = requireNonBlank(module, "Module is required.");
    }

    public void setAction(String action) {
        this.action = requireNonBlank(action, "Action is required.");
    }

    public void setRecordType(String recordType) {
        this.recordType = requireNonBlank(recordType, "Record type is required.");
    }

    public void setDescription(String description) {
        this.description = clean(description);
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
