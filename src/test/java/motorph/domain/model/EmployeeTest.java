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


    // rejects a null first name
    @Test
    @DisplayName("Negative - constructor should reject null first name")
    void constructorShouldRejectNullFirstName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Employee(
                        1, null, "Dela Cruz", LocalDate.of(1990, 1, 1), Gender.MALE,
                        null, null, null, null, EmploymentStatus.REGULAR, 1, 1,
                        LocalDate.of(2020, 1, 1), null, true, null)
        );
    }


    // rejects a hire date in the future
    @Test
    @DisplayName("Negative - constructor should reject a future hire date")
    void constructorShouldRejectFutureHireDate() {
        LocalDate future = LocalDate.now().plusDays(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> new Employee(
                        1, "Juan", "Dela Cruz", LocalDate.of(1990, 1, 1), Gender.MALE,
                        null, null, "0917-123-4567", null, EmploymentStatus.REGULAR, 1, 1,
                        future, null, true, null)
        );
    }


    // rejects an invalid phone number format
    @Test
    @DisplayName("Negative - constructor should reject an invalid phone number format")
    void constructorShouldRejectInvalidPhoneFormat() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Employee(
                        1, "Juan", "Dela Cruz", LocalDate.of(1990, 1, 1), Gender.MALE,
                        null, null, "not-a-phone", null, EmploymentStatus.REGULAR, 1, 1,
                        LocalDate.of(2020, 1, 1), null, true, null)
        );
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
