package motorph.application.dto.login;

/**
 * Carries the company email and password entered during login.
 */
public class LoginRequestDTO {

    private final String companyEmail;
    private final String password;

    // constructors
    public LoginRequestDTO(String companyEmail, String password) {
        this.companyEmail = companyEmail;
        this.password = password;
    }

    // getters
    public String getCompanyEmail() {
        return companyEmail;
    }

    public String getPassword() {
        return password;
    }
}