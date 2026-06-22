package motorph.application.service;

import motorph.application.dto.employee.EmployeeProfileDTO;
import motorph.application.dto.payroll.CompletedPayrunSummaryDTO;
import motorph.application.dto.payroll.GovernmentRatesDTO;
import motorph.application.dto.payroll.PayrollInputDTO;
import motorph.application.dto.payroll.PayrollPreviewDTO;
import motorph.application.dto.payroll.PayrollSummaryDTO;
import motorph.application.dto.payroll.PayrunCreateDTO;
import motorph.application.dto.payroll.PayrunReadinessDTO;
import motorph.application.dto.payroll.PendingPayrunSummaryDTO;
import motorph.application.dto.payroll.PayslipDTO;
import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.domain.calculator.PayrollCalculator;
import motorph.domain.model.Attendance;
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
import motorph.domain.value.ReadinessStatus;
import motorph.infrastructure.repository.BenefitTypeRepository;
import motorph.infrastructure.repository.DeductionTypeRepository;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.EarningTypeRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles payroll execution, including computation and result storage.
 */
public class PayrollService {

    private static final String BASIC_PAY_CODE = "BASIC_PAY";
    private static final String OVERTIME_PAY_CODE = "OVERTIME_PAY";
    private static final String RICE_SUBSIDY_CODE = "RICE_SUBSIDY";
    private static final String PHONE_ALLOWANCE_CODE = "PHONE_ALLOWANCE";
    private static final String CLOTHING_ALLOWANCE_CODE = "CLOTHING_ALLOWANCE";
    private static final String SSS_CODE = "SSS";
    private static final String PHILHEALTH_CODE = "PHILHEALTH";
    private static final String PAGIBIG_CODE = "PAGIBIG";
    private static final String WITHHOLDING_TAX_CODE = "WITHHOLDING_TAX";
    private static final String LATE_CODE = "LATE";
    private static final String UNDERTIME_CODE = "UNDERTIME";

    private final PayrunRepository payrunRepository;
    private final PayrollResultRepository payrollResultRepository;
    private final PayrollEarningRepository payrollEarningRepository;
    private final PayrollBenefitRepository payrollBenefitRepository;
    private final PayrollDeductionRepository payrollDeductionRepository;
    private final EarningTypeRepository earningTypeRepository;
    private final BenefitTypeRepository benefitTypeRepository;
    private final DeductionTypeRepository deductionTypeRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final EmployeeBenefitRepository employeeBenefitRepository;
    private final GovernmentDetailsRepository governmentDetailsRepository;
    private final SssRateRepository sssRateRepository;
    private final PhilHealthRateRepository philHealthRateRepository;
    private final PagIbigRateRepository pagIbigRateRepository;
    private final WithholdingTaxBracketRepository withholdingTaxBracketRepository;
    private final AttendanceService attendanceService;
    private final OvertimeService overtimeService;
    private final EmployeeService employeeService;
    private final PayrollCalculator payrollCalculator;

    // constructors
    public PayrollService(PayrunRepository payrunRepository,
                          PayrollResultRepository payrollResultRepository,
                          PayrollEarningRepository payrollEarningRepository,
                          PayrollBenefitRepository payrollBenefitRepository,
                          PayrollDeductionRepository payrollDeductionRepository,
                          EarningTypeRepository earningTypeRepository,
                          BenefitTypeRepository benefitTypeRepository,
                          DeductionTypeRepository deductionTypeRepository,
                          SalaryHistoryRepository salaryHistoryRepository,
                          EmployeeBenefitRepository employeeBenefitRepository,
                          GovernmentDetailsRepository governmentDetailsRepository,
                          SssRateRepository sssRateRepository,
                          PhilHealthRateRepository philHealthRateRepository,
                          PagIbigRateRepository pagIbigRateRepository,
                          WithholdingTaxBracketRepository withholdingTaxBracketRepository,
                          AttendanceService attendanceService,
                          OvertimeService overtimeService,
                          EmployeeService employeeService,
                          PayrollCalculator payrollCalculator) {

        this.payrunRepository = require(payrunRepository, "PayrunRepository is required.");
        this.payrollResultRepository = require(payrollResultRepository, "PayrollResultRepository is required.");
        this.payrollEarningRepository = require(payrollEarningRepository, "PayrollEarningRepository is required.");
        this.payrollBenefitRepository = require(payrollBenefitRepository, "PayrollBenefitRepository is required.");
        this.payrollDeductionRepository = require(payrollDeductionRepository, "PayrollDeductionRepository is required.");
        this.earningTypeRepository = require(earningTypeRepository, "EarningTypeRepository is required.");
        this.benefitTypeRepository = require(benefitTypeRepository, "BenefitTypeRepository is required.");
        this.deductionTypeRepository = require(deductionTypeRepository, "DeductionTypeRepository is required.");
        this.salaryHistoryRepository = require(salaryHistoryRepository, "SalaryHistoryRepository is required.");
        this.employeeBenefitRepository = require(employeeBenefitRepository, "EmployeeBenefitRepository is required.");
        this.governmentDetailsRepository = require(governmentDetailsRepository, "GovernmentDetailsRepository is required.");
        this.sssRateRepository = require(sssRateRepository, "SssRateRepository is required.");
        this.philHealthRateRepository = require(philHealthRateRepository, "PhilHealthRateRepository is required.");
        this.pagIbigRateRepository = require(pagIbigRateRepository, "PagIbigRateRepository is required.");
        this.withholdingTaxBracketRepository = require(withholdingTaxBracketRepository, "WithholdingTaxBracketRepository is required.");
        this.attendanceService = require(attendanceService, "AttendanceService is required.");
        this.overtimeService = require(overtimeService, "OvertimeService is required.");
        this.employeeService = require(employeeService, "EmployeeService is required.");
        this.payrollCalculator = require(payrollCalculator, "PayrollCalculator is required.");
    }

