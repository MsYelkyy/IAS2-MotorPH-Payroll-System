package motorph.backend.service;

import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.exception.ValidationException;
import motorph.backend.domain.Attendance;
import motorph.backend.domain.Employee;
import motorph.backend.domain.OvertimeRequest;
import motorph.backend.domain.value.RequestStatus;
import motorph.backend.repository.OvertimeRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles overtime requests and approvals. Approval is checked per request: an Admin can approve
 * any request, and anyone else only requests from employees they directly supervise.
 */
@Service
public class OvertimeService {

    private static final int OVERTIME_WINDOW_DAYS = 30;

    private final OvertimeRepository overtimeRepository;
    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final UserAccessLogService userAccessLogService;

    public OvertimeService(OvertimeRepository overtimeRepository,
                           AttendanceService attendanceService,
                           EmployeeService employeeService,
                           UserAccessLogService userAccessLogService) {
        this.overtimeRepository = overtimeRepository;
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
        this.userAccessLogService = userAccessLogService;
    }

    public List<OvertimeRequest> getAllOvertimeRequests() {
        return overtimeRepository.findAll();
    }

    /**
     * Sums the employee's approved overtime hours in the date range, for payroll computation.
     */
    public double getApprovedOvertimeHours(int employeeId, LocalDate startDate, LocalDate endDate) {
        validateEmployeeId(employeeId);

        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required.");
        }

