package motorph.domain.model;

/**
 * Represents a computed payroll summary and does not map directly to a database table.
 */
public class PayrollBreakdown {

    private final double basicPay;
    private final double riceSubsidy;
    private final double phoneAllowance;
    private final double clothingAllowance;
    private final double totalEarnings;
    private final double lateDeduction;
    private final double undertimeDeduction;
    private final double sss;
    private final double philhealth;
    private final double pagibig;
    private final double tax;
    private final double totalDeductions;
    private final double netPay;

    // constructors
    public PayrollBreakdown(
            double basicPay,
            double riceSubsidy,
            double phoneAllowance,
            double clothingAllowance,
            double totalEarnings,
            double lateDeduction,
            double undertimeDeduction,
            double sss,
            double philhealth,
            double pagibig,
            double tax,
            double totalDeductions,
            double netPay) {

        this.basicPay = requireNonNegative(basicPay, "Basic pay cannot be negative.");
        this.riceSubsidy = requireNonNegative(riceSubsidy, "Rice subsidy cannot be negative.");
        this.phoneAllowance = requireNonNegative(phoneAllowance, "Phone allowance cannot be negative.");
        this.clothingAllowance = requireNonNegative(clothingAllowance, "Clothing allowance cannot be negative.");
        this.totalEarnings = requireNonNegative(totalEarnings, "Total earnings cannot be negative.");
        this.lateDeduction = requireNonNegative(lateDeduction, "Late deduction cannot be negative.");
        this.undertimeDeduction = requireNonNegative(undertimeDeduction, "Undertime deduction cannot be negative.");
        this.sss = requireNonNegative(sss, "SSS cannot be negative.");
        this.philhealth = requireNonNegative(philhealth, "PhilHealth cannot be negative.");
        this.pagibig = requireNonNegative(pagibig, "Pag-IBIG cannot be negative.");
        this.tax = requireNonNegative(tax, "Tax cannot be negative.");
        this.totalDeductions = requireNonNegative(totalDeductions, "Total deductions cannot be negative.");
        this.netPay = netPay;

        validateTotals();
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

    public double getTotalEarnings() {
        return totalEarnings;
    }

    public double getLateDeduction() {
        return lateDeduction;
    }

    public double getUndertimeDeduction() {
        return undertimeDeduction;
    }

    public double getSss() {
        return sss;
    }

    public double getPhilhealth() {
        return philhealth;
    }

    public double getPagibig() {
        return pagibig;
    }

    public double getTax() {
        return tax;
    }

    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNetPay() {
        return netPay;
    }

    // returns the total allowances
    public double getTotalAllowances() {
        return riceSubsidy + phoneAllowance + clothingAllowance;
    }

    // returns the total attendance deductions
    public double getTotalAttendanceDeductions() {
        return lateDeduction + undertimeDeduction;
    }

    // returns the total government deductions
    public double getTotalGovernmentDeductions() {
        return sss + philhealth + pagibig + tax;
    }

    // returns the net pay after an additional deduction
    public double getNetPay(double additionalDeduction) {
        double deduction = requireNonNegative(additionalDeduction, "Additional deduction cannot be negative.");
        return netPay - deduction;
    }

    // validates the computed totals
    private void validateTotals() {
        double expectedTotalEarnings = basicPay + riceSubsidy + phoneAllowance + clothingAllowance;
        double expectedTotalDeductions = lateDeduction + undertimeDeduction + sss + philhealth + pagibig + tax;
        double expectedNetPay = totalEarnings - totalDeductions;

        validateAmount(totalEarnings, expectedTotalEarnings, "Total earnings does not match the payroll breakdown.");
        validateAmount(totalDeductions, expectedTotalDeductions, "Total deductions do not match the payroll breakdown.");
        validateAmount(netPay, expectedNetPay, "Net pay does not match total earnings minus total deductions.");
    }

    // validates a non-negative amount
    private double requireNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    // validates two amounts using a small tolerance
    private void validateAmount(double actual, double expected, String message) {
        double tolerance = 0.01;

        if (Math.abs(actual - expected) > tolerance) {
            throw new IllegalArgumentException(message);
        }
    }

    // returns the payroll breakdown summary
    @Override
    public String toString() {
        return "PayrollBreakdown{"
                + "basicPay=" + basicPay
                + ", totalEarnings=" + totalEarnings
                + ", totalDeductions=" + totalDeductions
                + ", netPay=" + netPay
                + '}';
    }
}