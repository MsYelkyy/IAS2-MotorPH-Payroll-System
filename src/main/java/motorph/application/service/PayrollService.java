package motorph.application.service;

import motorph.application.dto.employee.EmployeeProfileDTO;
import motorph.application.dto.payroll.CompletedPayrunSummaryDTO;
import motorph.application.dto.payroll.GovernmentRatesDTO;
import motorph.application.dto.payroll.PayrollInputDTO;
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

    private final PayrunRepository payrunRepository;
    private final PayrollResultRepository payrollResultRepository;
    private final PayrollEarningRepository payrollEarningRepository;
    private final PayrollBenefitRepository payrollBenefitRepository;
    private final PayrollDeductionRepository payrollDeductionRepository;
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
        Employee employee = employeeService.getEmployeeById(employeeId);

        if (payrollResultRepository.existsByEmployeeAndPayrunId(employeeId, payrunId)) {
            throw new BusinessRuleException("Payroll already generated for this employee and payrun.");
        }

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

        PayrollInputDTO input = buildPayrollInput(employee, salary, governmentDetails, benefits);

        GovernmentRatesDTO rates = fetchEffectiveRates(payrun.getEndDate());

        PayrollBreakdown breakdown = payrollCalculator.computeDeductions(
                input,
                logs,
                approvedOvertimeHours,
                payrun.getEndDate(),
                rates
        );

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

        savePayrollComponents(payrollResultId, breakdown);

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
    private void savePayrollComponents(int payrollResultId, PayrollBreakdown breakdown) {
        payrollEarningRepository.save(new PayrollEarning(null, payrollResultId, 1, breakdown.getBasicPay()));

        saveEarningIfPresent(payrollResultId, 2, breakdown.getOvertimePay());

        saveBenefitIfPresent(payrollResultId, 1, breakdown.getRiceSubsidy());
        saveBenefitIfPresent(payrollResultId, 2, breakdown.getPhoneAllowance());
        saveBenefitIfPresent(payrollResultId, 3, breakdown.getClothingAllowance());

        saveDeductionIfPresent(payrollResultId, 1, breakdown.getSssDeduction(), false);
        saveDeductionIfPresent(payrollResultId, 2, breakdown.getPhilhealthDeduction(), false);
        saveDeductionIfPresent(payrollResultId, 3, breakdown.getPagibigDeduction(), false);
        saveDeductionIfPresent(payrollResultId, 4, breakdown.getWithholdingTax(), true);
        saveDeductionIfPresent(payrollResultId, 5, breakdown.getLateDeduction(), true);
        saveDeductionIfPresent(payrollResultId, 6, breakdown.getUndertimeDeduction(), true);
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
    private void saveDeductionIfPresent(int payrollResultId, int deductionTypeId, double amount, boolean taxable) {
        if (amount > 0) {
            payrollDeductionRepository.save(new PayrollDeduction(null, payrollResultId, deductionTypeId, amount, taxable));
        }
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
                                              List<EmployeeBenefit> benefits) {

        double rice = 0.0;
        double phone = 0.0;
        double clothing = 0.0;

        for (EmployeeBenefit benefit : benefits) {
            switch (benefit.getBenefitTypeId()) {
                case 1 -> rice += benefit.getAmount();
                case 2 -> phone += benefit.getAmount();
                case 3 -> clothing += benefit.getAmount();
                default -> {
                    // ignore other benefit types
                }
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

    // creates a new payrun; rejects duplicate periods
    public Payrun createPayrun(PayrunCreateDTO dto) {
        if (dto == null) {
            throw new ValidationException("Payrun create data is required.");
        }
        if (dto.getStartDate() == null || dto.getEndDate() == null || dto.getPayDate() == null) {
            throw new ValidationException("Start date, end date, and pay date are required.");
        }
        if (payrunRepository.existsByPeriod(dto.getStartDate(), dto.getEndDate())) {
            throw new BusinessRuleException("A payrun already exists for this period.");
        }
        Payrun payrun = new Payrun(null, dto.getStartDate(), dto.getEndDate(),
                dto.getPayDate(), dto.getPayrollType());
        int id = payrunRepository.saveAndReturnId(payrun);
        return payrunRepository.findById(id);
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

        int rid = result.getPayrollResultId();
        List<PayrollEarning>   earnings   = payrollEarningRepository.findByPayrollResultId(rid);
        List<PayrollBenefit>   benefits   = payrollBenefitRepository.findByPayrollResultId(rid);
        List<PayrollDeduction> deductions = payrollDeductionRepository.findByPayrollResultId(rid);

        double basicPay      = extractEarning(earnings, 1);
        double rice          = extractBenefit(benefits, 1);
        double phone         = extractBenefit(benefits, 2);
        double clothing      = extractBenefit(benefits, 3);
        double totalComp     = basicPay + extractEarning(earnings, 2) + rice + phone + clothing;

        double sssDeduction  = extractDeduction(deductions, 1);
        double phDeduction   = extractDeduction(deductions, 2);
        double pagibigDed    = extractDeduction(deductions, 3);
        double taxDeduction  = extractDeduction(deductions, 4);
        double lateDeduction = extractDeduction(deductions, 5);
        double undertimeDed  = extractDeduction(deductions, 6);

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