package motorph.backend.service;

import motorph.backend.domain.Employee;
import motorph.backend.domain.EmployeeBenefit;
import motorph.backend.domain.GovernmentDetails;
import motorph.backend.domain.value.EmploymentStatus;
import motorph.backend.domain.value.Gender;
import motorph.backend.dto.employee.EmployeeUpdateRequest;
import motorph.backend.repository.EmployeeBenefitRepository;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.GovernmentDetailsRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The employee update audit entry names only the sections that actually changed, and unchanged
 * sections are not rewritten.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeServiceTest {

    private static final int EMPLOYEE_ID = 10028;
    private static final int ACTOR_ID = 1;
    private static final LocalDate HIRE_DATE = LocalDate.of(2020, 1, 6);

    @Mock private EmployeeRepository employeeRepository;
    @Mock private GovernmentDetailsRepository governmentDetailsRepository;
    @Mock private EmployeeBenefitRepository employeeBenefitRepository;
    @Mock private UserAccessLogService userAccessLogService;

    @InjectMocks private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        Employee stored = employee(Gender.MALE);

        when(employeeRepository.findAnyById(EMPLOYEE_ID)).thenReturn(stored);
        when(employeeRepository.findProfileByIdAny(EMPLOYEE_ID))
                .thenReturn(new EmployeeRepository.ProfileQueryResult(stored, "Analyst", "Finance", null));
        when(governmentDetailsRepository.findByEmployeeId(EMPLOYEE_ID))
                .thenReturn(new GovernmentDetails(5, EMPLOYEE_ID, "44-4506057-3", "442-605-657-000",
                        "691295330870", "820126853951"));
        when(employeeBenefitRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(
                new EmployeeBenefit(11, EMPLOYEE_ID, 1, 1500.0, HIRE_DATE, null),
                new EmployeeBenefit(12, EMPLOYEE_ID, 2, 1000.0, HIRE_DATE, null),
                new EmployeeBenefit(13, EMPLOYEE_ID, 3, 1000.0, HIRE_DATE, null)));
    }

    @Test
    @DisplayName("Changing only the gender logs a profile update and leaves IDs and benefits alone")
    void genderOnlyChangeLogsProfileUpdate() {
        employeeService.updateEmployee(EMPLOYEE_ID, request(Gender.FEMALE, "442-605-657-000"), ACTOR_ID);

        verify(employeeRepository).update(any(Employee.class));
        verify(governmentDetailsRepository, never()).update(any(GovernmentDetails.class));
        verify(employeeBenefitRepository, never()).update(any(EmployeeBenefit.class));
        verify(userAccessLogService).logEvent(ACTOR_ID, UserAccessLogService.ACTION_EMPLOYEE_UPDATED,
                "Updated profile of employee ID " + EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Changing the gender and a government ID names both sections")
    void profileAndGovernmentIdChangeNamesBothSections() {
        employeeService.updateEmployee(EMPLOYEE_ID, request(Gender.FEMALE, "442-605-657-999"), ACTOR_ID);

        verify(governmentDetailsRepository).update(any(GovernmentDetails.class));
        verify(userAccessLogService).logEvent(ACTOR_ID, UserAccessLogService.ACTION_EMPLOYEE_UPDATED,
                "Updated profile and government IDs of employee ID " + EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Saving without changes writes nothing and says so in the log")
    void unchangedSaveWritesNothing() {
        employeeService.updateEmployee(EMPLOYEE_ID, request(Gender.MALE, "442-605-657-000"), ACTOR_ID);

        verify(employeeRepository, never()).update(any(Employee.class));
        verify(governmentDetailsRepository, never()).update(any(GovernmentDetails.class));
        verify(employeeBenefitRepository, never()).update(any(EmployeeBenefit.class));
        verify(userAccessLogService).logEvent(ACTOR_ID, UserAccessLogService.ACTION_EMPLOYEE_UPDATED,
                "Saved employee ID " + EMPLOYEE_ID + " with no changes");
    }

    private Employee employee(Gender gender) {
        return new Employee(EMPLOYEE_ID, "Juan", "Cruz", LocalDate.of(1990, 5, 14), gender,
                "Quezon City", "Single", "0917-123-4567", "juan.cruz@gmail.com",
                EmploymentStatus.REGULAR, 3, 2, HIRE_DATE, null, true, null);
    }

    private EmployeeUpdateRequest request(Gender gender, String tinNumber) {
        return new EmployeeUpdateRequest("Juan", "Cruz", LocalDate.of(1990, 5, 14), gender,
                "Quezon City", "Single", "0917-123-4567", "juan.cruz@gmail.com",
                EmploymentStatus.REGULAR, 3, 2, HIRE_DATE, null,
                "44-4506057-3", "820126853951", tinNumber, "691295330870",
                1500.0, 1000.0, 1000.0);
    }
}