    // runs payroll for one employee
    public PayrollResult runPayrollForEmployee(int employeeId, int payrunId) {
        validate(employeeId, "Employee ID");
        validate(payrunId, "Payrun ID");

        Payrun payrun = getPayrunOrThrow(payrunId);

        if (payrollResultRepository.existsByEmployeeAndPayrunId(employeeId, payrunId)) {
            throw new BusinessRuleException("Payroll already generated for this employee and payrun.");
        }

        ComponentTypes componentTypes = resolveComponentTypes();
        ComputedPayroll computed = computeBreakdown(employeeId, payrun, componentTypes);
        PayrollBreakdown breakdown = computed.breakdown;
        double totalHoursWorked = computed.totalHoursWorked;

        LocalDateTime generatedAt = LocalDateTime.now();

        PayrollResult result = new PayrollResult(
                null,
                employeeId,
                payrunId,
                totalHoursWorked,
                breakdown.getTotalEarnings(),
                breakdown.getTotalDeductions(),
                breakdown.getNetPay(),
                generatedAt
        );

        int payrollResultId = payrollResultRepository.save(result);

        savePayrollComponents(payrollResultId, breakdown, componentTypes);

        return new PayrollResult(
                payrollResultId,
                employeeId,
                payrunId,
                totalHoursWorked,
                breakdown.getTotalEarnings(),
                breakdown.getTotalDeductions(),
                breakdown.getNetPay(),
                generatedAt
        );
    }

    // computes a payroll preview without persisting any result
    public PayrollPreviewDTO previewPayrollForEmployee(int employeeId, int payrunId) {
        validate(employeeId, "Employee ID");
        validate(payrunId, "Payrun ID");

        Payrun payrun = getPayrunOrThrow(payrunId);
        ComponentTypes componentTypes = resolveComponentTypes();
        ComputedPayroll computed = computeBreakdown(employeeId, payrun, componentTypes);
        PayrollBreakdown breakdown = computed.breakdown;

        return new PayrollPreviewDTO(
                employeeId,
                resolveEmployeeName(employeeId),
                payrun.getStartDate(),
                payrun.getEndDate(),
                computed.totalHoursWorked,
                breakdown.getTotalEarnings(),
                breakdown.getTotalDeductions(),
                breakdown.getNetPay()
        );
    }

