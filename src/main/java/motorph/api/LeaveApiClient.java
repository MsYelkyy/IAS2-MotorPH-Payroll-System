package motorph.api;

import motorph.api.dto.leave.LeaveCreditRowDTO;
import motorph.api.dto.leave.LeaveRequestDTO;
import motorph.api.dto.leave.LeaveRequestRowDTO;
import motorph.api.dto.leave.LeaveSubmitRequest;
import motorph.api.dto.leave.LeaveSummaryDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Thin wrapper around the backend's /api/v1/leave-requests endpoints.
 * Submitting/cancelling never send an employeeId - always the caller,
 * resolved server-side from the validated token.
 */
public final class LeaveApiClient {

    private final HttpApiClient httpApiClient;

    public LeaveApiClient(HttpApiClient httpApiClient) {
        this.httpApiClient = httpApiClient;
    }

    public void submitLeaveRequest(LeaveSubmitRequest request) {
        httpApiClient.postNoContent("/api/v1/leave-requests", request);
    }

    public List<LeaveRequestDTO> getOwnLeaveRequests() {
        return httpApiClient.getList("/api/v1/leave-requests/me", LeaveRequestDTO.class);
    }

    public List<LeaveRequestRowDTO> getAllLeaveRequests() {
        return httpApiClient.getList("/api/v1/leave-requests", LeaveRequestRowDTO.class);
    }

    public LeaveSummaryDTO getSummary() {
        return httpApiClient.get("/api/v1/leave-requests/summary", LeaveSummaryDTO.class);
    }

    public List<LeaveCreditRowDTO> getOwnLeaveCredits() {
        return httpApiClient.getList("/api/v1/leave-requests/credits/me", LeaveCreditRowDTO.class);
    }

    public List<LeaveCreditRowDTO> getLeaveCredits(int employeeId) {
        return httpApiClient.getList("/api/v1/leave-requests/credits/" + employeeId, LeaveCreditRowDTO.class);
    }

    public double computeLeaveHours(LocalDate startDate, LocalDate endDate) {
        return httpApiClient.get(
                "/api/v1/leave-requests/compute-hours?startDate=" + startDate + "&endDate=" + endDate,
                Double.class);
    }

    public void approveLeaveRequest(int leaveRequestId) {
        httpApiClient.postNoContent("/api/v1/leave-requests/" + leaveRequestId + "/approve", null);
    }

    public void rejectLeaveRequest(int leaveRequestId) {
        httpApiClient.postNoContent("/api/v1/leave-requests/" + leaveRequestId + "/reject", null);
    }

    public void cancelLeaveRequest(int leaveRequestId) {
        httpApiClient.postNoContent("/api/v1/leave-requests/" + leaveRequestId + "/cancel", null);
    }
}
