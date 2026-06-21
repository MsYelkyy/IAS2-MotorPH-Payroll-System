package motorph.application.service;

import motorph.application.exception.BusinessRuleException;
import motorph.application.exception.EntityNotFoundException;
import motorph.application.exception.ValidationException;
import motorph.domain.model.Attendance;
import motorph.domain.value.AttendanceStatus;
import motorph.infrastructure.repository.AttendanceRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Handles business logic for employee attendance and daily time logs.
 */
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    // constructors
    public AttendanceService(AttendanceRepository attendanceRepository) {
        if (attendanceRepository == null) {
            throw new IllegalArgumentException("AttendanceRepository is required.");
        }
        this.attendanceRepository = attendanceRepository;
    }

    // returns all attendance records
    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    // returns attendance records of one employee
    public List<Attendance> getAttendanceByEmployeeId(int employeeId) {
        validateEmployeeId(employeeId);
        return attendanceRepository.findByEmployeeId(employeeId);
    }

    // returns attendance records within a date range
    public List<Attendance> getAttendanceByDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return attendanceRepository.findByDateRange(startDate, endDate);
    }

    // returns attendance records of one employee within a date range
    public List<Attendance> getAttendanceByEmployeeAndDateRange(int employeeId, LocalDate startDate, LocalDate endDate) {
        validateEmployeeId(employeeId);
        validateDateRange(startDate, endDate);
        return attendanceRepository.findByEmployeeAndDateRange(employeeId, startDate, endDate);
    }

    // returns one employee attendance record for a work date
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

    // records employee time in for the current date
    public void timeIn(int employeeId) {
        timeIn(employeeId, LocalDate.now(), LocalTime.now(), AttendanceStatus.PRESENT);
    }

    // records employee time in using the provided date, time, and status
    public void timeIn(int employeeId, LocalDate workDate, LocalTime timeIn, AttendanceStatus attendanceStatus) {
        validateEmployeeId(employeeId);

        if (workDate == null) {
            throw new ValidationException("Work date is required.");
        }

        if (timeIn == null) {
            throw new ValidationException("Time in is required.");
        }

        if (attendanceStatus == null) {
            throw new ValidationException("Attendance status is required.");
        }

        if (attendanceRepository.existsByEmployeeAndDate(employeeId, workDate)) {
            throw new BusinessRuleException("Employee has already timed in for this work date.");
        }

        attendanceRepository.timeIn(employeeId, workDate, timeIn, attendanceStatus);
    }

    // records employee time out for the current date
    public void timeOut(int employeeId) {
        timeOut(employeeId, LocalDate.now(), LocalTime.now());
    }

    // records employee time out using the provided date and time
    public void timeOut(int employeeId, LocalDate workDate, LocalTime timeOut) {
        validateEmployeeId(employeeId);

        if (workDate == null) {
            throw new ValidationException("Work date is required.");
        }

        if (timeOut == null) {
            throw new ValidationException("Time out is required.");
        }

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

    // updates attendance status
    public void updateStatus(int attendanceId, AttendanceStatus attendanceStatus) {
        validateAttendanceId(attendanceId);

        if (attendanceStatus == null) {
            throw new ValidationException("Attendance status is required.");
        }

        attendanceRepository.updateStatus(attendanceId, attendanceStatus);
    }

    // deletes one attendance record
    public void deleteAttendance(int attendanceId) {
        validateAttendanceId(attendanceId);
        attendanceRepository.delete(attendanceId);
    }

    // validates employee id input
    private void validateEmployeeId(int employeeId) {
        if (employeeId <= 0) {
            throw new ValidationException("Employee ID is required.");
        }
    }

    // validates attendance id input
    private void validateAttendanceId(int attendanceId) {
        if (attendanceId <= 0) {
            throw new ValidationException("Attendance ID is required.");
        }
    }

    // validates a date range input
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