package motorph.backend.controller.v1;

import motorph.backend.domain.Attendance;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.exception.ValidationException;
import motorph.backend.security.AuthenticatedPrincipal;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.AttendanceService;
import motorph.backend.service.PermissionService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Attendance endpoints. Time-in and time-out always apply to the caller's own record, taken from
 * the access token, so a client cannot clock in for someone else. Viewing other employees'
 * attendance requires {@code VIEW_ATTENDANCE} or {@code MANAGE_ATTENDANCE}.
 * Attendance endpoints with date range input validation. C3 
 */
@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    public AttendanceController(AttendanceService attendanceService,
                                PermissionService permissionService,
                                CurrentUserProvider currentUserProvider) {
        this.attendanceService = attendanceService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/time-in")
    public void timeIn() {
        attendanceService.timeIn(callerEmployeeId());
    }

    @PostMapping("/time-out")
    public void timeOut() {
        attendanceService.timeOut(callerEmployeeId());
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Attendance> getOwnAttendance() {
        return attendanceService.getAttendanceByEmployeeId(callerEmployeeId());
    }

    @GetMapping(value = "/me/today", produces = MediaType.APPLICATION_JSON_VALUE)
    public Attendance getOwnAttendanceToday() {
        return attendanceService.getAttendanceForDate(callerEmployeeId(), LocalDate.now());
    }

    @GetMapping(value = "/me/range", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Attendance> getOwnAttendanceRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return attendanceService.getAttendanceByEmployeeAndDateRange(callerEmployeeId(), startDate, endDate);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Attendance> getAttendanceByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        permissionService.requireAnyPermission(PermissionKey.VIEW_ATTENDANCE, PermissionKey.MANAGE_ATTENDANCE);
        validateDateRange(startDate, endDate);
        return attendanceService.getAttendanceByDateRange(startDate, endDate);
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Attendance> getAllAttendance() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_ATTENDANCE, PermissionKey.MANAGE_ATTENDANCE);
        return attendanceService.getAllAttendance();
    }

    private int callerEmployeeId() {
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();
        return principal.employeeId();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required.");
        }
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date.");
        }
    }
}