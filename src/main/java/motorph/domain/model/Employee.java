package motorph.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import motorph.domain.value.EmploymentStatus;

import motorph.domain.value.Gender;

/**
 * Represents the core employee profile and maps to the employee table in the database.
 */
public class Employee {

    private Integer employeeId;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private Gender gender;
    private String address;
    private String civilStatus;
    private String phoneNumber;
    private String personalEmail;
    private EmploymentStatus employmentStatus;
    private int positionId;
    private int departmentId;
    private LocalDate hireDate;
    private Integer supervisorId;
    private boolean active;
    private String photoPath;

    // constructors
    public Employee(
            Integer employeeId,
            String firstName,
            String lastName,
            LocalDate birthDate,
            Gender gender,
            String address,
            String civilStatus,
            String phoneNumber,
            String personalEmail,
            EmploymentStatus employmentStatus,
            int positionId,
            int departmentId,
            LocalDate hireDate,
            Integer supervisorId,
            boolean active,
            String photoPath) {

        setEmployeeId(employeeId);
        setFirstName(firstName);
        setLastName(lastName);
        setBirthDate(birthDate);
        setGender(gender);
        setAddress(address);
        setCivilStatus(civilStatus);
        setPhoneNumber(phoneNumber);
        setPersonalEmail(personalEmail);
        setEmploymentStatus(employmentStatus);
        setPositionId(positionId);
        setDepartmentId(departmentId);
        setHireDate(hireDate);
        setSupervisorId(supervisorId);
        setActive(active);
        setPhotoPath(photoPath);
    }

    // getters
    public Integer getEmployeeId() {
        return employeeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getAddress() {
        return address;
    }

    public Gender getGender() {
        return gender;
    }

    public String getCivilStatus() {
        return civilStatus;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPersonalEmail() {
        return personalEmail;
    }

    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    public int getPositionId() {
        return positionId;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public Integer getSupervisorId() {
        return supervisorId;
    }

    public boolean isActive() {
        return active;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    // returns the employee full name
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // checks if the employee is regular
    public boolean isRegular() {
        return employmentStatus == EmploymentStatus.REGULAR;
    }

    // checks if the employee is probationary
    public boolean isProbationary() {
        return employmentStatus == EmploymentStatus.PROBATIONARY;
    }

    // checks if the employee is contractual
    public boolean isContractual() {
        return employmentStatus == EmploymentStatus.CONTRACTUAL;
    }

    // setters
    public void setEmployeeId(Integer employeeId) {
        if (employeeId != null && employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be positive.");
        }
        this.employeeId = employeeId;
    }

    public void setFirstName(String firstName) {
        this.firstName = requireNonBlank(firstName, "First name is required.");
    }

    public void setGender(Gender gender) {
        this.gender = Objects.requireNonNull(gender, "Gender is required.");
    }

    public void setLastName(String lastName) {
        this.lastName = requireNonBlank(lastName, "Last name is required.");
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public void setAddress(String address) {
        this.address = clean(address);
    }

    public void setCivilStatus(String civilStatus) {
        this.civilStatus = clean(civilStatus);
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = clean(phoneNumber);
    }

    public void setPersonalEmail(String personalEmail) {
        this.personalEmail = clean(personalEmail);
    }

    public void setEmploymentStatus(EmploymentStatus employmentStatus) {
        this.employmentStatus = Objects.requireNonNull(employmentStatus, "Employment status is required.");
    }

    public void setPositionId(int positionId) {
        if (positionId <= 0) {
            throw new IllegalArgumentException("Position ID must be positive.");
        }
        this.positionId = positionId;
    }

    public void setDepartmentId(int departmentId) {
        if (departmentId <= 0) {
            throw new IllegalArgumentException("Department ID must be positive.");
        }
        this.departmentId = departmentId;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = Objects.requireNonNull(hireDate, "Hire date is required.");
    }

    public void setSupervisorId(Integer supervisorId) {
        if (supervisorId != null && supervisorId <= 0) {
            throw new IllegalArgumentException("Supervisor ID must be positive.");
        }
        this.supervisorId = supervisorId;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = clean(photoPath);
    }

    // validates required text
    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    // cleans optional text
    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    // returns the employee summary
    @Override
    public String toString() {
        return "Employee{employeeId=" + employeeId
                + ", fullName='" + getFullName() + '\''
                + ", gender=" + gender
                + ", employmentStatus=" + employmentStatus
                + ", positionId=" + positionId
                + ", departmentId=" + departmentId
                + ", active=" + active
                + '}';
    }
}