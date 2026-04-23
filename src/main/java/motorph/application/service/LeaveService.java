package motorph.application.service;

import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.UnauthorizedException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.Employee;
import motorph.domain.model.LeaveRequest;
import motorph.domain.model.UserAccount;
import motorph.domain.value.EmploymentStatus;
import motorph.domain.value.LeaveStatus;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.LeaveRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles business logic for leave requests and leave approval actions.
 */
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeService employeeService;

    // constructors
    public LeaveService(LeaveRepository leaveRepository, EmployeeService employeeService) {
        if (leaveRepository == null) {
            throw new IllegalArgumentException("LeaveRepository is required.");
        }

        if (employeeService == null) {
            throw new IllegalArgumentException("EmployeeService is required.");
        }

        this.leaveRepository = leaveRepository;
        this.employeeService = employeeService;
    }

    // Returns all leave requests.
    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRepository.findAll();
    }

    // Returns one leave request by id.
    public LeaveRequest getLeaveRequestById(int leaveRequestId) {
        validateLeaveRequestId(leaveRequestId);

        LeaveRequest request = leaveRepository.findById(leaveRequestId);

        if (request == null) {
            throw new EntityNotFoundException("Leave request not found.");
        }

        return request;
    }

    // Returns leave requests of one employee.
    public List<LeaveRequest> getLeaveRequestsByEmployeeId(int employeeId) {
        validateEmployeeId(employeeId);
        return leaveRepository.findByEmployeeId(employeeId);
    }

    // Returns leave requests by status.
    public List<LeaveRequest> getLeaveRequestsByStatus(LeaveStatus status) {
        if (status == null) {
            throw new ValidationException("Leave status is required.");
        }

        return leaveRepository.findByStatus(status);
    }

    // Returns pending leave requests.
    public List<LeaveRequest> getPendingLeaveRequests() {
        return leaveRepository.findPending();
    }

    // Returns leave requests within a date range.
    public List<LeaveRequest> getLeaveRequestsByDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return leaveRepository.findByDateRange(startDate, endDate);
    }

    // Submits a new leave request.
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

        LeaveRequest request = new LeaveRequest(
                null,
                employeeId,
                LocalDateTime.now(),
                startDate,
                endDate,
                leaveHours,
                reason.trim(),
                leaveTypeId
        );

        leaveRepository.save(request);
    }

    // Approves a leave request.
    public void approveLeaveRequest(UserAccount actorAccount, int leaveRequestId) {
        validateActorAccount(actorAccount);
        validateLeaveRequestId(leaveRequestId);

        LeaveRequest request = getLeaveRequestById(leaveRequestId);

        requireHrOrAdmin(actorAccount);
        preventSelfApproval(actorAccount.getEmployeeId(), request);

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessRuleException("Only pending leave requests can be approved.");
        }

        leaveRepository.updateStatus(leaveRequestId, LeaveStatus.APPROVED);
    }

    // Rejects a leave request.
    public void rejectLeaveRequest(UserAccount actorAccount, int leaveRequestId) {
        validateActorAccount(actorAccount);
        validateLeaveRequestId(leaveRequestId);

        LeaveRequest request = getLeaveRequestById(leaveRequestId);

        requireHrOrAdmin(actorAccount);
        preventSelfApproval(actorAccount.getEmployeeId(), request);

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessRuleException("Only pending leave requests can be rejected.");
        }

        leaveRepository.updateStatus(leaveRequestId, LeaveStatus.REJECTED);
    }

    // Cancels a leave request.
    public void cancelLeaveRequest(int actorEmployeeId, int leaveRequestId) {
        validateEmployeeId(actorEmployeeId);
        validateLeaveRequestId(leaveRequestId);

        LeaveRequest request = getLeaveRequestById(leaveRequestId);

        if (request.getEmployeeId() != actorEmployeeId) {
            throw new UnauthorizedException("You can only cancel your own leave request.");
        }

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessRuleException("Only pending leave requests can be cancelled.");
        }

        leaveRepository.cancel(leaveRequestId);
    }

    // Counts pending leave requests.
    public int countPendingRequests() {
        return leaveRepository.findPending().size();
    }

    // Counts approved leave requests that include today.
    public int countApprovedLeavesToday() {
        LocalDate today = LocalDate.now();

        return (int) leaveRepository.findByStatus(LeaveStatus.APPROVED)
                .stream()
                .filter(request ->
                        !today.isBefore(request.getStartDate()) &&
                                !today.isAfter(request.getEndDate()))
                .count();
    }

    // Validates if employee is allowed to file leave.
    private void validateLeaveEligibility(Employee employee) {
        if (employee == null) {
            throw new ValidationException("Employee is required.");
        }

        if (employee.getEmploymentStatus() == EmploymentStatus.PROBATIONARY) {
            throw new BusinessRuleException("Probationary employees cannot file leave.");
        }
    }

    // Ensures only HR or ADMIN can approve or reject.
    private void requireHrOrAdmin(UserAccount actorAccount) {
        Role role = actorAccount.getRole();

        if (role != Role.HR && role != Role.ADMIN) {
            throw new UnauthorizedException("Access denied.");
        }
    }

    // Prevents users from approving or rejecting their own request.
    private void preventSelfApproval(int actorEmployeeId, LeaveRequest request) {
        if (request.getEmployeeId() == actorEmployeeId) {
            throw new UnauthorizedException("You cannot approve or reject your own leave request.");
        }
    }

    // Validates actor account input.
    private void validateActorAccount(UserAccount actorAccount) {
        if (actorAccount == null) {
            throw new UnauthorizedException("User account is required.");
        }

        if (actorAccount.getEmployeeId() <= 0) {
            throw new UnauthorizedException("Actor employee ID is invalid.");
        }

        if (actorAccount.getRole() == null) {
            throw new UnauthorizedException("Actor role is required.");
        }
    }

    // Validates employee id input.
    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    // Validates leave request id input.
    private void validateLeaveRequestId(int leaveRequestId) {
        if (leaveRequestId <= 0) {
            throw new ValidationException("Leave request ID is required.");
        }
    }

    // Validates leave type id input.
    private void validateLeaveTypeId(int leaveTypeId) {
        if (leaveTypeId <= 0) {
            throw new ValidationException("Leave type ID is required.");
        }
    }

    // Validates date range input.
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

    // Validates leave hours input.
    private void validateLeaveHours(double leaveHours) {
        if (leaveHours <= 0) {
            throw new ValidationException("Leave hours must be greater than zero.");
        }
    }

    // Validates leave reason input.
    private void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Reason is required.");
        }
    }
}