package motorph.application.dto.report;

import java.time.LocalDate;

/**
 * Holds organization payroll report data for JasperReports.
 */
public class OrganizationPayrollReportDTO {

    private final int payrollResultId;
    private final int employeeId;
    private final String employeeName;
    private final String departmentName;
    private final int payrunId;
    private final LocalDate periodStartDate;
    private final LocalDate periodEndDate;
    private final LocalDate payDate;
    private final double totalHoursWorked;
    private final double grossPay;
    private final double totalDeductions;
    private final double netPay;

    // constructors
    public OrganizationPayrollReportDTO(int payrollResultId,
                                        int employeeId,
                                        String employeeName,
                                        String departmentName,
                                        int payrunId,
                                        LocalDate periodStartDate,
                                        LocalDate periodEndDate,
                                        LocalDate payDate,
                                        double totalHoursWorked,
                                        double grossPay,
                                        double totalDeductions,
                                        double netPay) {

        this.payrollResultId = payrollResultId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.departmentName = departmentName;
        this.payrunId = payrunId;
        this.periodStartDate = periodStartDate;
        this.periodEndDate = periodEndDate;
        this.payDate = payDate;
        this.totalHoursWorked = totalHoursWorked;
        this.grossPay = grossPay;
        this.totalDeductions = totalDeductions;
        this.netPay = netPay;
    }

    // getters
    public int getPayrollResultId() {
        return payrollResultId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public int getPayrunId() {
        return payrunId;
    }

    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }

    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public double getTotalHoursWorked() {
        return totalHoursWorked;
    }

    public double getGrossPay() {
        return grossPay;
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNetPay() {
        return netPay;
    }
}