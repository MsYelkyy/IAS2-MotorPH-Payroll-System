package motorph.application.service;

import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.UnauthorizedException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.Attendance;
import motorph.domain.model.Employee;
import motorph.domain.model.OvertimeRequest;
import motorph.domain.model.UserAccount;
import motorph.domain.value.RequestStatus;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.OvertimeRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles business logic for overtime requests and approval actions.
 */
public class OvertimeService {

    private static final int OVERTIME_WINDOW_DAYS = 30;

    private final OvertimeRepository overtimeRepository;
    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;

    // constructors
    public OvertimeService(OvertimeRepository overtimeRepository,
                           AttendanceService attendanceService,
                           EmployeeService employeeService) {

        if (overtimeRepository == null) {
            throw new IllegalArgumentException("OvertimeRepository is required.");
        }

        if (attendanceService == null) {
            throw new IllegalArgumentException("AttendanceService is required.");
        }

        if (employeeService == null) {
            throw new IllegalArgumentException("EmployeeService is required.");
        }

        this.overtimeRepository = overtimeRepository;
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
    }

    // returns all overtime requests
    public List<OvertimeRequest> getAllOvertimeRequests() {
        return overtimeRepository.findAll();
    }

    // returns one overtime request by id
    public OvertimeRequest getOvertimeRequestById(int overtimeRequestId) {
        validateOvertimeRequestId(overtimeRequestId);

        OvertimeRequest request = overtimeRepository.findById(overtimeRequestId);

        if (request == null) {
            throw new EntityNotFoundException("Overtime request not found.");
        }

        return request;
    }

    // returns overtime requests of one employee
    public List<OvertimeRequest> getOvertimeRequestsByEmployeeId(int employeeId) {
        validateEmployeeId(employeeId);
        return overtimeRepository.findByEmployeeId(employeeId);
    }

    // returns overtime requests by status
    public List<OvertimeRequest> getOvertimeRequestsByStatus(RequestStatus status) {
        validateStatus(status);
        return overtimeRepository.findByStatus(status);
    }

    // returns pending overtime requests
    public List<OvertimeRequest> getPendingOvertimeRequests() {
        return overtimeRepository.findPending();
    }

    // returns recent completed attendance eligible for a new overtime request
    public List<Attendance> getEligibleAttendanceForOvertime(int employeeId, LocalDate asOfDate) {
        validateEmployeeId(employeeId);

        if (asOfDate == null) {
            throw new ValidationException("As-of date is required.");
        }

        LocalDate windowStart = asOfDate.minusDays(OVERTIME_WINDOW_DAYS);
        return overtimeRepository.findEligibleAttendanceForOvertime(employeeId, windowStart, asOfDate);
    }

    // submits a new overtime request
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

    // approves an overtime request
    public void approveOvertimeRequest(UserAccount actorAccount, int overtimeRequestId, String remarks) {
        OvertimeRequest request = validateApprovalAction(actorAccount, overtimeRequestId);

        overtimeRepository.updateStatus(
                request.getRequestId(),
                RequestStatus.APPROVED,
                actorAccount.getUserAccountId(),
                remarks
        );
    }

    // rejects an overtime request
    public void rejectOvertimeRequest(UserAccount actorAccount, int overtimeRequestId, String remarks) {
        OvertimeRequest request = validateApprovalAction(actorAccount, overtimeRequestId);

        overtimeRepository.updateStatus(
                request.getRequestId(),
                RequestStatus.REJECTED,
                actorAccount.getUserAccountId(),
                remarks
        );
    }

    // cancels a pending overtime request by the employee owner
    public void cancelOvertimeRequest(int actorEmployeeId, int overtimeRequestId) {
        validateEmployeeId(actorEmployeeId);
        validateOvertimeRequestId(overtimeRequestId);

        OvertimeRequest request = getOvertimeRequestById(overtimeRequestId);

        if (request.getEmployeeId() != actorEmployeeId) {
            throw new UnauthorizedException("You can only cancel your own overtime request.");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessRuleException("Only pending overtime requests can be cancelled.");
        }

        overtimeRepository.updateStatus(
                overtimeRequestId,
                RequestStatus.CANCELLED,
                null,
                null
        );
    }

