package motorph.application.dto.employee;

import java.time.LocalDate;

import motorph.domain.value.EmploymentStatus;

/**
 * Carries complete employee profile details for display and update use cases.
 */
public class EmployeeProfileDTO {

    private final Integer employeeId;
    private final String fullName;
    private final String firstName;
    private final String lastName;
    private final LocalDate birthDate;
    private final String address;
    private final String civilStatus;
    private final String phoneNumber;
    private final String personalEmail;
    private final EmploymentStatus employmentStatus;
    private final int positionId;
    private final int departmentId;
    private final LocalDate hireDate;
    private final Integer supervisorId;
    private final boolean active;
    private final String photoPath;

    // constructors
    public EmployeeProfileDTO(Integer employeeId,
                              String fullName,
                              String firstName,
                              String lastName,
                              LocalDate birthDate,
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
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.address = address;
        this.civilStatus = civilStatus;
        this.phoneNumber = phoneNumber;
        this.personalEmail = personalEmail;
        this.employmentStatus = employmentStatus;
        this.positionId = positionId;
        this.departmentId = departmentId;
        this.hireDate = hireDate;
        this.supervisorId = supervisorId;
        this.active = active;
        this.photoPath = photoPath;
    }

    // getters
    public Integer getEmployeeId() {
        return employeeId;
    }

    // getters
    public String getFullName() {
        return fullName;
    }

    // getters
    public String getFirstName() {
        return firstName;
    }

    // getters
    public String getLastName() {
        return lastName;
    }

    // getters
    public LocalDate getBirthDate() {
        return birthDate;
    }

    // getters
    public String getAddress() {
        return address;
    }

    // getters
    public String getCivilStatus() {
        return civilStatus;
    }

    // getters
    public String getPhoneNumber() {
        return phoneNumber;
    }

    // getters
    public String getPersonalEmail() {
        return personalEmail;
    }

    // getters
    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }

    // getters
    public int getPositionId() {
        return positionId;
    }

    // getters
    public int getDepartmentId() {
        return departmentId;
    }

    // getters
    public LocalDate getHireDate() {
        return hireDate;
    }

    // getters
    public Integer getSupervisorId() {
        return supervisorId;
    }

    // getters
    public boolean isActive() {
        return active;
    }

    // getters
    public String getPhotoPath() {
        return photoPath;
    }
}