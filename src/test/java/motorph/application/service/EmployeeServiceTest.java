package motorph.application.service;


import motorph.application.dto.employee.EmployeeProfileDTO;
import motorph.application.dto.employee.EmployeeSummaryDTO;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.Employee;
import motorph.infrastructure.repository.DepartmentRepository;
import motorph.infrastructure.repository.EmployeeRepository;
import motorph.infrastructure.repository.PositionRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests employee service using real database dependencies.
 */
@DisplayName("EmployeeService Database Tests")
class EmployeeServiceTest {


    private static final Logger logger =
            Logger.getLogger(EmployeeServiceTest.class.getName());


    // UT-EMP-001: retrieves employee by valid id
    @Test
    @DisplayName("UT-EMP-001 - Positive - getEmployeeById should return employee")
    void getEmployeeByIdShouldReturnEmployee() {
        EmployeeService employeeService = createEmployeeService();


        logger.info("UT-EMP-001 Testing employee retrieval by ID.");


        Employee employee = employeeService.getEmployeeById(10001);


        assertNotNull(employee);
        assertEquals(10001, employee.getEmployeeId());
    }


    // UT-EMP-002: rejects invalid employee id
    @Test
    @DisplayName("UT-EMP-002 - Negative - getEmployeeById should reject invalid employee ID")
    void getEmployeeByIdShouldRejectInvalidEmployeeId() {
        EmployeeService employeeService = createEmployeeService();


        logger.info("UT-EMP-002 Testing invalid employee ID validation.");


        assertThrows(
                ValidationException.class,
                () -> employeeService.getEmployeeById(0)
        );
    }


    // UT-EMP-003: rejects non-existing employee
    @Test
    @DisplayName("UT-EMP-003 - Negative - getEmployeeById should reject non-existing employee")
    void getEmployeeByIdShouldRejectMissingEmployee() {
        EmployeeService employeeService = createEmployeeService();


        logger.info("UT-EMP-003 Testing non-existing employee lookup.");


        assertThrows(
                EntityNotFoundException.class,
                () -> employeeService.getEmployeeById(99999)
        );
    }


    // UT-EMP-004: retrieves all active employees
    @Test
    @DisplayName("UT-EMP-004 - Positive - getAllActiveEmployees should return employees")
    void getAllActiveEmployeesShouldReturnEmployees() {
        EmployeeService employeeService = createEmployeeService();


        logger.info("UT-EMP-004 Testing active employee retrieval.");


        List<Employee> employees = employeeService.getAllActiveEmployees();


        assertNotNull(employees);
        assertFalse(employees.isEmpty());
    }


    // UT-EMP-005: retrieves all employees
    @Test
    @DisplayName("UT-EMP-005 - Positive - getAllEmployees should return employees")
    void getAllEmployeesShouldReturnEmployees() {
        EmployeeService employeeService = createEmployeeService();


        logger.info("UT-EMP-005 Testing all employee retrieval.");


        List<Employee> employees = employeeService.getAllEmployees();


        assertNotNull(employees);
        assertFalse(employees.isEmpty());
    }


    // UT-EMP-006: retrieves employee summaries
    @Test
    @DisplayName("UT-EMP-006 - Positive - getAllEmployeeSummaries should return summaries")
    void getAllEmployeeSummariesShouldReturnSummaries() {
        EmployeeService employeeService = createEmployeeService();


        logger.info("UT-EMP-006 Testing employee summary retrieval.");


        List<EmployeeSummaryDTO> summaries =
                employeeService.getAllEmployeeSummaries();


        assertNotNull(summaries);
        assertFalse(summaries.isEmpty());
    }


    // UT-EMP-007: retrieves employee profile
    @Test
    @DisplayName("UT-EMP-007 - Positive - getEmployeeProfile should return profile")
    void getEmployeeProfileShouldReturnProfile() {
        EmployeeService employeeService = createEmployeeService();


        logger.info("UT-EMP-007 Testing employee profile retrieval.");


        EmployeeProfileDTO profile =
                employeeService.getEmployeeProfile(10001);


        assertNotNull(profile);
        assertEquals(10001, profile.getEmployeeId());
    }


    // UT-EMP-008: rejects invalid profile employee id
    @Test
    @DisplayName("UT-EMP-008 - Negative - getEmployeeProfile should reject invalid employee ID")
    void getEmployeeProfileShouldRejectInvalidEmployeeId() {
        EmployeeService employeeService = createEmployeeService();


        logger.info("UT-EMP-008 Testing invalid employee profile lookup.");


        assertThrows(
                ValidationException.class,
                () -> employeeService.getEmployeeProfile(0)
        );
    }


    // UT-EMP-009: boundary test for negative employee id
    @Test
    @DisplayName("UT-EMP-009 - Boundary - getEmployeeById should reject negative employee ID")
    void getEmployeeByIdShouldRejectNegativeEmployeeId() {
        EmployeeService employeeService = createEmployeeService();


        logger.info("UT-EMP-009 Testing negative employee ID boundary.");


        assertThrows(
                ValidationException.class,
                () -> employeeService.getEmployeeById(-1)
        );
    }

    // creates employee service with real dependencies
    private EmployeeService createEmployeeService() {
        return new EmployeeService(
                new EmployeeRepository(),
                createUserAccountService(),
                new DepartmentRepository(),
                new PositionRepository()
        );
    }


    // creates user account service dependency
    private UserAccountService createUserAccountService() {
        return new UserAccountService(
                new motorph.infrastructure.repository.UserAccountRepository(),
                new EmailService(createEmailConfig())
        );
    }


    // creates email config test data
    private motorph.infrastructure.config.EmailConfig createEmailConfig() {
        return new motorph.infrastructure.config.EmailConfig(
                "sandbox.smtp.mailtrap.io",
                2525,
                "test_username",
                "test_password",
                "no-reply@motorph.com",
                true,
                true
        );
    }
}

