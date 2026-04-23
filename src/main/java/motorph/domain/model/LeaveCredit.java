package motorph.domain.model;

/**
 * Represents an employee leave credit record and maps to the leave_credit table in the database.
 */
public class LeaveCredit {

    private Integer leaveCreditId;
    private int employeeId;
    private int leaveTypeId;
    private double totalCredits;
    private double usedCredits;
    private double remainingCredits;
    private int year;

    // constructors
    public LeaveCredit(
            Integer leaveCreditId,
            int employeeId,
            int leaveTypeId,
            double totalCredits,
            double usedCredits,
            double remainingCredits,
            int year) {

        setLeaveCreditId(leaveCreditId);
        setEmployeeId(employeeId);
        setLeaveTypeId(leaveTypeId);
        setTotalCredits(totalCredits);
        setUsedCredits(usedCredits);
        setRemainingCredits(remainingCredits);
        setYear(year);
        validateCreditValues();
    }

    // getters
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

    public double getRemainingCredits() {
        return remainingCredits;
    }

    public int getYear() {
        return year;
    }

    // checks if the leave credit has remaining balance
    public boolean hasRemainingCredits() {
        return remainingCredits > 0;
    }

    // checks if the leave credit can cover the requested hours
    public boolean canCover(double requestedHours) {
        if (requestedHours < 0) {
            throw new IllegalArgumentException("Requested hours cannot be negative.");
        }
        return remainingCredits >= requestedHours;
    }

    // uses leave credits
    public void useCredits(double hours) {
        if (hours < 0) {
            throw new IllegalArgumentException("Used hours cannot be negative.");
        }

        if (hours > remainingCredits) {
            throw new IllegalArgumentException("Used hours cannot exceed remaining credits.");
        }

        this.usedCredits += hours;
        this.remainingCredits -= hours;
        validateCreditValues();
    }

    // setters
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

    public void setRemainingCredits(double remainingCredits) {
        if (remainingCredits < 0) {
            throw new IllegalArgumentException("Remaining credits cannot be negative.");
        }
        this.remainingCredits = remainingCredits;
    }

    public void setYear(int year) {
        if (year <= 0) {
            throw new IllegalArgumentException("Year must be positive.");
        }
        this.year = year;
    }

    // validates the leave credit values
    private void validateCreditValues() {
        if (usedCredits > totalCredits) {
            throw new IllegalArgumentException("Used credits cannot exceed total credits.");
        }

        double expectedRemainingCredits = totalCredits - usedCredits;
        double tolerance = 0.01;

        if (Math.abs(remainingCredits - expectedRemainingCredits) > tolerance) {
            throw new IllegalArgumentException("Remaining credits must match total credits minus used credits.");
        }
    }

    // returns the leave credit summary
    @Override
    public String toString() {
        return "LeaveCredit{"
                + "leaveCreditId=" + leaveCreditId
                + ", employeeId=" + employeeId
                + ", leaveTypeId=" + leaveTypeId
                + ", totalCredits=" + totalCredits
                + ", usedCredits=" + usedCredits
                + ", remainingCredits=" + remainingCredits
                + ", year=" + year
                + '}';
    }
}