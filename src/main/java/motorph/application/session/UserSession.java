package motorph.application.session;

import motorph.application.dto.login.LoginResultDTO;
import motorph.domain.model.UserAccount;
import motorph.domain.value.Role;

/**
 * Stores the logged-in user and selected active role.
 */
public class UserSession {

    private static UserSession instance;

    private UserAccount currentUser;
    private Integer userAccountId;
    private int employeeId;
    private String companyEmail;
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

    // stores the logged-in user from a user account
    public void login(UserAccount currentUser) {
        setCurrentUser(currentUser);

        this.userAccountId = currentUser.getUserAccountId();
        this.employeeId = currentUser.getEmployeeId();
        this.companyEmail = currentUser.getCompanyEmail();
        this.activeRole = currentUser.getActiveRole();
        this.loggedIn = true;
    }

    // stores the logged-in user from a login result
    public void login(LoginResultDTO result) {
        if (result == null || !result.isAuthenticated()) {
            throw new IllegalArgumentException("Authenticated login result is required.");
        }

        this.currentUser = null;
        this.userAccountId = result.getUserAccountId();
        this.employeeId = result.getEmployeeId();
        this.companyEmail = result.getCompanyEmail();
        this.activeRole = result.getRole();
        this.loggedIn = true;
    }

    // getters
    public UserAccount getCurrentUser() {
        return currentUser;
    }

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

    public boolean isLoggedIn() {
        return loggedIn;
    }

    // checks if the logged-in user has the given role
    public boolean hasRole(Role role) {
        if (role == null) {
            return false;
        }

        if (currentUser != null) {
            return currentUser.hasRole(role);
        }

        return activeRole == role;
    }

    // checks if the logged-in user has multiple roles
    public boolean hasMultipleRoles() {
        return currentUser != null && currentUser.hasMultipleRoles();
    }

    // checks if the logged-in user is using the given role
    public boolean isUsingRole(Role role) {
        if (role == null) {
            return false;
        }

        if (currentUser != null) {
            return currentUser.isUsingRole(role);
        }

        return activeRole == role;
    }

    // switches the active role of the logged-in user
    public void switchRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }

        if (currentUser != null) {
            currentUser.switchRole(role);
            this.activeRole = currentUser.getActiveRole();
            return;
        }

        if (activeRole != role) {
            throw new IllegalArgumentException("Cannot switch role because only one role is available.");
        }
    }

    // sets the current user account
    public void setCurrentUser(UserAccount currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user is required.");
        }

        if (!currentUser.canLogin()) {
            throw new IllegalArgumentException("Current user is not allowed to log in.");
        }

        if (currentUser.getRoles() == null || currentUser.getRoles().isEmpty()) {
            throw new IllegalArgumentException("Current user must have at least one role.");
        }

        if (currentUser.getActiveRole() == null) {
            throw new IllegalArgumentException("Current user must have an active role.");
        }

        this.currentUser = currentUser;
    }

    // clears the current user session
    public void clear() {
        this.currentUser = null;
        this.userAccountId = null;
        this.employeeId = 0;
        this.companyEmail = null;
        this.activeRole = null;
        this.loggedIn = false;
    }
}