    // fetches employee data and computes the payroll breakdown without saving
    private ComputedPayroll computeBreakdown(int employeeId, Payrun payrun, ComponentTypes componentTypes) {
        Employee employee = employeeService.getEmployeeById(employeeId);

        List<Attendance> logs = attendanceService.getAttendanceByEmployeeAndDateRange(
                employeeId,
                payrun.getStartDate(),
                payrun.getEndDate()
        );

        SalaryHistory salary = salaryHistoryRepository.findCurrentByEmployeeId(
                employeeId,
                payrun.getEndDate()
        );

        if (salary == null) {
            throw new EntityNotFoundException("Salary history not found.");
        }

        GovernmentDetails governmentDetails = governmentDetailsRepository.findByEmployeeId(employeeId);

        if (governmentDetails == null) {
            throw new EntityNotFoundException("Government details not found.");
        }

        List<EmployeeBenefit> benefits = employeeBenefitRepository.findActiveByEmployeeId(
                employeeId,
                payrun.getEndDate()
        );

        double regularHoursWorked = logs.stream()
                .mapToDouble(PayrollCalculator::calculatePaidRegularHours)
                .sum();

        double approvedOvertimeHours = overtimeService.getApprovedOvertimeHours(
                employeeId,
                payrun.getStartDate(),
                payrun.getEndDate()
        );

        double totalHoursWorked = regularHoursWorked + approvedOvertimeHours;

        PayrollInputDTO input = buildPayrollInput(employee, salary, governmentDetails, benefits, componentTypes);

        GovernmentRatesDTO rates = fetchEffectiveRates(payrun.getEndDate());

        PayrollBreakdown breakdown = payrollCalculator.computeDeductions(
                input,
                logs,
                approvedOvertimeHours,
                payrun.getEndDate(),
                rates
        );

        return new ComputedPayroll(breakdown, totalHoursWorked);
    }

    // holds a computed breakdown and its total hours before persistence
    private static final class ComputedPayroll {
        private final PayrollBreakdown breakdown;
        private final double totalHoursWorked;

        // constructors
        private ComputedPayroll(PayrollBreakdown breakdown, double totalHoursWorked) {
            this.breakdown = breakdown;
            this.totalHoursWorked = totalHoursWorked;
        }
    }

    // gets payroll results by payrun id
    public List<PayrollResult> getPayrollResult(int payrunId) {
        validate(payrunId, "Payrun ID");
        return payrollResultRepository.findByPayrunId(payrunId);
    }

    // gets payroll results for an employee
    public List<PayrollResult> getPayrollResultsByEmployee(int employeeId) {
        validate(employeeId, "Employee ID");
        return payrollResultRepository.findByEmployeeId(employeeId);
    }

    // gets payroll result by employee and payrun
    public PayrollResult getPayrollResult(int employeeId, int payrunId) {
        validate(employeeId, "Employee ID");
        validate(payrunId, "Payrun ID");

        PayrollResult result = payrollResultRepository.findByEmployeeAndPayrunId(employeeId, payrunId);

        if (result == null) {
            throw new EntityNotFoundException("Payroll result not found.");
        }

        return result;
    }

    // returns the payrun by id
    public Payrun getPayrun(int payrunId) {
        validate(payrunId, "Payrun ID");
        return payrunRepository.findById(payrunId);
    }

    // saves payroll component records for reporting
    private void savePayrollComponents(int payrollResultId, PayrollBreakdown breakdown, ComponentTypes componentTypes) {
        payrollEarningRepository.save(new PayrollEarning(
                null, payrollResultId, componentTypes.basicPayEarningTypeId, breakdown.getBasicPay()));

        saveEarningIfPresent(payrollResultId, componentTypes.overtimePayEarningTypeId, breakdown.getOvertimePay());

        saveBenefitIfPresent(payrollResultId, componentTypes.riceSubsidyBenefitTypeId, breakdown.getRiceSubsidy());
        saveBenefitIfPresent(payrollResultId, componentTypes.phoneAllowanceBenefitTypeId, breakdown.getPhoneAllowance());
        saveBenefitIfPresent(payrollResultId, componentTypes.clothingAllowanceBenefitTypeId, breakdown.getClothingAllowance());

        saveDeductionIfPresent(payrollResultId, componentTypes.sssDeductionType, breakdown.getSssDeduction());
        saveDeductionIfPresent(payrollResultId, componentTypes.philhealthDeductionType, breakdown.getPhilhealthDeduction());
        saveDeductionIfPresent(payrollResultId, componentTypes.pagibigDeductionType, breakdown.getPagibigDeduction());
        saveDeductionIfPresent(payrollResultId, componentTypes.withholdingTaxDeductionType, breakdown.getWithholdingTax());
        saveDeductionIfPresent(payrollResultId, componentTypes.lateDeductionType, breakdown.getLateDeduction());
        saveDeductionIfPresent(payrollResultId, componentTypes.undertimeDeductionType, breakdown.getUndertimeDeduction());
    }

    // saves earning only when amount exists
    private void saveEarningIfPresent(int payrollResultId, int earningTypeId, double amount) {
        if (amount > 0) {
            payrollEarningRepository.save(new PayrollEarning(null, payrollResultId, earningTypeId, amount));
        }
    }

