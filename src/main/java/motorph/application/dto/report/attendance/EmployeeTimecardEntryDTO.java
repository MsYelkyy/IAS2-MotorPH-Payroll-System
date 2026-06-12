package motorph.application.dto.report.attendance;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Stores employee timecard detail row data.
 */
public class EmployeeTimecardEntryDTO {

    private LocalDate workDate;
    private String day;
    private LocalTime timeIn;
    private LocalTime breakOut;
    private LocalTime breakIn;
    private LocalTime timeOut;
    private double totalHoursWorked;
    private String remarks;

    // constructors
    public EmployeeTimecardEntryDTO() {
    }

    public EmployeeTimecardEntryDTO(LocalDate workDate,
                                    String day,
                                    LocalTime timeIn,
                                    LocalTime breakOut,
                                    LocalTime breakIn,
                                    LocalTime timeOut,
                                    double totalHoursWorked,
                                    String remarks) {

        this.workDate = workDate;
        this.day = day;
        this.timeIn = timeIn;
        this.breakOut = breakOut;
        this.breakIn = breakIn;
        this.timeOut = timeOut;
        this.totalHoursWorked = totalHoursWorked;
        this.remarks = remarks;
    }

    // getters/setters
    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public LocalTime getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(LocalTime timeIn) {
        this.timeIn = timeIn;
    }

    public LocalTime getBreakOut() {
        return breakOut;
    }

    public void setBreakOut(LocalTime breakOut) {
        this.breakOut = breakOut;
    }

    public LocalTime getBreakIn() {
        return breakIn;
    }

    public void setBreakIn(LocalTime breakIn) {
        this.breakIn = breakIn;
    }

    public LocalTime getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(LocalTime timeOut) {
        this.timeOut = timeOut;
    }

    public double getTotalHoursWorked() {
        return totalHoursWorked;
    }

    public void setTotalHoursWorked(double totalHoursWorked) {
        this.totalHoursWorked = totalHoursWorked;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}