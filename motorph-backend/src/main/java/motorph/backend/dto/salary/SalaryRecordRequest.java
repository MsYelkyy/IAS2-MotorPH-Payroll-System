package motorph.backend.dto.salary;

import java.time.LocalDate;

/** Request body for {@code POST /api/v1/employees/{employeeId}/salary-history}. */
public record SalaryRecordRequest(
        String salaryType,
        Double basicSalary,
        Double hourlyRate,
        LocalDate startDate,
        LocalDate endDate
) {
}
