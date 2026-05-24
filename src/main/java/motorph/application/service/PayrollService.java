package motorph.application.service;

import motorph.application.dto.payroll.PayrollInputDTO;
import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.domain.calculator.PayrollCalculator;
import motorph.domain.model.Attendance;
import motorph.domain.model.Employee;
import motorph.domain.model.EmployeeBenefit;
import motorph.domain.model.GovernmentDetails;
import motorph.domain.model.Payrun;
import motorph.domain.model.PayrollBreakdown;
import motorph.domain.model.PayrollResult;
import motorph.domain.model.SalaryHistory;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.GovernmentDetailsRepository;
import motorph.infrastructure.repository.PayrunRepository;
import motorph.infrastructure.repository.PayrollResultRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles payroll execution, including computation and result storage.
 */
public class PayrollService {

    private final PayrunRepository payrunRepository;
    private final PayrollResultRepository payrollResultRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final EmployeeBenefitRepository employeeBenefitRepository;
    private final GovernmentDetailsRepository governmentDetailsRepository;
    private final AttendanceService attendanceService;
    private final OvertimeService overtimeService;
    private final EmployeeService employeeService;
    private final PayrollCalculator payrollCalculator;

    // constructors
    public PayrollService(PayrunRepository payrunRepository,
                          PayrollResultRepository payrollResultRepository,
                          SalaryHistoryRepository salaryHistoryRepository,
                          EmployeeBenefitRepository employeeBenefitRepository,
                          GovernmentDetailsRepository governmentDetailsRepository,
                          AttendanceService attendanceService,
                          OvertimeService overtimeService,
                          EmployeeService employeeService,
                          PayrollCalculator payrollCalculator) {

        this.payrunRepository = require(payrunRepository, "PayrunRepository is required.");
        this.payrollResultRepository = require(payrollResultRepository, "PayrollResultRepository is required.");
        this.salaryHistoryRepository = require(salaryHistoryRepository, "SalaryHistoryRepository is required.");
        this.employeeBenefitRepository = require(employeeBenefitRepository, "EmployeeBenefitRepository is required.");
        this.governmentDetailsRepository = require(governmentDetailsRepository, "GovernmentDetailsRepository is required.");
        this.attendanceService = require(attendanceService, "AttendanceService is required.");
        this.overtimeService = require(overtimeService, "OvertimeService is required.");
        this.employeeService = require(employeeService, "EmployeeService is required.");
        this.payrollCalculator = require(payrollCalculator, "PayrollCalculator is required.");
    }

    // runs payroll for one employee
    public PayrollResult runPayrollForEmployee(int employeeId, int payrunId) {
        validateEmployeeId(employeeId);
        validatePayrunId(payrunId);

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

        double regularHoursWorked = logs.stream()
                .mapToDouble(Attendance::getHoursWorked)
                .sum();

        double approvedOvertimeHours = overtimeService.getApprovedOvertimeHours(
                employeeId,
                payrun.getStartDate(),
                payrun.getEndDate()
        );

        double totalHoursWorked = regularHoursWorked + approvedOvertimeHours;

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

        PayrollInputDTO input = buildPayrollInput(employee, salary, governmentDetails, benefits);

        PayrollBreakdown breakdown = payrollCalculator.computeDeductions(
                input,
                logs,
                approvedOvertimeHours,
                payrun.getEndDate()
        );

        PayrollResult result = new PayrollResult(
                null,
                employeeId,
                payrunId,
                totalHoursWorked,
                breakdown.getTotalEarnings(),
                breakdown.getTotalDeductions(),
                breakdown.getNetPay(),
                LocalDateTime.now()
        );

        payrollResultRepository.save(result);
        return result;
    }

    // gets payroll results for a payrun
    public List<PayrollResult> getPayrollResultsByPayrun(int payrunId) {
        validatePayrunId(payrunId);
        return payrollResultRepository.findByPayrunId(payrunId);
    }

    // gets payroll results for an employee
    public List<PayrollResult> getPayrollResultsByEmployee(int employeeId) {
        validateEmployeeId(employeeId);
        return payrollResultRepository.findByEmployeeId(employeeId);
    }

    // gets one payroll result
    public PayrollResult getPayrollResult(int employeeId, int payrunId) {
        validateEmployeeId(employeeId);
        validatePayrunId(payrunId);

        PayrollResult result = payrollResultRepository.findByEmployeeAndPayrunId(employeeId, payrunId);

        if (result == null) {
            throw new EntityNotFoundException("Payroll result not found.");
        }

        return result;
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

    // returns the payrun or throws when not found
    private Payrun getPayrunOrThrow(int payrunId) {
        Payrun payrun = payrunRepository.findById(payrunId);

        if (payrun == null) {
            throw new EntityNotFoundException("Payrun not found.");
        }

        return payrun;
    }

    // validates employee id input
    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    // validates payrun id input
    private void validatePayrunId(int payrunId) {
        if (payrunId <= 0) {
            throw new ValidationException("Payrun ID is required.");
        }
    }

    // validates required dependency
    private <T> T require(T obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }
}