        return overtimeRepository.getApprovedHours(employeeId, startDate, endDate);
    }

    public OvertimeRequest getOvertimeRequestById(int overtimeRequestId) {
        validateOvertimeRequestId(overtimeRequestId);

        OvertimeRequest request = overtimeRepository.findById(overtimeRequestId);

        if (request == null) {
            throw new EntityNotFoundException("Overtime request not found.");
        }

        return request;
    }

    public List<OvertimeRequest> getOvertimeRequestsByEmployeeId(int employeeId) {
        validateEmployeeId(employeeId);
        return overtimeRepository.findByEmployeeId(employeeId);
    }

    /**
     * Returns recent completed attendance records that can still be used for a new overtime
     * request.
     */
    public List<Attendance> getEligibleAttendanceForOvertime(int employeeId, LocalDate asOfDate) {
        validateEmployeeId(employeeId);

        if (asOfDate == null) {
            throw new ValidationException("As-of date is required.");
        }

        LocalDate windowStart = asOfDate.minusDays(OVERTIME_WINDOW_DAYS);
        return overtimeRepository.findEligibleAttendanceForOvertime(employeeId, windowStart, asOfDate);
    }

    public void submitOvertimeRequest(int employeeId,
                                      int attendanceId,
                                      int overtimeTypeId,
                                      LocalDate overtimeDate,
                                      double requestedHours,
                                      String reason) {

        validateEmployeeId(employeeId);
        validateAttendanceId(attendanceId);
        validateOvertimeTypeId(overtimeTypeId);
        validateOvertimeDate(overtimeDate);
        validateOvertimeWindow(overtimeDate);
        validateRequestedHours(requestedHours);
        validateReason(reason);

        Attendance attendance = attendanceService.getAttendanceForDate(employeeId, overtimeDate);

        if (attendance.getAttendanceId() == null || attendance.getAttendanceId() != attendanceId) {
            throw new BusinessRuleException("Attendance record does not match the overtime request.");
        }

        if (!attendance.isComplete()) {
            throw new BusinessRuleException("Employee must have complete attendance before filing overtime.");
        }

        if (overtimeRepository.existsByAttendanceId(attendanceId)) {
            throw new BusinessRuleException("Attendance already has an active overtime request.");
        }

        OvertimeRequest request = new OvertimeRequest(
                null,
                employeeId,
                attendanceId,
                overtimeTypeId,
                overtimeDate,
                requestedHours,
                reason.trim(),
                LocalDateTime.now()
        );

        overtimeRepository.save(request);
    }

    public void approveOvertimeRequest(int actorEmployeeId, int actorUserAccountId, boolean actorIsAdmin,
                                       int overtimeRequestId, String remarks) {
        OvertimeRequest request = validateApprovalAction(actorEmployeeId, actorIsAdmin, overtimeRequestId);

        request.approve(actorUserAccountId, remarks);

        overtimeRepository.updateStatus(
                request.getRequestId(),
                request.getStatus(),
                request.getApproverAccountId(),
                request.getRemarks()
        );

        userAccessLogService.logEvent(actorUserAccountId, UserAccessLogService.ACTION_OVERTIME_APPROVED,
                "Approved overtime request ID " + overtimeRequestId + " of employee ID " + request.getEmployeeId()
                        + " on " + request.getOvertimeDate());
    }

    public void rejectOvertimeRequest(int actorEmployeeId, int actorUserAccountId, boolean actorIsAdmin,
                                      int overtimeRequestId, String remarks) {
        OvertimeRequest request = validateApprovalAction(actorEmployeeId, actorIsAdmin, overtimeRequestId);

        request.reject(actorUserAccountId, remarks);

        overtimeRepository.updateStatus(
                request.getRequestId(),
                request.getStatus(),
                request.getApproverAccountId(),
                request.getRemarks()
        );

        userAccessLogService.logEvent(actorUserAccountId, UserAccessLogService.ACTION_OVERTIME_REJECTED,
                "Rejected overtime request ID " + overtimeRequestId + " of employee ID " + request.getEmployeeId()
                        + " on " + request.getOvertimeDate());
    }

    /**
     * Cancels a pending overtime request. Only the employee who submitted it can cancel it.
     */
    public void cancelOvertimeRequest(int actorEmployeeId, int overtimeRequestId, String reason) {
        validateEmployeeId(actorEmployeeId);
        validateOvertimeRequestId(overtimeRequestId);

        OvertimeRequest request = getOvertimeRequestById(overtimeRequestId);

        if (request.getEmployeeId() != actorEmployeeId) {
            throw new UnauthorizedException("You can only cancel your own overtime request.");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessRuleException("Only pending overtime requests can be cancelled.");
        }

        request.cancel(reason);

        overtimeRepository.updateStatus(
                overtimeRequestId,
                request.getStatus(),
                request.getApproverAccountId(),
                request.getRemarks()
        );
    }

    private OvertimeRequest validateApprovalAction(int actorEmployeeId, boolean actorIsAdmin, int overtimeRequestId) {
        validateOvertimeRequestId(overtimeRequestId);

        OvertimeRequest request = getOvertimeRequestById(overtimeRequestId);

        requireActiveSupervisorOrAdmin(actorEmployeeId, actorIsAdmin, request);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessRuleException("Only pending overtime requests can be approved or rejected.");
        }

        return request;
    }

    /**
     * Allows the action only for an Admin or the employee's assigned supervisor. Supervisors have
     * no dedicated role, so this cannot be expressed as a permission key.
     */
    private void requireActiveSupervisorOrAdmin(int actorEmployeeId, boolean actorIsAdmin, OvertimeRequest request) {
        if (actorIsAdmin) {
            return;
        }

        Employee employee = employeeService.getEmployeeById(request.getEmployeeId());

        if (employee.getSupervisorId() == null || employee.getSupervisorId() != actorEmployeeId) {
            throw new UnauthorizedException("Only the assigned supervisor can approve or reject this overtime request.");
        }
    }

    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    private void validateAttendanceId(int attendanceId) {
        if (attendanceId <= 0) {
            throw new ValidationException("Attendance ID is required.");
        }
    }

    private void validateOvertimeRequestId(int overtimeRequestId) {
        if (overtimeRequestId <= 0) {
            throw new ValidationException("Overtime request ID is required.");
        }
    }

    private void validateOvertimeTypeId(int overtimeTypeId) {
        if (overtimeTypeId <= 0) {
            throw new ValidationException("Overtime type ID is required.");
        }
    }

    private void validateOvertimeDate(LocalDate overtimeDate) {
        if (overtimeDate == null) {
            throw new ValidationException("Overtime date is required.");
        }
    }

    /**
     * Ensures the overtime date falls within the allowed recent window.
     */
    private void validateOvertimeWindow(LocalDate overtimeDate) {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(OVERTIME_WINDOW_DAYS);

        if (overtimeDate.isBefore(windowStart) || overtimeDate.isAfter(today)) {
            throw new BusinessRuleException(
                    "Overtime can only be filed for attendance within the last "
                            + OVERTIME_WINDOW_DAYS + " days.");
        }
    }

    private void validateRequestedHours(double requestedHours) {
        if (requestedHours <= 0) {
            throw new ValidationException("Requested overtime hours must be greater than zero.");
        }
    }

    private void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Reason is required.");
        }
    }
}
