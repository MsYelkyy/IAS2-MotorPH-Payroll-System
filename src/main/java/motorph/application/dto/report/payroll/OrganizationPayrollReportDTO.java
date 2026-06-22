package motorph.application.dto.report.payroll;

import java.time.LocalDate;

/**
 * Holds organization payroll report data for JasperReports.
 *
 * <p>Backs the employee-level Monthly Payroll Summary Report: one row per
 * employee per payrun, carrying government identifiers and the persisted
 * per-component contributions (SSS, PhilHealth, Pag-IBIG, withholding tax).
 * All values originate from the persisted payroll result; nothing is
 * recalculated here.</p>
 */
public class OrganizationPayrollReportDTO {

    private final int payrollResultId;
    private final int employeeId;
    private final String employeeName;
    private final String positionName;
    private final String departmentName;
    private final int payrunId;
    private final LocalDate periodStartDate;
    private final LocalDate periodEndDate;
    private final LocalDate payDate;
    private final double totalHoursWorked;
    private final double grossPay;

    // government identifiers
    private final String sssNumber;
    private final String philhealthNumber;
    private final String pagibigNumber;
    private final String tinNumber;

    // persisted contribution components
    private final double sssContribution;
    private final double philhealthContribution;
    private final double pagibigContribution;
    private final double withholdingTax;

    private final double totalDeductions;
    private final double netPay;

    // constructors
    public OrganizationPayrollReportDTO(int payrollResultId,
                                        int employeeId,
                                        String employeeName,
                                        String positionName,
                                        String departmentName,
                                        int payrunId,
                                        LocalDate periodStartDate,
                                        LocalDate periodEndDate,
                                        LocalDate payDate,
                                        double totalHoursWorked,
                                        double grossPay,
                                        String sssNumber,
                                        String philhealthNumber,
                                        String pagibigNumber,
                                        String tinNumber,
                                        double sssContribution,
                                        double philhealthContribution,
                                        double pagibigContribution,
                                        double withholdingTax,
                                        double totalDeductions,
                                        double netPay) {

        this.payrollResultId = payrollResultId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.positionName = positionName;
        this.departmentName = departmentName;
        this.payrunId = payrunId;
        this.periodStartDate = periodStartDate;
        this.periodEndDate = periodEndDate;
        this.payDate = payDate;
        this.totalHoursWorked = totalHoursWorked;
        this.grossPay = grossPay;
        this.sssNumber = clean(sssNumber);
        this.philhealthNumber = clean(philhealthNumber);
        this.pagibigNumber = clean(pagibigNumber);
        this.tinNumber = clean(tinNumber);
        this.sssContribution = sssContribution;
        this.philhealthContribution = philhealthContribution;
        this.pagibigContribution = pagibigContribution;
        this.withholdingTax = withholdingTax;
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

    public String getPositionName() {
        return positionName;
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

    public String getSssNumber() {
        return sssNumber;
    }

    public String getPhilhealthNumber() {
        return philhealthNumber;
    }

    public String getPagibigNumber() {
        return pagibigNumber;
    }

    public String getTinNumber() {
        return tinNumber;
    }

    public double getSssContribution() {
        return sssContribution;
    }

    public double getPhilhealthContribution() {
        return philhealthContribution;
    }

    public double getPagibigContribution() {
        return pagibigContribution;
    }

    public double getWithholdingTax() {
        return withholdingTax;
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNetPay() {
        return netPay;
    }

    // cleans optional government identifier values
    private String clean(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }
}
