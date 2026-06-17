package motorph.application.service;

import motorph.application.dto.employee.EmployeeSummaryDTO;
import motorph.application.dto.leave.LeaveCreditRowDTO;
import motorph.application.dto.leave.LeaveRequestRowDTO;
import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.UnauthorizedException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.Employee;
import motorph.domain.model.LeaveCredit;
import motorph.domain.model.LeaveRequest;
import motorph.domain.model.UserAccount;
import motorph.domain.value.EmploymentStatus;
import motorph.domain.value.RequestStatus;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.LeaveCreditRepository;
import motorph.infrastructure.repository.LeaveRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles business logic for leave requests and leave approval actions.
 */
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final LeaveCreditRepository leaveCreditRepository;
    private final EmployeeService employeeService;

    // constructors
    public LeaveService(LeaveRepository leaveRepository,
                        LeaveCreditRepository leaveCreditRepository,
                        EmployeeService employeeService) {

        if (leaveRepository == null) {
            throw new IllegalArgumentException("LeaveRepository is required.");
        }

        if (leaveCreditRepository == null) {
            throw new IllegalArgumentException("LeaveCreditRepository is required.");
        }

        if (employeeService == null) {
            throw new IllegalArgumentException("EmployeeService is required.");
        }

        this.leaveRepository = leaveRepository;
        this.leaveCreditRepository = leaveCreditRepository;
        this.employeeService = employeeService;
    }

    // returns all leave requests
    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRepository.findAll();
    }

    // returns one leave request by id
    public LeaveRequest getLeaveRequestById(int leaveRequestId) {
        validateLeaveRequestId(leaveRequestId);

        LeaveRequest request = leaveRepository.findById(leaveRequestId);

        if (request == null) {
            throw new EntityNotFoundException("Leave request not found.");
        }

        return request;
    }

    // returns the employee linked to a leave request user
    public Employee getEmployeeById(int employeeId) {
        validateEmployeeId(employeeId);
        return employeeService.getEmployeeById(employeeId);
    }

    // returns leave requests of one employee
    public List<LeaveRequest> getLeaveRequestsByEmployeeId(int employeeId) {
        validateEmployeeId(employeeId);
        return leaveRepository.findByEmployeeId(employeeId);
    }

    // returns leave requests by status
    public List<LeaveRequest> getLeaveRequestsByStatus(RequestStatus status) {
        if (status == null) {
            throw new ValidationException("Request status is required.");
        }

        return leaveRepository.findByStatus(status);
    }

    // returns pending leave requests
    public List<LeaveRequest> getPendingLeaveRequests() {
        return leaveRepository.findPending();
    }

    // returns leave requests within a date range
    public List<LeaveRequest> getLeaveRequestsByDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return leaveRepository.findByDateRange(startDate, endDate);
    }

    // returns remaining leave credits
    public double getRemainingLeaveCredits(int employeeId, int leaveTypeId, int year) {
        validateEmployeeId(employeeId);
        validateLeaveTypeId(leaveTypeId);
        validateYear(year);

        return leaveCreditRepository.getRemainingCredits(employeeId, leaveTypeId, year);
    }

    // computes leave hours based on selected dates and paid business hours
    public double computeLeaveHours(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        LocalTime businessStart = LocalTime.of(9, 0);
        LocalTime lunchStart = LocalTime.of(12, 0);
        LocalTime lunchEnd = LocalTime.of(13, 0);
        LocalTime businessEnd = LocalTime.of(18, 0);

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double totalHours = days * 8.0;

        if (startDate.equals(today)) {
            if (now.isAfter(businessEnd)) {
                totalHours -= 8.0;
            } else if (now.isAfter(businessStart)) {
                double elapsedToday = ChronoUnit.MINUTES.between(businessStart, now) / 60.0;

                if (now.isAfter(lunchStart)) {
                    elapsedToday -= 1.0;
                }

                if (now.isAfter(lunchStart) && now.isBefore(lunchEnd)) {
                    elapsedToday = 3.0;
                }

                totalHours -= elapsedToday;
            }
        }

        return Math.max(totalHours, 0.0);
    }

    // submits a new leave request
    public void submitLeaveRequest(int employeeId,
                                   int leaveTypeId,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   double leaveHours,
                                   String reason) {

        validateEmployeeId(employeeId);
        validateLeaveTypeId(leaveTypeId);
        validateDateRange(startDate, endDate);
        validateLeaveHours(leaveHours);
        validateReason(reason);

        Employee employee = employeeService.getEmployeeById(employeeId);
        validateLeaveEligibility(employee);

        if (startDate.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Leave start date cannot be in the past.");
        }

        if (leaveRepository.hasConflict(employeeId, startDate, endDate)) {
            throw new BusinessRuleException("Employee already has an approved leave request within the selected dates.");
        }

        int year = startDate.getYear();

        if (!leaveCreditRepository.hasEnoughCredits(employeeId, leaveTypeId, year, leaveHours)) {
            throw new BusinessRuleException("Employee does not have enough leave credits.");
        }

        LeaveRequest request = new LeaveRequest(
                null,
                employeeId,
                leaveTypeId,
                startDate,
                endDate,
                leaveHours,
                reason.trim(),
                LocalDateTime.now()
        );

        leaveRepository.save(request);
    }

    // approves a leave request
    public void approveLeaveRequest(UserAccount actorAccount, int leaveRequestId) {
        validateActorAccount(actorAccount);
        validateLeaveRequestId(leaveRequestId);

        LeaveRequest request = getLeaveRequestById(leaveRequestId);

        requireActiveHrOrAdmin(actorAccount);
        preventSelfApproval(actorAccount.getEmployeeId(), request);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessRuleException("Only pending leave requests can be approved.");
        }

        int year = request.getStartDate().getYear();

        if (!leaveCreditRepository.hasEnoughCredits(
                request.getEmployeeId(),
                request.getLeaveTypeId(),
                year,
                request.getLeaveHours())) {
            throw new BusinessRuleException("Employee does not have enough leave credits.");
        }

        leaveRepository.updateStatus(
                leaveRequestId,
                RequestStatus.APPROVED,
                actorAccount.getUserAccountId()
        );

        leaveCreditRepository.deductCredits(
                request.getEmployeeId(),
                request.getLeaveTypeId(),
                year,
                request.getLeaveHours()
        );
    }

    // rejects a leave request
    public void rejectLeaveRequest(UserAccount actorAccount, int leaveRequestId) {
        validateActorAccount(actorAccount);
        validateLeaveRequestId(leaveRequestId);

        LeaveRequest request = getLeaveRequestById(leaveRequestId);

        requireActiveHrOrAdmin(actorAccount);
        preventSelfApproval(actorAccount.getEmployeeId(), request);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessRuleException("Only pending leave requests can be rejected.");
        }

        leaveRepository.updateStatus(
                leaveRequestId,
                RequestStatus.REJECTED,
                actorAccount.getUserAccountId()
        );
    }

    // cancels a pending leave request by the employee owner
    public void cancelLeaveRequest(int actorEmployeeId, int leaveRequestId) {
        validateEmployeeId(actorEmployeeId);
        validateLeaveRequestId(leaveRequestId);

        LeaveRequest request = getLeaveRequestById(leaveRequestId);

        if (request.getEmployeeId() != actorEmployeeId) {
            throw new UnauthorizedException("You can only cancel your own leave request.");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessRuleException("Only pending leave requests can be cancelled.");
        }

        leaveRepository.updateStatus(leaveRequestId, RequestStatus.CANCELLED, null);
    }

    // reverses an approved leave request and restores credits
    public void reverseApprovedLeave(UserAccount actorAccount, int leaveRequestId, RequestStatus newStatus) {
        validateActorAccount(actorAccount);
        validateLeaveRequestId(leaveRequestId);

        if (newStatus != RequestStatus.REJECTED && newStatus != RequestStatus.CANCELLED) {
            throw new ValidationException("Approved leave can only be changed to rejected or cancelled.");
        }

        LeaveRequest request = getLeaveRequestById(leaveRequestId);

        requireActiveHrOrAdmin(actorAccount);
        preventSelfApproval(actorAccount.getEmployeeId(), request);

        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new BusinessRuleException("Only approved leave requests can be reversed.");
        }

        leaveCreditRepository.restoreCredits(
                request.getEmployeeId(),
                request.getLeaveTypeId(),
                request.getStartDate().getYear(),
                request.getLeaveHours()
        );

        leaveRepository.updateStatus(
                leaveRequestId,
                newStatus,
                actorAccount.getUserAccountId()
        );
    }

    // counts pending leave requests
    public int countPendingRequests() {
        return leaveRepository.findPending().size();
    }

    // counts approved leave requests that include today
    public int countApprovedLeavesToday() {
        LocalDate today = LocalDate.now();

        return (int) leaveRepository.findByStatus(RequestStatus.APPROVED)
                .stream()
                .filter(request -> !today.isBefore(request.getStartDate())
                        && !today.isAfter(request.getEndDate()))
                .count();
    }

    // returns all leave requests as display rows with employee names and leave type names resolved
    public List<LeaveRequestRowDTO> getLeaveRequestRows() {
        List<LeaveRequest> requests = leaveRepository.findAll();

        Map<Integer, String> nameMap = employeeService.getAllEmployeeSummaries().stream()
                .collect(Collectors.toMap(
                        EmployeeSummaryDTO::getEmployeeId,
                        EmployeeSummaryDTO::getFullName,
                        (a, b) -> a
                ));

        return requests.stream()
                .map(r -> new LeaveRequestRowDTO(
                        r.getRequestId(),
                        r.getEmployeeId(),
                        nameMap.getOrDefault(r.getEmployeeId(), "Employee #" + r.getEmployeeId()),
                        resolveLeaveTypeName(r.getLeaveTypeId()),
                        r.getStartDate(),
                        r.getEndDate(),
                        r.getLeaveHours(),
                        r.getReason(),
                        r.getStatus(),
                        r.getRequestDate(),
                        r.getApproverAccountId(),
                        r.getRemarks(),
                        r.getDateReviewed()
                ))
                .collect(Collectors.toList());
    }

    // returns leave credit rows for an employee in the current year
    public List<LeaveCreditRowDTO> getLeaveCreditsForEmployee(int employeeId) {
        validateEmployeeId(employeeId);
        int year = LocalDate.now().getYear();

        return leaveCreditRepository.findByEmployeeId(employeeId, year).stream()
                .map(c -> new LeaveCreditRowDTO(
                        c.getLeaveTypeId(),
                        resolveLeaveTypeName(c.getLeaveTypeId()),
                        c.getTotalCredits(),
                        c.getUsedCredits(),
                        c.getRemainingCredits(),
                        c.getYear()
                ))
                .collect(Collectors.toList());
    }

    // resolves a leave type ID to its display name
    public static String resolveLeaveTypeName(int leaveTypeId) {
        return switch (leaveTypeId) {
            case 1 -> "Vacation Leave";
            case 2 -> "Sick Leave";
            case 3 -> "Emergency Leave";
            case 4 -> "Maternity Leave";
            case 5 -> "Paternity Leave";
            case 6 -> "Solo Parent Leave";
            case 7 -> "Unpaid Leave";
            default -> "Leave Type " + leaveTypeId;
        };
    }

    // validates if employee is allowed to file leave
    private void validateLeaveEligibility(Employee employee) {
        if (employee == null) {
            throw new ValidationException("Employee is required.");
        }

        if (employee.getEmploymentStatus() == EmploymentStatus.PROBATIONARY) {
            throw new BusinessRuleException("Probationary employees cannot file leave.");
        }
    }

    // ensures only active HR or ADMIN role can approve or reject
    private void requireActiveHrOrAdmin(UserAccount actorAccount) {
        Role activeRole = actorAccount.getActiveRole();

        if (activeRole != Role.HR && activeRole != Role.ADMIN) {
            throw new UnauthorizedException("Access denied.");
        }
    }

    // prevents users from approving or rejecting their own request
    private void preventSelfApproval(int actorEmployeeId, LeaveRequest request) {
        if (request.getEmployeeId() == actorEmployeeId) {
            throw new UnauthorizedException("You cannot approve or reject your own leave request.");
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

    // validates leave request id input
    private void validateLeaveRequestId(int leaveRequestId) {
        if (leaveRequestId <= 0) {
            throw new ValidationException("Leave request ID is required.");
        }
    }

    // validates leave type id input
    private void validateLeaveTypeId(int leaveTypeId) {
        if (leaveTypeId <= 0) {
            throw new ValidationException("Leave type ID is required.");
        }
    }

    // validates date range input
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new ValidationException("Start date is required.");
        }

        if (endDate == null) {
            throw new ValidationException("End date is required.");
        }

        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
    }

    // validates leave hours input
    private void validateLeaveHours(double leaveHours) {
        if (leaveHours <= 0) {
            throw new ValidationException("Leave hours must be greater than zero.");
        }
    }

    // validates leave reason input
    private void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Reason is required.");
        }
    }

    // validates leave credit year input
    private void validateYear(int year) {
        if (year < 2000) {
            throw new ValidationException("Leave credit year is invalid.");
        }
    }
}