package motorph.application.service;


import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.Attendance;
import motorph.domain.value.AttendanceStatus;
import motorph.infrastructure.repository.AttendanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests attendance service using real database dependencies.
 */
@DisplayName("AttendanceService Database Tests")
class AttendanceServiceTest {


    private static final Logger logger = Logger.getLogger(AttendanceServiceTest.class.getName());


    // UT-ATS-001: constructor rejects missing repository
    @Test
    @DisplayName("UT-ATS-001 - Negative - constructor should reject null repository")
    void constructorShouldRejectNullRepository() {
        logger.info("UT-ATS-001 Testing null attendance repository validation.");


        assertThrows(
                IllegalArgumentException.class,
                () -> new AttendanceService(null)
        );
    }


    // UT-ATS-002: retrieves attendance by employee
    @Test
    @DisplayName("UT-ATS-002 - Positive - getAttendanceByEmployeeId should return records")
    void getAttendanceByEmployeeIdShouldReturnRecords() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-002 Testing attendance retrieval by employee.");


        List<Attendance> records = attendanceService.getAttendanceByEmployeeId(10001);


        assertNotNull(records);
    }


    // UT-ATS-003: rejects invalid employee id
    @Test
    @DisplayName("UT-ATS-003 - Negative - getAttendanceByEmployeeId should reject invalid employee ID")
    void getAttendanceByEmployeeIdShouldRejectInvalidEmployeeId() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-003 Testing invalid employee ID validation.");


        assertThrows(
                ValidationException.class,
                () -> attendanceService.getAttendanceByEmployeeId(0)
        );
    }


    // UT-ATS-004: retrieves attendance by date range
    @Test
    @DisplayName("UT-ATS-004 - Positive - getAttendanceByDateRange should return records")
    void getAttendanceByDateRangeShouldReturnRecords() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-004 Testing attendance retrieval by date range.");


        List<Attendance> records = attendanceService.getAttendanceByDateRange(
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 6, 30)
        );


        assertNotNull(records);
    }


    // UT-ATS-005: rejects invalid date range
    @Test
    @DisplayName("UT-ATS-005 - Negative - getAttendanceByDateRange should reject invalid date range")
    void getAttendanceByDateRangeShouldRejectInvalidDateRange() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-005 Testing invalid date range validation.");


        assertThrows(
                ValidationException.class,
                () -> attendanceService.getAttendanceByDateRange(
                        LocalDate.of(2024, 6, 30),
                        LocalDate.of(2024, 6, 1)
                )
        );
    }


    // UT-ATS-006: retrieves attendance by employee and date range
    @Test
    @DisplayName("UT-ATS-006 - Positive - getAttendanceByEmployeeAndDateRange should return records")
    void getAttendanceByEmployeeAndDateRangeShouldReturnRecords() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-006 Testing attendance retrieval by employee and date range.");


        List<Attendance> records = attendanceService.getAttendanceByEmployeeAndDateRange(
                10001,
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2024, 6, 30)
        );


        assertNotNull(records);
    }


    // UT-ATS-007: retrieves one attendance record for date
    @Test
    @DisplayName("UT-ATS-007 - Positive - getAttendanceForDate should return attendance record")
    void getAttendanceForDateShouldReturnAttendanceRecord() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-007 Testing attendance retrieval for one date.");


        Attendance attendance = attendanceService.getAttendanceForDate(
                10001,
                LocalDate.of(2024, 6, 3)
        );


        assertNotNull(attendance);
        assertEquals(10001, attendance.getEmployeeId());
        assertEquals(LocalDate.of(2024, 6, 3), attendance.getWorkDate());
    }


    // UT-ATS-008: rejects missing attendance record
    @Test
    @DisplayName("UT-ATS-008 - Edge Case - getAttendanceForDate should reject missing attendance")
    void getAttendanceForDateShouldRejectMissingAttendance() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-008 Testing missing attendance record lookup.");


        assertThrows(
                EntityNotFoundException.class,
                () -> attendanceService.getAttendanceForDate(
                        99999,
                        LocalDate.of(2099, 1, 1)
                )
        );
    }


    // UT-ATS-009: rejects null work date for attendance lookup
    @Test
    @DisplayName("UT-ATS-009 - Negative - getAttendanceForDate should reject null work date")
    void getAttendanceForDateShouldRejectNullWorkDate() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-009 Testing null work date validation.");


        assertThrows(
                ValidationException.class,
                () -> attendanceService.getAttendanceForDate(10001, null)
        );
    }


    // UT-ATS-010: rejects null work date for time in
    @Test
    @DisplayName("UT-ATS-010 - Negative - timeIn should reject null work date")
    void timeInShouldRejectNullWorkDate() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-010 Testing null work date for time in.");


        assertThrows(
                ValidationException.class,
                () -> attendanceService.timeIn(
                        10001,
                        null,
                        LocalTime.of(9, 0),
                        AttendanceStatus.PRESENT
                )
        );
    }


    // UT-ATS-011: rejects null time in
    @Test
    @DisplayName("UT-ATS-011 - Negative - timeIn should reject null time in")
    void timeInShouldRejectNullTimeIn() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-011 Testing null time in validation.");


        assertThrows(
                ValidationException.class,
                () -> attendanceService.timeIn(
                        10001,
                        LocalDate.of(2024, 6, 3),
                        null,
                        AttendanceStatus.PRESENT
                )
        );
    }


    // UT-ATS-012: rejects duplicate time in
    @Test
    @DisplayName("UT-ATS-012 - Business Rule - timeIn should reject duplicate attendance")
    void timeInShouldRejectDuplicateAttendance() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-012 Testing duplicate time in business rule.");


        assertThrows(
                BusinessRuleException.class,
                () -> attendanceService.timeIn(
                        10001,
                        LocalDate.of(2024, 6, 3),
                        LocalTime.of(9, 0),
                        AttendanceStatus.PRESENT
                )
        );
    }


    // UT-ATS-013: rejects missing attendance for time out
    @Test
    @DisplayName("UT-ATS-013 - Edge Case - timeOut should reject missing attendance")
    void timeOutShouldRejectMissingAttendance() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-013 Testing missing attendance for time out.");


        assertThrows(
                EntityNotFoundException.class,
                () -> attendanceService.timeOut(
                        99999,
                        LocalDate.of(2099, 1, 1),
                        LocalTime.of(18, 0)
                )
        );
    }


    // UT-ATS-014: rejects null time out
    @Test
    @DisplayName("UT-ATS-014 - Negative - timeOut should reject null time out")
    void timeOutShouldRejectNullTimeOut() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-014 Testing null time out validation.");


        assertThrows(
                ValidationException.class,
                () -> attendanceService.timeOut(
                        10001,
                        LocalDate.of(2024, 6, 3),
                        null
                )
        );
    }
    // UT-ATS-015: rejects already timed out attendance
    @Test
    @DisplayName("UT-ATS-015 - Business Rule - timeOut should reject already timed out attendance")
    void timeOutShouldRejectAlreadyTimedOutAttendance() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-015 Testing already timed out business rule.");


        assertThrows(
                BusinessRuleException.class,
                () -> attendanceService.timeOut(
                        10001,
                        LocalDate.of(2024, 6, 3),
                        LocalTime.of(19, 0)
                )
        );
    }


    // UT-ATS-016: rejects null attendance status update
    @Test
    @DisplayName("UT-ATS-016 - Negative - updateStatus should reject null attendance status")
    void updateStatusShouldRejectNullAttendanceStatus() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-016 Testing null attendance status validation.");


        assertThrows(
                ValidationException.class,
                () -> attendanceService.updateStatus(1, null)
        );
    }


    // UT-ATS-017: rejects invalid attendance id for delete
    @Test
    @DisplayName("UT-ATS-017 - Negative - deleteAttendance should reject invalid attendance ID")
    void deleteAttendanceShouldRejectInvalidAttendanceId() {
        AttendanceService attendanceService = createAttendanceService();


        logger.info("UT-ATS-017 Testing invalid attendance ID for delete.");


        assertThrows(
                ValidationException.class,
                () -> attendanceService.deleteAttendance(0)
        );
    }


    // creates attendance service with real dependency
    private AttendanceService createAttendanceService() {
        return new AttendanceService(new AttendanceRepository());
    }
}

