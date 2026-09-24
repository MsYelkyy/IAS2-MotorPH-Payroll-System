package motorph.backend.controller.v1;

import motorph.backend.domain.Payrun;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.payroll.CompletedPayrunSummaryDTO;
import motorph.backend.dto.payroll.PayrollPreviewDTO;
import motorph.backend.dto.payroll.PayrollSummaryDTO;
import motorph.backend.dto.payroll.PayrunCreateRequest;
import motorph.backend.dto.payroll.PayrunReadinessDTO;
import motorph.backend.dto.payroll.PendingPayrunSummaryDTO;
import motorph.backend.service.PayrollService;
import motorph.backend.service.PermissionService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Payrun endpoints. Listing payruns is open to any authenticated user because it only exposes
 * period dates, which the employee dashboard uses to show the next payday. Anything that exposes
 * payroll amounts or runs a computation requires {@code VIEW_PAYROLL} or {@code PROCESS_PAYROLL}.
 */
@RestController
@RequestMapping("/api/v1/payruns")
public class PayrunController {

    private final PayrollService payrollService;
    private final PermissionService permissionService;

    public PayrunController(PayrollService payrollService, PermissionService permissionService) {
        this.payrollService = payrollService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<Payrun> getAllPayruns() {
        return payrollService.getAllPayruns();
    }

    @GetMapping("/{payrunId}")
    public Payrun getPayrun(@PathVariable int payrunId) {
        return payrollService.getPayrun(payrunId);
    }

    @GetMapping("/with-results")
    public List<Payrun> getPayrunsWithPayrollData() {
        return payrollService.getPayrunsWithPayrollData();
    }

    @GetMapping("/pending")
    public List<PendingPayrunSummaryDTO> getPendingPayruns() {
        permissionService.requirePermission(PermissionKey.VIEW_PAYROLL);
        return payrollService.getPendingPayruns();
    }

    @GetMapping("/completed")
    public List<CompletedPayrunSummaryDTO> getCompletedPayruns() {
        permissionService.requirePermission(PermissionKey.VIEW_PAYROLL);
        return payrollService.getCompletedPayruns();
    }

    @GetMapping("/{payrunId}/readiness")
    public List<PayrunReadinessDTO> getPayrunReadiness(@PathVariable int payrunId) {
        permissionService.requirePermission(PermissionKey.VIEW_PAYROLL);
        return payrollService.getPayrunReadiness(payrunId);
    }

    @GetMapping("/{payrunId}/summaries")
    public List<PayrollSummaryDTO> getPayrollSummaries(@PathVariable int payrunId) {
        permissionService.requirePermission(PermissionKey.VIEW_PAYROLL);
        return payrollService.getPayrollSummaries(payrunId);
    }

    @GetMapping("/{payrunId}/preview/{employeeId}")
    public PayrollPreviewDTO previewPayrollForEmployee(@PathVariable int payrunId, @PathVariable int employeeId) {
        permissionService.requirePermission(PermissionKey.PROCESS_PAYROLL);
        return payrollService.previewPayrollForEmployee(employeeId, payrunId);
    }

    @PostMapping
    public Payrun createPayrun(@RequestBody PayrunCreateRequest request) {
        permissionService.requirePermission(PermissionKey.PROCESS_PAYROLL);
        return payrollService.createPayrun(request);
    }

    @PostMapping("/{payrunId}/generate")
    public int generatePayroll(@PathVariable int payrunId) {
        permissionService.requirePermission(PermissionKey.PROCESS_PAYROLL);
        return payrollService.generatePayrollForReadyEmployees(payrunId);
    }
}