    // saves benefit only when amount exists
    private void saveBenefitIfPresent(int payrollResultId, int benefitTypeId, double amount) {
        if (amount > 0) {
            payrollBenefitRepository.save(new PayrollBenefit(null, payrollResultId, benefitTypeId, amount));
        }
    }

    // saves deduction only when amount exists
    private void saveDeductionIfPresent(int payrollResultId, DeductionType deductionType, double amount) {
        if (amount > 0) {
            payrollDeductionRepository.save(new PayrollDeduction(
                    null,
                    payrollResultId,
                    deductionType.getDeductionTypeId(),
                    amount,
                    deductionType.isTaxable()
            ));
        }
    }

    /** Holds resolved identifiers for required payroll component types. */
    private static final class ComponentTypes {
        private final int basicPayEarningTypeId;
        private final int overtimePayEarningTypeId;
        private final int riceSubsidyBenefitTypeId;
        private final int phoneAllowanceBenefitTypeId;
        private final int clothingAllowanceBenefitTypeId;
        private final DeductionType sssDeductionType;
        private final DeductionType philhealthDeductionType;
        private final DeductionType pagibigDeductionType;
        private final DeductionType withholdingTaxDeductionType;
        private final DeductionType lateDeductionType;
        private final DeductionType undertimeDeductionType;

        // constructors
        private ComponentTypes(int basicPayEarningTypeId,
                               int overtimePayEarningTypeId,
                               int riceSubsidyBenefitTypeId,
                               int phoneAllowanceBenefitTypeId,
                               int clothingAllowanceBenefitTypeId,
                               DeductionType sssDeductionType,
                               DeductionType philhealthDeductionType,
                               DeductionType pagibigDeductionType,
                               DeductionType withholdingTaxDeductionType,
                               DeductionType lateDeductionType,
                               DeductionType undertimeDeductionType) {
            this.basicPayEarningTypeId = basicPayEarningTypeId;
            this.overtimePayEarningTypeId = overtimePayEarningTypeId;
            this.riceSubsidyBenefitTypeId = riceSubsidyBenefitTypeId;
            this.phoneAllowanceBenefitTypeId = phoneAllowanceBenefitTypeId;
            this.clothingAllowanceBenefitTypeId = clothingAllowanceBenefitTypeId;
            this.sssDeductionType = sssDeductionType;
            this.philhealthDeductionType = philhealthDeductionType;
            this.pagibigDeductionType = pagibigDeductionType;
            this.withholdingTaxDeductionType = withholdingTaxDeductionType;
            this.lateDeductionType = lateDeductionType;
            this.undertimeDeductionType = undertimeDeductionType;
        }
    }

    // resolves all required payroll component types before result persistence
    private ComponentTypes resolveComponentTypes() {
        return new ComponentTypes(
                requireEarningTypeId(BASIC_PAY_CODE),
                requireEarningTypeId(OVERTIME_PAY_CODE),
                requireBenefitTypeId(RICE_SUBSIDY_CODE),
                requireBenefitTypeId(PHONE_ALLOWANCE_CODE),
                requireBenefitTypeId(CLOTHING_ALLOWANCE_CODE),
                requireDeductionType(SSS_CODE),
                requireDeductionType(PHILHEALTH_CODE),
                requireDeductionType(PAGIBIG_CODE),
                requireDeductionType(WITHHOLDING_TAX_CODE),
                requireDeductionType(LATE_CODE),
                requireDeductionType(UNDERTIME_CODE)
        );
    }

    // requires an earning type identifier for a stable code
    private int requireEarningTypeId(String code) {
        Integer typeId = earningTypeRepository.findIdByCode(code);
        if (typeId == null) {
            throw new EntityNotFoundException("Earning type not found for code: " + code + ".");
        }
        return typeId;
    }

    // requires a benefit type identifier for a stable code
    private int requireBenefitTypeId(String code) {
        Integer typeId = benefitTypeRepository.findIdByCode(code);
        if (typeId == null) {
            throw new EntityNotFoundException("Benefit type not found for code: " + code + ".");
        }
        return typeId;
    }

    // requires a deduction type for a stable code
    private DeductionType requireDeductionType(String code) {
        DeductionType deductionType = deductionTypeRepository.findByCode(code);
        if (deductionType == null) {
            throw new EntityNotFoundException("Deduction type not found for code: " + code + ".");
        }
        return deductionType;
    }

