package motorph.application.service;

import motorph.application.dto.payroll.GovernmentRatesDTO;
import motorph.application.dto.payroll.PayrollInputDTO;
import motorph.application.dto.systemconfig.PagIbigRateDTO;
import motorph.application.dto.systemconfig.PhilHealthRateDTO;
import motorph.application.dto.systemconfig.SssRateDTO;
import motorph.application.dto.systemconfig.WithholdingTaxBracketDTO;
import motorph.application.exception.EntityNotFoundException;
import motorph.domain.calculator.PayrollCalculator;
import motorph.domain.model.DeductionType;
import motorph.domain.model.Employee;
import motorph.domain.model.EmployeeBenefit;
import motorph.domain.model.GovernmentDetails;
import motorph.domain.model.PayrollBenefit;
import motorph.domain.model.PayrollBreakdown;
import motorph.domain.model.PayrollDeduction;
import motorph.domain.model.PayrollEarning;
import motorph.domain.model.PayrollResult;
import motorph.domain.model.Payrun;
import motorph.domain.model.SalaryHistory;
import motorph.domain.value.EmploymentStatus;
import motorph.domain.value.Gender;
import motorph.domain.value.PayrollType;
import motorph.domain.value.SalaryType;
import motorph.infrastructure.repository.BenefitTypeRepository;
import motorph.infrastructure.repository.DeductionTypeRepository;
import motorph.infrastructure.repository.EarningTypeRepository;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.GovernmentDetailsRepository;
import motorph.infrastructure.repository.PagIbigRateRepository;
import motorph.infrastructure.repository.PayrollBenefitRepository;
import motorph.infrastructure.repository.PayrollDeductionRepository;
import motorph.infrastructure.repository.PayrollEarningRepository;
import motorph.infrastructure.repository.PayrollResultRepository;
import motorph.infrastructure.repository.PayrunRepository;
import motorph.infrastructure.repository.PhilHealthRateRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;
import motorph.infrastructure.repository.SssRateRepository;
import motorph.infrastructure.repository.WithholdingTaxBracketRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies payroll component types are resolved from stable codes. */
class PayrollComponentTypeResolutionTest {

    private final PayrunRepository payrunRepository = mock(PayrunRepository.class);
    private final PayrollResultRepository payrollResultRepository = mock(PayrollResultRepository.class);
    private final PayrollEarningRepository payrollEarningRepository = mock(PayrollEarningRepository.class);
    private final PayrollBenefitRepository payrollBenefitRepository = mock(PayrollBenefitRepository.class);
    private final PayrollDeductionRepository payrollDeductionRepository = mock(PayrollDeductionRepository.class);
    private final EarningTypeRepository earningTypeRepository = mock(EarningTypeRepository.class);
    private final BenefitTypeRepository benefitTypeRepository = mock(BenefitTypeRepository.class);
    private final DeductionTypeRepository deductionTypeRepository = mock(DeductionTypeRepository.class);
    private final SalaryHistoryRepository salaryHistoryRepository = mock(SalaryHistoryRepository.class);
    private final EmployeeBenefitRepository employeeBenefitRepository = mock(EmployeeBenefitRepository.class);
    private final GovernmentDetailsRepository governmentDetailsRepository = mock(GovernmentDetailsRepository.class);
    private final SssRateRepository sssRateRepository = mock(SssRateRepository.class);
    private final PhilHealthRateRepository philHealthRateRepository = mock(PhilHealthRateRepository.class);
    private final PagIbigRateRepository pagIbigRateRepository = mock(PagIbigRateRepository.class);
    private final WithholdingTaxBracketRepository withholdingTaxBracketRepository = mock(WithholdingTaxBracketRepository.class);
    private final AttendanceService attendanceService = mock(AttendanceService.class);
    private final OvertimeService overtimeService = mock(OvertimeService.class);
    private final EmployeeService employeeService = mock(EmployeeService.class);
    private final PayrollCalculator payrollCalculator = mock(PayrollCalculator.class);

