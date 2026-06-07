package motorph.application.service;

import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.Employee;
import motorph.domain.model.PayrollBenefit;
import motorph.domain.model.PayrollDeduction;
import motorph.domain.model.PayrollEarning;
import motorph.domain.model.PayrollOvertime;
import motorph.domain.model.PayrollResult;
import motorph.domain.model.Payrun;
import motorph.domain.model.Payslip;
import motorph.infrastructure.repository.PayrollBenefitRepository;
import motorph.infrastructure.repository.PayrollDeductionRepository;
import motorph.infrastructure.repository.PayrollEarningRepository;
import motorph.infrastructure.repository.PayrollOvertimeRepository;
import motorph.infrastructure.repository.PayrollResultRepository;
import motorph.infrastructure.repository.PayrunRepository;
import motorph.infrastructure.reporting.JasperReportGenerator;

import net.sf.jasperreports.engine.JasperPrint;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Handles payslip generation from persisted payroll data.
 */
public class PayslipService {

    private final PayrollResultRepository payrollResultRepository;
    private final PayrollEarningRepository payrollEarningRepository;
    private final PayrollBenefitRepository payrollBenefitRepository;
    private final PayrollDeductionRepository payrollDeductionRepository;
    private final PayrollOvertimeRepository payrollOvertimeRepository;
    private final PayrunRepository payrunRepository;
    private final EmployeeService employeeService;
    private final JasperReportGenerator jasperReportGenerator;

    // constructors
    public PayslipService(PayrollResultRepository payrollResultRepository,
                          PayrollEarningRepository payrollEarningRepository,
                          PayrollBenefitRepository payrollBenefitRepository,
                          PayrollDeductionRepository payrollDeductionRepository,
                          PayrollOvertimeRepository payrollOvertimeRepository,
                          PayrunRepository payrunRepository,
                          EmployeeService employeeService,
                          JasperReportGenerator jasperReportGenerator) {

        this.payrollResultRepository = require(payrollResultRepository, "PayrollResultRepository is required.");
        this.payrollEarningRepository = require(payrollEarningRepository, "PayrollEarningRepository is required.");
        this.payrollBenefitRepository = require(payrollBenefitRepository, "PayrollBenefitRepository is required.");
        this.payrollDeductionRepository = require(payrollDeductionRepository, "PayrollDeductionRepository is required.");
        this.payrollOvertimeRepository = require(payrollOvertimeRepository, "PayrollOvertimeRepository is required.");
        this.payrunRepository = require(payrunRepository, "PayrunRepository is required.");
        this.employeeService = require(employeeService, "EmployeeService is required.");
        this.jasperReportGenerator = require(jasperReportGenerator, "JasperReportGenerator is required.");
    }

    // generates a payslip model from saved payroll data
    public Payslip generatePayslip(int employeeId, int payrunId) {
        validatePositive(employeeId, "Employee ID is required.");
        validatePositive(payrunId, "Payrun ID is required.");

        PayrollResult payrollResult = payrollResultRepository.findByEmployeeAndPayrunId(employeeId, payrunId);

        if (payrollResult == null) {
            throw new EntityNotFoundException("Payroll result not found.");
        }

        Employee employee = employeeService.getEmployeeById(employeeId);
        Payrun payrun = payrunRepository.findById(payrunId);

        if (payrun == null) {
            throw new EntityNotFoundException("Payrun not found.");
        }

        List<PayrollEarning> earnings = payrollEarningRepository.findByPayrollResultId(payrollResult.getPayrollResultId());
        List<PayrollBenefit> benefits = payrollBenefitRepository.findByPayrollResultId(payrollResult.getPayrollResultId());
        List<PayrollDeduction> deductions = payrollDeductionRepository.findByPayrollResultId(payrollResult.getPayrollResultId());
        List<PayrollOvertime> overtimeRecords = payrollOvertimeRepository.findByPayrollResultId(payrollResult.getPayrollResultId());

        double basicPay = getTotalEarnings(earnings);
        double benefitsTotal = getTotalBenefits(benefits);
        double overtimePay = getTotalOvertime(overtimeRecords);
        double deductionsTotal = getTotalDeductions(deductions);

        return new Payslip(
                employee.getEmployeeId(),
                employee.getFullName(),
                payrun.getPayrunId(),
                payrun.getPayrollType(),
                payrun.getStartDate(),
                payrun.getEndDate(),
                payrun.getPayDate(),
                basicPay,
                overtimePay,
                benefitsTotal,
                deductionsTotal,
                payrollResult.getGrossPay(),
                payrollResult.getNetPay(),
                payrollResult.getGeneratedAt()
        );
    }

    // previews a payslip using JasperReports
    public void previewPayslip(int employeeId, int payrunId) {
        Payslip payslip = generatePayslip(employeeId, payrunId);

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                "/motorph/reports/payslip.jrxml",
                new HashMap<>(),
                List.of(payslip)
        );

        jasperReportGenerator.showPreview(jasperPrint);
    }

    // exports a payslip to PDF
    public void exportPayslipToPdf(int employeeId, int payrunId, String outputPath) {
        if (outputPath == null || outputPath.isBlank()) {
            throw new ValidationException("Output path is required.");
        }

        Payslip payslip = generatePayslip(employeeId, payrunId);

        JasperPrint jasperPrint = jasperReportGenerator.generateReport(
                "/motorph/reports/payslip.jrxml",
                new HashMap<>(),
                List.of(payslip)
        );

        jasperReportGenerator.exportToPdf(jasperPrint, outputPath);
    }

    // totals payroll earnings
    private double getTotalEarnings(List<PayrollEarning> earnings) {
        return earnings.stream()
                .mapToDouble(PayrollEarning::getAmount)
                .sum();
    }

    // totals payroll benefits
    private double getTotalBenefits(List<PayrollBenefit> benefits) {
        return benefits.stream()
                .mapToDouble(PayrollBenefit::getAmount)
                .sum();
    }

    // totals payroll deductions
    private double getTotalDeductions(List<PayrollDeduction> deductions) {
        return deductions.stream()
                .mapToDouble(PayrollDeduction::getAmount)
                .sum();
    }

    // totals payroll overtime
    private double getTotalOvertime(List<PayrollOvertime> overtimeRecords) {
        return overtimeRecords.stream()
                .mapToDouble(PayrollOvertime::getAmount)
                .sum();
    }

    // validates positive input
    private void validatePositive(int value, String message) {
        if (value <= 0) {
            throw new ValidationException(message);
        }
    }

    // validates required dependency
    private <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }
}