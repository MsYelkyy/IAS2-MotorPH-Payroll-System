package motorph.backend.dto.leave;

import java.time.LocalDate;

/**
 * Request body for {@code POST /api/v1/leave-requests}. The employee is always the caller, so no
 * employee ID is accepted.
 */
public record LeaveSubmitRequest(
        int leaveTypeId,
        LocalDate startDate,
        LocalDate endDate,
        double leaveHours,
        String reason
) {
}