    // resolves non-seed ids and saves the configured deduction taxability
    @Test
    void runPayrollUsesResolvedComponentTypesAndTaxability() {
        LocalDate cutoffDate = LocalDate.of(2026, 6, 15);
        PayrollService service = createService();
        stubComponentTypes();
        stubPayrollInputs(cutoffDate);

        service.runPayrollForEmployee(1, 7);

        ArgumentCaptor<PayrollInputDTO> inputCaptor = ArgumentCaptor.forClass(PayrollInputDTO.class);
        verify(payrollCalculator).computeDeductions(
                inputCaptor.capture(), any(), anyDouble(), any(LocalDate.class), any(GovernmentRatesDTO.class));
        assertEquals(100.0, inputCaptor.getValue().getRiceSubsidy());
        assertEquals(200.0, inputCaptor.getValue().getPhoneAllowance());
        assertEquals(300.0, inputCaptor.getValue().getClothingAllowance());

        ArgumentCaptor<PayrollEarning> earningCaptor = ArgumentCaptor.forClass(PayrollEarning.class);
        verify(payrollEarningRepository, org.mockito.Mockito.times(2)).save(earningCaptor.capture());
        assertEquals(List.of(101, 102), earningCaptor.getAllValues().stream()
                .map(PayrollEarning::getEarningTypeId)
                .collect(Collectors.toList()));

        ArgumentCaptor<PayrollBenefit> benefitCaptor = ArgumentCaptor.forClass(PayrollBenefit.class);
        verify(payrollBenefitRepository, org.mockito.Mockito.times(3)).save(benefitCaptor.capture());
        assertEquals(List.of(501, 502, 503), benefitCaptor.getAllValues().stream()
                .map(PayrollBenefit::getBenefitTypeId)
                .collect(Collectors.toList()));

        ArgumentCaptor<PayrollDeduction> deductionCaptor = ArgumentCaptor.forClass(PayrollDeduction.class);
        verify(payrollDeductionRepository, org.mockito.Mockito.times(6)).save(deductionCaptor.capture());
        Map<Integer, Boolean> taxabilityByTypeId = deductionCaptor.getAllValues().stream()
                .collect(Collectors.toMap(PayrollDeduction::getDeductionTypeId, PayrollDeduction::isTaxable));
        assertEquals(Map.of(601, true, 602, false, 603, true, 604, false, 605, true, 606, false),
                taxabilityByTypeId);
    }

    // rejects missing component configuration before saving a payroll result
    @Test
    void runPayrollRejectsMissingComponentTypeBeforeResultPersistence() {
        PayrollService service = createService();
        when(payrunRepository.findById(7)).thenReturn(payrun(LocalDate.of(2026, 6, 15)));
        when(earningTypeRepository.findIdByCode("BASIC_PAY")).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> service.runPayrollForEmployee(1, 7));

