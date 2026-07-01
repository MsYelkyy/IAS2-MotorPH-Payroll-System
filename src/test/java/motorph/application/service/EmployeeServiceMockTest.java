package motorph.application.service;


import motorph.application.exception.ValidationException;
import motorph.domain.model.Employee;
import motorph.domain.value.EmploymentStatus;
import motorph.domain.value.Gender;
import motorph.infrastructure.repository.DepartmentRepository;
import motorph.infrastructure.repository.EmployeeBenefitRepository;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.GovernmentDetailsRepository;
import motorph.infrastructure.repository.PositionRepository;
import motorph.infrastructure.repository.SalaryHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * Tests employee CRUD operations using mocked dependencies.
 */
@DisplayName("EmployeeService Mock Tests")
class EmployeeServiceMockTest {


    private static final Logger logger = Logger.getLogger(EmployeeServiceMockTest.class.getName());


    // create saves a valid employee to the repository
    @Test
    @DisplayName("Positive - createEmployee should save valid employee data")
    void createEmployeeShouldSaveValidData() {
        logger.info("Testing valid employee creation saves the record.");

        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        EmployeeService service = serviceWith(employeeRepository);

        service.createEmployee(validEmployee(null));

        verify(employeeRepository).save(any(Employee.class));
    }


    // create rejects an employee with missing required fields
    @Test
    @DisplayName("Negative - createEmployee should reject null employee")
    void createEmployeeShouldRejectNullEmployee() {
        logger.info("Testing null employee creation is rejected.");

        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        EmployeeService service = serviceWith(employeeRepository);

        assertThrows(ValidationException.class, () -> service.createEmployee(null));
        verify(employeeRepository, never()).save(any());
    }


    // update persists changes for an employee that has an id
    @Test
    @DisplayName("Positive - updateEmployee should update existing employee")
    void updateEmployeeShouldUpdateExisting() {
        logger.info("Testing existing employee update.");

        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        EmployeeService service = serviceWith(employeeRepository);

        service.updateEmployee(validEmployee(10001));

        verify(employeeRepository).update(any(Employee.class));
    }


    // update rejects an employee without an id (no record to update)
    @Test
    @DisplayName("Negative - updateEmployee should reject employee without id")
    void updateEmployeeShouldRejectMissingId() {
        logger.info("Testing employee update without an id is rejected.");

        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        EmployeeService service = serviceWith(employeeRepository);

        assertThrows(ValidationException.class, () -> service.updateEmployee(validEmployee(null)));
        verify(employeeRepository, never()).update(any());
    }


    // deactivate removes the employee and its account
    @Test
    @DisplayName("Positive - deactivateEmployee should deactivate an existing employee")
    void deactivateEmployeeShouldDeactivateExisting() {
        logger.info("Testing existing employee deactivation.");

        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        UserAccountService userAccountService = mock(UserAccountService.class);
        EmployeeService service = serviceWith(employeeRepository, userAccountService);

        service.deactivateEmployee(10001);

        verify(employeeRepository).deactivate(10001);
        verify(userAccountService).deactivateAccountByEmployeeId(10001);
    }


    // deactivate rejects an invalid employee id
    @Test
    @DisplayName("Negative - deactivateEmployee should reject invalid employee id")
    void deactivateEmployeeShouldRejectInvalidId() {
        logger.info("Testing invalid employee id deactivation is rejected.");

        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        EmployeeService service = serviceWith(employeeRepository);

        assertThrows(ValidationException.class, () -> service.deactivateEmployee(0));
        verify(employeeRepository, never()).deactivate(anyInt());
    }


    // builds an EmployeeService with a mocked employee repository
    private EmployeeService serviceWith(EmployeeRepository employeeRepository) {
        return serviceWith(employeeRepository, mock(UserAccountService.class));
    }

    // builds an EmployeeService with mocked employee repository and account service
    private EmployeeService serviceWith(EmployeeRepository employeeRepository,
                                        UserAccountService userAccountService) {
        return new EmployeeService(
                employeeRepository,
                userAccountService,
                mock(DepartmentRepository.class),
                mock(PositionRepository.class),
                mock(SalaryHistoryRepository.class),
                mock(EmployeeBenefitRepository.class),
                mock(GovernmentDetailsRepository.class)
        );
    }

    // creates a valid employee, with or without an id
    private Employee validEmployee(Integer id) {
        return new Employee(
                id, "Juan", "Dela Cruz", LocalDate.of(1990, 1, 1), Gender.MALE,
                null, null, null, null, EmploymentStatus.REGULAR, 1, 1,
                LocalDate.of(2020, 1, 1), null, true, null
        );
    }
}
