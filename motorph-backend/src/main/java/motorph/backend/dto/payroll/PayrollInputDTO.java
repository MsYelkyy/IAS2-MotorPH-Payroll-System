package motorph.backend.dto.payroll;

/**
 * Carries all data needed to compute payroll for one employee.
 */
public class PayrollInputDTO {

    private final int employeeId;

    // personal info
    private final String firstName;
    private final String lastName;
    private final int positionId;
    private final Integer supervisorId;

    // government details
    private final String tinNumber;
    private final String sssNumber;
    private final String philhealthNumber;
    private final String pagibigNumber;

    // salary info
    private final double basicSalary;
    private final double grossSemiMonthly;
    private final double hourlyRate;

    // benefits
    private final double riceSubsidy;
    private final double phoneAllowance;
    private final double clothingAllowance;

    // constructors
    public PayrollInputDTO(
            int employeeId,
            String firstName,
            String lastName,
            int positionId,
            Integer supervisorId,
            String tinNumber,
            String sssNumber,
            String philhealthNumber,
            String pagibigNumber,
            double basicSalary,
            double grossSemiMonthly,
            double hourlyRate,
            double riceSubsidy,
            double phoneAllowance,
            double clothingAllowance) {

        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.positionId = positionId;
        this.supervisorId = supervisorId;
        this.tinNumber = tinNumber;
        this.sssNumber = sssNumber;
        this.philhealthNumber = philhealthNumber;
        this.pagibigNumber = pagibigNumber;
        this.basicSalary = basicSalary;
        this.grossSemiMonthly = grossSemiMonthly;
        this.hourlyRate = hourlyRate;
        this.riceSubsidy = riceSubsidy;
        this.phoneAllowance = phoneAllowance;
        this.clothingAllowance = clothingAllowance;
    }

    // getters
    public int getEmployeeId() {
        return employeeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    // returns the employee full name
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public int getPositionId() {
        return positionId;
    }

    public Integer getSupervisorId() {
        return supervisorId;
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

    public double getBasicSalary() {
        return basicSalary;
    }

    public double getGrossSemiMonthly() {
        return grossSemiMonthly;
    }

    public double getHourlyRate() {
        return hourlyRate;
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
}
