package motorph.backend.controller.v1;

import motorph.backend.domain.EmployeeBenefit;
import motorph.backend.domain.SalaryHistory;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.salary.SalaryDatesUpdateRequest;
import motorph.backend.dto.salary.SalaryRecordRequest;
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
 * Salary history and employee benefit endpoints. Gated by MANAGE_EMPLOYEE -
 * the only desktop screens that view or edit this data (AdminSalaryController,
 * HrEmployeeController's edit form) are already employee-management surfaces;
 * there is no employee-facing self-view of salary/benefit data in the
 * original app.
 */
@RestController
@RequestMapping("/api/v1")
public class SalaryBenefitController {

    private final SalaryBenefitService salaryBenefitService;
    private final PermissionService permissionService;

    public SalaryBenefitController(SalaryBenefitService salaryBenefitService,
                                   PermissionService permissionService) {
        this.salaryBenefitService = salaryBenefitService;
        this.permissionService = permissionService;
    }

    @GetMapping("/employees/{employeeId}/salary-history")
    public List<SalaryHistory> getSalaryHistory(@PathVariable int employeeId) {
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        return salaryBenefitService.getSalaryHistory(employeeId);
    }

    @GetMapping("/employees/{employeeId}/salary-current")
    public SalaryHistory getCurrentSalary(@PathVariable int employeeId) {
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        return salaryBenefitService.getCurrentSalary(employeeId);
    }

    @PostMapping("/employees/{employeeId}/salary-history")
    public void addSalaryRecord(@PathVariable int employeeId, @RequestBody SalaryRecordRequest request) {
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        salaryBenefitService.addSalaryRecord(employeeId, request);
    }

    @PutMapping("/salary-history/{salaryHistoryId}")
    public void updateSalaryDates(@PathVariable int salaryHistoryId, @RequestBody SalaryDatesUpdateRequest request) {
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        salaryBenefitService.updateSalaryDates(salaryHistoryId, request.startDate(), request.endDate());
    }

    @GetMapping("/employees/{employeeId}/benefits")
    public List<EmployeeBenefit> getAllBenefits(@PathVariable int employeeId) {
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        return salaryBenefitService.getAllBenefits(employeeId);
    }

    // ends the benefit effective today - the only caller (AdminSalaryController) always uses today's date
    @PostMapping("/employee-benefits/{employeeBenefitId}/end")
    public void endBenefit(@PathVariable int employeeBenefitId) {
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        salaryBenefitService.endBenefit(employeeBenefitId, LocalDate.now());
    }
}
