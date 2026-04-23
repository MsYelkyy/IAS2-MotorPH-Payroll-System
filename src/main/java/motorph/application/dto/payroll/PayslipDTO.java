package motorph.application.dto.payroll;

import java.time.LocalDate;

/**
 * Carries the complete payslip details for display or printing.
 */
public class PayslipDTO {

    private final int employeeId;
    private final String fullName;
    private final String position;
    private final String supervisor;

    private final LocalDate payrollDate;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;

    // government details
    private final String tinNumber;
    private final String sssNumber;
    private final String philhealthNumber;
    private final String pagibigNumber;

    // compensation
    private final double basicPay;
    private final double riceSubsidy;
    private final double phoneAllowance;
    private final double clothingAllowance;
    private final double totalCompensation;

    // deductions
    private final double lateDeduction;
    private final double undertimeDeduction;
    private final double absenceDeduction;
    private final double sssDeduction;
    private final double philhealthDeduction;
    private final double pagibigDeduction;
    private final double taxDeduction;
    private final double totalDeductions;

    // totals
    private final double netPay;

    // year to date
    private final double ytdTaxableGross;
    private final double ytdTax;
    private final double ytdSss;
    private final double ytdPhilhealth;
    private final double ytdPagibig;
    private final double ytdNonTaxable;

    // constructors
    public PayslipDTO(
            int employeeId,
            String fullName,
            String position,
            String supervisor,
            LocalDate payrollDate,
            LocalDate periodStart,
            LocalDate periodEnd,
            String tinNumber,
            String sssNumber,
            String philhealthNumber,
            String pagibigNumber,
            double basicPay,
            double riceSubsidy,
            double phoneAllowance,
            double clothingAllowance,
            double totalCompensation,
            double lateDeduction,
            double undertimeDeduction,
            double absenceDeduction,
            double sssDeduction,
            double philhealthDeduction,
            double pagibigDeduction,
            double taxDeduction,
            double totalDeductions,
            double netPay,
            double ytdTaxableGross,
            double ytdTax,
            double ytdSss,
            double ytdPhilhealth,
            double ytdPagibig,
            double ytdNonTaxable) {

        this.employeeId = employeeId;
        this.fullName = fullName;
        this.position = position;
        this.supervisor = supervisor;
        this.payrollDate = payrollDate;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.tinNumber = tinNumber;
        this.sssNumber = sssNumber;
        this.philhealthNumber = philhealthNumber;
        this.pagibigNumber = pagibigNumber;
        this.basicPay = basicPay;
        this.riceSubsidy = riceSubsidy;
        this.phoneAllowance = phoneAllowance;
        this.clothingAllowance = clothingAllowance;
        this.totalCompensation = totalCompensation;
        this.lateDeduction = lateDeduction;
        this.undertimeDeduction = undertimeDeduction;
        this.absenceDeduction = absenceDeduction;
        this.sssDeduction = sssDeduction;
        this.philhealthDeduction = philhealthDeduction;
        this.pagibigDeduction = pagibigDeduction;
        this.taxDeduction = taxDeduction;
        this.totalDeductions = totalDeductions;
        this.netPay = netPay;
        this.ytdTaxableGross = ytdTaxableGross;
        this.ytdTax = ytdTax;
        this.ytdSss = ytdSss;
        this.ytdPhilhealth = ytdPhilhealth;
        this.ytdPagibig = ytdPagibig;
        this.ytdNonTaxable = ytdNonTaxable;
    }

    // getters
    public int getEmployeeId() {
        return employeeId;
    }

    // getters
    public String getFullName() {
        return fullName;
    }

    // getters
    public String getPosition() {
        return position;
    }

    // getters
    public String getSupervisor() {
        return supervisor;
    }

    // getters
    public LocalDate getPayrollDate() {
        return payrollDate;
    }

    // getters
    public LocalDate getPeriodStart() {
        return periodStart;
    }

    // getters
    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    // getters
    public String getTinNumber() {
        return tinNumber;
    }

    // getters
    public String getSssNumber() {
        return sssNumber;
    }

    // getters
    public String getPhilhealthNumber() {
        return philhealthNumber;
    }

    // getters
    public String getPagibigNumber() {
        return pagibigNumber;
    }

    // getters
    public double getBasicPay() {
        return basicPay;
    }

    // getters
    public double getRiceSubsidy() {
        return riceSubsidy;
    }

    // getters
    public double getPhoneAllowance() {
        return phoneAllowance;
    }

    // getters
    public double getClothingAllowance() {
        return clothingAllowance;
    }

    // getters
    public double getTotalCompensation() {
        return totalCompensation;
    }

    // getters
    public double getLateDeduction() {
        return lateDeduction;
    }

    // getters
    public double getUndertimeDeduction() {
        return undertimeDeduction;
    }

    // getters
    public double getAbsenceDeduction() {
        return absenceDeduction;
    }

    // getters
    public double getSssDeduction() {
        return sssDeduction;
    }

    // getters
    public double getPhilhealthDeduction() {
        return philhealthDeduction;
    }

    // getters
    public double getPagibigDeduction() {
        return pagibigDeduction;
    }

    // getters
    public double getTaxDeduction() {
        return taxDeduction;
    }

    // getters
    public double getTotalDeductions() {
        return totalDeductions;
    }

    // getters
    public double getNetPay() {
        return netPay;
    }

    // getters
    public double getYtdTaxableGross() {
        return ytdTaxableGross;
    }

    // getters
    public double getYtdTax() {
        return ytdTax;
    }

    // getters
    public double getYtdSss() {
        return ytdSss;
    }

    // getters
    public double getYtdPhilhealth() {
        return ytdPhilhealth;
    }

    // getters
    public double getYtdPagibig() {
        return ytdPagibig;
    }

    // getters
    public double getYtdNonTaxable() {
        return ytdNonTaxable;
    }
}