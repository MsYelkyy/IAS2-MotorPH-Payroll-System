package motorph.backend.domain;

/**
 * Represents a department and maps to the department table in the database.
 */
public class Department {

    private Integer departmentId;
    private String departmentCode;
    private String departmentName;
    private boolean active;

    public Department(Integer departmentId, String departmentCode, String departmentName, boolean active) {
        setDepartmentId(departmentId);
        setDepartmentCode(departmentCode);
        setDepartmentName(departmentName);
        setActive(active);
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public boolean isActive() {
        return active;
    }

    public void setDepartmentId(Integer departmentId) {
        if (departmentId != null && departmentId <= 0) {
            throw new IllegalArgumentException("Department ID must be positive.");
        }
        this.departmentId = departmentId;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = requireNonBlank(departmentCode, "Department code is required.");
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = requireNonBlank(departmentName, "Department name is required.");
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
