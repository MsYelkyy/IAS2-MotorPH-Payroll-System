package motorph.domain.model;

import java.time.LocalDate;
import java.util.List;
import motorph.domain.value.Role;
import motorph.domain.contract.Payable;
import motorph.domain.calculator.PayrollCalculator;
import motorph.domain.model.Attendance;

/**
 * Abstract base class representing an employee in the MotorPH system.
 * Stores common employee information and payroll attributes shared by all
 * employee types.
 */
public abstract class Employee implements Payable {

    // Personal Information
    protected int employeeId;
    protected String lastName;
    protected String firstName;
    protected String birthday;
    protected String address;
    protected String phoneNumber;
    protected String personalEmail;
    protected String photoPath;

    // Government IDs
    protected String sssNumber;
    protected String philhealthNumber;
    protected String tinNumber;
    protected String pagIbigNumber;

    // Employment Information
    protected String status;
    protected String position;
    protected String immediateSupervisor;
    protected Role role;

    // Salary Components
    protected double basicSalary;
    protected double riceSubsidy;
    protected double phoneAllowance;
    protected double clothingAllowance;
    protected double grossSemiMonthlyRate;
    protected double hourlyRate;

    protected Employee(int employeeId, String lastName, String firstName, String birthday,
            String address, String phoneNumber, String sssNumber, String philhealthNumber,
            String tinNumber, String pagIbigNumber, String status, String position,
            String immediateSupervisor, double basicSalary, double riceSubsidy,
            double phoneAllowance, double clothingAllowance, double grossSemiMonthlyRate,
            double hourlyRate, String personalEmail, String photoPath, Role role) {

        setEmployeeId(employeeId);
        setLastName(lastName);
        setFirstName(firstName);
        setBirthday(birthday);
        setAddress(address);
        setPhoneNumber(phoneNumber);
        setPersonalEmail(personalEmail);
        setSssNumber(sssNumber);
        setPhilhealthNumber(philhealthNumber);
        setTinNumber(tinNumber);
        setPagIbigNumber(pagIbigNumber);
        setStatus(status);
        setPosition(position);
        setImmediateSupervisor(immediateSupervisor);
        setBasicSalary(basicSalary);
        setRiceSubsidy(riceSubsidy);
        setPhoneAllowance(phoneAllowance);
        setClothingAllowance(clothingAllowance);
        setGrossSemiMonthlyRate(grossSemiMonthlyRate);
        setHourlyRate(hourlyRate);
        setRole(role);
        setPhotoPath(photoPath);
    }

    // ABSTRACT METHODS

    /**
     * Calculates the gross salary for this employee.
     * Gross salary = basic salary + all allowances (rice, phone, clothing).
     * 
     * Implements Payable contract. Employee owns this payroll behavior.
     * Child classes inherit this implementation unless they need different logic.
     *
     * @return the gross salary amount
     */
    @Override
    public double getGrossSalary() {
        return basicSalary + riceSubsidy + phoneAllowance + clothingAllowance;
    }

    /**
     * Returns the menu items available for this employee type.
     */
    public abstract List<String> getMenuItems();

    // GETTERS

    public Role getRole() {
        return role;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getFullName(boolean lastNameFirst) {
        if (lastNameFirst) {
            return lastName + ", " + firstName;
        }
        return firstName + " " + lastName;
    }

    public String getBirthday() {
        return birthday;
    }

    public String getAddress() {
        return address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPersonalEmail() {return personalEmail; }

    public String getSssNumber() {
        return sssNumber;
    }

    public String getPhilhealthNumber() {
        return philhealthNumber;
    }

    public String getPhotoPath() { return photoPath; }

    public String getTinNumber() {
        return tinNumber;
    }

    public String getPagIbigNumber() {
        return pagIbigNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getPosition() {
        return position;
    }

    public String getImmediateSupervisor() {
        return immediateSupervisor;
    }

    public double getBasicSalary() {
        return basicSalary;
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

    public double getGrossSemiMonthlyRate() {
        return grossSemiMonthlyRate;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    // CONVENIENCE METHODS

    public boolean isRegular() {
        return "REGULAR".equalsIgnoreCase(status);
    }

    public boolean isProbationary() {
        return "PROBATIONARY".equalsIgnoreCase(status);
    }

    public boolean isContractual() {
        return "CONTRACTUAL".equalsIgnoreCase(status);
    }

    // SETTERS

    public void setEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }
        this.employeeId = employeeId;
    }

    public void setLastName(String lastName) {
        this.lastName = requireNonBlank(lastName, "Last name is required.");
    }

    public void setFirstName(String firstName) {
        this.firstName = requireNonBlank(firstName, "First name is required.");
    }

    public void setBirthday(String birthday) {
        this.birthday = clean(birthday);
    }

    public void setAddress(String address) {
        this.address = clean(address);
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = clean(phoneNumber);
    }

    public void setPersonalEmail(String personalEmail) { this.personalEmail = clean(personalEmail); }

    public void setPhotoPath(String photoPath) { this.photoPath = clean(photoPath); }

    public void setSssNumber(String sssNumber) {
        this.sssNumber = clean(sssNumber);
    }

    public void setPhilhealthNumber(String philhealthNumber) {
        this.philhealthNumber = clean(philhealthNumber);
    }

    public void setTinNumber(String tinNumber) {
        this.tinNumber = clean(tinNumber);
    }

    public void setPagIbigNumber(String pagIbigNumber) {
        this.pagIbigNumber = clean(pagIbigNumber);
    }

    public void setStatus(String status) {
        String value = requireNonBlank(status, "Employment status is required.").toUpperCase();

        if (!value.equals("REGULAR")
                && !value.equals("PROBATIONARY")
                && !value.equals("CONTRACTUAL")) {
            throw new IllegalArgumentException("Invalid employment status: " + status);
        }

        this.status = value;
    }

    public void setPosition(String position) {
        this.position = requireNonBlank(position, "Position is required.");
    }

    public void setImmediateSupervisor(String immediateSupervisor) {
        this.immediateSupervisor = clean(immediateSupervisor);
    }

    public void setRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }
        this.role = role;
    }

    public void setBasicSalary(double basicSalary) {
        this.basicSalary = requireNonNegative(basicSalary, "Basic salary cannot be negative.");
    }

    public void setRiceSubsidy(double riceSubsidy) {
        this.riceSubsidy = requireNonNegative(riceSubsidy, "Rice subsidy cannot be negative.");
    }

    public void setPhoneAllowance(double phoneAllowance) {
        this.phoneAllowance = requireNonNegative(phoneAllowance, "Phone allowance cannot be negative.");
    }

    public void setClothingAllowance(double clothingAllowance) {
        this.clothingAllowance = requireNonNegative(clothingAllowance, "Clothing allowance cannot be negative.");
    }

    public void setGrossSemiMonthlyRate(double grossSemiMonthlyRate) {
        this.grossSemiMonthlyRate = requireNonNegative(grossSemiMonthlyRate,
                "Gross semi-monthly rate cannot be negative.");
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = requireNonNegative(hourlyRate, "Hourly rate cannot be negative.");
    }

    @Override
    public PayrollBreakdown computeDeductions(
            List<Attendance> logs,
            LocalDate cutoffDate) {
        PayrollCalculator calculator = new PayrollCalculator();
        return calculator.computeDeductions(this, logs, cutoffDate);
    }

    protected static String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    protected static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    protected static double requireNonNegative(double value, String message) {
        if (value < 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    @Override
    public String toString() {
        return "Employee #" + employeeId
                + " | Name: " + firstName + " " + lastName
                + " | Position: " + position
                + " | Role: " + role;
    }
}