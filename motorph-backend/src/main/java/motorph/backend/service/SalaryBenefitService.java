package motorph.backend.service;

import motorph.backend.dto.salary.SalaryRecordRequest;
import motorph.backend.exception.ValidationException;
import motorph.backend.domain.EmployeeBenefit;
import motorph.backend.domain.SalaryHistory;
import motorph.backend.domain.value.SalaryType;
import motorph.backend.repository.EmployeeBenefitRepository;
import motorph.backend.repository.SalaryHistoryRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Manages salary history and employee benefits. New benefits are created through {@link
 * EmployeeService}, not here.
 */
@Service
public class SalaryBenefitService {

    private final SalaryHistoryRepository salaryHistoryRepository;
    private final EmployeeBenefitRepository employeeBenefitRepository;

    public SalaryBenefitService(SalaryHistoryRepository salaryHistoryRepository,
                                EmployeeBenefitRepository employeeBenefitRepository) {
        this.salaryHistoryRepository = salaryHistoryRepository;
        this.employeeBenefitRepository = employeeBenefitRepository;
    }

    public List<SalaryHistory> getSalaryHistory(int employeeId) {
        validateId(employeeId, "Employee ID");
        return salaryHistoryRepository.findByEmployeeId(employeeId);
    }

    public SalaryHistory getCurrentSalary(int employeeId) {
        validateId(employeeId, "Employee ID");
        return salaryHistoryRepository.findCurrentByEmployeeId(employeeId, LocalDate.now());
    }

    /**
     * Adds a salary record and closes any currently open one so the periods do not overlap.
     */
    public void addSalaryRecord(int employeeId, SalaryRecordRequest request) {
        validateId(employeeId, "Employee ID");
        if (request.startDate() == null) throw new ValidationException("Start date is required.");
        if (request.salaryType() == null || request.salaryType().isBlank())
            throw new ValidationException("Salary type is required.");

        SalaryHistory current = salaryHistoryRepository.findCurrentByEmployeeId(employeeId, request.startDate());
        if (current != null && current.getEndDate() == null) {
            LocalDate closeDate = request.startDate().minusDays(1);
            salaryHistoryRepository.update(new SalaryHistory(
                    current.getSalaryHistoryId(),
                    current.getEmployeeId(),
                    current.getSalaryType(),
                    current.getBasicSalary(),
                    current.getHourlyRate(),
                    current.getStartDate(),
                    closeDate
            ));
        }

        salaryHistoryRepository.save(new SalaryHistory(
                null,
                employeeId,
                SalaryType.fromValue(request.salaryType()),
                request.basicSalary(),
                request.hourlyRate(),
                request.startDate(),
                request.endDate()
        ));
    }

    /**
     * Updates only the start and end dates of a salary record.
     */
    public void updateSalaryDates(int salaryHistoryId, LocalDate startDate, LocalDate endDate) {
        validateId(salaryHistoryId, "Salary history ID");
        if (startDate == null) throw new ValidationException("Start date is required.");

        SalaryHistory existing = salaryHistoryRepository.findById(salaryHistoryId);
        if (existing == null) throw new ValidationException("Salary record not found.");

        salaryHistoryRepository.update(new SalaryHistory(
                existing.getSalaryHistoryId(),
                existing.getEmployeeId(),
                existing.getSalaryType(),
                existing.getBasicSalary(),
                existing.getHourlyRate(),
                startDate,
                endDate
        ));
    }

    public List<EmployeeBenefit> getAllBenefits(int employeeId) {
        validateId(employeeId, "Employee ID");
        return employeeBenefitRepository.findByEmployeeId(employeeId);
    }

    public void endBenefit(int employeeBenefitId, LocalDate endDate) {
        validateId(employeeBenefitId, "Employee benefit ID");
        if (endDate == null) throw new ValidationException("End date is required.");

        EmployeeBenefit existing = employeeBenefitRepository.findById(employeeBenefitId);
        if (existing == null) throw new ValidationException("Benefit record not found.");

        employeeBenefitRepository.update(new EmployeeBenefit(
                existing.getEmployeeBenefitId(),
                existing.getEmployeeId(),
                existing.getBenefitTypeId(),
                existing.getAmount(),
                existing.getEffectiveStartDate(),
                endDate
        ));
    }

    private void validateId(int id, String label) {
        if (id <= 0) throw new ValidationException(label + " is required.");
    }
}
