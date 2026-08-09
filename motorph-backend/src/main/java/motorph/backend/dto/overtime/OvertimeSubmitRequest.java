package motorph.backend.dto.overtime;

import java.time.LocalDate;

/** Wire request body for POST /api/v1/overtime-requests. employeeId is never included - always the caller. */
public record OvertimeSubmitRequest(
        int attendanceId,
        int overtimeTypeId,
        LocalDate overtimeDate,
        double requestedHours,
        String reason
) {
}
