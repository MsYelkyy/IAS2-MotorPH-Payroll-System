package motorph.backend.service;

import motorph.backend.domain.Attendance;
import motorph.backend.domain.DeductionType;
import motorph.backend.domain.Employee;
import motorph.backend.domain.EmployeeBenefit;
import motorph.backend.domain.GovernmentDetails;
import motorph.backend.domain.PayrollBenefit;
import motorph.backend.domain.PayrollBreakdown;
import motorph.backend.domain.PayrollDeduction;
import motorph.backend.domain.PayrollEarning;
import motorph.backend.domain.PayrollResult;
import motorph.backend.domain.Payrun;
import motorph.backend.domain.SalaryHistory;
import motorph.backend.domain.calculator.PayrollCalculator;
import motorph.backend.domain.value.ReadinessStatus;
import motorph.backend.dto.employee.EmployeeProfileDTO;
import motorph.backend.dto.payroll.CompletedPayrunSummaryDTO;
import motorph.backend.dto.payroll.GovernmentRatesDTO;
import motorph.backend.dto.payroll.PayrollInputDTO;
import motorph.backend.dto.payroll.PayrollPreviewDTO;
import motorph.backend.dto.payroll.PayrollSummaryDTO;
import motorph.backend.dto.payroll.PayrunCreateRequest;
import motorph.backend.dto.payroll.PayrunReadinessDTO;
import motorph.backend.dto.payroll.PendingPayrunSummaryDTO;
import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.ValidationException;
import motorph.backend.repository.BenefitTypeRepository;
import motorph.backend.repository.DeductionTypeRepository;
import motorph.backend.repository.EarningTypeRepository;
import motorph.backend.repository.EmployeeBenefitRepository;
import motorph.backend.repository.GovernmentDetailsRepository;
import motorph.backend.repository.PagIbigRateRepository;
import motorph.backend.repository.PayrollBenefitRepository;
import motorph.backend.repository.PayrollDeductionRepository;
import motorph.backend.repository.PayrollEarningRepository;
import motorph.backend.repository.PayrollResultRepository;
import motorph.backend.repository.PayrunRepository;
import motorph.backend.repository.PhilHealthRateRepository;
import motorph.backend.repository.SalaryHistoryRepository;
import motorph.backend.repository.SssRateRepository;
import motorph.backend.repository.WithholdingTaxBracketRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles payroll computation and persistence, payrun creation, and the pending and completed
 * payrun views.
 */
