package motorph.domain.model;

public class PayrollBreakdown {

    // Earnings (per cutoff)
    private final double basicPay;
    private final double riceSubsidy;
    private final double phoneAllowance;
    private final double clothingAllowance;
    private final double totalEarnings;

    // Attendance deductions
    private final double lateDeduction;
    private final double undertimeDeduction;

    // Government deductions
    private final double sss;
    private final double philhealth;
    private final double pagibig;
    private final double tax;

    // Totals
    private final double totalDeductions;
    private final double netPay;

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

        this.basicPay = requirePositive(basicPay, "Basic pay must be greater than 0.");
        this.riceSubsidy = requireNonNegative(riceSubsidy, "Rice subsidy cannot be negative.");
        this.phoneAllowance = requireNonNegative(phoneAllowance, "Phone allowance cannot be negative.");
        this.clothingAllowance = requireNonNegative(clothingAllowance, "Clothing allowance cannot be negative.");

        this.lateDeduction = requireNonNegative(lateDeduction, "Late deduction cannot be negative.");
        this.undertimeDeduction = requireNonNegative(undertimeDeduction, "Undertime deduction cannot be negative.");

        this.sss = requireNonNegative(sss, "SSS cannot be negative.");
        this.philhealth = requireNonNegative(philhealth, "PhilHealth cannot be negative.");
        this.pagibig = requireNonNegative(pagibig, "Pag-IBIG cannot be negative.");
        this.tax = requireNonNegative(tax, "Tax cannot be negative.");

        this.totalEarnings = requireNonNegative(totalEarnings, "Total earnings cannot be negative.");
        this.totalDeductions = requireNonNegative(totalDeductions, "Total deductions cannot be negative.");
        this.netPay = netPay;

        validateTotals();
    }

    // Earnings getters
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

    // Attendance deductions
    public double getLateDeduction() {
        return lateDeduction;
    }

    public double getUndertimeDeduction() {
        return undertimeDeduction;
    }

    // Government deductions
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

    // Totals
    public double getTotalDeductions() {
        return totalDeductions;
    }

    public double getNetPay() {
        return netPay;
    }

    public double getNetPay(double additionalDeduction) {
        double deduction = requireNonNegative(additionalDeduction, "Additional deduction cannot be negative.");
        return netPay - deduction;
    }

    // HELPERS
    private void validateTotals() {
        double expectedTotalEarnings = basicPay + riceSubsidy + phoneAllowance + clothingAllowance;
        double expectedTotalDeductions = lateDeduction + undertimeDeduction + sss + philhealth + pagibig + tax;
        double expectedNetPay = totalEarnings - totalDeductions;

        validateAmount(totalEarnings, expectedTotalEarnings, "Total earnings does not match breakdown.");
        validateAmount(totalDeductions, expectedTotalDeductions, "Total deductions does not match breakdown.");
        validateAmount(netPay, expectedNetPay, "Net pay does not match earnings minus deductions.");
    }

    /**
     * Throws an error if a numeric value is negative.
     */
    private double requireNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /** Error if a numeric value is 0 */
    private double requirePositive(double value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * Validates two amounts using a small tolerance for decimal computations.
     */
    private void validateAmount(double actual, double expected, String message) {
        double tolerance = 0.01;
        if (Math.abs(actual - expected) > tolerance) {
            throw new IllegalArgumentException(message);
        }
    }
}
