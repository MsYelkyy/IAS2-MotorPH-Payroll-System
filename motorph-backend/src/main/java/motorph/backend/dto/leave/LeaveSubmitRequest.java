package motorph.backend.dto.leave;

import java.time.LocalDate;

/** Wire request body for POST /api/v1/leave-requests. employeeId is never included - always the caller. */
public record LeaveSubmitRequest(
        int leaveTypeId,
        LocalDate startDate,
        LocalDate endDate,
        double leaveHours,
        String reason
) {
}
