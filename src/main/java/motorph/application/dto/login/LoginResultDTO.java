package motorph.application.dto.login;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import motorph.domain.value.Role;

/**
 * Carries the authentication result returned during login.
 */
public class LoginResultDTO {

    private final int userAccountId;
    private final int employeeId;
    private final String companyEmail;
    private final List<Role> roles;
    private final Role activeRole;
    private final boolean authenticated;
    private final boolean otpRequired;
    private final boolean multipleRoles;
    private final String message;

    // constructors
    public LoginResultDTO(int userAccountId,
                          int employeeId,
                          String companyEmail,
                          Role role,
                          boolean authenticated,
                          boolean otpRequired,
                          String message) {
        this(
                userAccountId,
                employeeId,
                companyEmail,
                List.of(role),
                role,
                authenticated,
                otpRequired,
                false,
                message
        );
    }

    // constructors
    public LoginResultDTO(int userAccountId,
                          int employeeId,
                          String companyEmail,
                          List<Role> roles,
                          Role activeRole,
                          boolean authenticated,
                          boolean otpRequired,
                          boolean multipleRoles,
                          String message) {

        this.userAccountId = userAccountId;
        this.employeeId = employeeId;
        this.companyEmail = requireNonBlank(companyEmail, "Company email is required.");
        this.roles = cleanRoles(roles);
        this.activeRole = validateActiveRole(activeRole, this.roles);
        this.authenticated = authenticated;
        this.otpRequired = otpRequired;
        this.multipleRoles = multipleRoles;
        this.message = message == null ? "" : message.trim();
    }

    // getters
    public int getUserAccountId() {
        return userAccountId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public List<Role> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    public Role getActiveRole() {
        return activeRole;
    }

    public Role getRole() {
        return activeRole;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean isOtpRequired() {
        return otpRequired;
    }

    public boolean hasMultipleRoles() {
        return multipleRoles;
    }

    public String getMessage() {
        return message;
    }

    // checks if the login result has the given role
    public boolean hasRole(Role role) {
        return role != null && roles.contains(role);
    }

    // validates and cleans assigned roles
    private List<Role> cleanRoles(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required.");
        }

        List<Role> cleanRoles = new ArrayList<>();

        for (Role role : roles) {
            if (role == null) {
                throw new IllegalArgumentException("Role cannot be null.");
            }

            if (!cleanRoles.contains(role)) {
                cleanRoles.add(role);
            }
        }

        return cleanRoles;
    }

    // validates active role
    private Role validateActiveRole(Role activeRole, List<Role> roles) {
        if (activeRole == null) {
            throw new IllegalArgumentException("Active role is required.");
        }

        if (!roles.contains(activeRole)) {
            throw new IllegalArgumentException("Active role must be included in assigned roles.");
        }

        return activeRole;
    }

    // validates required text
    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}