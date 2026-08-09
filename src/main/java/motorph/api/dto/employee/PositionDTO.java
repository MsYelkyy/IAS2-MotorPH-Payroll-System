package motorph.api.dto.employee;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire copy of the backend's Position domain model (read-only reference
 * data), mirroring the desktop's original motorph.domain.model.Position
 * getter names so call sites need no changes.
 */
public class PositionDTO {

    private final Integer positionId;
    private final String positionName;
    private final String description;
    private final int departmentId;

    @JsonCreator
    public PositionDTO(@JsonProperty("positionId") Integer positionId,
                       @JsonProperty("positionName") String positionName,
                       @JsonProperty("description") String description,
                       @JsonProperty("departmentId") int departmentId) {
        this.positionId = positionId;
        this.positionName = positionName;
        this.description = description;
        this.departmentId = departmentId;
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
}
