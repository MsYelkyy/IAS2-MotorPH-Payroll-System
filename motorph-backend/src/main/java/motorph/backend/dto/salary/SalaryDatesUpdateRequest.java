package motorph.backend.dto.salary;

import java.time.LocalDate;

/** Wire request body for PUT /api/v1/salary-history/{salaryHistoryId}. */
public record SalaryDatesUpdateRequest(
        LocalDate startDate,
        LocalDate endDate
) {
}
