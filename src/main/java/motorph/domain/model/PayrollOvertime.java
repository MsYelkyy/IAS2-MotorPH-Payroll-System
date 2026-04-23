package motorph.domain.model;

/**
 * Represents an overtime entry applied to a payroll result and maps to the payroll_overtime table in the database.
 */
public class PayrollOvertime {

    private Integer payrollOvertimeId;
    private int payrollResultId;
    private int overtimeTypeId;
    private double hoursWorked;
    private double hourlyRate;
    private double amount;

    // constructors
    public PayrollOvertime(
            Integer payrollOvertimeId,
            int payrollResultId,
            int overtimeTypeId,
            double hoursWorked,
            double hourlyRate,
            double amount) {

        setPayrollOvertimeId(payrollOvertimeId);
        setPayrollResultId(payrollResultId);
        setOvertimeTypeId(overtimeTypeId);
        setHoursWorked(hoursWorked);
        setHourlyRate(hourlyRate);
        setAmount(amount);
    }

    // getters
    public Integer getPayrollOvertimeId() {
        return payrollOvertimeId;
    }

    public int getPayrollResultId() {
        return payrollResultId;
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

    public double getAmount() {
        return amount;
    }

    // setters
    public void setPayrollOvertimeId(Integer payrollOvertimeId) {
        if (payrollOvertimeId != null && payrollOvertimeId <= 0) {
            throw new IllegalArgumentException("Payroll overtime ID must be positive.");
        }
        this.payrollOvertimeId = payrollOvertimeId;
    }

    public void setPayrollResultId(int payrollResultId) {
        if (payrollResultId <= 0) {
            throw new IllegalArgumentException("Payroll result ID must be positive.");
        }
        this.payrollResultId = payrollResultId;
    }

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

    public void setAmount(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Overtime amount cannot be negative.");
        }
        this.amount = amount;
    }

    // returns the payroll overtime summary
    @Override
    public String toString() {
        return "PayrollOvertime{"
                + "payrollOvertimeId=" + payrollOvertimeId
                + ", payrollResultId=" + payrollResultId
                + ", overtimeTypeId=" + overtimeTypeId
                + ", hoursWorked=" + hoursWorked
                + ", hourlyRate=" + hourlyRate
                + ", amount=" + amount
                + '}';
    }
}