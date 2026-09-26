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
 * Employee management and self-service endpoints. Permissions are checked on the server for every
 * request; menu restrictions in the client are for display only.
 */
@RestController
@RequestMapping("/api/v1")
public class EmployeeController {

    // read-only lookups that payroll and report screens need to pick employees and departments
    private static final PermissionKey[] DIRECTORY_READERS = {
            PermissionKey.VIEW_EMPLOYEE, PermissionKey.MANAGE_EMPLOYEE,
            PermissionKey.VIEW_PAYROLL, PermissionKey.VIEW_REPORTS, PermissionKey.GENERATE_REPORTS
    };

    // profile and government IDs shown on payroll screens and payslips
    private static final PermissionKey[] PAYROLL_DETAIL_READERS = {
            PermissionKey.VIEW_EMPLOYEE, PermissionKey.MANAGE_EMPLOYEE, PermissionKey.VIEW_PAYROLL
    };

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

    /**
     * Returns all employees, including inactive ones. Clients filter by active status as needed.
     */
    @GetMapping("/employees")
    public List<EmployeeSummaryDTO> listEmployees() {
        permissionService.requireAnyPermission(DIRECTORY_READERS);
        return employeeService.getAllEmployeeSummaries();
    }

    /**
     * Returns an employee's profile. Employees can always view their own; viewing anyone else's
     * requires {@code VIEW_EMPLOYEE} or {@code MANAGE_EMPLOYEE}, which also makes inactive
     * employees visible.
     */
    @GetMapping("/employees/{employeeId}")
    public EmployeeProfileDTO getEmployee(@PathVariable int employeeId) {
        if (permissionService.hasAnyPermission(PAYROLL_DETAIL_READERS)) {
            return employeeService.getEmployeeProfileAdmin(employeeId);
        }

        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();

        if (principal.employeeId() == employeeId) {
            return employeeService.getEmployeeProfile(employeeId);
        }

        throw new UnauthorizedException("You do not have permission to view this employee.");
    }

    /**
     * Updates the caller's own contact details. No employee management permission is needed because
     * the record is taken from the access token.
     */
    @PutMapping("/employees/me/contact")
    public void updateOwnContactInfo(@RequestBody ContactInfoUpdateRequest request) {
        int employeeId = currentUserProvider.getCurrentPrincipal().employeeId();
        employeeService.updateOwnContactInfo(employeeId, request.phoneNumber(), request.address());
    }

    /**
     * Uploads the caller's profile photo as raw image bytes. The extension is checked against an
     * allowlist because it becomes part of the stored filename.
     */
    @PostMapping(value = "/employees/me/photo", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void uploadOwnPhoto(@RequestParam String extension, @RequestBody byte[] content) {
        int employeeId = currentUserProvider.getCurrentPrincipal().employeeId();
        profilePhotoService.storePhoto(employeeId, content, extension);
    }

    /**
     * Returns an employee's profile photo, using the same visibility rules as the profile.
     */
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
        permissionService.requireAnyPermission(PAYROLL_DETAIL_READERS);
        return employeeService.getGovernmentDetails(employeeId);
    }

    @GetMapping("/departments")
    public List<Department> listDepartments() {
        permissionService.requireAnyPermission(DIRECTORY_READERS);
        return employeeService.getAllDepartments();
    }

    @GetMapping("/positions")
    public List<Position> listPositions(@RequestParam(required = false) Integer departmentId) {
        permissionService.requireAnyPermission(DIRECTORY_READERS);

        return departmentId != null
                ? employeeService.getPositionsByDepartment(departmentId)
                : employeeService.getAllPositions();
    }

    /**
     * Creates an employee along with their initial salary, government IDs, benefits, and a login
     * account with a temporary password.
     */
    @PostMapping("/employees")
    public EmployeeCreateResponse createEmployee(@RequestBody EmployeeCreateRequest request) {
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        return employeeService.createEmployeeWithSalaryAndBenefits(request, currentUserProvider.getCurrentUserAccountId());
    }

    @PutMapping("/employees/{employeeId}")
    public EmployeeProfileDTO updateEmployee(@PathVariable int employeeId, @RequestBody EmployeeUpdateRequest request) {
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        return employeeService.updateEmployee(employeeId, request, currentUserProvider.getCurrentUserAccountId());
    }

    /**
     * Deactivates an employee. Callers cannot deactivate their own record.
     */
    @DeleteMapping("/employees/{employeeId}")
    public void deactivateEmployee(@PathVariable int employeeId) {
        permissionService.requirePermission(PermissionKey.MANAGE_EMPLOYEE);
        AuthenticatedPrincipal principal = currentUserProvider.getCurrentPrincipal();
        employeeService.deactivateEmployee(employeeId, principal.employeeId(), principal.userAccountId());
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