        verify(payrollResultRepository, never()).save(any(PayrollResult.class));
    }

    // creates the payroll service with mocked dependencies
    private PayrollService createService() {
        return new PayrollService(
                payrunRepository,
                payrollResultRepository,
                payrollEarningRepository,
                payrollBenefitRepository,
                payrollDeductionRepository,
                earningTypeRepository,
                benefitTypeRepository,
                deductionTypeRepository,
                salaryHistoryRepository,
                employeeBenefitRepository,
                governmentDetailsRepository,
                sssRateRepository,
                philHealthRateRepository,
                pagIbigRateRepository,
                withholdingTaxBracketRepository,
                attendanceService,
                overtimeService,
                employeeService,
                payrollCalculator
        );
    }

    // configures component lookups with non-seed identifiers
    private void stubComponentTypes() {
        when(earningTypeRepository.findIdByCode("BASIC_PAY")).thenReturn(101);
        when(earningTypeRepository.findIdByCode("OVERTIME_PAY")).thenReturn(102);
        when(benefitTypeRepository.findIdByCode("RICE_SUBSIDY")).thenReturn(501);
        when(benefitTypeRepository.findIdByCode("PHONE_ALLOWANCE")).thenReturn(502);
        when(benefitTypeRepository.findIdByCode("CLOTHING_ALLOWANCE")).thenReturn(503);
        when(deductionTypeRepository.findByCode("SSS")).thenReturn(new DeductionType(601, "SSS", true));
        when(deductionTypeRepository.findByCode("PHILHEALTH")).thenReturn(new DeductionType(602, "PHILHEALTH", false));
        when(deductionTypeRepository.findByCode("PAGIBIG")).thenReturn(new DeductionType(603, "PAGIBIG", true));
        when(deductionTypeRepository.findByCode("WITHHOLDING_TAX")).thenReturn(new DeductionType(604, "WITHHOLDING_TAX", false));
        when(deductionTypeRepository.findByCode("LATE")).thenReturn(new DeductionType(605, "LATE", true));
        when(deductionTypeRepository.findByCode("UNDERTIME")).thenReturn(new DeductionType(606, "UNDERTIME", false));
    }

    // configures payroll dependencies without a database
    private void stubPayrollInputs(LocalDate cutoffDate) {
        when(payrunRepository.findById(7)).thenReturn(payrun(cutoffDate));
        when(payrollResultRepository.save(any(PayrollResult.class))).thenReturn(900);
        when(employeeService.getEmployeeById(1)).thenReturn(employee());
        when(attendanceService.getAttendanceByEmployeeAndDateRange(anyInt(), any(), any())).thenReturn(List.of());
        when(overtimeService.getApprovedOvertimeHours(anyInt(), any(), any())).thenReturn(0.0);
        when(salaryHistoryRepository.findCurrentByEmployeeId(1, cutoffDate)).thenReturn(salary(cutoffDate));
        when(governmentDetailsRepository.findByEmployeeId(1)).thenReturn(governmentDetails());
        when(employeeBenefitRepository.findActiveByEmployeeId(1, cutoffDate)).thenReturn(List.of(
                new EmployeeBenefit(null, 1, 503, 300.0, cutoffDate.minusMonths(1), null),
                new EmployeeBenefit(null, 1, 501, 100.0, cutoffDate.minusMonths(1), null),
                new EmployeeBenefit(null, 1, 502, 200.0, cutoffDate.minusMonths(1), null)
        ));
        when(sssRateRepository.findEffectiveOn(cutoffDate)).thenReturn(mock(SssRateDTO.class));
        when(philHealthRateRepository.findEffectiveOn(cutoffDate)).thenReturn(mock(PhilHealthRateDTO.class));
        when(pagIbigRateRepository.findEffectiveOn(cutoffDate)).thenReturn(List.of(mock(PagIbigRateDTO.class)));
        when(withholdingTaxBracketRepository.findEffectiveOn(cutoffDate))
                .thenReturn(List.of(mock(WithholdingTaxBracketDTO.class)));
        when(payrollCalculator.computeDeductions(
                any(PayrollInputDTO.class), any(), anyDouble(), any(LocalDate.class), any(GovernmentRatesDTO.class)))
                .thenReturn(breakdown());
    }

    // creates a payrun for the test cutoff date
    private Payrun payrun(LocalDate cutoffDate) {
        return new Payrun(7, cutoffDate.minusDays(14), cutoffDate, cutoffDate.plusDays(2), PayrollType.SEMI_MONTHLY);
    }

    // creates an employee for the test payroll input
    private Employee employee() {
        return new Employee(1, "Ada", "Lovelace", LocalDate.of(1990, 1, 1), Gender.FEMALE,
                null, null, null, null, EmploymentStatus.REGULAR, 1, 1,
                LocalDate.of(2020, 1, 1), null, true, null);
    }

    // creates salary data for the test payroll input
    private SalaryHistory salary(LocalDate cutoffDate) {
        return new SalaryHistory(1, 1, SalaryType.MONTHLY, 20_000.0, 100.0, cutoffDate.minusYears(1), null);
    }

    // creates government data for the test payroll input
    private GovernmentDetails governmentDetails() {
        return new GovernmentDetails(1, 1, "SSS-1", "TIN-1", "PAGIBIG-1", "PHILHEALTH-1");
    }

    // creates a positive payroll breakdown for component persistence
    private PayrollBreakdown breakdown() {
        return new PayrollBreakdown(1_000.0, 100.0, 200.0, 300.0, 500.0, 2_100.0,
                60.0, 70.0, 80.0, 90.0, 100.0, 110.0, 510.0, 1_590.0);
    }
}
