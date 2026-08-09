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
 * Manages salary history records and employee benefit allocations. Ported
 * from the desktop's SalaryBenefitService. addBenefit() was not ported -
 * grepping the desktop UI found no caller (dead code, same precedent as
 * other skipped methods this session); benefit creation only happens
 * through EmployeeService.createEmployeeWithSalaryAndBenefits/updateBenefits.
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

    // adds a new salary record; auto-closes any currently open salary to prevent overlap
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

    // updates only the start and end dates of an existing salary record
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

    // ends an active benefit by setting its effective end date
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
