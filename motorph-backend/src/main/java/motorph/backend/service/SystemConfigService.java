package motorph.backend.service;

import motorph.backend.dto.systemconfig.PagIbigRateDTO;
import motorph.backend.dto.systemconfig.PhilHealthRateDTO;
import motorph.backend.dto.systemconfig.SssRateDTO;
import motorph.backend.dto.systemconfig.WithholdingTaxBracketDTO;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.PagIbigRateRepository;
import motorph.backend.repository.PhilHealthRateRepository;
import motorph.backend.repository.SssRateRepository;
import motorph.backend.repository.WithholdingTaxBracketRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Manages government contribution rate configuration. Every change is recorded in the audit log
 * under the acting user, whose ID the controller takes from the access token.
 */
@Service
public class SystemConfigService {

    private final PhilHealthRateRepository philRepo;
    private final SssRateRepository sssRepo;
    private final PagIbigRateRepository pagibigRepo;
    private final WithholdingTaxBracketRepository taxRepo;
    private final UserAccessLogService logService;

    public SystemConfigService(PhilHealthRateRepository philRepo,
                               SssRateRepository sssRepo,
                               PagIbigRateRepository pagibigRepo,
                               WithholdingTaxBracketRepository taxRepo,
                               UserAccessLogService logService) {
        this.philRepo = philRepo;
        this.sssRepo = sssRepo;
        this.pagibigRepo = pagibigRepo;
        this.taxRepo = taxRepo;
        this.logService = logService;
    }

    public List<PhilHealthRateDTO> getAllPhilHealthRates() {
        return philRepo.findAll();
    }

    public void addPhilHealthRate(PhilHealthRateDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        philRepo.closeAllOpenEnded(dto.getEffectiveStartDate().minusDays(1));
        philRepo.save(dto);
        logService.logEvent(actorAccountId, "CONFIG_PHILHEALTH_ADDED",
                "Added PhilHealth rate effective " + dto.getEffectiveStartDate(),
                UserAccessLogService.TARGET_GOVERNMENT_RATE, null);
    }

    public void updatePhilHealthRate(PhilHealthRateDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        philRepo.update(dto);
        logService.logEvent(actorAccountId, "CONFIG_PHILHEALTH_UPDATED",
                "Updated PhilHealth rate ID " + dto.getPhilhealthRateId(),
                UserAccessLogService.TARGET_GOVERNMENT_RATE, dto.getPhilhealthRateId());
    }

    public List<SssRateDTO> getAllSssRates() {
        return sssRepo.findAll();
    }

    public void addSssRate(SssRateDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        sssRepo.closeAllOpenEnded(dto.getEffectiveStartDate().minusDays(1));
        sssRepo.save(dto);
        logService.logEvent(actorAccountId, "CONFIG_SSS_ADDED",
                "Added SSS rate effective " + dto.getEffectiveStartDate(),
                UserAccessLogService.TARGET_GOVERNMENT_RATE, null);
    }

    public void updateSssRate(SssRateDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        sssRepo.update(dto);
        logService.logEvent(actorAccountId, "CONFIG_SSS_UPDATED",
                "Updated SSS rate ID " + dto.getSssRateId(),
                UserAccessLogService.TARGET_GOVERNMENT_RATE, dto.getSssRateId());
    }

    public List<PagIbigRateDTO> getAllPagIbigRates() {
        return pagibigRepo.findAll();
    }

    public void addPagIbigRate(PagIbigRateDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        pagibigRepo.closeAllOpenEnded(dto.getEffectiveStartDate().minusDays(1));
        pagibigRepo.save(dto);
        logService.logEvent(actorAccountId, "CONFIG_PAGIBIG_ADDED",
                "Added Pag-IBIG rate effective " + dto.getEffectiveStartDate(),
                UserAccessLogService.TARGET_GOVERNMENT_RATE, null);
    }

    public void updatePagIbigRate(PagIbigRateDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        pagibigRepo.update(dto);
        logService.logEvent(actorAccountId, "CONFIG_PAGIBIG_UPDATED",
                "Updated Pag-IBIG rate ID " + dto.getPagibigRateId(),
                UserAccessLogService.TARGET_GOVERNMENT_RATE, dto.getPagibigRateId());
    }

    public List<WithholdingTaxBracketDTO> getAllTaxBrackets() {
        return taxRepo.findAll();
    }

    public void addTaxBracket(WithholdingTaxBracketDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        taxRepo.closeAllOpenEnded(dto.getEffectiveStartDate().minusDays(1));
        taxRepo.save(dto);
        logService.logEvent(actorAccountId, "CONFIG_TAX_ADDED",
                "Added tax bracket effective " + dto.getEffectiveStartDate(),
                UserAccessLogService.TARGET_TAX_BRACKET, null);
    }

    public void updateTaxBracket(WithholdingTaxBracketDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        taxRepo.update(dto);
        logService.logEvent(actorAccountId, "CONFIG_TAX_UPDATED",
                "Updated tax bracket ID " + dto.getTaxBracketId(),
                UserAccessLogService.TARGET_TAX_BRACKET, dto.getTaxBracketId());
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null)
            throw new ValidationException("Effective Start Date is required.");
        if (end != null && !end.isAfter(start))
            throw new ValidationException("Effective End Date must be after Start Date.");
    }
}
