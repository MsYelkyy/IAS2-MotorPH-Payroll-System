package motorph.application.dto.login;

import motorph.domain.value.Role;

/**
 * Carries the authentication result returned during login.
 */
public class LoginResultDTO {

    private final int userAccountId;
    private final int employeeId;
    private final String companyEmail;
    private final Role role;
    private final boolean authenticated;
    private final boolean otpRequired;
    private final String message;

    // constructors
    public LoginResultDTO(int userAccountId,
                          int employeeId,
                          String companyEmail,
                          Role role,
                          boolean authenticated,
                          boolean otpRequired,
                          String message) {
        this.userAccountId = userAccountId;
        this.employeeId = employeeId;
        this.companyEmail = companyEmail;
        this.role = role;
        this.authenticated = authenticated;
        this.otpRequired = otpRequired;
        this.message = message;
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

    public Role getRole() {
        return role;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean isOtpRequired() {
        return otpRequired;
    }

    public String getMessage() {
        return message;
    }
}