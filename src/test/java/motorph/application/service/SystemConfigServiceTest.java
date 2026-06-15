package motorph.application.service;

import motorph.application.dto.systemconfig.*;
import motorph.infrastructure.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock PhilHealthRateRepository        philRepo;
    @Mock SssRateRepository               sssRepo;
    @Mock PagIbigRateRepository           pagibigRepo;
    @Mock WithholdingTaxBracketRepository taxRepo;
    @Mock UserAccessLogRepository         logRepo;

    private SystemConfigService service;

    @BeforeEach
    void setUp() {
        service = new SystemConfigService(philRepo, sssRepo, pagibigRepo, taxRepo,
                new UserAccessLogService(logRepo));
    }

    // ── PhilHealth validation ──────────────────────────────────────────────────

    @Test
    void addPhilHealthRate_shouldRejectNullStartDate() {
        PhilHealthRateDTO dto = new PhilHealthRateDTO(0, 2, 0.05, 0, 999999, 0.5, 0.5, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.addPhilHealthRate(dto, 1));
    }

    @Test
    void addPhilHealthRate_shouldRejectEndDateBeforeStartDate() {
        PhilHealthRateDTO dto = new PhilHealthRateDTO(0, 2, 0.05, 0, 999999, 0.5, 0.5,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> service.addPhilHealthRate(dto, 1));
    }

    @Test
    void addPhilHealthRate_shouldCloseOpenEndedRowsAndSave() {
        PhilHealthRateDTO dto = new PhilHealthRateDTO(0, 2, 0.05, 0, 999999, 0.5, 0.5,
                LocalDate.of(2026, 7, 1), null);
        service.addPhilHealthRate(dto, 1);
        verify(philRepo).closeAllOpenEnded(LocalDate.of(2026, 6, 30));
        verify(philRepo).save(dto);
    }

    // ── SSS validation ─────────────────────────────────────────────────────────

    @Test
    void addSssRate_shouldRejectNullStartDate() {
        SssRateDTO dto = new SssRateDTO(0, 1, 0, 25000, 500, 500, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.addSssRate(dto, 1));
    }

    @Test
    void addSssRate_shouldRejectEndDateBeforeStartDate() {
        SssRateDTO dto = new SssRateDTO(0, 1, 0, 25000, 500, 500,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> service.addSssRate(dto, 1));
    }

    // ── Pag-IBIG validation ────────────────────────────────────────────────────

    @Test
    void addPagIbigRate_shouldRejectNullStartDate() {
        PagIbigRateDTO dto = new PagIbigRateDTO(0, 3, 0, 1500, 0.01, 0.02, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.addPagIbigRate(dto, 1));
    }

    // ── Tax validation ─────────────────────────────────────────────────────────

    @Test
    void addTaxBracket_shouldRejectNullStartDate() {
        WithholdingTaxBracketDTO dto = new WithholdingTaxBracketDTO(0, 4, 0, 20833, 0, 0, 0, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.addTaxBracket(dto, 1));
    }

    @Test
    void updatePhilHealthRate_shouldRejectNullStartDate() {
        PhilHealthRateDTO dto = new PhilHealthRateDTO(1, 2, 0.05, 0, 999999, 0.5, 0.5, null, null);
        assertThrows(IllegalArgumentException.class, () -> service.updatePhilHealthRate(dto, 1));
    }

    @Test
    void getAllPhilHealthRates_shouldDelegateToRepository() {
        when(philRepo.findAll()).thenReturn(List.of());
        List<PhilHealthRateDTO> result = service.getAllPhilHealthRates();
        assertNotNull(result);
        verify(philRepo).findAll();
    }
}