    // fetches government contribution rates effective on the given cutoff date
    private GovernmentRatesDTO fetchEffectiveRates(LocalDate cutoffDate) {
        var sss = sssRateRepository.findEffectiveOn(cutoffDate);
        if (sss == null) {
            throw new EntityNotFoundException("No effective SSS rate found for " + cutoffDate + ".");
        }

        var philHealth = philHealthRateRepository.findEffectiveOn(cutoffDate);
        if (philHealth == null) {
            throw new EntityNotFoundException("No effective PhilHealth rate found for " + cutoffDate + ".");
        }

        var pagIbig = pagIbigRateRepository.findEffectiveOn(cutoffDate);
        if (pagIbig.isEmpty()) {
            throw new EntityNotFoundException("No effective Pag-IBIG rates found for " + cutoffDate + ".");
        }

        var taxBrackets = withholdingTaxBracketRepository.findEffectiveOn(cutoffDate);
        if (taxBrackets.isEmpty()) {
            throw new EntityNotFoundException("No effective withholding tax brackets found for " + cutoffDate + ".");
        }

        return new GovernmentRatesDTO(sss, philHealth, pagIbig, taxBrackets);
    }

    // builds payroll input from employee, salary, government details, and benefits
    private PayrollInputDTO buildPayrollInput(Employee employee,
                                              SalaryHistory salary,
                                              GovernmentDetails governmentDetails,
                                              List<EmployeeBenefit> benefits,
                                              ComponentTypes componentTypes) {

        double rice = 0.0;
        double phone = 0.0;
        double clothing = 0.0;

        for (EmployeeBenefit benefit : benefits) {
            if (benefit.getBenefitTypeId() == componentTypes.riceSubsidyBenefitTypeId) {
                rice += benefit.getAmount();
            } else if (benefit.getBenefitTypeId() == componentTypes.phoneAllowanceBenefitTypeId) {
                phone += benefit.getAmount();
            } else if (benefit.getBenefitTypeId() == componentTypes.clothingAllowanceBenefitTypeId) {
                clothing += benefit.getAmount();
            }
        }

        return new PayrollInputDTO(
                employee.getEmployeeId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getPositionId(),
                employee.getSupervisorId(),
                null,
                null,
                governmentDetails.getTinNumber(),
                governmentDetails.getSssNumber(),
                governmentDetails.getPhilhealthNumber(),
                governmentDetails.getPagibigNumber(),
                salary.getBasicSalary(),
                salary.getGrossSemiMonthly(),
                salary.getHourlyRate(),
                rice,
                phone,
                clothing
        );
    }

    // creates a new payrun; rejects duplicate, overlapping, invalid, and future periods
    public Payrun createPayrun(PayrunCreateDTO dto) {
        if (dto == null) {
            throw new ValidationException("Payrun create data is required.");
        }
        if (dto.getStartDate() == null || dto.getEndDate() == null || dto.getPayDate() == null) {
            throw new ValidationException("Start date, end date, and pay date are required.");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new ValidationException("End date cannot be before start date.");
        }
        if (dto.getEndDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Payrun period must be complete before it can be created.");
        }
        validateSemiMonthlyPeriod(dto.getStartDate(), dto.getEndDate());
        if (payrunRepository.existsByPeriod(dto.getStartDate(), dto.getEndDate())) {
            throw new BusinessRuleException("A payrun already exists for this period.");
        }
        if (payrunRepository.existsOverlapping(dto.getStartDate(), dto.getEndDate())) {
            throw new BusinessRuleException("A payrun already overlaps with the selected period.");
        }
        Payrun payrun = new Payrun(null, dto.getStartDate(), dto.getEndDate(),
                dto.getPayDate(), dto.getPayrollType());
        int id = payrunRepository.saveAndReturnId(payrun);
        return payrunRepository.findById(id);
    }

    // enforces standard semi-monthly cutoffs: 1st-15th or 16th-end of month
    private void validateSemiMonthlyPeriod(LocalDate start, LocalDate end) {
        if (start.getYear() != end.getYear() || start.getMonthValue() != end.getMonthValue()) {
            throw new ValidationException(
                    "Payroll period must stay within a single calendar month.");
        }
        boolean firstHalf = start.getDayOfMonth() == 1 && end.getDayOfMonth() == 15;
        boolean secondHalf = start.getDayOfMonth() == 16
                && end.getDayOfMonth() == end.lengthOfMonth();
        if (!firstHalf && !secondHalf) {
            throw new ValidationException(
                    "Payroll period must be the 1st-15th or 16th-end of the month.");
        }
    }

