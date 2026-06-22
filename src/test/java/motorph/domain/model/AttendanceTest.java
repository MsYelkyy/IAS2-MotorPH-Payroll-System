package motorph.domain.model;

import motorph.domain.value.AttendanceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.time.LocalTime;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests attendance business rules and helper methods.
 */
@DisplayName("Attendance Tests")
class AttendanceTest {


    // creates attendance with valid data
    @Test
    @DisplayName("constructor should create attendance with valid data")
    void constructorShouldCreateAttendanceWithValidData() {
        Attendance attendance = createAttendance(
                1,
                10001,
                LocalDate.of(2025, 6, 1),
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                AttendanceStatus.PRESENT
        );


        assertEquals(10001, attendance.getEmployeeId());
        assertEquals(AttendanceStatus.PRESENT, attendance.getAttendanceStatus());
    }


    // computes worked hours
    @Test
    @DisplayName("getHoursWorked should return total worked hours")
    void getHoursWorkedShouldReturnTotalWorkedHours() {
        Attendance attendance = createAttendance(
                1,
                10001,
                LocalDate.of(2025, 6, 1),
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                AttendanceStatus.PRESENT
        );


        assertEquals(9.0, attendance.getHoursWorked(), 0.01);
    }


    // returns zero when time out is missing
    @Test
    @DisplayName("getHoursWorked should return zero when attendance is incomplete")
    void getHoursWorkedShouldReturnZeroWhenAttendanceIsIncomplete() {
        Attendance attendance = createAttendance(
                1,
                10001,
                LocalDate.of(2025, 6, 1),
                LocalTime.of(8, 0),
                null,
                AttendanceStatus.PRESENT
        );


        assertEquals(0.0, attendance.getHoursWorked(), 0.01);
    }


    // identifies complete attendance
    @Test
    @DisplayName("isComplete should return true for complete attendance")
    void isCompleteShouldReturnTrueForCompleteAttendance() {
        Attendance attendance = createAttendance(
                1,
                10001,
                LocalDate.of(2025, 6, 1),
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                AttendanceStatus.PRESENT
        );


        assertTrue(attendance.isComplete());
    }


    // identifies incomplete attendance
    @Test
    @DisplayName("isComplete should return false for incomplete attendance")
    void isCompleteShouldReturnFalseForIncompleteAttendance() {
        Attendance attendance = createAttendance(
                1,
                10001,
                LocalDate.of(2025, 6, 1),
                LocalTime.of(8, 0),
                null,
                AttendanceStatus.PRESENT
        );


        assertFalse(attendance.isComplete());
    }


    // identifies present status
    @Test
    @DisplayName("isPresent should return true for present attendance")
    void isPresentShouldReturnTrueForPresentAttendance() {
        Attendance attendance = createAttendance(
                1,
                10001,
                LocalDate.now(),
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                AttendanceStatus.PRESENT
        );


        assertTrue(attendance.isPresent());
        assertFalse(attendance.isLate());
        assertFalse(attendance.isAbsent());
    }


    // identifies late status
    @Test
    @DisplayName("isLate should return true for late attendance")
    void isLateShouldReturnTrueForLateAttendance() {
        Attendance attendance = createAttendance(
                1,
                10001,
                LocalDate.now(),
                LocalTime.of(8, 30),
                LocalTime.of(17, 0),
                AttendanceStatus.LATE
        );


        assertTrue(attendance.isLate());
        assertFalse(attendance.isPresent());
        assertFalse(attendance.isAbsent());
    }


    // identifies absent status
    @Test
    @DisplayName("isAbsent should return true for absent attendance")
    void isAbsentShouldReturnTrueForAbsentAttendance() {
        Attendance attendance = createAttendance(
                1,
                10001,
                LocalDate.now(),
                null,
                null,
                AttendanceStatus.ABSENT
        );


        assertTrue(attendance.isAbsent());
        assertFalse(attendance.isPresent());
        assertFalse(attendance.isLate());
    }


    // rejects invalid employee id
    @Test
    @DisplayName("constructor should reject invalid employee id")
    void constructorShouldRejectInvalidEmployeeId() {
        assertThrows(IllegalArgumentException.class,
                () -> createAttendance(
                        1,
                        0,
                        LocalDate.now(),
                        null,
                        null,
                        AttendanceStatus.ABSENT
                ));
    }


    // rejects null work date
    @Test
    @DisplayName("constructor should reject null work date")
    void constructorShouldRejectNullWorkDate() {
        assertThrows(IllegalArgumentException.class,
                () -> createAttendance(
                        1,
                        10001,
                        null,
                        null,
                        null,
                        AttendanceStatus.ABSENT
                ));
    }


    // rejects null attendance status
    @Test
    @DisplayName("constructor should reject null attendance status")
    void constructorShouldRejectNullAttendanceStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> createAttendance(
                        1,
                        10001,
                        LocalDate.now(),
                        null,
                        null,
                        null
                ));
    }


    // creates attendance test data
    private Attendance createAttendance(Integer attendanceId,
                                        int employeeId,
                                        LocalDate workDate,
                                        LocalTime timeIn,
                                        LocalTime timeOut,
                                        AttendanceStatus status) {
        return new Attendance(
                attendanceId,
                employeeId,
                workDate,
                timeIn,
                timeOut,
                status
        );
    }
}
