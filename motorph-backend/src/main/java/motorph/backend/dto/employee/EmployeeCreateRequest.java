package motorph.backend.dto.employee;

import java.time.LocalDate;

import motorph.backend.domain.value.EmploymentStatus;
import motorph.backend.domain.value.Gender;

/** Wire request body for POST /api/v1/employees. */
public record EmployeeCreateRequest(
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
        double basicSalary,
        double hourlyRate,
        String sssNumber,
        String philhealthNumber,
        String tinNumber,
        String pagibigNumber,
        double riceSubsidy,
        double phoneAllowance,
        double clothingAllowance
) {
}
