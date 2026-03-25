package motorph.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;

/**
 * Represents a single attendance record from the database.
 */
public class Attendance {

    private final int employeeId;
    private final LocalDate workDate;
    private final LocalTime timeIn;
    private final LocalTime timeOut;

    // constructor
    public Attendance(int employeeId, LocalDate workDate, LocalTime timeIn, LocalTime timeOut) {
        this.employeeId = validateEmployeeId(employeeId);
        this.workDate = workDate;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
    }

    // validates employee id
    private int validateEmployeeId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }
        return id;
    }

    // GETTERS
    public int getEmployeeId() {
        return employeeId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public LocalTime getTimeIn() {
        return timeIn;
    }

    public LocalTime getTimeOut() {
        return timeOut;
    }

    // calculates total hours worked
    public double getHoursWorked() {
        if (timeIn == null || timeOut == null) return 0.0;

        long minutes = Duration.between(timeIn, timeOut).toMinutes();
        return minutes / 60.0;
    }

    // checks if attendance record is complete
    public boolean isComplete() {
        return timeIn != null && timeOut != null;
    }
}