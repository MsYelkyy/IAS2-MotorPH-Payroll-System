package motorph.backend.controller.v1;

import jakarta.validation.Valid;
import motorph.backend.domain.Attendance;
import motorph.backend.domain.OvertimeRequest;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.overtime.OvertimeSubmitRequest;
import motorph.backend.exception.ValidationException;
import motorph.backend.security.AuthenticatedPrincipal;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.OvertimeService;
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
 * Overtime request endpoints with path variable and payload validation.
 * Submitting and cancelling always act on the caller's own record.
 *
 * <p>Approval is open to holders of {@code APPROVE_OVERTIME} and to the employee's assigned
 * supervisor, who is a regular Employee account; the service enforces both.
 */
@RestController
@RequestMapping("/api/v1/overtime-requests")
public class OvertimeController {

    private final OvertimeService overtimeService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    public OvertimeController(OvertimeService overtimeService,
                              PermissionService permissionService,
                              CurrentUserProvider currentUserProvider) {
        this.overtimeService = overtimeService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public void submitOvertimeRequest(@RequestBody @Valid OvertimeSubmitRequest request) {
        String cleanReason = sanitizeText(request.reason());
        overtimeService.submitOvertimeRequest(
                callerEmployeeId(),
                request.attendanceId(),
                request.overtimeTypeId(),
                request.overtimeDate(),
                request.requestedHours(),
                cleanReason
        );
    }

    @GetMapping("/me")
    public List<OvertimeRequest> getOwnOvertimeRequests() {
        return overtimeService.getOvertimeRequestsByEmployeeId(callerEmployeeId());
    }

    @GetMapping
    public List<OvertimeRequest> getAllOvertimeRequests() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_OVERTIME, PermissionKey.APPROVE_OVERTIME);
        return overtimeService.getAllOvertimeRequests();
    }

    @GetMapping("/eligible-attendance")
    public List<Attendance> getEligibleAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return overtimeService.getEligibleAttendanceForOvertime(
                callerEmployeeId(), asOfDate != null ? asOfDate : LocalDate.now());
    }

    @PostMapping("/{overtimeRequestId}/approve")
    public void approveOvertimeRequest(@PathVariable int overtimeRequestId) {
        validatePositiveId(overtimeRequestId, "Overtime Request ID");
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();
        boolean canApproveAny = permissionService.hasPermission(PermissionKey.APPROVE_OVERTIME);
        overtimeService.approveOvertimeRequest(
                principal.employeeId(), principal.userAccountId(), canApproveAny, overtimeRequestId, null);
    }

    @PostMapping("/{overtimeRequestId}/reject")
    public void rejectOvertimeRequest(@PathVariable int overtimeRequestId) {
        validatePositiveId(overtimeRequestId, "Overtime Request ID");
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();
        boolean canApproveAny = permissionService.hasPermission(PermissionKey.APPROVE_OVERTIME);
        overtimeService.rejectOvertimeRequest(
                principal.employeeId(), principal.userAccountId(), canApproveAny, overtimeRequestId, null);
    }

    @PostMapping("/{overtimeRequestId}/cancel")
    public void cancelOvertimeRequest(@PathVariable int overtimeRequestId) {
        validatePositiveId(overtimeRequestId, "Overtime Request ID");
        overtimeService.cancelOvertimeRequest(callerEmployeeId(), overtimeRequestId, "Cancelled by employee.");
    }

    private int callerEmployeeId() {
        return currentUserProvider.getCurrentPrincipal().employeeId();
    }

    private void validatePositiveId(int id, String name) {
        if (id <= 0) {
            throw new ValidationException(name + " must be a positive integer.");
        }
    }

    private String sanitizeText(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("<[^>]*>", "").trim();
    }
}