    // returns payroll summaries for a payrun enriched with employee full names
    public List<PayrollSummaryDTO> getPayrollSummaries(int payrunId) {
        validate(payrunId, "Payrun ID");
        Payrun payrun = getPayrunOrThrow(payrunId);
        List<PayrollResult> results = payrollResultRepository.findByPayrunId(payrunId);
        List<PayrollSummaryDTO> summaries = new ArrayList<>();
        for (PayrollResult r : results) {
            String name = resolveEmployeeName(r.getEmployeeId());
            summaries.add(new PayrollSummaryDTO(
                    r.getPayrollResultId(),
                    r.getEmployeeId(),
                    r.getPayrunId(),
                    name,
                    payrun.getPayDate(),
                    payrun.getStartDate(),
                    payrun.getEndDate(),
                    r.getGrossPay(),
                    r.getTotalDeductions(),
                    r.getNetPay()
            ));
        }
        return summaries;
    }

    // assembles a full payslip DTO from persisted payroll components
    public PayslipDTO buildPayslip(int employeeId, int payrunId) {
        validate(employeeId, "Employee ID");
        validate(payrunId, "Payrun ID");

        PayrollResult result = payrollResultRepository.findByEmployeeAndPayrunId(employeeId, payrunId);
        if (result == null) {
            throw new EntityNotFoundException("Payroll result not found.");
        }

        Payrun payrun = getPayrunOrThrow(payrunId);
        EmployeeProfileDTO profile = employeeService.getEmployeeProfileAdmin(employeeId);
        if (profile == null) {
            throw new EntityNotFoundException("Employee not found.");
        }

        GovernmentDetails govt = governmentDetailsRepository.findByEmployeeId(employeeId);

        ComponentTypes componentTypes = resolveComponentTypes();

        int rid = result.getPayrollResultId();
        List<PayrollEarning>   earnings   = payrollEarningRepository.findByPayrollResultId(rid);
        List<PayrollBenefit>   benefits   = payrollBenefitRepository.findByPayrollResultId(rid);
        List<PayrollDeduction> deductions = payrollDeductionRepository.findByPayrollResultId(rid);

        double basicPay      = extractEarning(earnings, componentTypes.basicPayEarningTypeId);
        double overtimePay   = extractEarning(earnings, componentTypes.overtimePayEarningTypeId);
        double rice          = extractBenefit(benefits, componentTypes.riceSubsidyBenefitTypeId);
        double phone         = extractBenefit(benefits, componentTypes.phoneAllowanceBenefitTypeId);
        double clothing      = extractBenefit(benefits, componentTypes.clothingAllowanceBenefitTypeId);
        double totalComp     = basicPay + overtimePay + rice + phone + clothing;

        double sssDeduction  = extractDeduction(deductions, componentTypes.sssDeductionType.getDeductionTypeId());
        double phDeduction   = extractDeduction(deductions, componentTypes.philhealthDeductionType.getDeductionTypeId());
        double pagibigDed    = extractDeduction(deductions, componentTypes.pagibigDeductionType.getDeductionTypeId());
        double taxDeduction  = extractDeduction(deductions, componentTypes.withholdingTaxDeductionType.getDeductionTypeId());
        double lateDeduction = extractDeduction(deductions, componentTypes.lateDeductionType.getDeductionTypeId());
        double undertimeDed  = extractDeduction(deductions, componentTypes.undertimeDeductionType.getDeductionTypeId());

        String tin     = govt != null ? govt.getTinNumber()        : "—";
        String sss     = govt != null ? govt.getSssNumber()        : "—";
        String philh   = govt != null ? govt.getPhilhealthNumber() : "—";
        String pagibig = govt != null ? govt.getPagibigNumber()    : "—";
        String supervisor = profile.getSupervisorName() != null ? profile.getSupervisorName() : "—";

        return new PayslipDTO(
                employeeId,
                profile.getFullName(),
                profile.getPositionName(),
                supervisor,
                payrun.getPayDate(),
                payrun.getStartDate(),
                payrun.getEndDate(),
                tin, sss, philh, pagibig,
                basicPay, rice, phone, clothing, totalComp,
                lateDeduction, undertimeDed, 0.0,
                sssDeduction, phDeduction, pagibigDed, taxDeduction,
                result.getTotalDeductions(), result.getNetPay(),
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        );
    }

    // returns the payrun or throws when not found
    private Payrun getPayrunOrThrow(int payrunId) {
        Payrun payrun = payrunRepository.findById(payrunId);

        if (payrun == null) {
            throw new EntityNotFoundException("Payrun not found.");
        }

        return payrun;
    }

