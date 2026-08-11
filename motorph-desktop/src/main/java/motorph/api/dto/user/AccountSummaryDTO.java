package motorph.api.dto.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Wire copy of the backend's display-only account summary. */
public record AccountSummaryDTO(
        int userAccountId,
        int employeeId,
        String fullName,
        String companyEmail
) {

    @JsonCreator
    public AccountSummaryDTO(@JsonProperty("userAccountId") int userAccountId,
                             @JsonProperty("employeeId") int employeeId,
                             @JsonProperty("fullName") String fullName,
                             @JsonProperty("companyEmail") String companyEmail) {
        this.userAccountId = userAccountId;
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.companyEmail = companyEmail;
    }
}
