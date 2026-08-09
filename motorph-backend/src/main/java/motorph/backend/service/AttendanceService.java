package motorph.backend.service;

import motorph.backend.exception.BusinessRuleException;
import motorph.backend.exception.EntityNotFoundException;
import motorph.backend.exception.ValidationException;
import motorph.backend.domain.Attendance;
import motorph.backend.domain.value.AttendanceStatus;
import motorph.backend.repository.AttendanceRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Handles business logic for employee attendance and daily time logs.
 * Ported from the desktop's AttendanceService (updateStatus/delete excluded -
 * dead code, no UI caller). timeIn/timeOut always use the server clock -
 * the desktop's date/time-parameterized overloads were only ever invoked
 * with LocalDate.now()/LocalTime.now(), never client-supplied values, so
 * only the no-arg shape is exposed here.
 */
@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    public List<Attendance> getAttendanceByEmployeeId(int employeeId) {
        validateEmployeeId(employeeId);
        return attendanceRepository.findByEmployeeId(employeeId);
    }

    public List<Attendance> getAttendanceByDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return attendanceRepository.findByDateRange(startDate, endDate);
    }

    public List<Attendance> getAttendanceByEmployeeAndDateRange(int employeeId, LocalDate startDate, LocalDate endDate) {
        validateEmployeeId(employeeId);
        validateDateRange(startDate, endDate);
        return attendanceRepository.findByEmployeeAndDateRange(employeeId, startDate, endDate);
    }

    public Attendance getAttendanceForDate(int employeeId, LocalDate workDate) {
        validateEmployeeId(employeeId);

        if (workDate == null) {
            throw new ValidationException("Work date is required.");
        }

        Attendance attendance = attendanceRepository.findToday(employeeId, workDate);

        if (attendance == null) {
            throw new EntityNotFoundException("Attendance record not found.");
        }

        return attendance;
    }

    // records time in for the current server date/time - employeeId is always
    // the caller's own, resolved server-side by the controller
    public void timeIn(int employeeId) {
        LocalDate workDate = LocalDate.now();

        validateEmployeeId(employeeId);

        if (attendanceRepository.existsByEmployeeAndDate(employeeId, workDate)) {
            throw new BusinessRuleException("Employee has already timed in for this work date.");
        }

        attendanceRepository.timeIn(employeeId, workDate, LocalTime.now(), AttendanceStatus.PRESENT);
    }

    // records time out for the current server date/time
    public void timeOut(int employeeId) {
        LocalDate workDate = LocalDate.now();
        LocalTime timeOut = LocalTime.now();

        validateEmployeeId(employeeId);

        Attendance attendance = attendanceRepository.findToday(employeeId, workDate);

        if (attendance == null) {
            throw new EntityNotFoundException("No attendance record found for this work date.");
        }

        if (attendance.getTimeIn() == null) {
            throw new BusinessRuleException("Attendance record is missing time in.");
        }

        if (attendance.getTimeOut() != null) {
            throw new BusinessRuleException("Employee has already timed out for this work date.");
        }

        if (timeOut.isBefore(attendance.getTimeIn())) {
            throw new ValidationException("Time out cannot be earlier than time in.");
        }

        attendanceRepository.timeOut(employeeId, workDate, timeOut);
    }

    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new ValidationException("Start date is required.");
        }

        if (endDate == null) {
            throw new ValidationException("End date is required.");
        }

        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
    }
}
