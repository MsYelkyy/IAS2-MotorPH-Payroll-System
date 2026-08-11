package motorph.api;

import motorph.api.dto.attendance.AttendanceDTO;
import motorph.api.dto.overtime.OvertimeRequestDTO;
import motorph.api.dto.overtime.OvertimeSubmitRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Thin wrapper around the backend's /api/v1/overtime-requests endpoints.
 * Submitting/cancelling never send an employeeId - always the caller.
 */
public final class OvertimeApiClient {

    private final HttpApiClient httpApiClient;

    public OvertimeApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public void submitOvertimeRequest(OvertimeSubmitRequest request) {
        httpApiClient.postNoContent("/api/v1/overtime-requests", request);
    }

    public List<OvertimeRequestDTO> getOwnOvertimeRequests() {
        return httpApiClient.getList("/api/v1/overtime-requests/me", OvertimeRequestDTO.class);
    }

    public List<OvertimeRequestDTO> getAllOvertimeRequests() {
        return httpApiClient.getList("/api/v1/overtime-requests", OvertimeRequestDTO.class);
    }

    public List<AttendanceDTO> getEligibleAttendance(LocalDate asOfDate) {
        return httpApiClient.getList(
                "/api/v1/overtime-requests/eligible-attendance?asOfDate=" + asOfDate,
                AttendanceDTO.class);
    }

    public void approveOvertimeRequest(int overtimeRequestId) {
        httpApiClient.postNoContent("/api/v1/overtime-requests/" + overtimeRequestId + "/approve", null);
    }

    public void rejectOvertimeRequest(int overtimeRequestId) {
        httpApiClient.postNoContent("/api/v1/overtime-requests/" + overtimeRequestId + "/reject", null);
    }

    public void cancelOvertimeRequest(int overtimeRequestId) {
        httpApiClient.postNoContent("/api/v1/overtime-requests/" + overtimeRequestId + "/cancel", null);
    }
}
