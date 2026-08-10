package motorph.backend.domain;

/**
 * Represents the computed payroll breakdown for one payroll result.
 */
public class PayrollBreakdown {

    private final double basicPay;
    private final double riceSubsidy;
    private final double phoneAllowance;
    private final double clothingAllowance;
    private final double overtimePay;
    private final double totalEarnings;

    private final double lateDeduction;
    private final double undertimeDeduction;
    private final double sssDeduction;
    private final double philhealthDeduction;
    private final double pagibigDeduction;
    private final double withholdingTax;

    private final double totalDeductions;
    private final double netPay;

    // constructors
    public PayrollBreakdown(
            double basicPay,
            double riceSubsidy,
            double phoneAllowance,
            double clothingAllowance,
            double overtimePay,
            double totalEarnings,
            double lateDeduction,
            double undertimeDeduction,
            double sssDeduction,
            double philhealthDeduction,
            double pagibigDeduction,
            double withholdingTax,
            double totalDeductions,
            double netPay) {

        this.basicPay = validateNonNegative(basicPay, "Basic pay cannot be negative.");
        this.riceSubsidy = validateNonNegative(riceSubsidy, "Rice subsidy cannot be negative.");
        this.phoneAllowance = validateNonNegative(phoneAllowance, "Phone allowance cannot be negative.");
        this.clothingAllowance = validateNonNegative(clothingAllowance, "Clothing allowance cannot be negative.");
        this.overtimePay = validateNonNegative(overtimePay, "Overtime pay cannot be negative.");
        this.totalEarnings = validateNonNegative(totalEarnings, "Total earnings cannot be negative.");

        this.lateDeduction = validateNonNegative(lateDeduction, "Late deduction cannot be negative.");
        this.undertimeDeduction = validateNonNegative(undertimeDeduction, "Undertime deduction cannot be negative.");
        this.sssDeduction = validateNonNegative(sssDeduction, "SSS deduction cannot be negative.");
        this.philhealthDeduction = validateNonNegative(philhealthDeduction, "PhilHealth deduction cannot be negative.");
        this.pagibigDeduction = validateNonNegative(pagibigDeduction, "Pag-IBIG deduction cannot be negative.");
        this.withholdingTax = validateNonNegative(withholdingTax, "Withholding tax cannot be negative.");

        this.totalDeductions = validateNonNegative(totalDeductions, "Total deductions cannot be negative.");
        this.netPay = netPay;

        validateNetPay();
    }

    // getters
    public double getBasicPay() {
        return basicPay;
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

    public double getOvertimePay() {
        return overtimePay;
    }

    public double getTotalEarnings() {
        return totalEarnings;
    }

    public double getLateDeduction() {
        return lateDeduction;
    }

    public double getUndertimeDeduction() {
        return undertimeDeduction;
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

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNetPay() {
        return netPay;
    }

    // validates non-negative numeric values
    private double validateNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // validates computed net pay
    private void validateNetPay() {
        double expectedNetPay = totalEarnings - totalDeductions;
        double tolerance = 0.01;

        if (Math.abs(netPay - expectedNetPay) > tolerance) {
            throw new IllegalArgumentException(
                    "Net pay does not match total earnings minus total deductions."
            );
        }
    }
}
