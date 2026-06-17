package motorph.application.dto.salary;

import java.time.LocalDate;

public class SalaryHistoryDTO {

    private final int       employeeId;
    private final String    salaryType;   // "MONTHLY" or "HOURLY"
    private final Double    basicSalary;  // null when salaryType is HOURLY
    private final Double    hourlyRate;   // null when salaryType is MONTHLY
    private final LocalDate startDate;
    private final LocalDate endDate;      // null = open-ended

    public SalaryHistoryDTO(int employeeId, String salaryType, Double basicSalary,
                            Double hourlyRate, LocalDate startDate, LocalDate endDate) {
        this.employeeId  = employeeId;
        this.salaryType  = salaryType;
        this.basicSalary = basicSalary;
        this.hourlyRate  = hourlyRate;
        this.startDate   = startDate;
        this.endDate     = endDate;
    }

    public int       getEmployeeId()  { return employeeId; }
    public String    getSalaryType()  { return salaryType; }
    public Double    getBasicSalary() { return basicSalary; }
    public Double    getHourlyRate()  { return hourlyRate; }
    public LocalDate getStartDate()   { return startDate; }
    public LocalDate getEndDate()     { return endDate; }
}
