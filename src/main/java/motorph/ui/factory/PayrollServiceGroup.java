package motorph.ui.factory;

import motorph.application.service.AttendanceService;
import motorph.application.service.EmployeeService;
import motorph.application.service.OvertimeService;
import motorph.application.service.PayrollService;
import motorph.application.service.SalaryBenefitService;
import motorph.domain.calculator.PayrollCalculator;
import motorph.infrastructure.repository.BenefitTypeRepository;
import motorph.infrastructure.repository.DeductionTypeRepository;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.EarningTypeRepository;
import motorph.infrastructure.repository.GovernmentDetailsRepository;
import motorph.infrastructure.repository.OvertimeRepository;
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

/** Owns payroll, overtime, and salary benefit services. */
class PayrollServiceGroup {

    private final PayrollService payrollService;
    private final SalaryBenefitService salaryBenefitService;

    // constructors
    PayrollServiceGroup(AttendanceService attendanceService, EmployeeService employeeService) {
        OvertimeService overtimeService = new OvertimeService(
                new OvertimeRepository(),
                attendanceService,
                employeeService
        );

        this.payrollService = new PayrollService(
                new PayrunRepository(),
                new PayrollResultRepository(),
                new PayrollEarningRepository(),
                new PayrollBenefitRepository(),
                new PayrollDeductionRepository(),
                new EarningTypeRepository(),
                new BenefitTypeRepository(),
                new DeductionTypeRepository(),
                new SalaryHistoryRepository(),
                new EmployeeBenefitRepository(),
                new GovernmentDetailsRepository(),
                new SssRateRepository(),
                new PhilHealthRateRepository(),
                new PagIbigRateRepository(),
                new WithholdingTaxBracketRepository(),
                attendanceService,
                overtimeService,
                employeeService,
                new PayrollCalculator()
        );

        this.salaryBenefitService = new SalaryBenefitService(
                new SalaryHistoryRepository(),
                new EmployeeBenefitRepository()
        );
    }

    // getters
    PayrollService getPayrollService() {
        return payrollService;
    }

    SalaryBenefitService getSalaryBenefitService() {
        return salaryBenefitService;
    }
}
