package motorph.api.dto.salary;

import java.time.LocalDate;

/** Wire request body for POST /api/v1/employees/{employeeId}/salary-history. Outbound only. */
public record SalaryRecordRequest(
        String salaryType,
        Double basicSalary,
        Double hourlyRate,
        LocalDate startDate,
        LocalDate endDate
) {
}
