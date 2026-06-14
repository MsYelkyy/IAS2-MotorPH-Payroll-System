package motorph.application.service;

import motorph.application.dto.it.PasswordResetRequestDTO;
import motorph.application.dto.it.PasswordResetRequestRowDTO;
import motorph.domain.model.Employee;
import motorph.domain.model.PasswordResetRequest;
import motorph.domain.model.UserAccount;
import motorph.domain.value.PasswordResetStatus;
import motorph.infrastructure.repository.PasswordResetRequestRepository;
import motorph.infrastructure.repository.UserAccountRepository;

import java.util.List;
import java.util.stream.Collectors;

/** Builds IT password reset request display data and delegates reset actions. */
public class ITPasswordResetService {

    private final PasswordResetService passwordResetService;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeService employeeService;

    // constructors
    public ITPasswordResetService(PasswordResetService passwordResetService,
                                  PasswordResetRequestRepository passwordResetRequestRepository,
                                  UserAccountRepository userAccountRepository,
                                  EmployeeService employeeService) {
        if (passwordResetService == null) {
            throw new IllegalArgumentException("PasswordResetService is required.");
        }

        if (passwordResetRequestRepository == null) {
            throw new IllegalArgumentException("PasswordResetRequestRepository is required.");
        }

        if (userAccountRepository == null) {
            throw new IllegalArgumentException("UserAccountRepository is required.");
        }

        if (employeeService == null) {
            throw new IllegalArgumentException("EmployeeService is required.");
        }

        this.passwordResetService = passwordResetService;
        this.passwordResetRequestRepository = passwordResetRequestRepository;
        this.userAccountRepository = userAccountRepository;
        this.employeeService = employeeService;
    }

    // returns all password reset requests as IT table rows
    public List<PasswordResetRequestRowDTO> getPasswordResetRequestRows() {
        return passwordResetRequestRepository.findAll()
                .stream()
                .map(this::toRowDTO)
                .collect(Collectors.toList());
    }

    // returns password reset request count summary
    public PasswordResetRequestDTO getCounts() {
        int pending = passwordResetRequestRepository.findByStatus(PasswordResetStatus.PENDING).size();
        int completed = passwordResetRequestRepository.findByStatus(PasswordResetStatus.COMPLETED).size();
        int cancelled = passwordResetRequestRepository.findByStatus(PasswordResetStatus.CANCELLED).size();
        int total = passwordResetRequestRepository.findAll().size();

        return new PasswordResetRequestDTO(pending, completed, cancelled, total);
    }

    // processes a pending password reset request
    public void processPasswordResetRequest(int resetRequestId, int resolverEmployeeId) {
        passwordResetService.processPasswordResetRequest(resetRequestId, resolverEmployeeId);
    }

    // cancels a pending password reset request
    public void cancelPasswordResetRequest(int resetRequestId, int cancellerEmployeeId) {
        passwordResetService.cancelPasswordResetRequest(resetRequestId, cancellerEmployeeId);
    }

    // converts password reset request to IT row DTO
    private PasswordResetRequestRowDTO toRowDTO(PasswordResetRequest request) {
        String employeeName = "Employee";
        String companyEmail = "-";

        UserAccount account = userAccountRepository.findById(request.getUserAccountId());

        if (account != null) {
            companyEmail = account.getCompanyEmail();

            try {
                Employee employee = employeeService.getEmployeeById(account.getEmployeeId());
                employeeName = employee.getFullName();
            } catch (Exception ignored) {
                employeeName = "Employee #" + account.getEmployeeId();
            }
        }

        return new PasswordResetRequestRowDTO(
                request.getResetRequestId(),
                request.getUserAccountId(),
                employeeName,
                companyEmail,
                request.getRequestStatus(),
                request.getRequestedAt(),
                request.getResolvedByEmployeeId() == null ? "-" : "Employee #" + request.getResolvedByEmployeeId(),
                request.getCompletedAt(),
                request.getCancelledAt()
        );
    }
}