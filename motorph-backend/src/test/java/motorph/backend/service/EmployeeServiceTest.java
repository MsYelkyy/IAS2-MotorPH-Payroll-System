package motorph.backend.service;

import motorph.backend.domain.Employee;
import motorph.backend.domain.EmployeeBenefit;
import motorph.backend.domain.GovernmentDetails;
import motorph.backend.domain.SalaryHistory;
import motorph.backend.domain.value.EmploymentStatus;
import motorph.backend.domain.value.Gender;
import motorph.backend.domain.value.SalaryType;
import motorph.backend.dto.employee.EmployeeUpdateRequest;
import motorph.backend.repository.EmployeeBenefitRepository;
import motorph.backend.repository.EmployeeRepository;
import motorph.backend.repository.GovernmentDetailsRepository;
import motorph.backend.repository.SalaryHistoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The employee update audit entry names only the form tabs that actually changed, and unchanged
 * sections are not rewritten.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeServiceTest {

    private static final int EMPLOYEE_ID = 10028;
    private static final int ACTOR_ID = 1;
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 14);
    private static final LocalDate HIRE_DATE = LocalDate.of(2020, 1, 6);

    private static final String SSS = "44-4506057-3";
    private static final String TIN = "442-605-657-000";
    private static final String PAGIBIG = "691295330870";
    private static final String PHILHEALTH = "820126853951";
    private static final double BASIC_SALARY = 50000.0;
    private static final double HOURLY_RATE = 297.62;

    @Mock private EmployeeRepository employeeRepository;
    @Mock private GovernmentDetailsRepository governmentDetailsRepository;
    @Mock private EmployeeBenefitRepository employeeBenefitRepository;
    @Mock private SalaryHistoryRepository salaryHistoryRepository;
    @Mock private UserAccessLogService userAccessLogService;

    @InjectMocks private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        // civil status comes back from the database ENUM in capitals, while the form sends "Single"
        Employee stored = new Employee(EMPLOYEE_ID, "Juan", "Cruz", BIRTH_DATE, Gender.MALE,
                "Quezon City", "SINGLE", "0917-123-4567", "juan.cruz@gmail.com",
                EmploymentStatus.REGULAR, 3, 2, HIRE_DATE, null, true, null);

        when(employeeRepository.findAnyById(EMPLOYEE_ID)).thenReturn(stored);
        when(employeeRepository.findProfileByIdAny(EMPLOYEE_ID))
                .thenReturn(new EmployeeRepository.ProfileQueryResult(stored, "Analyst", "Finance", null));
        when(governmentDetailsRepository.findByEmployeeId(EMPLOYEE_ID))
                .thenReturn(new GovernmentDetails(5, EMPLOYEE_ID, SSS, TIN, PAGIBIG, PHILHEALTH));
        when(employeeBenefitRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(
                new EmployeeBenefit(11, EMPLOYEE_ID, 1, 1500.0, HIRE_DATE, null),
                new EmployeeBenefit(12, EMPLOYEE_ID, 2, 1000.0, HIRE_DATE, null),
                new EmployeeBenefit(13, EMPLOYEE_ID, 3, 1000.0, HIRE_DATE, null)));
        when(salaryHistoryRepository.findCurrentByEmployeeId(eq(EMPLOYEE_ID), any(LocalDate.class)))
                .thenReturn(new SalaryHistory(21, EMPLOYEE_ID, SalaryType.MONTHLY, BASIC_SALARY, HOURLY_RATE, HIRE_DATE, null));
    }

    @Test
    @DisplayName("Changing only the gender logs personal information and leaves IDs and benefits alone")
    void genderOnlyChangeLogsPersonalInformation() {
        update(new Form().gender(Gender.FEMALE));

        verify(employeeRepository).update(any(Employee.class));
        verify(governmentDetailsRepository, never()).update(any(GovernmentDetails.class));
        verify(employeeBenefitRepository, never()).update(any(EmployeeBenefit.class));
        assertLogged("Updated personal information of employee ID " + EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Changing only the employment status logs employment information")
    void employmentStatusOnlyChangeLogsEmploymentInformation() {
        update(new Form().employmentStatus(EmploymentStatus.PROBATIONARY));

        verify(employeeRepository).update(any(Employee.class));
        assertLogged("Updated employment information of employee ID " + EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Changing the gender and a government ID names both tabs")
    void personalAndGovernmentChangeNamesBothTabs() {
        update(new Form().gender(Gender.FEMALE).tin("442-605-657-999"));

        verify(governmentDetailsRepository).update(any(GovernmentDetails.class));
        assertLogged("Updated personal and government information of employee ID " + EMPLOYEE_ID);
    }

    @Test
    @DisplayName("A government ID that only gained dashes from the form is not a change")
    void reformattedGovernmentIdIsNotAChange() {
        update(new Form().employmentStatus(EmploymentStatus.PROBATIONARY).philhealth("82-012685395-1"));

        verify(governmentDetailsRepository, never()).update(any(GovernmentDetails.class));
        assertLogged("Updated employment information of employee ID " + EMPLOYEE_ID);
    }

    @Test
    @DisplayName("An allowance change is logged as compensation")
    void allowanceChangeLogsCompensation() {
        update(new Form().gender(Gender.FEMALE).riceSubsidy(2000.0));

        verify(employeeBenefitRepository).update(any(EmployeeBenefit.class));
        assertLogged("Updated personal information and compensation of employee ID " + EMPLOYEE_ID);
    }

    @Test
    @DisplayName("A new basic salary closes the current record and starts one effective today")
    void basicSalaryChangeStartsNewSalaryRecord() {
        update(new Form().basicSalary(55000.0));

        ArgumentCaptor<SalaryHistory> closed = ArgumentCaptor.forClass(SalaryHistory.class);
        ArgumentCaptor<SalaryHistory> started = ArgumentCaptor.forClass(SalaryHistory.class);
        verify(salaryHistoryRepository).update(closed.capture());
        verify(salaryHistoryRepository).save(started.capture());

        assertThat(closed.getValue().getBasicSalary()).isEqualTo(BASIC_SALARY);
        assertThat(closed.getValue().getEndDate()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(started.getValue().getBasicSalary()).isEqualTo(55000.0);
        assertThat(started.getValue().getStartDate()).isEqualTo(LocalDate.now());
        assertLogged("Updated compensation of employee ID " + EMPLOYEE_ID);
    }

    @Test
    @DisplayName("Saving without changes writes nothing and logs nothing")
    void unchangedSaveWritesNothing() {
        update(new Form());

        verify(employeeRepository, never()).update(any(Employee.class));
        verify(governmentDetailsRepository, never()).update(any(GovernmentDetails.class));
        verify(employeeBenefitRepository, never()).update(any(EmployeeBenefit.class));
        verify(salaryHistoryRepository, never()).save(any(SalaryHistory.class));
        verify(userAccessLogService, never()).logEvent(anyInt(), anyString(), anyString());
    }

    private void update(Form form) {
        employeeService.updateEmployee(EMPLOYEE_ID, form.toRequest(), ACTOR_ID);
    }

    private void assertLogged(String details) {
        verify(userAccessLogService).logEvent(ACTOR_ID, UserAccessLogService.ACTION_EMPLOYEE_UPDATED, details);
    }

    /** The edit form as submitted, starting from the stored values. */
    private static final class Form {
        private Gender gender = Gender.MALE;
        private EmploymentStatus employmentStatus = EmploymentStatus.REGULAR;
        private String tin = TIN;
        private String philhealth = PHILHEALTH;
        private double riceSubsidy = 1500.0;
        private double basicSalary = BASIC_SALARY;

        Form gender(Gender value) {
            gender = value;
            return this;
        }

        Form employmentStatus(EmploymentStatus value) {
            employmentStatus = value;
            return this;
        }

        Form tin(String value) {
            tin = value;
            return this;
        }

        Form philhealth(String value) {
            philhealth = value;
            return this;
        }

        Form riceSubsidy(double value) {
            riceSubsidy = value;
            return this;
        }

        Form basicSalary(double value) {
            basicSalary = value;
            return this;
        }

        EmployeeUpdateRequest toRequest() {
            return new EmployeeUpdateRequest("Juan", "Cruz", BIRTH_DATE, gender,
                    "Quezon City", "Single", "0917-123-4567", "juan.cruz@gmail.com",
                    employmentStatus, 3, 2, HIRE_DATE, null,
                    SSS, philhealth, tin, PAGIBIG,
                    riceSubsidy, 1000.0, 1000.0,
                    basicSalary, basicSalary / 168.0);
        }
    }
}
