package motorph.api.dto.overtime;

import java.time.LocalDate;

/** Wire request body for POST /api/v1/overtime-requests. Outbound only - never deserialized. */
public record OvertimeSubmitRequest(
        int attendanceId,
        int overtimeTypeId,
        LocalDate overtimeDate,
        double requestedHours,
        String reason
) {
}
