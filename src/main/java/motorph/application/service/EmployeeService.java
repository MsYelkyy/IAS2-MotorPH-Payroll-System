package motorph.application.service;

import motorph.application.dto.employee.EmployeeProfileDTO;
import motorph.application.dto.employee.EmployeeSummaryDTO;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.Employee;
import motorph.domain.model.UserAccount;
import motorph.infrastructure.repository.EmployeeRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles business logic for employee management.
 */
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserAccountService userAccountService;

    // constructors
    public EmployeeService(EmployeeRepository employeeRepository,
                           UserAccountService userAccountService) {
        if (employeeRepository == null) {
            throw new IllegalArgumentException("EmployeeRepository is required.");
        }

        if (userAccountService == null) {
            throw new IllegalArgumentException("UserAccountService is required.");
        }

        this.employeeRepository = employeeRepository;
        this.userAccountService = userAccountService;
    }

    // returns an active employee by id
    public Employee getEmployeeById(int employeeId) {
        validateEmployeeId(employeeId);

        Employee employee = employeeRepository.findActiveById(employeeId);

        if (employee == null) {
            throw new EntityNotFoundException("Employee not found.");
        }

        return employee;
    }

    // returns all active employees
    public List<Employee> getAllActiveEmployees() {
        return employeeRepository.findAllActive();
    }

    // returns all employees including inactive
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAllWithInactive();
    }

    // returns all employees as summary DTOs
    public List<EmployeeSummaryDTO> getAllEmployeeSummaries() {
        return employeeRepository.findAllWithInactive()
                .stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    // returns employee profile DTO
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

    // creates a new employee only
    public void createEmployee(Employee employee) {
        validateEmployee(employee);
        employeeRepository.save(employee);
    }

    // creates a new employee with an automatic user account
    public UserAccount createEmployeeWithTemporaryAccount(Employee employee) {
        validateEmployee(employee);

        int employeeId = employeeRepository.saveAndReturnId(employee);

        return userAccountService.createTemporaryEmployeeAccount(
                employeeId,
                employee.getFirstName(),
                employee.getLastName(),
                employee.getPersonalEmail()
        );
    }

    // updates an existing employee
    public void updateEmployee(Employee employee) {
        if (employee == null || employee.getEmployeeId() == null) {
            throw new ValidationException("Employee ID is required for update.");
        }

        validateEmployee(employee);
        employeeRepository.update(employee);
    }

    // deactivates an employee and disables the linked user account
    public void deactivateEmployee(int employeeId) {
        validateEmployeeId(employeeId);

        employeeRepository.deactivate(employeeId);
        userAccountService.deactivateAccountByEmployeeId(employeeId);
    }

    // updates the employee's own contact information
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

    // converts employee to summary DTO
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

    // validates employee fields
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

    // validates employee id input
    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }
}