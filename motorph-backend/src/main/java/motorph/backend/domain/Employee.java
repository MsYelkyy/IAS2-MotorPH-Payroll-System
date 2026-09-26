package motorph.backend.domain;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

import motorph.backend.domain.value.EmploymentStatus;
import motorph.backend.domain.value.Gender;

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

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getFullName(boolean lastNameFirst) {
        return lastNameFirst ? lastName + ", " + firstName : getFullName();
    }

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
        String cleaned = clean(civilStatus);
        this.civilStatus = cleaned == null || cleaned.isEmpty() ? cleaned : cleaned.toUpperCase(Locale.ROOT);
    }

    public void setPhoneNumber(String phoneNumber) {
        String cleaned = clean(phoneNumber);
        if (cleaned != null && !cleaned.isEmpty() && !cleaned.matches("[0-9+()\\-\\s]{7,}")) {
            throw new IllegalArgumentException("Phone number format is invalid.");
        }
        this.phoneNumber = cleaned;
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
        Objects.requireNonNull(hireDate, "Hire date is required.");
        if (hireDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Hire date cannot be in the future.");
        }
        this.hireDate = hireDate;
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

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
