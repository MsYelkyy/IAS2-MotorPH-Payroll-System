package motorph.backend.service;

import motorph.backend.dto.employee.EmployeeCreateRequest;
import motorph.backend.dto.employee.EmployeeCreateResponse;
import motorph.backend.dto.employee.EmployeeProfileDTO;
import motorph.backend.dto.employee.EmployeeSummaryDTO;
import motorph.backend.dto.employee.EmployeeUpdateRequest;
import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.ValidationException;
import motorph.backend.domain.Department;
import motorph.backend.domain.Employee;
import motorph.backend.domain.EmployeeBenefit;
import motorph.backend.domain.GovernmentDetails;
import motorph.backend.domain.Position;
import motorph.backend.domain.SalaryHistory;
import motorph.backend.domain.value.SalaryType;
import motorph.backend.repository.DepartmentRepository;
import motorph.backend.repository.EmployeeBenefitRepository;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.GovernmentDetailsRepository;
import motorph.backend.repository.PositionRepository;
import motorph.backend.repository.SalaryHistoryRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles employee lookups and CRUD. Ported from the desktop's
 * EmployeeService; write methods (create/update/deactivate) were added in
 * Phase 3 alongside a narrow backend UserAccountService slice for account
 * provisioning/deactivation.
 */
@Service
public class EmployeeService {

    private static final int BENEFIT_RICE_SUBSIDY    = 1;
    private static final int BENEFIT_PHONE_ALLOWANCE = 2;
    private static final int BENEFIT_CLOTHING        = 3;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final GovernmentDetailsRepository governmentDetailsRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final EmployeeBenefitRepository employeeBenefitRepository;
    private final UserAccountService userAccountService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           PositionRepository positionRepository,
                           GovernmentDetailsRepository governmentDetailsRepository,
                           SalaryHistoryRepository salaryHistoryRepository,
                           EmployeeBenefitRepository employeeBenefitRepository,
                           UserAccountService userAccountService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.governmentDetailsRepository = governmentDetailsRepository;
        this.salaryHistoryRepository = salaryHistoryRepository;
        this.employeeBenefitRepository = employeeBenefitRepository;
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

