package motorph.application.service;


import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.ValidationException;
import motorph.domain.calculator.PayrollCalculator;
import motorph.domain.model.Payrun;
import motorph.domain.value.PayrollType;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Tests payroll service rules using mocked dependencies.
 */
@DisplayName("PayrollService Tests")
class PayrollServiceTest {


    // constructor rejects a missing payrun repository
    @Test
    @DisplayName("Negative - constructor should reject null payrun repository")
    void constructorShouldRejectNullPayrunRepository() {
        Deps deps = new Deps();
        deps.payrunRepository = null;

        assertThrows(IllegalArgumentException.class, deps::build);
    }


    // runPayrollForEmployee rejects an invalid employee id
    @Test
    @DisplayName("Negative - runPayrollForEmployee should reject invalid employee id")
    void runPayrollForEmployeeShouldRejectInvalidEmployeeId() {
        PayrollService service = new Deps().build();

        assertThrows(ValidationException.class, () -> service.runPayrollForEmployee(0, 1));
    }


    // runPayrollForEmployee rejects an invalid payrun id
    @Test
    @DisplayName("Negative - runPayrollForEmployee should reject invalid payrun id")
    void runPayrollForEmployeeShouldRejectInvalidPayrunId() {
        PayrollService service = new Deps().build();

        assertThrows(ValidationException.class, () -> service.runPayrollForEmployee(10001, 0));
    }


    // runPayrollForEmployee blocks a duplicate payroll run
    @Test
    @DisplayName("Negative - runPayrollForEmployee should block a duplicate run")
    void runPayrollForEmployeeShouldBlockDuplicateRun() {
        Deps deps = new Deps();
        when(deps.payrunRepository.findById(1)).thenReturn(
                new Payrun(1, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 15),
                        LocalDate.of(2025, 6, 20), PayrollType.SEMI_MONTHLY));
        when(deps.payrollResultRepository.existsByEmployeeAndPayrunId(10001, 1)).thenReturn(true);

        PayrollService service = deps.build();

        assertThrows(BusinessRuleException.class, () -> service.runPayrollForEmployee(10001, 1));
    }


    // getPayrun rejects an invalid payrun id
    @Test
    @DisplayName("Negative - getPayrun should reject an invalid payrun id")
    void getPayrunShouldRejectInvalidId() {
        PayrollService service = new Deps().build();

        assertThrows(ValidationException.class, () -> service.getPayrun(0));
    }


    // holds mocked dependencies and builds a PayrollService from them
    private static final class Deps {
        PayrunRepository payrunRepository = mock(PayrunRepository.class);
        PayrollResultRepository payrollResultRepository = mock(PayrollResultRepository.class);

        PayrollService build() {
            return new PayrollService(
                    payrunRepository,
                    payrollResultRepository,
                    mock(PayrollEarningRepository.class),
                    mock(PayrollBenefitRepository.class),
                    mock(PayrollDeductionRepository.class),
                    mock(EarningTypeRepository.class),
                    mock(BenefitTypeRepository.class),
                    mock(DeductionTypeRepository.class),
                    mock(SalaryHistoryRepository.class),
                    mock(EmployeeBenefitRepository.class),
                    mock(GovernmentDetailsRepository.class),
                    mock(SssRateRepository.class),
                    mock(PhilHealthRateRepository.class),
                    mock(PagIbigRateRepository.class),
                    mock(WithholdingTaxBracketRepository.class),
                    mock(AttendanceService.class),
                    mock(OvertimeService.class),
                    mock(EmployeeService.class),
                    new PayrollCalculator()
            );
        }
    }
}
