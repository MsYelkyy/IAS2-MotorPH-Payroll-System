package motorph.application.dto.report.payroll;

import java.time.LocalDate;

/**
 * Holds organization payroll report data for JasperReports.
 *
 * <p>Backs the employee-level Monthly Payroll Summary Report: one row per
 * employee per CALENDAR MONTH (both semi-monthly cutoffs merged), carrying
 * government identifiers and the monthly statutory contributions (SSS,
 * PhilHealth, Pag-IBIG, withholding tax). All values originate from the
 * {@code vw_payroll_summary_monthly} view, which sums the persisted payroll
 * results; nothing is recalculated here.</p>
 */
public class OrganizationPayrollReportDTO {

    private final int employeeId;
    private final String employeeName;
    private final String positionName;
    private final String departmentName;

    // calendar month this row summarizes (e.g. "2024-06") and its bounds
    private final String payrollMonth;
    private final LocalDate monthStart;
    private final LocalDate monthEnd;

    private final double grossPay;

    // government identifiers
    private final String sssNumber;
    private final String philhealthNumber;
    private final String pagibigNumber;
    private final String tinNumber;

    // monthly contribution components
    private final double sssContribution;
    private final double philhealthContribution;
    private final double pagibigContribution;
    private final double withholdingTax;

    private final double totalDeductions;
    private final double netPay;

    // constructors
    public OrganizationPayrollReportDTO(int employeeId,
                                        String employeeName,
                                        String positionName,
                                        String departmentName,
                                        String payrollMonth,
                                        LocalDate monthStart,
                                        LocalDate monthEnd,
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

        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.positionName = positionName;
        this.departmentName = departmentName;
        this.payrollMonth = payrollMonth;
        this.monthStart = monthStart;
        this.monthEnd = monthEnd;
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

    public String getPayrollMonth() {
        return payrollMonth;
    }

    public LocalDate getMonthStart() {
        return monthStart;
    }

    public LocalDate getMonthEnd() {
        return monthEnd;
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
