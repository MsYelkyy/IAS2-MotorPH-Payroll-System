package motorph.backend.controller.v1;

import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.payroll.PayslipReportDTO;
import motorph.backend.dto.payroll.PayslipSummaryDTO;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.security.AuthenticatedPrincipal;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.PayslipService;
import motorph.backend.service.PermissionService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Payslip viewing endpoints. An employee always sees their own payslips;
 * viewing someone else's requires VIEW_PAYSLIP (HR/Finance), re-checked
 * server-side on every call - the desktop's role-based menu visibility is a
 * display hint only.
 */
@RestController
@RequestMapping("/api/v1/payslips")
public class PayslipController {

    private final PayslipService payslipService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    public PayslipController(PayslipService payslipService,
                             PermissionService permissionService,
                             CurrentUserProvider currentUserProvider) {
        this.payslipService = payslipService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
    }

    // the caller's own payroll history
    @GetMapping
    public List<PayslipSummaryDTO> getOwnPayslips() {
        int employeeId = currentUserProvider.getCurrentPrincipal().employeeId();
        return payslipService.getPayslipSummaries(employeeId);
    }

    @GetMapping("/{employeeId}/{payrunId}")
    public PayslipReportDTO getPayslip(@PathVariable int employeeId, @PathVariable int payrunId) {
        requireOwnOrPermitted(employeeId);
        return payslipService.getPayslipReport(employeeId, payrunId);
    }

    @GetMapping(value = "/{employeeId}/{payrunId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPayslipPdf(@PathVariable int employeeId, @PathVariable int payrunId) {
        requireOwnOrPermitted(employeeId);

        byte[] pdf = payslipService.generatePayslipPdf(employeeId, payrunId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"payslip-" + employeeId + "-" + payrunId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private void requireOwnOrPermitted(int employeeId) {
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();

        if (principal.employeeId() == employeeId) {
            return;
        }

        if (!permissionService.hasPermission(PermissionKey.VIEW_PAYSLIP)) {
            throw new UnauthorizedException("You do not have permission to view this payslip.");
        }
    }
}
