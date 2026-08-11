package motorph.api;

import motorph.api.dto.attendance.AttendanceDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Thin wrapper around the backend's /api/v1/attendance endpoints. time-in/
 * time-out never send an employeeId - the backend always resolves the
 * caller's own employeeId server-side from the validated token.
 */
public final class AttendanceApiClient {

    private final HttpApiClient httpApiClient;

    public AttendanceApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public void timeIn() {
        httpApiClient.postNoContent("/api/v1/attendance/time-in", null);
    }

    public void timeOut() {
        httpApiClient.postNoContent("/api/v1/attendance/time-out", null);
    }

    public List<AttendanceDTO> getOwnAttendance() {
        return httpApiClient.getList("/api/v1/attendance/me", AttendanceDTO.class);
    }

    public AttendanceDTO getOwnAttendanceToday() {
        return httpApiClient.get("/api/v1/attendance/me/today", AttendanceDTO.class);
    }

    public List<AttendanceDTO> getOwnAttendanceRange(LocalDate startDate, LocalDate endDate) {
        return httpApiClient.getList(
                "/api/v1/attendance/me/range?startDate=" + startDate + "&endDate=" + endDate,
                AttendanceDTO.class);
    }

    public List<AttendanceDTO> getAttendanceByDateRange(LocalDate startDate, LocalDate endDate) {
        return httpApiClient.getList(
                "/api/v1/attendance?startDate=" + startDate + "&endDate=" + endDate,
                AttendanceDTO.class);
    }

    public List<AttendanceDTO> getAllAttendance() {
        return httpApiClient.getList("/api/v1/attendance/all", AttendanceDTO.class);
    }
}
