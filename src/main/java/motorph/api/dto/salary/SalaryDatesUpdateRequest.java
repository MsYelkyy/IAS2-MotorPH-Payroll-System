package motorph.api.dto.salary;

import java.time.LocalDate;

/** Wire request body for PUT /api/v1/salary-history/{salaryHistoryId}. Outbound only. */
public record SalaryDatesUpdateRequest(
        LocalDate startDate,
        LocalDate endDate
) {
}
