package motorph.backend.controller.v1;

import jakarta.validation.Valid;
import motorph.backend.domain.LeaveRequest;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.leave.LeaveCreditRowDTO;
import motorph.backend.dto.leave.LeaveRequestRowDTO;
import motorph.backend.dto.leave.LeaveSubmitRequest;
import motorph.backend.dto.leave.LeaveSummaryDTO;
import motorph.backend.exception.ValidationException;
import motorph.backend.security.AuthenticatedPrincipal;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.LeaveService;
import motorph.backend.service.PermissionService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Leave request endpoints. Submitting and cancelling always act on the caller's own record.
 * Approving or rejecting requires {@code APPROVE_LEAVE}, and the service separately prevents
 * employees from approving their own requests.
 */
@RestController
@RequestMapping("/api/v1/leave-requests")
public class LeaveController {

    private final LeaveService leaveService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    public LeaveController(LeaveService leaveService,
                           PermissionService permissionService,
                           CurrentUserProvider currentUserProvider) {
        this.leaveService = leaveService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public void submitLeaveRequest(@RequestBody @Valid LeaveSubmitRequest request) {
        String cleanReason = sanitizeText(request.reason());
        leaveService.submitLeaveRequest(
                callerEmployeeId(),
                request.leaveTypeId(),
                request.startDate(),
                request.endDate(),
                request.leaveHours(),
                cleanReason
        );
    }

    @GetMapping("/me")
    public List<LeaveRequest> getOwnLeaveRequests() {
        return leaveService.getLeaveRequestsByEmployeeId(callerEmployeeId());
    }

    @GetMapping
    public List<LeaveRequestRowDTO> getAllLeaveRequests() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_LEAVE, PermissionKey.APPROVE_LEAVE);
        return leaveService.getLeaveRequestRows();
    }

    @GetMapping("/summary")
    public LeaveSummaryDTO getSummary() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_LEAVE, PermissionKey.APPROVE_LEAVE);
        return new LeaveSummaryDTO(leaveService.countPendingRequests(), leaveService.countApprovedLeavesToday());
    }

    @GetMapping("/credits/me")
    public List<LeaveCreditRowDTO> getOwnLeaveCredits() {
        return leaveService.getLeaveCreditsForEmployee(callerEmployeeId());
    }

    @GetMapping("/credits/{employeeId}")
    public List<LeaveCreditRowDTO> getLeaveCredits(@PathVariable int employeeId) {
        validatePositiveId(employeeId, "Employee ID");
        if (employeeId != callerEmployeeId()) {
            permissionService.requireAnyPermission(PermissionKey.VIEW_LEAVE, PermissionKey.APPROVE_LEAVE);
        }
        return leaveService.getLeaveCreditsForEmployee(employeeId);
    }

    @GetMapping("/compute-hours")
    public double computeLeaveHours(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return leaveService.computeLeaveHours(startDate, endDate);
    }

    @PostMapping("/{leaveRequestId}/approve")
    public void approveLeaveRequest(@PathVariable int leaveRequestId) {
        validatePositiveId(leaveRequestId, "Leave Request ID");
        permissionService.requirePermission(PermissionKey.APPROVE_LEAVE);
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();
        leaveService.approveLeaveRequest(principal.employeeId(), principal.userAccountId(), leaveRequestId);
    }

    @PostMapping("/{leaveRequestId}/reject")
    public void rejectLeaveRequest(@PathVariable int leaveRequestId) {
        validatePositiveId(leaveRequestId, "Leave Request ID");
        permissionService.requirePermission(PermissionKey.APPROVE_LEAVE);
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();
        leaveService.rejectLeaveRequest(principal.employeeId(), principal.userAccountId(), leaveRequestId);
    }

    @PostMapping("/{leaveRequestId}/cancel")
    public void cancelLeaveRequest(@PathVariable int leaveRequestId) {
        validatePositiveId(leaveRequestId, "Leave Request ID");
        leaveService.cancelLeaveRequest(callerEmployeeId(), leaveRequestId);
    }

    private int callerEmployeeId() {
        return currentUserProvider.getCurrentPrincipal().employeeId();
    }

    private void validatePositiveId(int id, String name) {
        if (id <= 0) {
            throw new ValidationException(name + " must be a positive integer.");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required.");
        }
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date.");
        }
    }

    private String sanitizeText(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
