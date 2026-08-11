package motorph.api.dto.payroll;

import motorph.domain.value.PayrollType;

import java.time.LocalDate;

/** Outbound-only request body to create a new payrun. */
public record PayrunCreateRequest(
        LocalDate startDate,
        LocalDate endDate,
        LocalDate payDate,
        PayrollType payrollType
) {
}
