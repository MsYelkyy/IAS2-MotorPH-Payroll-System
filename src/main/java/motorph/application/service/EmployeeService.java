package motorph.application.service;

import motorph.application.dto.employee.EmployeeProfileDTO;
import motorph.application.dto.employee.EmployeeSummaryDTO;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.application.security.PasswordHasher;
import motorph.domain.model.Employee;
import motorph.domain.model.UserAccount;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.UserAccountRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles business logic for employee management and linked account creation.
 */
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;

    // constructors
    public EmployeeService(EmployeeRepository employeeRepository,
                           UserAccountRepository userAccountRepository) {
        if (employeeRepository == null) {
            throw new IllegalArgumentException("EmployeeRepository is required.");
        }

        if (userAccountRepository == null) {
            throw new IllegalArgumentException("UserAccountRepository is required.");
        }

        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
    }

    // Returns an active employee by id.
    public Employee getEmployeeById(int employeeId) {
        validateEmployeeId(employeeId);

        Employee employee = employeeRepository.findActiveById(employeeId);

        if (employee == null) {
            throw new EntityNotFoundException("Employee not found.");
        }

        return employee;
    }

    // Returns all active employees.
    public List<Employee> getAllActiveEmployees() {
        return employeeRepository.findAllActive();
    }

    // Returns all employees including inactive.
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAllWithInactive();
    }

    // Returns all employees as summary DTOs.
    public List<EmployeeSummaryDTO> getAllEmployeeSummaries() {
        return employeeRepository.findAllWithInactive()
                .stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    // Returns employee profile DTO.
    public EmployeeProfileDTO getEmployeeProfile(int employeeId) {
        Employee employee = getEmployeeById(employeeId);

        return new EmployeeProfileDTO(
                employee.getEmployeeId(),
                employee.getFullName(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getBirthDate(),
                employee.getAddress(),
                employee.getCivilStatus(),
                employee.getPhoneNumber(),
                employee.getPersonalEmail(),
                employee.getEmploymentStatus(),
                employee.getPositionId(),
                employee.getDepartmentId(),
                employee.getHireDate(),
                employee.getSupervisorId(),
                employee.isActive(),
                employee.getPhotoPath()
        );
    }

    // Creates a new employee only.
    public void createEmployee(Employee employee) {
        validateEmployee(employee);
        employeeRepository.save(employee);
    }

    // Creates a new employee and linked user account.
    public UserAccount createEmployeeWithAccount(Employee employee, Role role, String rawPassword) {
        validateEmployee(employee);
        validateRole(role);
        validatePassword(rawPassword);

        int employeeId = employeeRepository.saveAndReturnId(employee);
        String companyEmail = generateUniqueCompanyEmail(employee.getFirstName(), employee.getLastName());
        String username = companyEmail;
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(rawPassword, salt);

        UserAccount userAccount = new UserAccount(
                null,
                employeeId,
                companyEmail,
                username,
                hash,
                salt,
                role,
                false,
                true,
                null
        );

        userAccountRepository.save(userAccount);

        UserAccount savedAccount = userAccountRepository.findByCompanyEmail(companyEmail);

        if (savedAccount == null) {
            throw new IllegalStateException("User account was not created successfully.");
        }

        return savedAccount;
    }

    // Updates an existing employee.
    public void updateEmployee(Employee employee) {
        if (employee == null || employee.getEmployeeId() == null) {
            throw new ValidationException("Employee ID is required for update.");
        }

        validateEmployee(employee);
        employeeRepository.update(employee);
    }

    // Deactivates an employee instead of deleting the record.
    public void deactivateEmployee(int employeeId) {
        validateEmployeeId(employeeId);
        employeeRepository.deactivate(employeeId);
    }

    // Updates the employee's own contact information.
    public void updateOwnContactInfo(int employeeId, String phoneNumber, String address) {
        Employee employee = getEmployeeById(employeeId);

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new ValidationException("Phone number is required.");
        }

        if (address == null || address.isBlank()) {
            throw new ValidationException("Address is required.");
        }

        employee.setPhoneNumber(phoneNumber);
        employee.setAddress(address);

        employeeRepository.update(employee);
    }

    // Generates a unique company email for a new account.
    public String generateUniqueCompanyEmail(String firstName, String lastName) {
        String baseEmail = buildBaseCompanyEmail(firstName, lastName);
        String candidate = baseEmail;
        int suffix = 2;

        while (userAccountRepository.existsByCompanyEmail(candidate)) {
            candidate = appendEmailSuffix(baseEmail, suffix);
            suffix++;
        }

        return candidate;
    }

    // Converts employee to summary DTO.
    private EmployeeSummaryDTO toSummaryDTO(Employee employee) {
        return new EmployeeSummaryDTO(
                employee.getEmployeeId(),
                employee.getFullName(),
                employee.getEmploymentStatus(),
                employee.getPositionId(),
                employee.getDepartmentId(),
                employee.isActive()
        );
    }

    // Validates employee fields.
    private void validateEmployee(Employee employee) {
        if (employee == null) {
            throw new ValidationException("Employee cannot be null.");
        }

        if (employee.getFirstName() == null || employee.getFirstName().isBlank()) {
            throw new ValidationException("First name is required.");
        }

        if (employee.getLastName() == null || employee.getLastName().isBlank()) {
            throw new ValidationException("Last name is required.");
        }

        if (employee.getEmploymentStatus() == null) {
            throw new ValidationException("Employment status is required.");
        }

        if (employee.getPositionId() <= 0) {
            throw new ValidationException("Position is required.");
        }

        if (employee.getDepartmentId() <= 0) {
            throw new ValidationException("Department is required.");
        }

        if (employee.getHireDate() == null) {
            throw new ValidationException("Hire date is required.");
        }
    }

    // Validates employee id input.
    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    // Validates role input.
    private void validateRole(Role role) {
        if (role == null) {
            throw new ValidationException("Role is required.");
        }
    }

    // Validates raw password input.
    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ValidationException("Password is required.");
        }
    }

    // Builds the base company email format.
    private String buildBaseCompanyEmail(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank()) {
            throw new ValidationException("First name is required.");
        }

        if (lastName == null || lastName.isBlank()) {
            throw new ValidationException("Last name is required.");
        }

        String cleanFirstName = sanitizeEmailName(firstName);
        String cleanLastName = sanitizeEmailName(lastName);

        return cleanFirstName + "." + cleanLastName + "@motorph.com";
    }

    // Appends a numeric suffix to a duplicate company email.
    private String appendEmailSuffix(String baseEmail, int suffix) {
        int atIndex = baseEmail.indexOf("@");

        if (atIndex < 0) {
            throw new IllegalStateException("Base company email is invalid.");
        }

        String localPart = baseEmail.substring(0, atIndex);
        String domainPart = baseEmail.substring(atIndex);

        return localPart + suffix + domainPart;
    }

    // Sanitizes a name for email generation.
    private String sanitizeEmailName(String value) {
        return value.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }
}