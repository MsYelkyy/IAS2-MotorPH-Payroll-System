package motorph.application.service;

import motorph.application.dto.employee.EmployeeProfileDTO;
import motorph.application.dto.employee.EmployeeSummaryDTO;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.Department;
import motorph.domain.model.Employee;
import motorph.domain.model.EmployeeBenefit;
import motorph.domain.model.GovernmentDetails;
import motorph.domain.model.Position;
import motorph.domain.model.SalaryHistory;
import motorph.domain.model.UserAccount;
import motorph.domain.value.SalaryType;
import motorph.infrastructure.repository.DepartmentRepository;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.GovernmentDetailsRepository;
import motorph.infrastructure.repository.PositionRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles business logic for employee management.
 */
public class EmployeeService {

    private static final int BENEFIT_RICE_SUBSIDY    = 1;
    private static final int BENEFIT_PHONE_ALLOWANCE = 2;
    private static final int BENEFIT_CLOTHING        = 3;

    private final EmployeeRepository employeeRepository;
    private final UserAccountService userAccountService;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final EmployeeBenefitRepository employeeBenefitRepository;
    private final GovernmentDetailsRepository governmentDetailsRepository;

    // constructors
    public EmployeeService(EmployeeRepository employeeRepository,
                           UserAccountService userAccountService,
                           DepartmentRepository departmentRepository,
                           PositionRepository positionRepository) {
        this(employeeRepository, userAccountService, departmentRepository, positionRepository,
                new SalaryHistoryRepository(), new EmployeeBenefitRepository(),
                new GovernmentDetailsRepository());
    }

