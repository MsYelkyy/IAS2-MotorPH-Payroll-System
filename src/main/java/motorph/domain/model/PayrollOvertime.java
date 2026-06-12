package motorph.domain.model;

/**
 * Represents an overtime entry applied to a payroll result and maps to the payroll_overtime table in the database.
 */
public class PayrollOvertime extends PayrollComponent {

    private int overtimeTypeId;
    private double hoursWorked;
    private double hourlyRate;

    // constructors
    public PayrollOvertime(
            Integer payrollOvertimeId,
            int payrollResultId,
            int overtimeTypeId,
            double hoursWorked,
            double hourlyRate,
            double amount) {

        super(payrollOvertimeId, payrollResultId, amount);
        setOvertimeTypeId(overtimeTypeId);
        setHoursWorked(hoursWorked);
        setHourlyRate(hourlyRate);
    }

    // getters
    public Integer getPayrollOvertimeId() {
        return getPayrollComponentId();
    }

    public int getOvertimeTypeId() {
        return overtimeTypeId;
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    // setters
    public void setOvertimeTypeId(int overtimeTypeId) {
        if (overtimeTypeId <= 0) {
            throw new IllegalArgumentException("Overtime type ID must be positive.");
        }
        this.overtimeTypeId = overtimeTypeId;
    }

    public void setHoursWorked(double hoursWorked) {
        if (hoursWorked < 0) {
            throw new IllegalArgumentException("Hours worked cannot be negative.");
        }
        this.hoursWorked = hoursWorked;
    }

    public void setHourlyRate(double hourlyRate) {
        if (hourlyRate < 0) {
            throw new IllegalArgumentException("Hourly rate cannot be negative.");
        }
        this.hourlyRate = hourlyRate;
    }

    // returns the expected overtime amount
    public double calculateExpectedAmount() {
        return hoursWorked * hourlyRate;
    }

    // returns the payroll overtime summary
    @Override
    public String toString() {
        return "PayrollOvertime{"
                + "payrollOvertimeId=" + getPayrollOvertimeId()
                + ", payrollResultId=" + getPayrollResultId()
                + ", overtimeTypeId=" + overtimeTypeId
                + ", hoursWorked=" + hoursWorked
                + ", hourlyRate=" + hourlyRate
                + ", amount=" + getAmount()
                + '}';
    }
}