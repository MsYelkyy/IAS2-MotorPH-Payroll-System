package motorph.application.service;

import motorph.application.dto.salary.EmployeeBenefitDTO;
import motorph.application.dto.salary.SalaryHistoryDTO;
import motorph.application.exception.ValidationException;
import motorph.domain.model.EmployeeBenefit;
import motorph.domain.model.SalaryHistory;
import motorph.domain.value.SalaryType;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;

import java.time.LocalDate;
import java.util.List;

/** Manages salary history records and employee benefit allocations. */
public class SalaryBenefitService {

    private final SalaryHistoryRepository   salaryHistoryRepository;
    private final EmployeeBenefitRepository employeeBenefitRepository;

    public SalaryBenefitService(SalaryHistoryRepository salaryHistoryRepository,
                                EmployeeBenefitRepository employeeBenefitRepository) {
        if (salaryHistoryRepository == null)
            throw new IllegalArgumentException("SalaryHistoryRepository is required.");
        if (employeeBenefitRepository == null)
            throw new IllegalArgumentException("EmployeeBenefitRepository is required.");
        this.salaryHistoryRepository   = salaryHistoryRepository;
        this.employeeBenefitRepository = employeeBenefitRepository;
    }

    // returns all salary records for an employee ordered by most recent first
    public List<SalaryHistory> getSalaryHistory(int employeeId) {
        validateId(employeeId, "Employee ID");
        return salaryHistoryRepository.findByEmployeeId(employeeId);
    }

    // returns the salary record effective today, or null if none
    public SalaryHistory getCurrentSalary(int employeeId) {
        validateId(employeeId, "Employee ID");
        return salaryHistoryRepository.findCurrentByEmployeeId(employeeId, LocalDate.now());
    }

    // adds a new salary record; auto-closes any currently open salary to prevent overlap
    public void addSalaryRecord(SalaryHistoryDTO dto) {
        if (dto == null) throw new ValidationException("Salary data is required.");
        validateId(dto.getEmployeeId(), "Employee ID");
        if (dto.getStartDate() == null) throw new ValidationException("Start date is required.");
        if (dto.getSalaryType() == null || dto.getSalaryType().isBlank())
            throw new ValidationException("Salary type is required.");

        SalaryHistory current = salaryHistoryRepository.findCurrentByEmployeeId(
                dto.getEmployeeId(), dto.getStartDate());
        if (current != null && current.getEndDate() == null) {
            LocalDate closeDate = dto.getStartDate().minusDays(1);
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
                dto.getEmployeeId(),
                SalaryType.fromValue(dto.getSalaryType()),
                dto.getBasicSalary(),
                dto.getHourlyRate(),
                dto.getStartDate(),
                dto.getEndDate()
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

    // returns all benefit records for an employee ordered by most recent first
    public List<EmployeeBenefit> getAllBenefits(int employeeId) {
        validateId(employeeId, "Employee ID");
        return employeeBenefitRepository.findByEmployeeId(employeeId);
    }

    // adds a new benefit allocation for an employee
    public void addBenefit(EmployeeBenefitDTO dto) {
        if (dto == null) throw new ValidationException("Benefit data is required.");
        validateId(dto.getEmployeeId(), "Employee ID");
        if (dto.getStartDate() == null) throw new ValidationException("Effective start date is required.");
        if (dto.getAmount() <= 0) throw new ValidationException("Amount must be positive.");

        employeeBenefitRepository.save(new EmployeeBenefit(
                null,
                dto.getEmployeeId(),
                dto.getBenefitTypeId(),
                dto.getAmount(),
                dto.getStartDate(),
                dto.getEndDate()
        ));
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
