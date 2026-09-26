package motorph.backend.dto.overtime;

import java.time.LocalDate;

/**
 * Request body for {@code POST /api/v1/overtime-requests}. The employee is always the caller, so no
 * employee ID is accepted.
 */
public record OvertimeSubmitRequest(
        int attendanceId,
        int overtimeTypeId,
        LocalDate overtimeDate,
        double requestedHours,
        String reason
) {
}
