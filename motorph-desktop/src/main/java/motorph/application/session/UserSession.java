package motorph.application.session;

import motorph.application.dto.login.LoginResultDTO;
import motorph.domain.value.Role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the logged-in user and selected active role.
 */
public class UserSession {

    private static UserSession instance;

    private Integer userAccountId;
    private int employeeId;
    private String companyEmail;
    private List<Role> roles = new ArrayList<>();
    private Role activeRole;
    private boolean loggedIn;

    // constructors
    private UserSession() {
    }

    // returns the shared user session
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }

        return instance;
    }

    // stores the logged-in user from a login result
    public void login(LoginResultDTO result) {
        if (result == null || !result.isAuthenticated()) {
            throw new IllegalArgumentException("Authenticated login result is required.");
        }

        this.userAccountId = result.getUserAccountId();
        this.employeeId = result.getEmployeeId();
        this.companyEmail = result.getCompanyEmail();
        this.roles = new ArrayList<>(result.getRoles());
        this.activeRole = result.getRole();
        this.loggedIn = true;
    }

    // getters
    public Integer getUserAccountId() {
        return userAccountId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public Role getActiveRole() {
        return activeRole;
    }

    public List<Role> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    // checks if the logged-in user has the given role
    public boolean hasRole(Role role) {
        if (role == null) {
            return false;
        }

        return roles.contains(role);
    }

    // checks if the logged-in user has multiple roles
    public boolean hasMultipleRoles() {
        return roles.size() > 1;
    }

    // checks if the logged-in user is using the given role
    public boolean isUsingRole(Role role) {
        if (role == null) {
            return false;
        }

        return activeRole == role;
    }

    // switches the active role of the logged-in user
    public void switchRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        if (!roles.contains(role)) {
            throw new IllegalArgumentException("Cannot switch to a role that is not assigned to this account.");
        }

        this.activeRole = role;
    }

    // clears the current user session
    public void clear() {
        this.userAccountId = null;
        this.employeeId = 0;
        this.companyEmail = null;
        this.roles.clear();
        this.activeRole = null;
        this.loggedIn = false;
    }
}