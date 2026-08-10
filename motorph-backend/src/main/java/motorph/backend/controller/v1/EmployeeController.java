package motorph.backend.controller.v1;

import motorph.backend.domain.Department;
import motorph.backend.domain.GovernmentDetails;
import motorph.backend.domain.Position;
import motorph.backend.domain.value.PermissionKey;
import motorph.backend.dto.employee.ContactInfoUpdateRequest;
import motorph.backend.dto.employee.EmployeeCreateRequest;
import motorph.backend.dto.employee.EmployeeCreateResponse;
import motorph.backend.dto.employee.EmployeeProfileDTO;
import motorph.backend.dto.employee.EmployeeSummaryDTO;
import motorph.backend.dto.employee.EmployeeUpdateRequest;
import motorph.backend.exception.UnauthorizedException;
import motorph.backend.security.AuthenticatedPrincipal;
import motorph.backend.security.CurrentUserProvider;
import motorph.backend.service.EmployeeService;
import motorph.backend.service.PermissionService;
import motorph.backend.service.ProfilePhotoService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Phase 2 read-only employee endpoints. Every method re-checks permissions
 * server-side (see PermissionService) - the desktop client's own menu/role
 * checks are display hints only, never trusted here.
 */
@RestController
@RequestMapping("/api/v1")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final PermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;
    private final ProfilePhotoService profilePhotoService;

    public EmployeeController(EmployeeService employeeService,
                              PermissionService permissionService,
                              CurrentUserProvider currentUserProvider,
                              ProfilePhotoService profilePhotoService) {
        this.employeeService = employeeService;
        this.permissionService = permissionService;
        this.currentUserProvider = currentUserProvider;
        this.profilePhotoService = profilePhotoService;
    }

    // full employee list (sorted, includes inactive) - clients filter by isActive() as needed
    @GetMapping("/employees")
    public List<EmployeeSummaryDTO> listEmployees() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_EMPLOYEE, PermissionKey.MANAGE_EMPLOYEE);
        return employeeService.getAllEmployeeSummaries();
    }

    // an employee may always view their own profile; viewing anyone else's
    // requires VIEW_EMPLOYEE/MANAGE_EMPLOYEE, in which case inactive employees are visible too
    @GetMapping("/employees/{employeeId}")
    public EmployeeProfileDTO getEmployee(@PathVariable int employeeId) {
        if (permissionService.hasAnyPermission(PermissionKey.VIEW_EMPLOYEE, PermissionKey.MANAGE_EMPLOYEE)) {
            return employeeService.getEmployeeProfileAdmin(employeeId);
        }

        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();

        if (principal.employeeId() == employeeId) {
            return employeeService.getEmployeeProfile(employeeId);
        }

        throw new UnauthorizedException("You do not have permission to view this employee.");
    }

    // self-service: acts only on the caller's own record, resolved from the
    // token, so it needs no employee-administration permission
    @PutMapping("/employees/me/contact")
    public void updateOwnContactInfo(@RequestBody ContactInfoUpdateRequest request) {
        int employeeId = currentUserProvider.getCurrentPrincipal().employeeId();
        employeeService.updateOwnContactInfo(employeeId, request.phoneNumber(), request.address());
    }

    // self-service photo upload: raw image bytes, extension checked against an
    // allowlist server-side because it becomes part of the stored filename
    @PostMapping(value = "/employees/me/photo", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void uploadOwnPhoto(@RequestParam String extension, @RequestBody byte[] content) {
        int employeeId = currentUserProvider.getCurrentPrincipal().employeeId();
        profilePhotoService.storePhoto(employeeId, content, extension);
    }

    // same visibility rule as the profile itself: your own always, anyone
    // else's only with employee-view rights
    @GetMapping("/employees/{employeeId}/photo")
    public ResponseEntity<byte[]> getEmployeePhoto(@PathVariable int employeeId) {
        requireOwnOrEmployeeViewer(employeeId);

        byte[] photo = profilePhotoService.readPhoto(employeeId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, profilePhotoService.contentTypeFor(employeeId))
                .body(photo);
    }

    @GetMapping("/employees/{employeeId}/government-details")
    public GovernmentDetails getGovernmentDetails(@PathVariable int employeeId) {
        permissionService.requireAnyPermission(PermissionKey.VIEW_EMPLOYEE, PermissionKey.MANAGE_EMPLOYEE);
        return employeeService.getGovernmentDetails(employeeId);
    }

    @GetMapping("/departments")
    public List<Department> listDepartments() {
        permissionService.requireAnyPermission(PermissionKey.VIEW_EMPLOYEE, PermissionKey.MANAGE_EMPLOYEE);
        return employeeService.getAllDepartments();
    }

    @GetMapping("/positions")
    public List<Position> listPositions(@RequestParam(required = false) Integer departmentId) {
        permissionService.requireAnyPermission(PermissionKey.VIEW_EMPLOYEE, PermissionKey.MANAGE_EMPLOYEE);

        return departmentId != null
                ? employeeService.getPositionsByDepartment(departmentId)
                : employeeService.getAllPositions();
    }

    // creates a new employee with an initial salary, government IDs, benefits, and a temporary login account
    @PostMapping("/employees")
    public EmployeeCreateResponse createEmployee(@RequestBody EmployeeCreateRequest request) {
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        return employeeService.createEmployeeWithSalaryAndBenefits(request);
    }

    @PutMapping("/employees/{employeeId}")
    public EmployeeProfileDTO updateEmployee(@PathVariable int employeeId, @RequestBody EmployeeUpdateRequest request) {
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        return employeeService.updateEmployee(employeeId, request);
    }

    // deactivates an employee; the caller may never deactivate their own account, server-enforced
    @DeleteMapping("/employees/{employeeId}")
    public void deactivateEmployee(@PathVariable int employeeId) {
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();
        employeeService.deactivateEmployee(employeeId, principal.employeeId());
    }
    private void requireOwnOrEmployeeViewer(int employeeId) {
        if (permissionService.hasAnyPermission(PermissionKey.VIEW_EMPLOYEE, PermissionKey.MANAGE_EMPLOYEE)) {
            return;
        }

        if (currentUserProvider.getCurrentPrincipal().employeeId() == employeeId) {
            return;
        }

        throw new UnauthorizedException("You do not have permission to view this employee.");
    }
}