    // returns any employee by id including inactive (for IT display use only)
    public Employee getEmployeeByIdAny(int employeeId) {
        validateEmployeeId(employeeId);

        Employee employee = employeeRepository.findAnyById(employeeId);

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
                .sorted(Comparator.comparing(e -> e.getFullName(true), String.CASE_INSENSITIVE_ORDER))
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

    // creates a new employee together with initial salary, government IDs, and benefit records
    public EmployeeCreateResponse createEmployeeWithSalaryAndBenefits(EmployeeCreateRequest request) {
        Employee employee = buildEmployeeForCreate(request);
        validateEmployee(employee);

        int employeeId = employeeRepository.saveAndReturnId(employee);

        LocalDate hireDate = employee.getHireDate();

        salaryHistoryRepository.save(new SalaryHistory(
                null, employeeId, SalaryType.MONTHLY, request.basicSalary(), request.hourlyRate(), hireDate, null));

        governmentDetailsRepository.save(new GovernmentDetails(
                null, employeeId, request.sssNumber(), request.tinNumber(),
                request.pagibigNumber(), request.philhealthNumber()));

        if (request.riceSubsidy() > 0) {
            employeeBenefitRepository.save(new EmployeeBenefit(
                    null, employeeId, BENEFIT_RICE_SUBSIDY, request.riceSubsidy(), hireDate, null));
        }
        if (request.phoneAllowance() > 0) {
            employeeBenefitRepository.save(new EmployeeBenefit(
                    null, employeeId, BENEFIT_PHONE_ALLOWANCE, request.phoneAllowance(), hireDate, null));
        }
        if (request.clothingAllowance() > 0) {
            employeeBenefitRepository.save(new EmployeeBenefit(
                    null, employeeId, BENEFIT_CLOTHING, request.clothingAllowance(), hireDate, null));
        }

        UserAccountService.AccountProvisioningResult provisioning = userAccountService.createTemporaryEmployeeAccount(
                employeeId, employee.getFirstName(), employee.getLastName(), employee.getPersonalEmail());

        return new EmployeeCreateResponse(getEmployeeProfileAdmin(employeeId), provisioning.credentialsPreview());
    }

    // updates an existing employee's profile, government IDs, and benefits
    public EmployeeProfileDTO updateEmployee(int employeeId, EmployeeUpdateRequest request) {
        validateEmployeeId(employeeId);

        Employee existing = employeeRepository.findAnyById(employeeId);
        if (existing == null) {
            throw new EntityNotFoundException("Employee not found.");
        }

        Employee updated = new Employee(
                employeeId,
                request.firstName(),
                request.lastName(),
                request.birthDate(),
                request.gender(),
                request.address(),
                request.civilStatus(),
                request.phoneNumber(),
                request.personalEmail(),
                request.employmentStatus(),
                request.positionId(),
                request.departmentId(),
                request.hireDate(),
                request.supervisorId(),
                existing.isActive(),
                existing.getPhotoPath()
        );

        validateEmployee(updated);
        employeeRepository.update(updated);

        updateGovernmentDetails(employeeId, request.sssNumber(), request.philhealthNumber(),
                request.tinNumber(), request.pagibigNumber());
        updateBenefits(employeeId, request.riceSubsidy(), request.phoneAllowance(), request.clothingAllowance());

        return getEmployeeProfileAdmin(employeeId);
    }

    // updates (or creates) the government IDs for an existing employee
    private void updateGovernmentDetails(int employeeId, String sssNumber, String philhealthNumber,
                                         String tinNumber, String pagibigNumber) {
        GovernmentDetails existing = governmentDetailsRepository.findByEmployeeId(employeeId);
        if (existing == null) {
            governmentDetailsRepository.save(new GovernmentDetails(
                    null, employeeId, sssNumber, tinNumber, pagibigNumber, philhealthNumber));
        } else {
            governmentDetailsRepository.update(new GovernmentDetails(
                    existing.getGovernmentDetailsId(), employeeId,
                    sssNumber, tinNumber, pagibigNumber, philhealthNumber));
        }
    }

    // updates the three allowance benefits for an existing employee, keyed by benefit type
    private void updateBenefits(int employeeId, double riceSubsidy, double phoneAllowance, double clothingAllowance) {
        upsertBenefit(employeeId, BENEFIT_RICE_SUBSIDY, riceSubsidy);
        upsertBenefit(employeeId, BENEFIT_PHONE_ALLOWANCE, phoneAllowance);
        upsertBenefit(employeeId, BENEFIT_CLOTHING, clothingAllowance);
    }

    // sets a benefit amount: updates the current record, ends it when cleared, or adds a new one
    private void upsertBenefit(int employeeId, int benefitTypeId, double amount) {
        EmployeeBenefit current = null;
        for (EmployeeBenefit benefit : employeeBenefitRepository.findByEmployeeId(employeeId)) {
            if (benefit.getBenefitTypeId() == benefitTypeId && benefit.isCurrent()) {
                current = benefit;
                break;
            }
        }

        if (current != null) {
            if (amount <= 0) {
                employeeBenefitRepository.update(new EmployeeBenefit(
                        current.getEmployeeBenefitId(), employeeId, benefitTypeId,
                        current.getAmount(), current.getEffectiveStartDate(), LocalDate.now()));
            } else {
                employeeBenefitRepository.update(new EmployeeBenefit(
                        current.getEmployeeBenefitId(), employeeId, benefitTypeId,
                        amount, current.getEffectiveStartDate(), current.getEffectiveEndDate()));
            }
        } else if (amount > 0) {
            employeeBenefitRepository.save(new EmployeeBenefit(
                    null, employeeId, benefitTypeId, amount, LocalDate.now(), null));
        }
    }

    // deactivates an employee and disables the linked user account; an actor can never deactivate themselves
    public void deactivateEmployee(int employeeId, int actingEmployeeId) {
        validateEmployeeId(employeeId);

        if (employeeId == actingEmployeeId) {
            throw new BusinessRuleException("You cannot deactivate your own account.");
        }

        employeeRepository.deactivate(employeeId);
        userAccountService.deactivateAccountByEmployeeId(employeeId);
    }

    private Employee buildEmployeeForCreate(EmployeeCreateRequest request) {
        return new Employee(
                null,
                request.firstName(),
                request.lastName(),
                request.birthDate(),
                request.gender(),
                request.address(),
                request.civilStatus(),
                request.phoneNumber(),
                request.personalEmail(),
                request.employmentStatus(),
                request.positionId(),
                request.departmentId(),
                request.hireDate(),
                request.supervisorId(),
                true,
                null
        );
    }

    private void validateEmployee(Employee employee) {
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

    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }
}
