package motorph.api.dto.employee;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import motorph.domain.value.EmploymentStatus;
import motorph.domain.value.Gender;

import java.time.LocalDate;

/**
 * Wire copy of the backend's EmployeeProfileDTO. Deliberately mirrors the
 * old motorph.application.dto.employee.EmployeeProfileDTO's getter names
 * (JavaBean style, not record accessors) so existing UI call sites
 * (profile.getFullName(), etc.) need no changes beyond the service call itself.
 */
public class EmployeeProfileDTO {

    private final Integer employeeId;
    private final String fullName;
    private final String firstName;
    private final String lastName;
    private final Gender gender;
    private final LocalDate birthDate;
    private final String address;
    private final String civilStatus;
    private final String phoneNumber;
    private final String personalEmail;
    private final EmploymentStatus employmentStatus;
    private final int positionId;
    private final String positionName;
    private final int departmentId;
    private final String departmentName;
    private final LocalDate hireDate;
    private final Integer supervisorId;
    private final String supervisorName;
    private final boolean active;
    private final String photoPath;

    @JsonCreator
    public EmployeeProfileDTO(@JsonProperty("employeeId") Integer employeeId,
                              @JsonProperty("fullName") String fullName,
                              @JsonProperty("firstName") String firstName,
                              @JsonProperty("lastName") String lastName,
                              @JsonProperty("gender") Gender gender,
                              @JsonProperty("birthDate") LocalDate birthDate,
                              @JsonProperty("address") String address,
                              @JsonProperty("civilStatus") String civilStatus,
                              @JsonProperty("phoneNumber") String phoneNumber,
                              @JsonProperty("personalEmail") String personalEmail,
                              @JsonProperty("employmentStatus") EmploymentStatus employmentStatus,
                              @JsonProperty("positionId") int positionId,
                              @JsonProperty("positionName") String positionName,
                              @JsonProperty("departmentId") int departmentId,
                              @JsonProperty("departmentName") String departmentName,
                              @JsonProperty("hireDate") LocalDate hireDate,
                              @JsonProperty("supervisorId") Integer supervisorId,
                              @JsonProperty("supervisorName") String supervisorName,
                              @JsonProperty("active") boolean active,
                              @JsonProperty("photoPath") String photoPath) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.address = address;
        this.civilStatus = civilStatus;
        this.phoneNumber = phoneNumber;
        this.personalEmail = personalEmail;
        this.employmentStatus = employmentStatus;
        this.positionId = positionId;
        this.positionName = positionName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.hireDate = hireDate;
        this.supervisorId = supervisorId;
        this.supervisorName = supervisorName;
        this.active = active;
        this.photoPath = photoPath;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getAddress() {
        return address;
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

    public String getPositionName() {
        return positionName;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public Integer getSupervisorId() {
        return supervisorId;
    }

    public String getSupervisorName() {
        return supervisorName;
    }

    public boolean isActive() {
        return active;
    }

    public String getPhotoPath() {
        return photoPath;
    }
}
