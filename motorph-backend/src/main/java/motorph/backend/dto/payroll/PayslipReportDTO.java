package motorph.backend.dto.payroll;

import java.time.LocalDate;

/**
 * Maps one-to-one to the {@code vw_employee_payslip_report} view; no figures are computed here. The
 * getter names are bound by {@code payslip_report.jrxml}, so renaming a field requires updating the
 * template as well.
 */
public class PayslipReportDTO {

    private final int payrollResultId;
    private final int employeeId;
    private final int payrunId;

    private final String employeeName;
    private final String positionName;
    private final String departmentName;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDate payDate;

    private final double monthlySalary;
    private final double dailyRate;
    private final double daysWorked;

    private final double basicPay;
    private final double overtimePay;

    private final double riceSubsidy;
    private final double phoneAllowance;
    private final double clothingAllowance;
    private final double totalBenefits;

    private final double sssDeduction;
    private final double philhealthDeduction;
    private final double pagibigDeduction;
    private final double withholdingTax;
    private final double otherDeductions;

    private final double grossPay;
    private final double totalDeductions;
    private final double netPay;

    public PayslipReportDTO(int payrollResultId,
                            int employeeId,
                            int payrunId,
                            String employeeName,
                            String positionName,
                            String departmentName,
                            LocalDate startDate,
                            LocalDate endDate,
                            LocalDate payDate,
                            double monthlySalary,
                            double dailyRate,
                            double daysWorked,
                            double basicPay,
                            double overtimePay,
                            double riceSubsidy,
                            double phoneAllowance,
                            double clothingAllowance,
                            double totalBenefits,
                            double sssDeduction,
                            double philhealthDeduction,
                            double pagibigDeduction,
                            double withholdingTax,
                            double otherDeductions,
                            double grossPay,
                            double totalDeductions,
                            double netPay) {

        this.payrollResultId = payrollResultId;
        this.employeeId = employeeId;
        this.payrunId = payrunId;
        this.employeeName = employeeName;
        this.positionName = positionName;
        this.departmentName = departmentName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.payDate = payDate;
        this.monthlySalary = monthlySalary;
        this.dailyRate = dailyRate;
        this.daysWorked = daysWorked;
        this.basicPay = basicPay;
        this.overtimePay = overtimePay;
        this.riceSubsidy = riceSubsidy;
        this.phoneAllowance = phoneAllowance;
        this.clothingAllowance = clothingAllowance;
        this.totalBenefits = totalBenefits;
        this.sssDeduction = sssDeduction;
        this.philhealthDeduction = philhealthDeduction;
        this.pagibigDeduction = pagibigDeduction;
        this.withholdingTax = withholdingTax;
        this.otherDeductions = otherDeductions;
        this.grossPay = grossPay;
        this.totalDeductions = totalDeductions;
        this.netPay = netPay;
    }

    public int getPayrollResultId() {
        return payrollResultId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public int getPayrunId() {
        return payrunId;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalDate getPayDate() {
        return payDate;
    }

    public double getMonthlySalary() {
        return monthlySalary;
    }

    public double getDailyRate() {
        return dailyRate;
    }

    public double getDaysWorked() {
        return daysWorked;
    }

    public double getBasicPay() {
        return basicPay;
    }

    public double getOvertimePay() {
        return overtimePay;
    }

    public double getRiceSubsidy() {
        return riceSubsidy;
    }

    public double getPhoneAllowance() {
        return phoneAllowance;
    }

    public double getClothingAllowance() {
        return clothingAllowance;
    }

    public double getTotalBenefits() {
        return totalBenefits;
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
