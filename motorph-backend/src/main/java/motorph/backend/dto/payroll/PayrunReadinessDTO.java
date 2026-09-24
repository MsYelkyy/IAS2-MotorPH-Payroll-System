package motorph.backend.dto.payroll;

import motorph.backend.domain.value.ReadinessStatus;

/**
 * Carries one employee's attendance readiness for a payroll period,
 * used by the pending payroll detail view before generation.
 */
public class PayrunReadinessDTO {

    private final int employeeId;
    private final String fullName;
    private final String department;
    private final String position;
    private final int daysWorked;
    private final double totalHours;
    private final ReadinessStatus status;

    public PayrunReadinessDTO(int employeeId,
                              String fullName,
                              String department,
                              String position,
                              int daysWorked,
                              double totalHours,
                              ReadinessStatus status) {
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
}
