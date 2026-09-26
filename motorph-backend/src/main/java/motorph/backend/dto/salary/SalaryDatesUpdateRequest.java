package motorph.backend.dto.salary;

import java.time.LocalDate;

/** Request body for {@code PUT /api/v1/salary-history/{salaryHistoryId}}. */
public record SalaryDatesUpdateRequest(
        LocalDate startDate,
        LocalDate endDate
) {
}
