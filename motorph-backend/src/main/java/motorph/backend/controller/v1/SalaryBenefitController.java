package motorph.backend.controller.v1;

import jakarta.validation.Valid;
import motorph.backend.domain.EmployeeBenefit;
import motorph.backend.domain.SalaryHistory;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.salary.SalaryDatesUpdateRequest;
import motorph.backend.dto.salary.SalaryRecordRequest;
import motorph.backend.exception.ValidationException;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.PermissionService;
import motorph.backend.service.SalaryBenefitService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Salary history and employee benefit endpoints with path variable and payload validation.
 * All of them require {@code MANAGE_EMPLOYEE};
 * employees have no self-service view of this data.
 */
@RestController
@RequestMapping("/api/v1")
public class SalaryBenefitController {

    private final SalaryBenefitService salaryBenefitService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    public SalaryBenefitController(SalaryBenefitService salaryBenefitService,
                                   PermissionService permissionService,
                                   CurrentUserProvider currentUserProvider) {
        this.salaryBenefitService = salaryBenefitService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/employees/{employeeId}/salary-history")
    public List<SalaryHistory> getSalaryHistory(@PathVariable int employeeId) {
        validatePositiveId(employeeId, "Employee ID");
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        return salaryBenefitService.getSalaryHistory(employeeId);
    }

    @GetMapping("/employees/{employeeId}/salary-current")
    public SalaryHistory getCurrentSalary(@PathVariable int employeeId) {
        validatePositiveId(employeeId, "Employee ID");
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        return salaryBenefitService.getCurrentSalary(employeeId);
    }

    @PostMapping("/employees/{employeeId}/salary-history")
    public void addSalaryRecord(@PathVariable int employeeId, @RequestBody @Valid SalaryRecordRequest request) {
        validatePositiveId(employeeId, "Employee ID");
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        salaryBenefitService.addSalaryRecord(employeeId, request, currentUserProvider.getCurrentUserAccountId());
    }

    @PutMapping("/salary-history/{salaryHistoryId}")
    public void updateSalaryDates(@PathVariable int salaryHistoryId, @RequestBody @Valid SalaryDatesUpdateRequest request) {
        validatePositiveId(salaryHistoryId, "Salary History ID");
        validateDateRange(request.startDate(), request.endDate());
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        salaryBenefitService.updateSalaryDates(salaryHistoryId, request.startDate(), request.endDate(),
                currentUserProvider.getCurrentUserAccountId());
    }

    @GetMapping("/employees/{employeeId}/benefits")
    public List<EmployeeBenefit> getAllBenefits(@PathVariable int employeeId) {
        validatePositiveId(employeeId, "Employee ID");
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        return salaryBenefitService.getAllBenefits(employeeId);
    }

    /**
     * Ends the benefit effective today.
     */
    @PostMapping("/employee-benefits/{employeeBenefitId}/end")
    public void endBenefit(@PathVariable int employeeBenefitId) {
        validatePositiveId(employeeBenefitId, "Employee Benefit ID");
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        salaryBenefitService.endBenefit(employeeBenefitId, LocalDate.now(), currentUserProvider.getCurrentUserAccountId());
    }

    private void validatePositiveId(int id, String name) {
        if (id <= 0) {
            throw new ValidationException(name + " must be a positive integer.");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date.");
        }
    }
}