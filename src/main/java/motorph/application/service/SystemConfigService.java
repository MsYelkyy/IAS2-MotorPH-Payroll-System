package motorph.application.service;

import motorph.application.dto.systemconfig.*;
import motorph.infrastructure.repository.*;

import java.time.LocalDate;
import java.util.List;

public class SystemConfigService {

    private final PhilHealthRateRepository     philRepo;
    private final SssRateRepository            sssRepo;
    private final PagIbigRateRepository        pagibigRepo;
    private final WithholdingTaxBracketRepository taxRepo;
    private final UserAccessLogService         logService;

    public SystemConfigService(PhilHealthRateRepository philRepo,
                               SssRateRepository sssRepo,
                               PagIbigRateRepository pagibigRepo,
                               WithholdingTaxBracketRepository taxRepo,
                               UserAccessLogService logService) {
        this.philRepo    = philRepo;
        this.sssRepo     = sssRepo;
        this.pagibigRepo = pagibigRepo;
        this.taxRepo     = taxRepo;
        this.logService  = logService;
    }

    // ── PhilHealth ────────────────────────────────────────────────────────────

    public List<PhilHealthRateDTO> getAllPhilHealthRates() {
        return philRepo.findAll();
    }

    public void addPhilHealthRate(PhilHealthRateDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        philRepo.closeAllOpenEnded(dto.getEffectiveStartDate().minusDays(1));
        philRepo.save(dto);
        logService.logAction(actorAccountId, "CONFIG_PHILHEALTH_ADDED",
                "Added PhilHealth rate effective " + dto.getEffectiveStartDate(), "", "");
    }

    public void updatePhilHealthRate(PhilHealthRateDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        philRepo.update(dto);
        logService.logAction(actorAccountId, "CONFIG_PHILHEALTH_UPDATED",
                "Updated PhilHealth rate ID " + dto.getPhilhealthRateId(), "", "");
    }

    // ── SSS ───────────────────────────────────────────────────────────────────

    public List<SssRateDTO> getAllSssRates() {
        return sssRepo.findAll();
    }

    public void addSssRate(SssRateDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        sssRepo.closeAllOpenEnded(dto.getEffectiveStartDate().minusDays(1));
        sssRepo.save(dto);
        logService.logAction(actorAccountId, "CONFIG_SSS_ADDED",
                "Added SSS rate effective " + dto.getEffectiveStartDate(), "", "");
    }

    public void updateSssRate(SssRateDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        sssRepo.update(dto);
        logService.logAction(actorAccountId, "CONFIG_SSS_UPDATED",
                "Updated SSS rate ID " + dto.getSssRateId(), "", "");
    }

    // ── Pag-IBIG ──────────────────────────────────────────────────────────────

    public List<PagIbigRateDTO> getAllPagIbigRates() {
        return pagibigRepo.findAll();
    }

    public void addPagIbigRate(PagIbigRateDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        pagibigRepo.closeAllOpenEnded(dto.getEffectiveStartDate().minusDays(1));
        pagibigRepo.save(dto);
        logService.logAction(actorAccountId, "CONFIG_PAGIBIG_ADDED",
                "Added Pag-IBIG rate effective " + dto.getEffectiveStartDate(), "", "");
    }

    public void updatePagIbigRate(PagIbigRateDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        pagibigRepo.update(dto);
        logService.logAction(actorAccountId, "CONFIG_PAGIBIG_UPDATED",
                "Updated Pag-IBIG rate ID " + dto.getPagibigRateId(), "", "");
    }

    // ── Withholding Tax ───────────────────────────────────────────────────────

    public List<WithholdingTaxBracketDTO> getAllTaxBrackets() {
        return taxRepo.findAll();
    }

    public void addTaxBracket(WithholdingTaxBracketDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        taxRepo.closeAllOpenEnded(dto.getEffectiveStartDate().minusDays(1));
        taxRepo.save(dto);
        logService.logAction(actorAccountId, "CONFIG_TAX_ADDED",
                "Added tax bracket effective " + dto.getEffectiveStartDate(), "", "");
    }

    public void updateTaxBracket(WithholdingTaxBracketDTO dto, int actorAccountId) {
        validateDates(dto.getEffectiveStartDate(), dto.getEffectiveEndDate());
        taxRepo.update(dto);
        logService.logAction(actorAccountId, "CONFIG_TAX_UPDATED",
                "Updated tax bracket ID " + dto.getTaxBracketId(), "", "");
    }

    // ── Shared validation ─────────────────────────────────────────────────────

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null)
            throw new IllegalArgumentException("Effective Start Date is required.");
        if (end != null && !end.isAfter(start))
            throw new IllegalArgumentException("Effective End Date must be after Start Date.");
    }
}
