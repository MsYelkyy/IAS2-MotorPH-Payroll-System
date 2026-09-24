package motorph.backend.controller.v1;

import motorph.backend.domain.value.Role;
import motorph.backend.dto.systemconfig.OtpPolicyDTO;
import motorph.backend.dto.systemconfig.PagIbigRateDTO;
import motorph.backend.dto.systemconfig.PhilHealthRateDTO;
import motorph.backend.dto.systemconfig.SssRateDTO;
import motorph.backend.dto.systemconfig.WithholdingTaxBracketDTO;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.PermissionService;
import motorph.backend.service.SystemConfigService;
import motorph.backend.service.TwoFactorService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Government rate configuration endpoints. Restricted to the Admin role because there is no
 * permission key for system configuration.
 */
@RestController
@RequestMapping("/api/v1/system-config")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    public SystemConfigController(SystemConfigService systemConfigService,
                                  PermissionService permissionService,
                                  CurrentUserProvider currentUserProvider) {
        this.systemConfigService = systemConfigService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Returns the OTP policy currently enforced by the backend.
     */
    @GetMapping("/otp-policy")
    public OtpPolicyDTO getOtpPolicy() {
        requireAdmin();

        return new OtpPolicyDTO(
                TwoFactorService.getOtpLength(),
                TwoFactorService.getOtpExpiryMinutes(),
                TwoFactorService.getMaxAttempts()
        );
    }

    @GetMapping("/philhealth-rates")
    public List<PhilHealthRateDTO> getAllPhilHealthRates() {
        requireAdmin();
        return systemConfigService.getAllPhilHealthRates();
    }

    @PostMapping("/philhealth-rates")
    public void addPhilHealthRate(@RequestBody PhilHealthRateDTO dto) {
        requireAdmin();
        systemConfigService.addPhilHealthRate(dto, currentUserProvider.getCurrentUserAccountId());
    }

    @PutMapping("/philhealth-rates")
    public void updatePhilHealthRate(@RequestBody PhilHealthRateDTO dto) {
        requireAdmin();
        systemConfigService.updatePhilHealthRate(dto, currentUserProvider.getCurrentUserAccountId());
    }

    @GetMapping("/sss-rates")
    public List<SssRateDTO> getAllSssRates() {
        requireAdmin();
        return systemConfigService.getAllSssRates();
    }

    @PostMapping("/sss-rates")
    public void addSssRate(@RequestBody SssRateDTO dto) {
        requireAdmin();
        systemConfigService.addSssRate(dto, currentUserProvider.getCurrentUserAccountId());
    }

    @PutMapping("/sss-rates")
    public void updateSssRate(@RequestBody SssRateDTO dto) {
        requireAdmin();
        systemConfigService.updateSssRate(dto, currentUserProvider.getCurrentUserAccountId());
    }

    @GetMapping("/pagibig-rates")
    public List<PagIbigRateDTO> getAllPagIbigRates() {
        requireAdmin();
        return systemConfigService.getAllPagIbigRates();
    }

    @PostMapping("/pagibig-rates")
    public void addPagIbigRate(@RequestBody PagIbigRateDTO dto) {
        requireAdmin();
        systemConfigService.addPagIbigRate(dto, currentUserProvider.getCurrentUserAccountId());
    }

    @PutMapping("/pagibig-rates")
    public void updatePagIbigRate(@RequestBody PagIbigRateDTO dto) {
        requireAdmin();
        systemConfigService.updatePagIbigRate(dto, currentUserProvider.getCurrentUserAccountId());
    }

    @GetMapping("/tax-brackets")
    public List<WithholdingTaxBracketDTO> getAllTaxBrackets() {
        requireAdmin();
        return systemConfigService.getAllTaxBrackets();
    }

    @PostMapping("/tax-brackets")
    public void addTaxBracket(@RequestBody WithholdingTaxBracketDTO dto) {
        requireAdmin();
        systemConfigService.addTaxBracket(dto, currentUserProvider.getCurrentUserAccountId());
    }

    @PutMapping("/tax-brackets")
    public void updateTaxBracket(@RequestBody WithholdingTaxBracketDTO dto) {
        requireAdmin();
        systemConfigService.updateTaxBracket(dto, currentUserProvider.getCurrentUserAccountId());
    }

    private void requireAdmin() {
        if (permissionService.getActiveRole() != Role.ADMIN) {
            throw new UnauthorizedException("You do not have permission to perform this action.");
        }
    }
}
