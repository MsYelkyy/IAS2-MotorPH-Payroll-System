package motorph.backend.service;

import motorph.backend.dto.employee.EmployeeSummaryDTO;
import motorph.backend.dto.leave.LeaveCreditRowDTO;
import motorph.backend.dto.leave.LeaveRequestRowDTO;
import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.exception.ValidationException;
import motorph.backend.domain.Employee;
import motorph.backend.domain.LeaveRequest;
import motorph.backend.domain.value.EmploymentStatus;
import motorph.backend.domain.value.RequestStatus;
import motorph.backend.repository.LeaveCreditRepository;
import motorph.backend.repository.LeaveRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles leave requests and approvals. Permission checks are done in the controller; this service
 * enforces the business rules, including preventing employees from approving their own requests.
 */
@Service
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final LeaveCreditRepository leaveCreditRepository;
    private final EmployeeService employeeService;

    public LeaveService(LeaveRepository leaveRepository,
                        LeaveCreditRepository leaveCreditRepository,
                        EmployeeService employeeService) {
        this.leaveRepository = leaveRepository;
        this.leaveCreditRepository = leaveCreditRepository;
        this.employeeService = employeeService;
    }

    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRepository.findAll();
    }

    public LeaveRequest getLeaveRequestById(int leaveRequestId) {
        validateLeaveRequestId(leaveRequestId);

        LeaveRequest request = leaveRepository.findById(leaveRequestId);

        if (request == null) {
            throw new EntityNotFoundException("Leave request not found.");
        }

        return request;
    }

    public List<LeaveRequest> getLeaveRequestsByEmployeeId(int employeeId) {
        validateEmployeeId(employeeId);
        return leaveRepository.findByEmployeeId(employeeId);
    }

    /**
     * Computes leave hours for a date range based on paid business hours.
     */
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

    public void approveLeaveRequest(int actorEmployeeId, int actorUserAccountId, int leaveRequestId) {
        validateLeaveRequestId(leaveRequestId);

        LeaveRequest request = getLeaveRequestById(leaveRequestId);
        preventSelfApproval(actorEmployeeId, request);

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

        request.approve(actorUserAccountId, null);

        leaveRepository.updateStatus(
                leaveRequestId,
                request.getStatus(),
                request.getApproverAccountId()
        );

        leaveCreditRepository.deductCredits(
                request.getEmployeeId(),
                request.getLeaveTypeId(),
                year,
                request.getLeaveHours()
        );
    }

    public void rejectLeaveRequest(int actorEmployeeId, int actorUserAccountId, int leaveRequestId) {
        validateLeaveRequestId(leaveRequestId);

        LeaveRequest request = getLeaveRequestById(leaveRequestId);
        preventSelfApproval(actorEmployeeId, request);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessRuleException("Only pending leave requests can be rejected.");
        }

        request.reject(actorUserAccountId, null);

        leaveRepository.updateStatus(
                leaveRequestId,
                request.getStatus(),
                request.getApproverAccountId()
        );
    }

    /**
     * Cancels a pending leave request. Only the employee who submitted it can cancel it.
     */
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

        request.cancel();

        leaveRepository.updateStatus(leaveRequestId, request.getStatus(), null);
    }

    public int countPendingRequests() {
        return leaveRepository.findPending().size();
    }

    public int countApprovedLeavesToday() {
        LocalDate today = LocalDate.now();

        return (int) leaveRepository.findByStatus(RequestStatus.APPROVED)
                .stream()
                .filter(request -> !today.isBefore(request.getStartDate())
                        && !today.isAfter(request.getEndDate()))
                .count();
    }

    /**
     * Returns all leave requests as display rows, with employee and leave type names resolved.
     */
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

    /**
     * Returns the employee's leave credits for the current year.
     */
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

    private void validateLeaveEligibility(Employee employee) {
        if (employee.getEmploymentStatus() == EmploymentStatus.PROBATIONARY) {
            throw new BusinessRuleException("Probationary employees cannot file leave.");
        }
    }

    private void preventSelfApproval(int actorEmployeeId, LeaveRequest request) {
        if (request.getEmployeeId() == actorEmployeeId) {
            throw new UnauthorizedException("You cannot approve or reject your own leave request.");
        }
    }

    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    private void validateLeaveRequestId(int leaveRequestId) {
        if (leaveRequestId <= 0) {
            throw new ValidationException("Leave request ID is required.");
        }
    }

    private void validateLeaveTypeId(int leaveTypeId) {
        if (leaveTypeId <= 0) {
            throw new ValidationException("Leave type ID is required.");
        }
    }

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

    private void validateLeaveHours(double leaveHours) {
        if (leaveHours <= 0) {
            throw new ValidationException("Leave hours must be greater than zero.");
        }
    }

    private void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Reason is required.");
        }
    }
}
