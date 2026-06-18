package motorph.application.dto.report.payroll;

import java.time.LocalDate;

/**
 * Stores employee payslip report data.
 */
public class PayslipReportDTO {

    private final int employeeId;
    private final String employeeName;
    private final String departmentName;
    private final String positionName;

    private final String tinNumber;
    private final String sssNumber;
    private final String philhealthNumber;
    private final String pagibigNumber;

    private final int payrunId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDate payDate;
    private final double totalHoursWorked;

    private final double basicPay;
    private final double totalBenefits;
    private final double totalOvertime;
    private final double grossPay;

    private final double sssDeduction;
    private final double philhealthDeduction;
    private final double pagibigDeduction;
    private final double withholdingTax;
    private final double otherDeductions;
    private final double totalDeductions;

    private final double netPay;

    private final double ytdGrossPay;
    private final double ytdWithholdingTax;
    private final double ytdSss;
    private final double ytdPhilhealth;
    private final double ytdPagibig;
    private final double ytdNetPay;

    // constructors
    public PayslipReportDTO(int employeeId,
                            String employeeName,
                            String departmentName,
                            String positionName,
                            String tinNumber,
                            String sssNumber,
                            String philhealthNumber,
                            String pagibigNumber,
                            int payrunId,
                            LocalDate startDate,
                            LocalDate endDate,
                            LocalDate payDate,
                            double totalHoursWorked,
                            double basicPay,
                            double totalBenefits,
                            double totalOvertime,
                            double grossPay,
                            double sssDeduction,
                            double philhealthDeduction,
                            double pagibigDeduction,
                            double withholdingTax,
                            double otherDeductions,
                            double totalDeductions,
                            double netPay,
                            double ytdGrossPay,
                            double ytdWithholdingTax,
                            double ytdSss,
                            double ytdPhilhealth,
                            double ytdPagibig,
                            double ytdNetPay) {

        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.departmentName = departmentName;
        this.positionName = positionName;
        this.tinNumber = clean(tinNumber);
        this.sssNumber = clean(sssNumber);
        this.philhealthNumber = clean(philhealthNumber);
        this.pagibigNumber = clean(pagibigNumber);
        this.payrunId = payrunId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.payDate = payDate;
        this.totalHoursWorked = totalHoursWorked;
        this.basicPay = basicPay;
        this.totalBenefits = totalBenefits;
        this.totalOvertime = totalOvertime;
        this.grossPay = grossPay;
        this.sssDeduction = sssDeduction;
        this.philhealthDeduction = philhealthDeduction;
        this.pagibigDeduction = pagibigDeduction;
        this.withholdingTax = withholdingTax;
        this.otherDeductions = otherDeductions;
        this.totalDeductions = totalDeductions;
        this.netPay = netPay;
        this.ytdGrossPay = ytdGrossPay;
        this.ytdWithholdingTax = ytdWithholdingTax;
        this.ytdSss = ytdSss;
        this.ytdPhilhealth = ytdPhilhealth;
        this.ytdPagibig = ytdPagibig;
        this.ytdNetPay = ytdNetPay;
    }

    // getters
    public int getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getPositionName() {
        return positionName;
    }

    public String getTinNumber() {
        return tinNumber;
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

    public int getPayrunId() {
        return payrunId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public double getTotalHoursWorked() {
        return totalHoursWorked;
    }

    public double getBasicPay() {
        return basicPay;
    }

    public double getTotalBenefits() {
        return totalBenefits;
    }

    public double getTotalOvertime() {
        return totalOvertime;
    }

    public double getGrossPay() {
        return grossPay;
    }

    public double getSssDeduction() {
        return sssDeduction;
    }

    public double getPhilhealthDeduction() {
        return philhealthDeduction;
    }

    public double getPagibigDeduction() {
        return pagibigDeduction;
    }

    public double getWithholdingTax() {
        return withholdingTax;
    }

    public double getOtherDeductions() {
        return otherDeductions;
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNetPay() {
        return netPay;
    }

    public double getYtdGrossPay() {
        return ytdGrossPay;
    }

    public double getYtdWithholdingTax() {
        return ytdWithholdingTax;
    }

    public double getYtdSss() {
        return ytdSss;
    }

    public double getYtdPhilhealth() {
        return ytdPhilhealth;
    }

    public double getYtdPagibig() {
        return ytdPagibig;
    }

    public double getYtdNetPay() {
        return ytdNetPay;
    }

    // cleans optional text values
    private String clean(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }
}