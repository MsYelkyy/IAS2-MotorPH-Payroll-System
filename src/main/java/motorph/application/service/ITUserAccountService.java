package motorph.application.service;

import motorph.application.dto.user.UserAccountRowDTO;
import motorph.domain.model.Employee;
import motorph.domain.model.UserAccount;
import motorph.infrastructure.repository.UserAccountRepository;

import java.util.List;
import java.util.stream.Collectors;

/** Builds IT user account display data from account and employee records. */
public class ITUserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final EmployeeService employeeService;

    // constructors
    public ITUserAccountService(UserAccountRepository userAccountRepository,
                                EmployeeService employeeService) {
        if (userAccountRepository == null) {
            throw new IllegalArgumentException("UserAccountRepository is required.");
        }

        if (employeeService == null) {
            throw new IllegalArgumentException("EmployeeService is required.");
        }

        this.userAccountRepository = userAccountRepository;
        this.employeeService = employeeService;
    }

    // returns all user accounts as table rows for IT account management
    public List<UserAccountRowDTO> getUserAccountRows() {
        return userAccountRepository.findAll()
                .stream()
                .map(this::toUserAccountRowDTO)
                .collect(Collectors.toList());
    }

    // converts user account data to an IT table row
    private UserAccountRowDTO toUserAccountRowDTO(UserAccount account) {
        String fullName = "Employee #" + account.getEmployeeId();

        try {
            Employee employee = employeeService.getEmployeeById(account.getEmployeeId());
            fullName = employee.getFullName();
        } catch (Exception ignored) {
        }

        return new UserAccountRowDTO(
                account.getUserAccountId(),
                account.getEmployeeId(),
                fullName,
                account.getCompanyEmail(),
                account.getRoles(),
                account.getActiveRole(),
                account.getAccountStatus(),
                account.isLocked(),
                account.isActive(),
                account.hasMultipleRoles(),
                account.getLastLoginAt()
        );
    }
}