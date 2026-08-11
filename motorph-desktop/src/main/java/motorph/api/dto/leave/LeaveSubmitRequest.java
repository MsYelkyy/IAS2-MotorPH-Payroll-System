package motorph.api.dto.leave;

import java.time.LocalDate;

/** Wire request body for POST /api/v1/leave-requests. Outbound only - never deserialized. */
public record LeaveSubmitRequest(
        int leaveTypeId,
        LocalDate startDate,
        LocalDate endDate,
        double leaveHours,
        String reason
) {
}
