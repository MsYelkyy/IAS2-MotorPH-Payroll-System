package motorph.api.dto.payroll;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import motorph.domain.value.ReadinessStatus;

/** Wire copy of the backend's payroll.PayrunReadinessDTO. */
public class PayrunReadinessDTO {
    private final int employeeId;
    private final String fullName;
    private final String department;
    private final String position;
    private final int daysWorked;
    private final double totalHours;
    private final ReadinessStatus status;

    @JsonCreator
    public PayrunReadinessDTO(@JsonProperty("employeeId") int employeeId,
                              @JsonProperty("fullName") String fullName,
                              @JsonProperty("department") String department,
                              @JsonProperty("position") String position,
                              @JsonProperty("daysWorked") int daysWorked,
                              @JsonProperty("totalHours") double totalHours,
                              @JsonProperty("status") ReadinessStatus status) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.department = department;
        this.position = position;
        this.daysWorked = daysWorked;
        this.totalHours = totalHours;
        this.status = status;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDepartment() {
        return department;
    }

    public String getPosition() {
        return position;
    }

    public int getDaysWorked() {
        return daysWorked;
    }

    public double getTotalHours() {
        return totalHours;
    }

    public ReadinessStatus getStatus() {
        return status;
    }

    public boolean isReady() {
        return status == ReadinessStatus.READY;
    }
}
