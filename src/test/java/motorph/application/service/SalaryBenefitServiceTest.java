package motorph.application.service;


import motorph.application.dto.salary.EmployeeBenefitDTO;
import motorph.application.dto.salary.SalaryHistoryDTO;
import motorph.application.exception.ValidationException;
import motorph.domain.model.EmployeeBenefit;
import motorph.domain.model.SalaryHistory;
import motorph.domain.value.SalaryType;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Tests salary and benefit business rules using mocked dependencies.
 */
@DisplayName("SalaryBenefitService Tests")
class SalaryBenefitServiceTest {


    private static final Logger logger = Logger.getLogger(SalaryBenefitServiceTest.class.getName());


    // constructor rejects a missing salary repository
    @Test
    @DisplayName("Negative - constructor should reject null salary repository")
    void constructorShouldRejectNullSalaryRepository() {
        logger.info("Testing null salary repository validation.");

        assertThrows(
                IllegalArgumentException.class,
                () -> new SalaryBenefitService(null, mock(EmployeeBenefitRepository.class)));
    }


    // adding a salary record saves the new record
    @Test
    @DisplayName("Positive - addSalaryRecord should save a valid record")
    void addSalaryRecordShouldSaveValidRecord() {
        logger.info("Testing valid salary record is saved.");

        SalaryHistoryRepository salaryRepo = mock(SalaryHistoryRepository.class);
        when(salaryRepo.findCurrentByEmployeeId(anyInt(), any())).thenReturn(null);

        SalaryBenefitService service = new SalaryBenefitService(
                salaryRepo, mock(EmployeeBenefitRepository.class));

        service.addSalaryRecord(new SalaryHistoryDTO(
                10001, "MONTHLY", 30000.0, 187.5, LocalDate.of(2025, 1, 1), null));

        verify(salaryRepo).save(any(SalaryHistory.class));
    }


    // adding a salary record auto-closes the currently open record
    @Test
    @DisplayName("Positive - addSalaryRecord should close the open salary record")
    void addSalaryRecordShouldCloseOpenRecord() {
        logger.info("Testing adding a salary record closes the open one.");

        SalaryHistoryRepository salaryRepo = mock(SalaryHistoryRepository.class);
        SalaryHistory openRecord = new SalaryHistory(
                7, 10001, SalaryType.MONTHLY, 25000.0, 156.25, LocalDate.of(2024, 1, 1), null);
        when(salaryRepo.findCurrentByEmployeeId(anyInt(), any())).thenReturn(openRecord);

        SalaryBenefitService service = new SalaryBenefitService(
                salaryRepo, mock(EmployeeBenefitRepository.class));

        service.addSalaryRecord(new SalaryHistoryDTO(
                10001, "MONTHLY", 30000.0, 187.5, LocalDate.of(2025, 1, 1), null));

        verify(salaryRepo).update(any(SalaryHistory.class));
        verify(salaryRepo).save(any(SalaryHistory.class));
    }


    // adding a salary record rejects a missing start date
    @Test
    @DisplayName("Negative - addSalaryRecord should reject a missing start date")
    void addSalaryRecordShouldRejectMissingStartDate() {
        logger.info("Testing salary record rejects a missing start date.");

        SalaryBenefitService service = new SalaryBenefitService(
                mock(SalaryHistoryRepository.class), mock(EmployeeBenefitRepository.class));

        assertThrows(
                ValidationException.class,
                () -> service.addSalaryRecord(new SalaryHistoryDTO(
                        10001, "MONTHLY", 30000.0, 187.5, null, null)));
    }


    // adding a benefit saves the new allocation
    @Test
    @DisplayName("Positive - addBenefit should save a valid benefit")
    void addBenefitShouldSaveValidBenefit() {
        logger.info("Testing valid benefit is saved.");

        EmployeeBenefitRepository benefitRepo = mock(EmployeeBenefitRepository.class);

        SalaryBenefitService service = new SalaryBenefitService(
                mock(SalaryHistoryRepository.class), benefitRepo);

        service.addBenefit(new EmployeeBenefitDTO(
                10001, 1, 1500.0, LocalDate.of(2025, 1, 1), null));

        verify(benefitRepo).save(any(EmployeeBenefit.class));
    }


    // adding a benefit rejects a non-positive amount
    @Test
    @DisplayName("Negative - addBenefit should reject a non-positive amount")
    void addBenefitShouldRejectNonPositiveAmount() {
        logger.info("Testing benefit rejects a non-positive amount.");

        EmployeeBenefitRepository benefitRepo = mock(EmployeeBenefitRepository.class);

        SalaryBenefitService service = new SalaryBenefitService(
                mock(SalaryHistoryRepository.class), benefitRepo);

        assertThrows(
                ValidationException.class,
                () -> service.addBenefit(new EmployeeBenefitDTO(
                        10001, 1, 0.0, LocalDate.of(2025, 1, 1), null)));
        verify(benefitRepo, never()).save(any());
    }


    // ending a missing benefit is rejected
    @Test
    @DisplayName("Negative - endBenefit should reject a missing benefit record")
    void endBenefitShouldRejectMissingRecord() {
        logger.info("Testing ending a missing benefit is rejected.");

        EmployeeBenefitRepository benefitRepo = mock(EmployeeBenefitRepository.class);
        when(benefitRepo.findById(99)).thenReturn(null);

        SalaryBenefitService service = new SalaryBenefitService(
                mock(SalaryHistoryRepository.class), benefitRepo);

        assertThrows(
                ValidationException.class,
                () -> service.endBenefit(99, LocalDate.of(2025, 12, 31)));
    }
}
