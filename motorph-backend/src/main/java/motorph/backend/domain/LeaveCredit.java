package motorph.backend.domain;

/**
 * Represents an employee leave credit record and maps to the leave_credit table.
 */
public class LeaveCredit {

    private Integer leaveCreditId;
    private int employeeId;
    private int leaveTypeId;
    private double totalCredits;
    private double usedCredits;
    private int year;

    public LeaveCredit(
            Integer leaveCreditId,
            int employeeId,
            int leaveTypeId,
            double totalCredits,
            double usedCredits,
            int year) {

        setLeaveCreditId(leaveCreditId);
        setEmployeeId(employeeId);
        setLeaveTypeId(leaveTypeId);
        setTotalCredits(totalCredits);
        setUsedCredits(usedCredits);
        setYear(year);
        validateCreditValues();
    }

    public Integer getLeaveCreditId() {
        return leaveCreditId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public int getLeaveTypeId() {
        return leaveTypeId;
    }

    public double getTotalCredits() {
        return totalCredits;
    }

    public double getUsedCredits() {
        return usedCredits;
    }

    public int getYear() {
        return year;
    }

    public double getRemainingCredits() {
        return totalCredits - usedCredits;
    }

    public void setLeaveCreditId(Integer leaveCreditId) {
        if (leaveCreditId != null && leaveCreditId <= 0) {
            throw new IllegalArgumentException("Leave credit ID must be positive.");
        }
        this.leaveCreditId = leaveCreditId;
    }

    public void setEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }
        this.employeeId = employeeId;
    }

    public void setLeaveTypeId(int leaveTypeId) {
        if (leaveTypeId <= 0) {
            throw new IllegalArgumentException("Leave type ID must be positive.");
        }
        this.leaveTypeId = leaveTypeId;
    }

    public void setTotalCredits(double totalCredits) {
        if (totalCredits < 0) {
            throw new IllegalArgumentException("Total credits cannot be negative.");
        }
        this.totalCredits = totalCredits;
    }

    public void setUsedCredits(double usedCredits) {
        if (usedCredits < 0) {
            throw new IllegalArgumentException("Used credits cannot be negative.");
        }
        this.usedCredits = usedCredits;
    }

    public void setYear(int year) {
        if (year <= 0) {
            throw new IllegalArgumentException("Year must be positive.");
        }
        this.year = year;
    }

    private void validateCreditValues() {
        if (usedCredits > totalCredits) {
            throw new IllegalArgumentException("Used credits cannot exceed total credits.");
        }
    }
}
