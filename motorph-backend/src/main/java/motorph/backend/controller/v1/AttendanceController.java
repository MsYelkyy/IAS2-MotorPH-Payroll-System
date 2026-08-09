package motorph.backend.controller.v1;

import motorph.backend.domain.Attendance;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.security.AuthenticatedPrincipal;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.AttendanceService;
import motorph.backend.service.PermissionService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Attendance endpoints. time-in/time-out always act on the caller's own
 * employeeId, resolved server-side via CurrentUserProvider - never trusted
 * from the client, so a modified desktop client can never clock in as
 * someone else. Viewing all-employee records requires VIEW_ATTENDANCE or
 * MANAGE_ATTENDANCE; viewing one's own records needs no extra permission.
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

    @GetMapping("/me")
    public List<Attendance> getOwnAttendance() {
        return attendanceService.getAttendanceByEmployeeId(callerEmployeeId());
    }

    @GetMapping("/me/today")
    public Attendance getOwnAttendanceToday() {
        return attendanceService.getAttendanceForDate(callerEmployeeId(), LocalDate.now());
    }

    @GetMapping("/me/range")
    public List<Attendance> getOwnAttendanceRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return attendanceService.getAttendanceByEmployeeAndDateRange(callerEmployeeId(), startDate, endDate);
    }

    @GetMapping
    public List<Attendance> getAttendanceByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        permissionService.requireAnyPermission(PermissionKey.VIEW_ATTENDANCE, PermissionKey.MANAGE_ATTENDANCE);
        return attendanceService.getAttendanceByDateRange(startDate, endDate);
    }

    @GetMapping("/all")
    public List<Attendance> getAllAttendance() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_ATTENDANCE, PermissionKey.MANAGE_ATTENDANCE);
        return attendanceService.getAllAttendance();
    }

    private int callerEmployeeId() {
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();
        return principal.employeeId();
    }
}
