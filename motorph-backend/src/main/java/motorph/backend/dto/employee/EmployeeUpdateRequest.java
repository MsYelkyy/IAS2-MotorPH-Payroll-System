package motorph.backend.dto.employee;

import java.time.LocalDate;

import motorph.backend.domain.value.EmploymentStatus;
import motorph.backend.domain.value.Gender;

/**
 * Wire request body for PUT /api/v1/employees/{employeeId}. Salary is not
 * editable through this endpoint - the desktop's edit form never exposed
 * salary changes either, only profile fields plus government IDs/benefits.
 */
public record EmployeeUpdateRequest(
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
        String sssNumber,
        String philhealthNumber,
        String tinNumber,
        String pagibigNumber,
        double riceSubsidy,
        double phoneAllowance,
        double clothingAllowance
) {
}