    public EmployeeService(EmployeeRepository employeeRepository,
                           UserAccountService userAccountService,
                           DepartmentRepository departmentRepository,
                           PositionRepository positionRepository,
                           SalaryHistoryRepository salaryHistoryRepository,
                           EmployeeBenefitRepository employeeBenefitRepository,
                           GovernmentDetailsRepository governmentDetailsRepository) {
        if (employeeRepository == null) {
            throw new IllegalArgumentException("EmployeeRepository is required.");
        }

        if (userAccountService == null) {
            throw new IllegalArgumentException("UserAccountService is required.");
        }

        this.employeeRepository = employeeRepository;
        this.userAccountService = userAccountService;
        this.departmentRepository = departmentRepository != null ? departmentRepository : new DepartmentRepository();
        this.positionRepository   = positionRepository   != null ? positionRepository   : new PositionRepository();
        this.salaryHistoryRepository = salaryHistoryRepository != null
                ? salaryHistoryRepository : new SalaryHistoryRepository();
        this.employeeBenefitRepository = employeeBenefitRepository != null
                ? employeeBenefitRepository : new EmployeeBenefitRepository();
        this.governmentDetailsRepository = governmentDetailsRepository != null
                ? governmentDetailsRepository : new GovernmentDetailsRepository();
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

    // returns all active departments
    public List<Department> getAllDepartments() {
        return departmentRepository.findAllActive();
    }

    // returns all positions
    public List<Position> getAllPositions() {
        return positionRepository.findAll();
    }

    // returns all positions for a given department
    public List<Position> getPositionsByDepartment(int departmentId) {
        return positionRepository.findByDepartmentId(departmentId);
    }

    // returns all employees as summary DTOs
    public List<EmployeeSummaryDTO> getAllEmployeeSummaries() {
        return employeeRepository.findAllWithInactive()
                .stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    // returns employee profile DTO for an active employee (used by employee portal)
    public EmployeeProfileDTO getEmployeeProfile(int employeeId) {
        validateEmployeeId(employeeId);

        EmployeeRepository.ProfileQueryResult result = employeeRepository.findProfileById(employeeId);

        if (result == null) {
            throw new EntityNotFoundException("Employee not found.");
        }

        return toProfileDTO(result);
    }

    // returns employee profile DTO for any employee including inactive (used by admin)
    public EmployeeProfileDTO getEmployeeProfileAdmin(int employeeId) {
        validateEmployeeId(employeeId);

        EmployeeRepository.ProfileQueryResult result = employeeRepository.findProfileByIdAny(employeeId);

        if (result == null) {
            throw new EntityNotFoundException("Employee not found.");
        }

        return toProfileDTO(result);
    }

    // returns government details for an employee, or null when not recorded
    public GovernmentDetails getGovernmentDetails(int employeeId) {
        validateEmployeeId(employeeId);
        return governmentDetailsRepository.findByEmployeeId(employeeId);
    }

    private EmployeeProfileDTO toProfileDTO(EmployeeRepository.ProfileQueryResult result) {
        Employee employee = result.getEmployee();
        return new EmployeeProfileDTO(
                employee.getEmployeeId(),
                employee.getFullName(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getGender(),
                employee.getBirthDate(),
                employee.getAddress(),
                employee.getCivilStatus(),
                employee.getPhoneNumber(),
                employee.getPersonalEmail(),
                employee.getEmploymentStatus(),
                employee.getPositionId(),
                result.getPositionName(),
                employee.getDepartmentId(),
                result.getDepartmentName(),
                employee.getHireDate(),
                employee.getSupervisorId(),
                result.getSupervisorName(),
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

    // creates a new employee together with initial salary, government IDs, and benefit records
    public UserAccount createEmployeeWithSalaryAndBenefits(
            Employee employee,
            double basicSalary,
            double hourlyRate,
            String sssNumber,
            String philhealthNumber,
            String tinNumber,
            String pagibigNumber,
            double riceSubsidy,
            double phoneAllowance,
            double clothingAllowance) {

        validateEmployee(employee);

        int employeeId = employeeRepository.saveAndReturnId(employee);

        LocalDate hireDate = employee.getHireDate();

        salaryHistoryRepository.save(new SalaryHistory(
                null, employeeId, SalaryType.MONTHLY, basicSalary, hourlyRate, hireDate, null));

        governmentDetailsRepository.save(new GovernmentDetails(
                null, employeeId, sssNumber, tinNumber, pagibigNumber, philhealthNumber));

        if (riceSubsidy > 0) {
            employeeBenefitRepository.save(new EmployeeBenefit(
                    null, employeeId, BENEFIT_RICE_SUBSIDY, riceSubsidy, hireDate, null));
        }
        if (phoneAllowance > 0) {
            employeeBenefitRepository.save(new EmployeeBenefit(
                    null, employeeId, BENEFIT_PHONE_ALLOWANCE, phoneAllowance, hireDate, null));
        }
        if (clothingAllowance > 0) {
            employeeBenefitRepository.save(new EmployeeBenefit(
                    null, employeeId, BENEFIT_CLOTHING, clothingAllowance, hireDate, null));
        }

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

    // copies the selected image file and persists the relative path for the employee's profile photo
    public void updateOwnProfilePhoto(int employeeId, String sourcePath) {
        getEmployeeById(employeeId);

        if (sourcePath == null || sourcePath.isBlank()) {
            throw new ValidationException("Photo path is required.");
        }

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            throw new ValidationException("Selected image file does not exist.");
        }

        String extension = resolveExtension(sourceFile.getName());
        String relativePath = "src/main/resources/images/profile-images/employee-" + employeeId + "." + extension;

        File destFile = new File(relativePath);
        if (destFile.getParentFile() != null) {
            destFile.getParentFile().mkdirs();
        }

        try {
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save profile photo: " + e.getMessage(), e);
        }

        employeeRepository.updatePhotoPath(employeeId, relativePath);
    }

    // updates the employee's own contact information
    public void updateOwnContactInfo(int employeeId, String phoneNumber, String address) {
        getEmployeeById(employeeId);

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new ValidationException("Phone number is required.");
        }

        if (address == null || address.isBlank()) {
            throw new ValidationException("Address is required.");
        }

        employeeRepository.updateContactInfo(
                employeeId,
                phoneNumber.trim(),
                address.trim()
        );
    }

    // extracts the lowercased file extension; defaults to png if none found
    private String resolveExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            return filename.substring(dot + 1).toLowerCase();
        }
        return "png";
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
