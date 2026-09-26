package motorph.api.dto.employee;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Client-side copy of the backend's {@code Department} reference data.
 */
public class DepartmentDTO {

    private final Integer departmentId;
    private final String departmentCode;
    private final String departmentName;
    private final boolean active;

    @JsonCreator
    public DepartmentDTO(@JsonProperty("departmentId") Integer departmentId,
                         @JsonProperty("departmentCode") String departmentCode,
                         @JsonProperty("departmentName") String departmentName,
                         @JsonProperty("active") boolean active) {
        this.departmentId = departmentId;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.active = active;
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
}