    // resolves an employee's full name; falls back to id on error
    private String resolveEmployeeName(int employeeId) {
        try {
            EmployeeProfileDTO profile = employeeService.getEmployeeProfileAdmin(employeeId);
            return profile != null ? profile.getFullName() : "Employee #" + employeeId;
        } catch (Exception e) {
            return "Employee #" + employeeId;
        }
    }

    // sums earning amounts matching a type id
    private double extractEarning(List<PayrollEarning> list, int typeId) {
        return list.stream().filter(e -> e.getEarningTypeId() == typeId)
                .mapToDouble(PayrollEarning::getAmount).sum();
    }

    // sums benefit amounts matching a type id
    private double extractBenefit(List<PayrollBenefit> list, int typeId) {
        return list.stream().filter(b -> b.getBenefitTypeId() == typeId)
                .mapToDouble(PayrollBenefit::getAmount).sum();
    }

    // sums deduction amounts matching a type id
    private double extractDeduction(List<PayrollDeduction> list, int typeId) {
        return list.stream().filter(d -> d.getDeductionTypeId() == typeId)
                .mapToDouble(PayrollDeduction::getAmount).sum();
    }

    // validates id input with context
    private void validate(int id, String fieldName) {
        if (id <= 0) {
            throw new ValidationException(fieldName + " is required.");
        }
    }

    // validates required dependency
    private <T> T require(T obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }

