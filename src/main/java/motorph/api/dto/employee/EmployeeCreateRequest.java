package motorph.api.dto.employee;

import motorph.domain.value.EmploymentStatus;
import motorph.domain.value.Gender;

import java.time.LocalDate;

/** Wire request body for POST /api/v1/employees. Outbound only - never deserialized. */
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
