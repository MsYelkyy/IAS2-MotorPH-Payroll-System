package motorph.backend.domain;

/**
 * Represents a job position and maps to the position table in the database.
 */
public class Position {

    private Integer positionId;
    private String positionName;
    private String description;
    private int departmentId;

    public Position(Integer positionId, String positionName, String description, int departmentId) {
        setPositionId(positionId);
        setPositionName(positionName);
        setDescription(description);
        setDepartmentId(departmentId);
    }

    public Integer getPositionId() {
        return positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public String getDescription() {
        return description;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public void setPositionId(Integer positionId) {
        if (positionId != null && positionId <= 0) {
            throw new IllegalArgumentException("Position ID must be positive.");
        }
        this.positionId = positionId;
    }

    public void setPositionName(String positionName) {
        this.positionName = requireNonBlank(positionName, "Position name is required.");
    }

    public void setDescription(String description) {
        this.description = clean(description);
    }

    public void setDepartmentId(int departmentId) {
        if (departmentId <= 0) {
            throw new IllegalArgumentException("Department ID must be positive.");
        }
        this.departmentId = departmentId;
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