    // returns approved overtime hours for payroll
    public double getApprovedOvertimeHours(int employeeId, LocalDate startDate, LocalDate endDate) {
        validateEmployeeId(employeeId);
        validateDateRange(startDate, endDate);

        return overtimeRepository.getApprovedHours(employeeId, startDate, endDate);
    }

    // validates approval action
    private OvertimeRequest validateApprovalAction(UserAccount actorAccount, int overtimeRequestId) {
        validateActorAccount(actorAccount);
        validateOvertimeRequestId(overtimeRequestId);

        OvertimeRequest request = getOvertimeRequestById(overtimeRequestId);

        requireActiveSupervisorOrAdmin(actorAccount, request);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessRuleException("Only pending overtime requests can be approved or rejected.");
        }

        return request;
    }

    // ensures only active supervisor or admin role can approve or reject
    private void requireActiveSupervisorOrAdmin(UserAccount actorAccount, OvertimeRequest request) {
        if (actorAccount.getActiveRole() == Role.ADMIN) {
            return;
        }

        Employee employee = employeeService.getEmployeeById(request.getEmployeeId());

        if (employee.getSupervisorId() == null || employee.getSupervisorId() != actorAccount.getEmployeeId()) {
            throw new UnauthorizedException("Only the assigned supervisor can approve or reject this overtime request.");
        }
    }

    // validates actor account input
    private void validateActorAccount(UserAccount actorAccount) {
        if (actorAccount == null) {
            throw new UnauthorizedException("User account is required.");
        }

        if (actorAccount.getUserAccountId() == null || actorAccount.getUserAccountId() <= 0) {
            throw new UnauthorizedException("Actor user account ID is invalid.");
        }

        if (actorAccount.getEmployeeId() <= 0) {
            throw new UnauthorizedException("Actor employee ID is invalid.");
        }

        if (actorAccount.getRoles() == null || actorAccount.getRoles().isEmpty()) {
            throw new UnauthorizedException("Actor must have at least one assigned role.");
        }

        if (actorAccount.getActiveRole() == null) {
            throw new UnauthorizedException("Actor active role is required.");
        }

        if (!actorAccount.hasRole(actorAccount.getActiveRole())) {
            throw new UnauthorizedException("Actor active role is not assigned to the account.");
        }
    }

    // validates employee id input
    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    // validates attendance id input
    private void validateAttendanceId(int attendanceId) {
        if (attendanceId <= 0) {
            throw new ValidationException("Attendance ID is required.");
        }
    }

    // validates overtime request id input
    private void validateOvertimeRequestId(int overtimeRequestId) {
        if (overtimeRequestId <= 0) {
            throw new ValidationException("Overtime request ID is required.");
        }
    }

    // validates overtime type id input
    private void validateOvertimeTypeId(int overtimeTypeId) {
        if (overtimeTypeId <= 0) {
            throw new ValidationException("Overtime type ID is required.");
        }
    }

    // validates request status input
    private void validateStatus(RequestStatus status) {
        if (status == null) {
            throw new ValidationException("Request status is required.");
        }
    }

    // validates overtime date input
    private void validateOvertimeDate(LocalDate overtimeDate) {
        if (overtimeDate == null) {
            throw new ValidationException("Overtime date is required.");
        }
    }

    // ensures the overtime date falls within the recent allowed window
    private void validateOvertimeWindow(LocalDate overtimeDate) {
        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(OVERTIME_WINDOW_DAYS);

        if (overtimeDate.isBefore(windowStart) || overtimeDate.isAfter(today)) {
            throw new BusinessRuleException(
                    "Overtime can only be filed for attendance within the last "
                            + OVERTIME_WINDOW_DAYS + " days.");
        }
    }

    // validates date range input
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required.");
        }

        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
    }

    // validates requested overtime hours input
    private void validateRequestedHours(double requestedHours) {
        if (requestedHours <= 0) {
            throw new ValidationException("Requested overtime hours must be greater than zero.");
        }
    }

    // validates overtime reason input
    private void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Reason is required.");
        }
    }
}