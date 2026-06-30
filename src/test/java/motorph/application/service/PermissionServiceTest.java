package motorph.application.service;


import motorph.application.dto.permission.MenuPermissionDTO;
import motorph.application.exception.UnauthorizedException;
import motorph.application.exception.ValidationException;
import motorph.application.session.UserSession;
import motorph.domain.model.UserAccount;
import motorph.domain.value.PermissionKey;
import motorph.domain.value.Role;
import motorph.infrastructure.repository.PermissionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;


import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests permission service using real database dependencies.
 */
@DisplayName("PermissionService Database Tests")
class PermissionServiceTest {


    private static final Logger logger = Logger.getLogger(PermissionServiceTest.class.getName());


    // clears user session after each test
    @AfterEach
    void clearSession() {
        UserSession.getInstance().clear();
    }


    // positive: employee can view own payslip
    @Test
    @DisplayName("Positive - Employee should have view payslip permission")
    void employeeShouldHaveViewPayslipPermission() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.EMPLOYEE);


        logger.info("Testing employee VIEW_PAYSLIP permission.");


        assertTrue(permissionService.hasPermission(PermissionKey.VIEW_PAYSLIP));
    }


    // negative: employee cannot manage users
    @Test
    @DisplayName("Negative - Employee should not have manage users permission")
    void employeeShouldNotHaveManageUsersPermission() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.EMPLOYEE);


        logger.info("Testing employee MANAGE_USERS restriction.");


        assertFalse(permissionService.hasPermission(PermissionKey.MANAGE_USERS));
    }


    // positive: IT can manage users
    @Test
    @DisplayName("Positive - IT should have manage users permission")
    void itShouldHaveManageUsersPermission() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.IT);


        logger.info("Testing IT MANAGE_USERS permission.");


        assertTrue(permissionService.hasPermission(PermissionKey.MANAGE_USERS));
    }


    // negative: IT cannot process payroll
    @Test
    @DisplayName("Negative - IT should not have process payroll permission")
    void itShouldNotHaveProcessPayrollPermission() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.IT);


        logger.info("Testing IT PROCESS_PAYROLL restriction.");


        assertFalse(permissionService.hasPermission(PermissionKey.PROCESS_PAYROLL));
    }


    // positive: admin can manage roles
    @Test
    @DisplayName("Positive - Admin should have manage roles permission")
    void adminShouldHaveManageRolesPermission() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.ADMIN);


        logger.info("Testing admin MANAGE_ROLES permission.");


        assertTrue(permissionService.hasPermission(PermissionKey.MANAGE_ROLES));
    }


    // positive: finance can process payroll
    @Test
    @DisplayName("Positive - Finance should have process payroll permission")
    void financeShouldHaveProcessPayrollPermission() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.FINANCE);


        logger.info("Testing finance PROCESS_PAYROLL permission.");


        assertTrue(permissionService.hasPermission(PermissionKey.PROCESS_PAYROLL));
    }


    // negative: HR cannot manage role permissions
    @Test
    @DisplayName("Negative - HR should not have manage roles permission")
    void hrShouldNotHaveManageRolesPermission() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.HR);


        logger.info("Testing HR MANAGE_ROLES restriction.");


        assertFalse(permissionService.hasPermission(PermissionKey.MANAGE_ROLES));
    }


    // positive: gets active role permission keys
    @Test
    @DisplayName("Positive - getActiveRolePermissionKeys should return permissions")
    void getActiveRolePermissionKeysShouldReturnPermissions() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.ADMIN);


        logger.info("Testing retrieval of active role permission keys.");


        Set<String> permissionKeys = permissionService.getActiveRolePermissionKeys();


        assertNotNull(permissionKeys);
        assertFalse(permissionKeys.isEmpty());
        assertTrue(permissionKeys.contains(PermissionKey.VIEW_DASHBOARD.name()));
    }


    // positive: gets menu permissions for employee
    @Test
    @DisplayName("Positive - getMenuPermissions should return employee menu visibility")
    void getMenuPermissionsShouldReturnEmployeeMenuVisibility() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.EMPLOYEE);


        logger.info("Testing employee menu permission visibility.");


        MenuPermissionDTO menuPermissions = permissionService.getMenuPermissions();


        assertNotNull(menuPermissions);
        assertTrue(menuPermissions.isDashboardVisible());
        assertTrue(menuPermissions.isPayrollVisible());
        assertFalse(menuPermissions.isUserManagementVisible());
        assertFalse(menuPermissions.isRoleManagementVisible());
    }


    // edge case: rejects permission check without login
    @Test
    @DisplayName("Edge Case - hasPermission should reject when user is not logged in")
    void hasPermissionShouldRejectWhenUserIsNotLoggedIn() {
        PermissionService permissionService = createPermissionService();


        logger.info("Testing permission check without active login session.");


        assertThrows(
                UnauthorizedException.class,
                () -> permissionService.hasPermission(PermissionKey.VIEW_DASHBOARD)
        );
    }


    // negative: rejects null permission key
    @Test
    @DisplayName("Negative - hasPermission should reject null permission key")
    void hasPermissionShouldRejectNullPermissionKey() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.ADMIN);


        logger.info("Testing null permission key validation.");


        assertThrows(
                ValidationException.class,
                () -> permissionService.hasPermission((PermissionKey) null)
        );
    }


    // role-based access: an employee cannot edit other employees
    @Test
    @DisplayName("RBAC - Employee cannot manage (edit) other employees")
    void employeeCannotEditOthers() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.EMPLOYEE);

        logger.info("Testing employee cannot edit other employees.");

        assertFalse(permissionService.hasPermission(PermissionKey.MANAGE_EMPLOYEE));
    }


    // role-based access: finance can generate reports
    @Test
    @DisplayName("RBAC - Finance can generate reports")
    void financeCanGenerateReports() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.FINANCE);

        logger.info("Testing finance can generate reports.");

        assertTrue(permissionService.hasPermission(PermissionKey.GENERATE_REPORTS));
    }


    // role-based access: an employee can view own payslip but not other employee records
    @Test
    @DisplayName("RBAC - Employee can view own record only")
    void employeeCanOnlyViewOwnRecord() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.EMPLOYEE);

        logger.info("Testing employee can only view own record.");

        assertTrue(permissionService.hasPermission(PermissionKey.VIEW_PAYSLIP));
        assertFalse(permissionService.hasPermission(PermissionKey.VIEW_EMPLOYEE));
    }


    // role-based access: an unauthorized action is denied
    @Test
    @DisplayName("RBAC - Access denied for unauthorized action")
    void accessDeniedForUnauthorizedAction() {
        PermissionService permissionService = createPermissionService();
        loginAs(Role.EMPLOYEE);

        logger.info("Testing access denied for unauthorized action.");

        assertFalse(permissionService.hasPermission(PermissionKey.PROCESS_PAYROLL));
    }


    // creates permission service with real dependency
    private PermissionService createPermissionService() {
        return new PermissionService(new PermissionRepository());
    }


    // stores a fake logged-in user with selected role
    private void loginAs(Role role) {
        UserAccount account = new UserAccount(
                1,
                10001,
                "test@motorph.com",
                "testuser",
                "sampleHash",
                "sampleSalt",
                List.of(role),
                role,
                false,
                true,
                LocalDateTime.now(),
                false,
                null
        );


        UserSession.getInstance().login(account);
    }
}

