package motorph.application.dto.payroll;

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
    private final String positionName;
    private final String supervisorName;

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
            String positionName,
            String supervisorName,
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
        this.positionName = positionName;
        this.supervisorName = supervisorName;
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

    // getters
    public String getFirstName() {
        return firstName;
    }

    // getters
    public String getLastName() {
        return lastName;
    }

    // returns the employee full name
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // getters
    public int getPositionId() {
        return positionId;
    }

    // getters
    public Integer getSupervisorId() {
        return supervisorId;
    }

    // getters
    public String getPositionName() {
        return positionName;
    }

    // getters
    public String getSupervisorName() {
        return supervisorName;
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
    public double getBasicSalary() {
        return basicSalary;
    }

    // getters
    public double getGrossSemiMonthly() {
        return grossSemiMonthly;
    }

    // getters
    public double getHourlyRate() {
        return hourlyRate;
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
}