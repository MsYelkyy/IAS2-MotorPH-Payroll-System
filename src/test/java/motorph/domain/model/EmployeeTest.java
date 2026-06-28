package motorph.domain.model;


import motorph.domain.value.EmploymentStatus;
import motorph.domain.value.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests employee full name formatting.
 */
@DisplayName("Employee Tests")
class EmployeeTest {


    // returns the default "First Last" full name
    @Test
    @DisplayName("getFullName should return First Last format")
    void getFullNameShouldReturnFirstLastFormat() {
        Employee employee = createEmployee("Juan", "Dela Cruz");


        assertEquals("Juan Dela Cruz", employee.getFullName());
    }


    // returns "First Last" when lastNameFirst is false
    @Test
    @DisplayName("getFullName overload should return First Last when lastNameFirst is false")
    void getFullNameOverloadShouldReturnFirstLastWhenFalse() {
        Employee employee = createEmployee("Juan", "Dela Cruz");


        assertEquals("Juan Dela Cruz", employee.getFullName(false));
    }


    // returns "Last, First" when lastNameFirst is true
    @Test
    @DisplayName("getFullName overload should return Last, First when lastNameFirst is true")
    void getFullNameOverloadShouldReturnLastFirstWhenTrue() {
        Employee employee = createEmployee("Juan", "Dela Cruz");


        assertEquals("Dela Cruz, Juan", employee.getFullName(true));
    }


    // creates an employee with the given names
    private Employee createEmployee(String firstName, String lastName) {
        return new Employee(
                1, firstName, lastName, LocalDate.of(1990, 1, 1), Gender.MALE,
                null, null, null, null, EmploymentStatus.REGULAR, 1, 1,
                LocalDate.of(2020, 1, 1), null, true, null
        );
    }
}
