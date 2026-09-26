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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final UserAccessLogService userAccessLogService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           PositionRepository positionRepository,
                           GovernmentDetailsRepository governmentDetailsRepository,
                           SalaryHistoryRepository salaryHistoryRepository,
                           EmployeeBenefitRepository employeeBenefitRepository,
                           UserAccountService userAccountService,
                           UserAccessLogService userAccessLogService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.governmentDetailsRepository = governmentDetailsRepository;
        this.salaryHistoryRepository = salaryHistoryRepository;
        this.employeeBenefitRepository = employeeBenefitRepository;
        this.userAccountService = userAccountService;
        this.userAccessLogService = userAccessLogService;
    }

    public Employee getEmployeeById(int employeeId) {
        validateEmployeeId(employeeId);

        Employee employee = employeeRepository.findActiveById(employeeId);

        if (employee == null) {
            throw new EntityNotFoundException("Employee not found.");
        }

        return employee;
    }

    public Employee getEmployeeByIdAny(int employeeId) {
        validateEmployeeId(employeeId);

        Employee employee = employeeRepository.findAnyById(employeeId);

        if (employee == null) {
            throw new EntityNotFoundException("Employee not found.");
        }

        return employee;
    }

    public void updateOwnContactInfo(int employeeId, String phoneNumber, String address) {
        getEmployeeById(employeeId);

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new ValidationException("Phone number is required.");
        }

        if (address == null || address.isBlank()) {
            throw new ValidationException("Address is required.");
        }

        employeeRepository.updateContactInfo(employeeId, phoneNumber.trim(), address.trim());
    }

    public List<Employee> getAllActiveEmployees() {
        return employeeRepository.findAllActive();
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAllWithInactive();
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAllActive();
    }

    public List<Position> getAllPositions() {
        return positionRepository.findAll();
    }

    public List<Position> getPositionsByDepartment(int departmentId) {
        return positionRepository.findByDepartmentId(departmentId);
    }

    public List<EmployeeSummaryDTO> getAllEmployeeSummaries() {
        return employeeRepository.findAllWithInactive()
                .stream()
                .sorted(Comparator.comparing(e -> e.getFullName(true), String.CASE_INSENSITIVE_ORDER))
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    //Only active employees are returned.
    public EmployeeProfileDTO getEmployeeProfile(int employeeId) {
        validateEmployeeId(employeeId);

        EmployeeRepository.ProfileQueryResult result = employeeRepository.findProfileById(employeeId);

        if (result == null) {
            throw new EntityNotFoundException("Employee not found.");
        }

        return toProfileDTO(result);
    }

    //Includes inactive employees.
    public EmployeeProfileDTO getEmployeeProfileAdmin(int employeeId) {
        validateEmployeeId(employeeId);

        EmployeeRepository.ProfileQueryResult result = employeeRepository.findProfileByIdAny(employeeId);

        if (result == null) {
            throw new EntityNotFoundException("Employee not found.");
        }

        return toProfileDTO(result);
    }

    //Returns null if no government details are saved.
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

    // Also creates government details and a login account.
    public EmployeeCreateResponse createEmployeeWithSalaryAndBenefits(EmployeeCreateRequest request,
                                                                     int actorUserAccountId) {
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

        userAccessLogService.logEvent(actorUserAccountId, UserAccessLogService.ACTION_EMPLOYEE_CREATED,
                "Created employee ID " + employeeId + " (" + employee.getFullName() + ")");

        return new EmployeeCreateResponse(getEmployeeProfileAdmin(employeeId), provisioning.credentialsPreview());
    }

    public EmployeeProfileDTO updateEmployee(int employeeId, EmployeeUpdateRequest request, int actorUserAccountId) {
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

        List<String> changedSections = new ArrayList<>();

        boolean personalChanged = personalInfoChanged(existing, updated);
        boolean employmentChanged = employmentInfoChanged(existing, updated);

        if (personalChanged || employmentChanged) {
            employeeRepository.update(updated);
        }
        if (personalChanged) {
            changedSections.add("personal");
        }
        if (employmentChanged) {
            changedSections.add("employment");
        }

        if (governmentDetailsChanged(employeeId, request)) {
            updateGovernmentDetails(employeeId, request.sssNumber(), request.philhealthNumber(),
                    request.tinNumber(), request.pagibigNumber());
            changedSections.add("government");
        }

        boolean salaryChanged = salaryChanged(employeeId, request.basicSalary());
        boolean benefitsChanged = benefitsChanged(employeeId, request);

        if (salaryChanged) {
            applySalaryChange(employeeId, request.basicSalary(), request.hourlyRate());
        }
        if (benefitsChanged) {
            updateBenefits(employeeId, request.riceSubsidy(), request.phoneAllowance(), request.clothingAllowance());
        }
        if (salaryChanged || benefitsChanged) {
            changedSections.add("compensation");
        }

        if (!changedSections.isEmpty()) {
            userAccessLogService.logEvent(actorUserAccountId, UserAccessLogService.ACTION_EMPLOYEE_UPDATED,
                    describeUpdate(employeeId, changedSections));
        }

        return getEmployeeProfileAdmin(employeeId);
    }

    private boolean salaryChanged(int employeeId, Double basicSalary) {
        if (basicSalary == null) {
            return false;
        }

        SalaryHistory current = salaryHistoryRepository.findCurrentByEmployeeId(employeeId, LocalDate.now());
        return current == null || current.getBasicSalary() == null || !sameAmount(current.getBasicSalary(), basicSalary);
    }

    /**
     * Salary changes start today; earlier periods keep the old rate.
     * Another change today updates the same record.
     */
    private void applySalaryChange(int employeeId, double basicSalary, Double hourlyRate) {
        if (basicSalary <= 0) {
            throw new ValidationException("Basic salary must be greater than zero.");
        }
        if (hourlyRate == null || hourlyRate <= 0) {
            throw new ValidationException("Hourly rate must be greater than zero.");
        }

        LocalDate today = LocalDate.now();
        SalaryHistory current = salaryHistoryRepository.findCurrentByEmployeeId(employeeId, today);
        SalaryType salaryType = current != null ? current.getSalaryType() : SalaryType.MONTHLY;
        LocalDate endDate = salaryChangeEndDate(employeeId, current, today);

        if (current != null && today.equals(current.getStartDate())) {
            salaryHistoryRepository.update(new SalaryHistory(current.getSalaryHistoryId(), employeeId, salaryType,
                    basicSalary, hourlyRate, today, endDate));
            return;
        }

        if (current != null) {
            salaryHistoryRepository.update(new SalaryHistory(current.getSalaryHistoryId(), employeeId, salaryType,
                    current.getBasicSalary(), current.getHourlyRate(), current.getStartDate(), today.minusDays(1)));
        }

        salaryHistoryRepository.save(new SalaryHistory(null, employeeId, salaryType, basicSalary, hourlyRate, today, endDate));
    }

    private LocalDate salaryChangeEndDate(int employeeId, SalaryHistory current, LocalDate today) {
        LocalDate endDate = current != null ? current.getEndDate() : null;
        LocalDate nextStart = salaryHistoryRepository.findByEmployeeId(employeeId).stream()
                .map(SalaryHistory::getStartDate)
                .filter(start -> start.isAfter(today))
                .min(LocalDate::compareTo)
                .orElse(null);

        if (nextStart != null) {
            LocalDate latestEnd = nextStart.minusDays(1);
            if (endDate == null || latestEnd.isBefore(endDate)) {
                return latestEnd;
            }
        }
        return endDate;
    }

    private boolean personalInfoChanged(Employee before, Employee after) {
        return !Objects.equals(before.getFirstName(), after.getFirstName())
                || !Objects.equals(before.getLastName(), after.getLastName())
                || !Objects.equals(before.getBirthDate(), after.getBirthDate())
                || before.getGender() != after.getGender()
                || !sameText(before.getAddress(), after.getAddress())
                || !sameText(before.getCivilStatus(), after.getCivilStatus())
                || !sameText(before.getPhoneNumber(), after.getPhoneNumber())
                || !sameText(before.getPersonalEmail(), after.getPersonalEmail());
    }

    private boolean employmentInfoChanged(Employee before, Employee after) {
        return before.getEmploymentStatus() != after.getEmploymentStatus()
                || before.getPositionId() != after.getPositionId()
                || before.getDepartmentId() != after.getDepartmentId()
                || !Objects.equals(before.getHireDate(), after.getHireDate())
                || !Objects.equals(before.getSupervisorId(), after.getSupervisorId());
    }

    private boolean governmentDetailsChanged(int employeeId, EmployeeUpdateRequest request) {
        GovernmentDetails existing = governmentDetailsRepository.findByEmployeeId(employeeId);

        if (existing == null) {
            return !sameDigits(null, request.sssNumber()) || !sameDigits(null, request.philhealthNumber())
                    || !sameDigits(null, request.tinNumber()) || !sameDigits(null, request.pagibigNumber());
        }

        return !sameDigits(existing.getSssNumber(), request.sssNumber())
                || !sameDigits(existing.getPhilhealthNumber(), request.philhealthNumber())
                || !sameDigits(existing.getTinNumber(), request.tinNumber())
                || !sameDigits(existing.getPagibigNumber(), request.pagibigNumber());
    }

    private boolean benefitsChanged(int employeeId, EmployeeUpdateRequest request) {
        List<EmployeeBenefit> benefits = employeeBenefitRepository.findByEmployeeId(employeeId);

        return !sameAmount(currentBenefitAmount(benefits, BENEFIT_RICE_SUBSIDY), request.riceSubsidy())
                || !sameAmount(currentBenefitAmount(benefits, BENEFIT_PHONE_ALLOWANCE), request.phoneAllowance())
                || !sameAmount(currentBenefitAmount(benefits, BENEFIT_CLOTHING), request.clothingAllowance());
    }

    private double currentBenefitAmount(List<EmployeeBenefit> benefits, int benefitTypeId) {
        for (EmployeeBenefit benefit : benefits) {
            if (benefit.getBenefitTypeId() == benefitTypeId && benefit.isCurrent()) {
                return benefit.getAmount();
            }
        }
        return 0.0;
    }

    // Treat blank and null as the same to avoid false changes.
    private boolean sameText(String a, String b) {
        String left = a == null ? "" : a.trim();
        String right = b == null ? "" : b.trim();
        return left.equals(right);
    }

    // Ignore dashes added by the employee form when comparing IDs.
    private boolean sameDigits(String a, String b) {
        String left = a == null ? "" : a.replaceAll("\\D", "");
        String right = b == null ? "" : b.replaceAll("\\D", "");
        return left.equals(right);
    }

    private boolean sameAmount(double a, double b) {
        return Math.abs(Math.max(a, 0) - Math.max(b, 0)) < 0.005;
    }

    private String describeUpdate(int employeeId, List<String> changedSections) {
        List<String> infoTabs = new ArrayList<>(changedSections);
        boolean compensation = infoTabs.remove("compensation");

        List<String> parts = new ArrayList<>();
        if (!infoTabs.isEmpty()) {
            parts.add(joinWords(infoTabs) + " information");
        }
        if (compensation) {
            parts.add("compensation");
        }

        return "Updated " + joinWords(parts) + " of employee ID " + employeeId;
    }

    private String joinWords(List<String> words) {
        int count = words.size();
        return switch (count) {
            case 1 -> words.get(0);
            case 2 -> words.get(0) + " and " + words.get(1);
            default -> String.join(", ", words.subList(0, count - 1)) + ", and " + words.get(count - 1);
        };
    }

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

    private void updateBenefits(int employeeId, double riceSubsidy, double phoneAllowance, double clothingAllowance) {
        upsertBenefit(employeeId, BENEFIT_RICE_SUBSIDY, riceSubsidy);
        upsertBenefit(employeeId, BENEFIT_PHONE_ALLOWANCE, phoneAllowance);
        upsertBenefit(employeeId, BENEFIT_CLOTHING, clothingAllowance);
    }

    // Clearing the amount ends the current benefit record.
    private void upsertBenefit(int employeeId, int benefitTypeId, double amount) {
        EmployeeBenefit current = null;
        for (EmployeeBenefit benefit : employeeBenefitRepository.findByEmployeeId(employeeId)) {
            if (benefit.getBenefitTypeId() == benefitTypeId && benefit.isCurrent()) {
                current = benefit;
                break;
            }
        }

        if (current != null) {
            if (sameAmount(current.getAmount(), amount)) {
                return;
            }
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

    // Also disables the login account. Employees cannot deactivate themselves.
    public void deactivateEmployee(int employeeId, int actingEmployeeId, int actorUserAccountId) {
        validateEmployeeId(employeeId);

        if (employeeId == actingEmployeeId) {
            throw new BusinessRuleException("You cannot deactivate your own account.");
        }

        employeeRepository.deactivate(employeeId);
        userAccountService.deactivateAccountByEmployeeId(employeeId);

        userAccessLogService.logEvent(actorUserAccountId, UserAccessLogService.ACTION_EMPLOYEE_DEACTIVATED,
                "Deactivated employee ID " + employeeId + " and disabled their user account");
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
