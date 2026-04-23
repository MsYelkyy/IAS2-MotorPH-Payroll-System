package motorph.application.dto.login;

import motorph.domain.value.Role;

/**
 * Carries the authenticated user details returned after a login attempt.
 */
public class LoginResultDTO {

    private final int userAccountId;
    private final int employeeId;
    private final String companyEmail;
    private final Role role;
    private final boolean authenticated;
    private final String message;

    // constructors
    public LoginResultDTO(int userAccountId,
                          int employeeId,
                          String companyEmail,
                          Role role,
                          boolean authenticated,
                          String message) {
        this.userAccountId = userAccountId;
        this.employeeId = employeeId;
        this.companyEmail = companyEmail;
        this.role = role;
        this.authenticated = authenticated;
        this.message = message;
    }

    // getters
    public int getUserAccountId() {
        return userAccountId;
    }

    // getters
    public int getEmployeeId() {
        return employeeId;
    }

    // getters
    public String getCompanyEmail() {
        return companyEmail;
    }

    // getters
    public Role getRole() {
        return role;
    }

    // getters
    public boolean isAuthenticated() {
        return authenticated;
    }

    // getters
    public String getMessage() {
        return message;
    }
}