@Service
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

        this.payrunRepository = payrunRepository;
        this.payrollResultRepository = payrollResultRepository;
        this.payrollEarningRepository = payrollEarningRepository;
        this.payrollBenefitRepository = payrollBenefitRepository;
        this.payrollDeductionRepository = payrollDeductionRepository;
        this.earningTypeRepository = earningTypeRepository;
        this.benefitTypeRepository = benefitTypeRepository;
        this.deductionTypeRepository = deductionTypeRepository;
        this.salaryHistoryRepository = salaryHistoryRepository;
        this.employeeBenefitRepository = employeeBenefitRepository;
        this.governmentDetailsRepository = governmentDetailsRepository;
        this.sssRateRepository = sssRateRepository;
        this.philHealthRateRepository = philHealthRateRepository;
        this.pagIbigRateRepository = pagIbigRateRepository;
        this.withholdingTaxBracketRepository = withholdingTaxBracketRepository;
        this.attendanceService = attendanceService;
        this.overtimeService = overtimeService;
        this.employeeService = employeeService;
        this.payrollCalculator = payrollCalculator;
    }

    /**
     * Computes and saves payroll for one employee in a payrun. Fails if a result already exists for
     * that employee and payrun.
     */
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

    /**
     * Computes payroll for one employee without saving it.
     */
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

    private static final class ComputedPayroll {
        private final PayrollBreakdown breakdown;
        private final double totalHoursWorked;

        private ComputedPayroll(PayrollBreakdown breakdown, double totalHoursWorked) {
            this.breakdown = breakdown;
            this.totalHoursWorked = totalHoursWorked;
        }
    }

    public List<PayrollResult> getPayrollResultsByEmployee(int employeeId) {
        validate(employeeId, "Employee ID");
        return payrollResultRepository.findByEmployeeId(employeeId);
    }

    public Payrun getPayrun(int payrunId) {
        validate(payrunId, "Payrun ID");
        return payrunRepository.findById(payrunId);
    }

    /**
     * Saves the earning, benefit, and deduction rows that make up a payroll result.
     */
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

    private void saveEarningIfPresent(int payrollResultId, int earningTypeId, double amount) {
        if (amount > 0) {
            payrollEarningRepository.save(new PayrollEarning(null, payrollResultId, earningTypeId, amount));
        }
    }

    private void saveBenefitIfPresent(int payrollResultId, int benefitTypeId, double amount) {
        if (amount > 0) {
            payrollBenefitRepository.save(new PayrollBenefit(null, payrollResultId, benefitTypeId, amount));
        }
    }

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

    /**
     * Resolves the component type IDs needed to save a payroll result. This runs before anything is
     * saved, so a missing type fails the run early.
     */
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

    private int requireEarningTypeId(String code) {
        Integer typeId = earningTypeRepository.findIdByCode(code);
        if (typeId == null) {
            throw new EntityNotFoundException("Earning type not found for code: " + code + ".");
        }
        return typeId;
    }

    private int requireBenefitTypeId(String code) {
        Integer typeId = benefitTypeRepository.findIdByCode(code);
        if (typeId == null) {
            throw new EntityNotFoundException("Benefit type not found for code: " + code + ".");
        }
        return typeId;
    }

    private DeductionType requireDeductionType(String code) {
        DeductionType deductionType = deductionTypeRepository.findByCode(code);
        if (deductionType == null) {
            throw new EntityNotFoundException("Deduction type not found for code: " + code + ".");
        }
        return deductionType;
    }

    /**
     * Loads the government contribution rates in effect on the cutoff date.
     */
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

    /**
     * Creates a payrun. Rejects periods that are invalid, in the future, or that duplicate or
     * overlap an existing payrun.
     */
    public Payrun createPayrun(PayrunCreateRequest request) {
        if (request == null) {
            throw new ValidationException("Payrun create data is required.");
        }
        if (request.getStartDate() == null || request.getEndDate() == null || request.getPayDate() == null) {
            throw new ValidationException("Start date, end date, and pay date are required.");
        }
        if (request.getPayrollType() == null) {
            throw new ValidationException("Payroll type is required.");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("End date cannot be before start date.");
        }
        if (request.getEndDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Payroll period must be complete before it can be created.");
        }
        validateSemiMonthlyPeriod(request.getStartDate(), request.getEndDate());
        if (payrunRepository.existsByPeriod(request.getStartDate(), request.getEndDate())) {
            throw new BusinessRuleException("A payrun already exists for this period.");
        }
        if (payrunRepository.existsOverlapping(request.getStartDate(), request.getEndDate())) {
            throw new BusinessRuleException("A payrun already overlaps with the selected period.");
        }
        Payrun payrun = new Payrun(null, request.getStartDate(), request.getEndDate(),
                request.getPayDate(), request.getPayrollType());
        int id = payrunRepository.saveAndReturnId(payrun);
        return payrunRepository.findById(id);
    }

    /**
     * Ensures the period is a standard semi-monthly cutoff: the 1st to the 15th, or the 16th to the
     * end of the month.
     */
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

    /**
     * Returns the payroll summaries for a payrun with employee names filled in.
     */
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

    private Payrun getPayrunOrThrow(int payrunId) {
        Payrun payrun = payrunRepository.findById(payrunId);

        if (payrun == null) {
            throw new EntityNotFoundException("Payrun not found.");
        }

        return payrun;
    }

    /**
     * Returns the employee's full name, or a fallback based on the ID if the employee cannot be
     * loaded.
     */
    private String resolveEmployeeName(int employeeId) {
        try {
            EmployeeProfileDTO profile = employeeService.getEmployeeProfileAdmin(employeeId);
            return profile != null ? profile.getFullName() : "Employee #" + employeeId;
        } catch (Exception e) {
            return "Employee #" + employeeId;
        }
    }

    private void validate(int id, String fieldName) {
        if (id <= 0) {
            throw new ValidationException(fieldName + " is required.");
        }
    }

    /**
     * Returns all payruns, most recent first.
     */
    public List<Payrun> getAllPayruns() {
        return payrunRepository.findAll();
    }

    public List<Payrun> getPayrunsWithPayrollData() {
        return payrunRepository.findAllWithPayrollResults();
    }


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
                // Fall back to placeholders if the profile cannot be loaded.
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

    /**
     * Computes an employee's attendance readiness, days worked, and total hours for the payrun
     * period.
     */
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