        return obj;
    }

    // returns all payruns ordered by most recent first
    public List<Payrun> getAllPayruns() {
        return payrunRepository.findAll();
    }

    // returns only payruns with generated payroll results
    public List<Payrun> getPayrunsWithPayrollData() {
        return payrunRepository.findAllWithPayrollResults();
    }

    // runs payroll for all employees in one payrun
    public void runPayrollForPayrun(int payrunId) {
        validate(payrunId, "Payrun ID");

        List<Employee> employees = employeeService.getAllEmployees();

        for (Employee employee : employees) {
            if (!payrollResultRepository.existsByEmployeeAndPayrunId(employee.getEmployeeId(), payrunId)) {
                runPayrollForEmployee(employee.getEmployeeId(), payrunId);
            }
        }
    }

    // ----------------------------------------------------------------------
    // Payroll-date / payrun-centered workflow (pending vs completed)
    // ----------------------------------------------------------------------

    /** Returns pending payruns (no payroll results yet) with attendance readiness counts. */
    public List<PendingPayrunSummaryDTO> getPendingPayruns() {
        List<PendingPayrunSummaryDTO> pending = new ArrayList<>();
        List<Employee> activeEmployees = employeeService.getAllActiveEmployees();

        for (Payrun payrun : payrunRepository.findAll()) {
            if (!payrollResultRepository.findByPayrunId(payrun.getPayrunId()).isEmpty()) {
                continue; // already has results -> completed, not pending
            }

            Map<Integer, List<Attendance>> attendanceByEmployee = getAttendanceByEmployee(payrun);
            if (attendanceByEmployee.isEmpty()) {
                continue; // no activity in the payrun period -> do not show on Pending landing
            }

            int ready = 0;
            int incomplete = 0;
            int noAttendance = 0;
            for (Employee employee : activeEmployees) {
                switch (computeReadiness(attendanceByEmployee.get(employee.getEmployeeId())).status) {
                    case READY -> ready++;
                    case INCOMPLETE -> incomplete++;
                    case NO_ATTENDANCE -> noAttendance++;
                }
            }

            pending.add(new PendingPayrunSummaryDTO(
                    payrun.getPayrunId(),
                    payrun.getStartDate(),
                    payrun.getEndDate(),
                    payrun.getPayDate(),
                    payrun.getPayrollType(),
                    activeEmployees.size(),
                    ready,
                    incomplete,
                    noAttendance
            ));
        }
        return pending;
    }

    /** Returns completed payruns (with payroll results) and their aggregated totals. */
    public List<CompletedPayrunSummaryDTO> getCompletedPayruns() {
        List<CompletedPayrunSummaryDTO> completed = new ArrayList<>();

        for (Payrun payrun : payrunRepository.findAllWithPayrollResults()) {
            List<PayrollResult> results = payrollResultRepository.findByPayrunId(payrun.getPayrunId());

            double totalGross = results.stream().mapToDouble(PayrollResult::getGrossPay).sum();
            double totalDeductions = results.stream().mapToDouble(PayrollResult::getTotalDeductions).sum();
            double totalNet = results.stream().mapToDouble(PayrollResult::getNetPay).sum();

            completed.add(new CompletedPayrunSummaryDTO(
                    payrun.getPayrunId(),
                    payrun.getStartDate(),
                    payrun.getEndDate(),
                    payrun.getPayDate(),
                    payrun.getPayrollType(),
                    results.size(),
                    totalGross,
                    totalDeductions,
                    totalNet
            ));
        }
        return completed;
    }

    /** Returns per-employee attendance readiness rows for a payrun's pending detail view. */
    public List<PayrunReadinessDTO> getPayrunReadiness(int payrunId) {
        validate(payrunId, "Payrun ID");
        Payrun payrun = getPayrunOrThrow(payrunId);
        Map<Integer, List<Attendance>> attendanceByEmployee = getAttendanceByEmployee(payrun);

        List<PayrunReadinessDTO> rows = new ArrayList<>();
        for (Employee employee : employeeService.getAllActiveEmployees()) {
            ReadinessResult readiness = computeReadiness(attendanceByEmployee.get(employee.getEmployeeId()));

            String department = "—";
            String position = "—";
            try {
                EmployeeProfileDTO profile = employeeService.getEmployeeProfileAdmin(employee.getEmployeeId());
                if (profile != null) {
                    department = profile.getDepartmentName() != null ? profile.getDepartmentName() : "—";
                    position = profile.getPositionName() != null ? profile.getPositionName() : "—";
                }
            } catch (Exception ignored) {
                // fall back to placeholders when profile cannot be resolved
            }

            rows.add(new PayrunReadinessDTO(
                    employee.getEmployeeId(),
                    employee.getFullName(),
                    department,
                    position,
                    readiness.daysWorked,
                    readiness.totalHours,
                    readiness.status
            ));
        }
        return rows;
    }

    /**
     * Generates payroll for the payrun, processing only employees whose attendance
     * is READY and who do not already have a result. Returns the number generated.
     */
    public int generatePayrollForReadyEmployees(int payrunId) {
        validate(payrunId, "Payrun ID");
        Payrun payrun = getPayrunOrThrow(payrunId);

        int generated = 0;
        for (Employee employee : employeeService.getAllActiveEmployees()) {
            int employeeId = employee.getEmployeeId();
            if (computeReadiness(employeeId, payrun).status != ReadinessStatus.READY) {
                continue;
            }
            if (payrollResultRepository.existsByEmployeeAndPayrunId(employeeId, payrunId)) {
                continue;
            }
            runPayrollForEmployee(employeeId, payrunId);
            generated++;
        }
        return generated;
    }

    // computes an employee's attendance readiness, days worked, and total hours for a payrun period
    private ReadinessResult computeReadiness(int employeeId, Payrun payrun) {
        return computeReadiness(attendanceService.getAttendanceByEmployeeAndDateRange(
                employeeId, payrun.getStartDate(), payrun.getEndDate()));
    }

    private ReadinessResult computeReadiness(List<Attendance> logs) {
        if (logs == null || logs.isEmpty()) {
            return new ReadinessResult(ReadinessStatus.NO_ATTENDANCE, 0, 0.0);
        }

        boolean allComplete = logs.stream().allMatch(Attendance::isComplete);
        double totalHours = logs.stream().mapToDouble(Attendance::getHoursWorked).sum();

        ReadinessStatus status = allComplete ? ReadinessStatus.READY : ReadinessStatus.INCOMPLETE;
        return new ReadinessResult(status, logs.size(), totalHours);
    }

    private Map<Integer, List<Attendance>> getAttendanceByEmployee(Payrun payrun) {
        Map<Integer, List<Attendance>> attendanceByEmployee = new HashMap<>();
        List<Attendance> logs = attendanceService.getAttendanceByDateRange(
                payrun.getStartDate(), payrun.getEndDate());

        for (Attendance log : logs) {
            attendanceByEmployee
                    .computeIfAbsent(log.getEmployeeId(), id -> new ArrayList<>())
                    .add(log);
        }

        return attendanceByEmployee;
    }

    // holds the computed readiness for one employee in one payrun period
    private static final class ReadinessResult {
        private final ReadinessStatus status;
        private final int daysWorked;
        private final double totalHours;

        private ReadinessResult(ReadinessStatus status, int daysWorked, double totalHours) {
            this.status = status;
            this.daysWorked = daysWorked;
            this.totalHours = totalHours;
        }
    }
}
